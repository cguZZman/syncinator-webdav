package com.syncinator.webdav.server;

import javax.jcr.Repository;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;

public class SyncinatorSimpleWebdavServlet extends SimpleWebdavServlet {
	
	private static final long serialVersionUID = -7815464174102606881L;

	
	public SyncinatorSimpleWebdavServlet() {
		setDavSessionProvider(new DavSessionProvider(){
			@Override
			public boolean attachSession(WebdavRequest request) throws DavException {
				return true;
			}
			@Override
			public void releaseSession(WebdavRequest request) {
				
			}
		});
		setResourceFactory(new SyncinatorDavResourceFactory());
	}


	@Override
	public Repository getRepository() {
		return null;
	}

}
