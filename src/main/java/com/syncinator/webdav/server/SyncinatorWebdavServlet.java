package com.syncinator.webdav.server;

import javax.jcr.Repository;

import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;

public class SyncinatorWebdavServlet extends JCRWebdavServerServlet {
	
	private static final long serialVersionUID = -7815464174102606881L;
	private Repository repository;
	
	@Override
	protected Repository getRepository() {
		if (repository == null){
			repository = new SyncinatorRepository();
		}
		return repository;
	}
	

}
