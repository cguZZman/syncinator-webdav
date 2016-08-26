package com.syncinator.webdav;

import javax.jcr.Repository;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;

public class WebdavServlet extends SimpleWebdavServlet {
	private static final long serialVersionUID = 6534346523809670081L;
	
	private DavLocatorFactory locatorFactory;
	
	@Override
	public DavLocatorFactory getLocatorFactory() {
		if (locatorFactory == null) {
			locatorFactory = new LocatorFactoryImpl(getPathPrefix());
		}
		return locatorFactory;

	}

	@Override
	public Repository getRepository() {
		return null;
	}

}
