package com.syncinator.webdav.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavCompliance;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
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

public class SyncinatorDavResource implements DavResource {

	private DavResourceFactory factory;
	private DavResourceLocator locator;
	private DavSession session;
	private long modificationTime = IOUtil.UNDEFINED_TIME;
	protected DavPropertySet properties = new DavPropertySet();
	protected boolean propsInitialized = false;
    private boolean isCollection = true;
    private ResourceConfig config;
    
	public static final String METHODS = DavResource.METHODS + ", " + BindConstants.METHODS;
    public static final String COMPLIANCE_CLASSES = DavCompliance.concatComplianceClasses(
        new String[] {
            DavCompliance._1_,
            DavCompliance._2_,
            DavCompliance._3_,
            DavCompliance.BIND
        }
    );
    
	public SyncinatorDavResource(DavResourceLocator locator, DavSession session, DavResourceFactory factory) {
		this.locator = locator;
		this.session = session;
		this.factory = factory;
	}

	protected void initProperties() {
        if (!exists() || propsInitialized) {
            return;
        }

//        try {
//            config.getPropertyManager().exportProperties(getPropertyExportContext(), isCollection());
//        } catch (RepositoryException e) {
//            log.warn("Error while accessing resource properties", e);
//        }

        // set (or reset) fundamental properties
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

//        if (rfc4122Uri != null) {
//            properties.add(new HrefProperty(BindConstants.RESOURCEID, rfc4122Uri, true));
//        }
//
//        Set<ParentElement> parentElements = getParentElements();
//        if (!parentElements.isEmpty()) {
//            properties.add(new ParentSet(parentElements));
//        }

        /* set current lock information. If no lock is set to this resource,
        an empty lock discovery will be returned in the response. */
        properties.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));

        /* lock support information: all locks are lockable. */
        SupportedLock supportedLock = new SupportedLock();
        supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
        properties.add(supportedLock);

        propsInitialized = true;
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
	public boolean exists() {
		return true;
	}

	@Override
	public boolean isCollection() {
		return locator.getResourcePath() == null || locator.getResourcePath().length() == 1;
	}

	@Override
	public String getDisplayName() {
		String resPath = getResourcePath();
        return (resPath != null) ? Text.getName(resPath) : resPath;
	}

	@Override
	public DavResourceLocator getLocator() {
		return locator;
	}

	@Override
	public String getResourcePath() {
		return locator.getResourcePath();
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
	public void spool(OutputContext outputContext) throws IOException {
		// TODO Auto-generated method stub

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
	public DavResourceIterator getMembers() {
		ArrayList<DavResource> list = new ArrayList<DavResource>();
		String path = locator.getResourcePath();
		if (path == null) path = "";
		String workspace = locator.getWorkspacePath();
		if (workspace == null) workspace = "";
		DavResourceLocator resourceLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), workspace, path + "/ingon.doc");
		list.add(new SyncinatorDavResource(resourceLocator, session, factory));
		return new DavResourceIteratorImpl(list);
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
		return factory;
	}

	@Override
	public DavSession getSession() {
		return session;
	}

}
