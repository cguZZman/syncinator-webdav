package com.syncinator.webdav.server;

import java.io.IOException;
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
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.DefaultActiveLock;
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
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syncinator.webdav.SyncinatorCacheManager;

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
	
	public static final DavPropertyName ISFOLDER = DavPropertyName.create("isFolder");
	public static final DavPropertyName ISHIDDEN = DavPropertyName.create("ishidden");
	
	public static final String METHODS = DavResource.METHODS + ", " + BindConstants.METHODS;
    public static final String COMPLIANCE_CLASSES = DavCompliance.concatComplianceClasses(new String[] { DavCompliance._1_, DavCompliance._2_, DavCompliance._3_, DavCompliance.BIND });
    
    private Cache<String, SyncinatorDavResource> resourceCache;
    
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
		resourceCache = SyncinatorCacheManager.getResourceCache();
	}

	protected abstract void fetchResource() throws Exception;
	protected abstract void fetchChildren() throws Exception;
	protected abstract void download(OutputContext context) throws IOException;

	@Override
	public DavResourceIterator getMembers() {
		if (!childrenInitialized) {
			synchronized (children) {
				if (!childrenInitialized) {
					childrenInitialized = true;
					try {
						fetchChildren();
						for (DavResource resource : children){
							SyncinatorDavResource syncinatorResource = (SyncinatorDavResource) resource;
							syncinatorResource.initProperties();
							resourceCache.putIfAbsent(resource.getLocator().getResourcePath(), (SyncinatorDavResource) syncinatorResource);
						}
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
	            properties.add(new DefaultDavProperty<String>(ISFOLDER, "t"));
	            properties.add(new DefaultDavProperty<String>(ISHIDDEN, "0"));
	            
	            // Windows XP support
	            properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "1"));
	            
	        } else {
	            properties.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
	            // Windows XP support
	            properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "0"));
	        }
	
	        /* set current lock information. If no lock is set to this resource,
	        an empty lock discovery will be returned in the response. */
	        //properties.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));
	        properties.add(new LockDiscovery());
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
		if (exists()){
			download(outputContext);
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
        return Text.getName(getResourcePath());
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
		log.info("*** setProperty NOT IMPLEMENTED!");
	}

	@Override
	public void removeProperty(DavPropertyName propertyName) throws DavException {
		log.info("*** removeProperty NOT IMPLEMENTED!");
	}

	@Override
	public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
		log.info("*** alterProperties NOT IMPLEMENTED!");
		return null;
	}

	@Override
	public boolean isLockable(Type type, Scope scope) {
		log.info("*** isLockable NOT IMPLEMENTED!");
		return false;
	}

	@Override
	public boolean hasLock(Type type, Scope scope) {
		log.info("*** hasLock NOT IMPLEMENTED!");
		return false;
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope) {
		log.info("*** getLock NOT IMPLEMENTED!");
		return null;
	}

	@Override
	public ActiveLock[] getLocks() {
		log.info("*** getLocks NOT IMPLEMENTED!");
		return null;
	}

	@Override
	public ActiveLock lock(LockInfo reqLockInfo) throws DavException {
		log.info("*** lock NOT IMPLEMENTED!");
		ActiveLock activeLock = new DefaultActiveLock();
		activeLock.setOwner("carlos");
		return activeLock;
	}

	@Override
	public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException {
		log.info("*** refreshLock NOT IMPLEMENTED!");
		ActiveLock activeLock = new DefaultActiveLock();
		activeLock.setOwner("carlos");
		return activeLock;
	}

	@Override
	public void unlock(String lockToken) throws DavException {
		log.info("*** unlock NOT IMPLEMENTED!");
	}

	@Override
	public void addLockManager(LockManager lockmgr) {
		log.info("*** addLockManager NOT IMPLEMENTED!");
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
