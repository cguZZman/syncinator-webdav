package com.syncinator.webdav.server;

import java.util.Enumeration;

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
		if (!request.getMethod().equals(DavMethods.METHOD_PROPFIND) && !request.getMethod().equals(DavMethods.METHOD_GET)){
			System.out.println(">> "+request.getMethod()+": " + locator.getResourcePath() + ", deep: " + request.getDepth());
//			Enumeration<String> names = request.getHeaderNames();
//			while (names.hasMoreElements()){
//				String name = names.nextElement();
//				log.info(">> "+ name + ": " + request.getHeader(name));
//			}
		}
		
		String workspace = locator.getWorkspacePath();
		if (workspace == null) {
			throw new DavException(HttpServletResponse.SC_FORBIDDEN);
		} else {
			if (workspace.equals("/onedrive")){
				SyncinatorDavResource resource = resourceCache.get(locator.getResourcePath());
				if (resource == null){
					//System.out.println("<<<"+locator.getResourcePath()+">>>");
					resource = new OneDriveDavResource(locator, config, request, response);
					resourceCache.putIfAbsent(locator.getResourcePath(), resource);
				}
				resource.request = request;
				resource.response = response;
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
