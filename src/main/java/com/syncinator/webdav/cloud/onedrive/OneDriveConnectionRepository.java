package com.syncinator.webdav.cloud.onedrive;

import java.util.HashMap;
import java.util.Map;

import com.onedrive.api.OneDrive;

public class OneDriveConnectionRepository {
	private static Map<String, OneDrive> repository = new HashMap<String, OneDrive>();
	
	public static void addConnection(String id, OneDrive connection){
		repository.put(id, connection);
	}
	public static OneDrive getConnection(String id){
		return repository.get(id);
	}
}
