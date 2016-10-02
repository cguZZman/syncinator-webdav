package com.syncinator.webdav.cloud.onedrive;

import java.io.File;

import com.onedrive.api.OneDrive;
import com.onedrive.api.support.SerializatorAccessTokenListener;

public class IdSerializatorAccessTokenListener extends SerializatorAccessTokenListener {
	private String id;
	
	public IdSerializatorAccessTokenListener(String id) {
		this.id = id;
	}

	public String getFileName(OneDrive reference){
		return id + ACCESS_TOKEN_EXTENSION;
	}
	
	public String getApplicationFolder(){
		return System.getProperty("user.home", EMPTY_STRING) + File.separator 
				+ ".syncinator" + File.separator + "provider" + File.separator + "onedrive";
	}
}
