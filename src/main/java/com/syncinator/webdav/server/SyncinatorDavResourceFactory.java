package com.syncinator.webdav.server;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syncinator.webdav.cloud.onedrive.OneDriveDavResource;

public class SyncinatorDavResourceFactory implements DavResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(SyncinatorDavResourceFactory.class);
	
	private ResourceConfig config;
	
	
	public SyncinatorDavResourceFactory(ResourceConfig config) {
		this.config = config;
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request,
			DavServletResponse response) throws DavException {
		log.info(">>> Requested: " + locator.getResourcePath().trim()
				+ " [" + ((int)locator.getResourcePath().charAt(locator.getResourcePath().length() - 1))
				+ "] " + request.getMethod() + ", deep: " + request.getDepth()
				+ ", empty props: " + request.getPropFindProperties().isEmpty());
		request.getPropFindProperties().forEach(p -> System.out.println(p));
		return createResource(locator, request);
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		return createResource(locator, (DavServletRequest) null);
	}
	
	private DavResource createResource(DavResourceLocator locator, DavServletRequest request) throws DavException {
		String workspace = locator.getWorkspaceName();
		if (workspace.equals("onedrive")){
			return new OneDriveDavResource(locator, config, request);	
		}
		return null;
	}
}
