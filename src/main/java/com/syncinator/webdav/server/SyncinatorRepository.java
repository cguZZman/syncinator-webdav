package com.syncinator.webdav.server;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

public class SyncinatorRepository implements Repository{

	@Override
	public String[] getDescriptorKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isStandardDescriptor(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSingleValueDescriptor(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Value getDescriptorValue(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value[] getDescriptorValues(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescriptor(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException, RepositoryException {
		return new SyncinatorSession();
	}

	@Override
	public Session login(Credentials credentials) throws LoginException, RepositoryException {
		return login(credentials, null) ;
	}

	@Override
	public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
		return login(null, workspaceName) ;
	}

	@Override
	public Session login() throws LoginException, RepositoryException {
		return login(null, null) ;
	}

}
