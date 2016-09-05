package com.syncinator.webdav.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavCompliance;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.bind.BindConstants;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.SupportedLock;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SyncinatorDavResource implements DavResource {

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	public long SIMPLE_UPLOAD_LIMIT_SIZE = 5 * 1024 * 1024;
	
	protected DavPropertySet properties = new DavPropertySet();
	protected boolean propsInitialized = false;
	protected List<DavResource> children = new ArrayList<DavResource>();
	protected boolean childrenInitialized = false;
	
	protected long modificationTime = IOUtil.UNDEFINED_TIME;
	protected long creationTime = IOUtil.UNDEFINED_TIME;
	protected String contentType = "application/octet-stream";
	protected long size;
	protected String eTag;
	
	private DavResourceLocator locator;
	protected String localAccountId;
	protected String resourcePath;
	protected DavServletRequest request;
	protected DavServletResponse response;
	
	protected ResourceConfig config;
	
	public static final String METHODS = DavResource.METHODS + ", " + BindConstants.METHODS;
    public static final String COMPLIANCE_CLASSES = DavCompliance.concatComplianceClasses(
        new String[] {
            DavCompliance._1_,
            DavCompliance._2_,
            DavCompliance._3_,
            DavCompliance.BIND
        }
    );
    
	public SyncinatorDavResource(DavResourceLocator locator, ResourceConfig config, DavServletRequest request, DavServletResponse response) throws DavException {
		this.locator = locator;
		this.config = config;
		this.request = request;
		this.response = response;
		String path = locator.getResourcePath();
		int ipos = locator.getWorkspacePath().length()+1;
		if (path.length() > ipos){
			int rpos = path.indexOf('/', ipos);
			if (rpos == -1) {
				localAccountId = path.substring(ipos);
	        } else {
	        	localAccountId = path.substring(ipos, rpos);
	            resourcePath = path.substring(rpos);
	        }
		}
		if (localAccountId == null){
			throw new DavException(HttpServletResponse.SC_FORBIDDEN);
		}
	}

	protected abstract void fetchResource() throws Exception;
	protected abstract void fetchChildren() throws Exception;
	protected abstract void download(OutputStream os) throws Exception;

	@Override
	public DavResourceIterator getMembers() {
		if (!childrenInitialized) {
			synchronized (children) {
				if (!childrenInitialized) {
					childrenInitialized = true;
					try {
						fetchChildren();
					} catch(Exception e){
						childrenInitialized = false;
						log.error(e.getMessage());
					}
					
				}
			}
		}
		return new DavResourceIteratorImpl(children);
	}
	protected void initProperties() {
		if (propsInitialized) return;
		synchronized (properties) {
			if (propsInitialized) return;
			propsInitialized = true;
			try {
				fetchResource();
			} catch(Exception e){
				propsInitialized = false;
				log.error(e.getMessage() + " - [" + getResourcePath() + "]");
				return;
				//throw new DavException(HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
				//response.sendError(HttpServletResponse.SC_BAD_GATEWAY);
			}
			
			properties.add(new DefaultDavProperty<String>(DavPropertyName.GETLASTMODIFIED, IOUtil.getLastModified(modificationTime)));
			properties.add(new DefaultDavProperty<String>(DavPropertyName.CREATIONDATE, IOUtil.getLastModified(creationTime)));
			properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTTYPE, contentType));
			properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTLENGTH, size + ""));
			if (eTag != null){
				properties.add(new DefaultDavProperty<String>(DavPropertyName.GETETAG, eTag));
			}
			
	        if (getDisplayName() != null) {
	            properties.add(new DefaultDavProperty<String>(DavPropertyName.DISPLAYNAME, getDisplayName()));
	        }
	        if (isCollection()) {
	            properties.add(new ResourceType(ResourceType.COLLECTION));
	            // Windows XP support
	            properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "1"));
	        } else {
	            properties.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
	            // Windows XP support
	            properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "0"));
	        }
	
	        /* set current lock information. If no lock is set to this resource,
	        an empty lock discovery will be returned in the response. */
	        properties.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));
	
	        /* lock support information: all locks are lockable. */
	        SupportedLock supportedLock = new SupportedLock();
	        supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
	        properties.add(supportedLock);
		}
        
    }
	
	@Override
	public void spool(OutputContext outputContext) throws IOException {
		if (isCollection()){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		OutputStream os = outputContext.getOutputStream(); 
		if (os != null){
			try {
				if (exists()){
					download(os);
				}
			} catch (Exception e){
				log.error("Error spooling: " + e.getMessage());
			} 
		}
		
	}
	@Override
	public String getComplianceClass() {
		return COMPLIANCE_CLASSES;
	}

	@Override
	public String getSupportedMethods() {
		return METHODS;
	}

	@Override
	public String getDisplayName() {
		String resPath = getResourcePath();
        return (resPath != null) ? Text.getName(resPath) : "carlos' onedrive";
	}

	@Override
	public DavResourceLocator getLocator() {
		return locator;
	}

	@Override
	public String getResourcePath() {
		return resourcePath;
	}

	@Override
	public String getHref() {
		return locator.getHref(isCollection());
	}

	@Override
	public long getModificationTime() {
		initProperties();
		return modificationTime;
	}

	@Override
	public DavPropertyName[] getPropertyNames() {
		return getProperties().getPropertyNames();
	}

	@Override
	public DavProperty<?> getProperty(DavPropertyName name) {
		initProperties();
		return properties.get(name);
	}

	@Override
	public DavPropertySet getProperties() {
		return properties;
	}

	@Override
	public void setProperty(DavProperty<?> property) throws DavException {
		//alterProperty(property);
	}

	@Override
	public void removeProperty(DavPropertyName propertyName) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLockable(Type type, Scope scope) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasLock(Type type, Scope scope) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ActiveLock[] getLocks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ActiveLock lock(LockInfo reqLockInfo) throws DavException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unlock(String lockToken) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addLockManager(LockManager lockmgr) {
		// TODO Auto-generated method stub

	}

	@Override
	public DavResourceFactory getFactory() {
		return null;
	}

	@Override
	public DavSession getSession() {
		return null;
	}
	
	public boolean isRoot(){
		return resourcePath == null;
	}

}
