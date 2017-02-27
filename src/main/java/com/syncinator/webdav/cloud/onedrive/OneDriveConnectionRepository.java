package com.syncinator.webdav.cloud.onedrive;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.onedrive.api.OneDrive;
import com.onedrive.api.resource.Drive;
import com.onedrive.api.support.ClientCredential;
import com.onedrive.api.support.Scope;

public class OneDriveConnectionRepository {
	private static Map<String, OneDrive> repository = new HashMap<String, OneDrive>();
	
	public static OneDrive getConnection(String id){
		return repository.get(id);
	}

	public static void addConnection(String id, OneDrive connection){
		repository.put(id, connection);
	}
	
	public static String addConnectionWithAuthCode(String authCode) {
		OneDrive oneDrive = getOneDriveInstance();
		oneDrive.setAuthorizationCode(authCode);
		oneDrive.setAccessTokenListener(new InMemoryAccessTokenListener());
		Drive drive = oneDrive.drive().fetch();
		String id = drive.getOwner().getUser().getId();
		IdSerializatorAccessTokenListener idTokenListener = new IdSerializatorAccessTokenListener(id);
		SyncinatorAccessToken accessToken = (SyncinatorAccessToken) oneDrive.getAccessTokenListener().onAccessTokenRequired(oneDrive);
		accessToken.setDriveId(drive.getId());
		accessToken.setOwner(drive.getOwner().getUser().getDisplayName());
		oneDrive.setExistingToken(accessToken);
		idTokenListener.onAccessTokenReceived(oneDrive, accessToken);
		oneDrive.setAccessTokenListener(idTokenListener);
		addConnection(id, oneDrive);
		return id;
	}
	
	public static void addConnectionFromDisk(String id) {
		IdSerializatorAccessTokenListener tokenListener = new IdSerializatorAccessTokenListener(id);
		OneDrive oneDrive = getOneDriveInstance();
		oneDrive.setAccessTokenListener(tokenListener);
		oneDrive.setExistingToken(tokenListener.onAccessTokenRequired(oneDrive));
		addConnection(id, oneDrive);
	}
	
	public static OneDrive removeConnection(String id){
		return repository.remove(id);
	}
	
	private static OneDrive getOneDriveInstance() {
		return new OneDrive(new ClientCredential("0000000048145120"),
				Arrays.asList(Scope.OFFLINE_ACCESS, "wl.skydrive", "wl.signin", "onedrive.readwrite"),
				OneDrive.MOBILE_REDIRECT_URI);
	}
}
