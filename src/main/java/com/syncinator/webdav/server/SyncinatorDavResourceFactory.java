package com.syncinator.webdav.server;

import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syncinator.webdav.SyncinatorCacheManager;
import com.syncinator.webdav.cloud.onedrive.OneDriveDavResource;

public class SyncinatorDavResourceFactory implements DavResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(SyncinatorDavResourceFactory.class);
	
	private ResourceConfig config;
	
	private Cache<String, SyncinatorDavResource> resourceCache;
	
	public SyncinatorDavResourceFactory(ResourceConfig config) {
		this.config = config;
		resourceCache = SyncinatorCacheManager.getResourceCache();
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		String method = request.getMethod();
		if (!method.equals(DavMethods.METHOD_PROPFIND) ){ //&& !request.getMethod().equals(DavMethods.METHOD_GET)){
			log.info("+ " + method+": " + locator.getResourcePath() + (method.equals(DavMethods.METHOD_PROPFIND)? ", deep: " + request.getDepth():""));
//			for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();){
//				String name = names.nextElement();
//				if (name.equals("Range")) {
//					log.info("    "+ name + ": " + request.getHeader(name));
//				}
//			}
		}
		
		String workspace = locator.getWorkspacePath();
		if (workspace == null) {
			throw new DavException(HttpServletResponse.SC_FORBIDDEN);
		} else {
			if (workspace.equals("/onedrive")){
				SyncinatorDavResource resource = resourceCache.get(locator.getResourcePath());
				if (resource == null){
					resource = new OneDriveDavResource(locator, config, request, response);
					resourceCache.putIfAbsent(locator.getResourcePath(), resource);
				}
				resource.request = request;
				resource.response = response;
				response.setHeader("Syncinator-Thread-Name", Thread.currentThread().getName());
				return resource; 
				
			} else {
				throw new DavException(HttpServletResponse.SC_NOT_FOUND);
			}
		}
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		log.info("***********************************************************************");
		log.info("* createResource NOT IMPLEMENTED!!                                    *");
		log.info("***********************************************************************");
		return null;
	}
	
}
