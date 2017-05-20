package com.syncinator.webdav.server;

import java.io.IOException;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;

public class SyncinatorWebdavServlet extends SimpleWebdavServlet {
	
	private static final long serialVersionUID = -7815464174102606881L;

	private DavResourceFactory resourceFactory;
	
	public SyncinatorWebdavServlet() {
		setDavSessionProvider(new DavSessionProvider(){
			@Override
			public boolean attachSession(WebdavRequest request) throws DavException { return true; }
			@Override
			public void releaseSession(WebdavRequest request) { }
		});
	}

	@Override
    public DavResourceFactory getResourceFactory() {
        if (resourceFactory == null) {
            resourceFactory = new SyncinatorDavResourceFactory(getResourceConfig());
        }
        return resourceFactory;
    }
	@Override
    protected boolean isPreconditionValid(WebdavRequest request, DavResource resource) {
        return request.matchesIfHeader(resource);
    }
	
	
	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		super.service(new SyncinatorRequest((HttpServletRequest) req), res);
	}

	@Override
	public Repository getRepository() {
		return null;
	}

}
