package com.syncinator.webdav.server;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syncinator.webdav.cloud.onedrive.OneDriveDavResource;

public class SyncinatorDavResourceFactory implements DavResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(SyncinatorDavResourceFactory.class);
	
	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request,
			DavServletResponse response) throws DavException {
		return createResource(locator, null);
		
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		String workspace = locator.getWorkspaceName();
		log.info(">>> Requested: " + locator.getResourcePath());
		if (workspace.equals("onedrive")){
			return new OneDriveDavResource(locator);	
		}
		return null;
	}
	
}
