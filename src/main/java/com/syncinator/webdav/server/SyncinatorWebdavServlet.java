package com.syncinator.webdav.server;

import javax.jcr.Repository;

import org.apache.jackrabbit.webdav.DavException;
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
	public Repository getRepository() {
		return null;
	}

}
