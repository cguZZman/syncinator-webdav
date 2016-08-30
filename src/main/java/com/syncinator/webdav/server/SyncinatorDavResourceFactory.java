package com.syncinator.webdav.server;

import javax.servlet.http.HttpServletResponse;

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
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		String workspace = locator.getWorkspacePath();
		if (workspace == null) {
			throw new DavException(HttpServletResponse.SC_FORBIDDEN);
		} else {
			log.info(">> "+request.getMethod()+": " + locator.getResourcePath() + ", deep: " + request.getDepth());
			if (!request.getPropFindProperties().isEmpty()){
				StringBuffer sb = new StringBuffer(">> Properties requested: ");
				request.getPropFindProperties().forEach(p -> sb.append(p).append(" "));
				log.info(sb.toString());
			}
			if (workspace.equals("onedrive")){
				return new OneDriveDavResource(locator, config, request, response);	
			}
		}
		return null;
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		log.info("***********************************************************************");
		log.info("* createResource NOT IMPLEMENTED!!                                    *");
		log.info("***********************************************************************");
		return null;
	}
	
}
