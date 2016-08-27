package com.syncinator.webdav.server;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;

public class SyncinatorDavResourceFactory implements DavResourceFactory {

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request,
			DavServletResponse response) throws DavException {
		return createResource(locator, null);
		
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		return new SyncinatorDavResource(locator, session, this);
	}

}
