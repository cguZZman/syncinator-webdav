package com.syncinator.webdav.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavCompliance;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
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
	

	protected boolean propsInitialized = false;
	protected DavPropertySet properties = new DavPropertySet();
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

	protected abstract void fetch();
	
	protected void initProperties() {
		if (propsInitialized) return;
		propsInitialized = true;
		fetch();
		
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
        
//      if (rfc4122Uri != null) {
//	      properties.add(new HrefProperty(BindConstants.RESOURCEID, rfc4122Uri, true));
//	  }
//	
//	  Set<ParentElement> parentElements = getParentElements();
//	  if (!parentElements.isEmpty()) {
//	      properties.add(new ParentSet(parentElements));
//	  }
        
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
	public DavResource getCollection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(DavResource destination) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	public void copy(DavResource destination, boolean shallow) throws DavException {
		// TODO Auto-generated method stub

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
