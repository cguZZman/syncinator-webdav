package com.syncinator.webdav.cloud.onedrive;

import java.util.Arrays;

import com.onedrive.api.OneDrive;
import com.onedrive.api.resource.Item;
import com.onedrive.api.support.ClientCredential;
import com.onedrive.api.support.Scope;
import com.onedrive.api.support.SerializatorAccessTokenListener;

public class IdSerializatorAccessTokenListener extends SerializatorAccessTokenListener {
	private String id;
	
	public static void main(String[] args) {
		OneDrive oneDrive = new OneDrive(new ClientCredential("0000000048145120"),
				Arrays.asList(Scope.OFFLINE_ACCESS, "wl.skydrive", "wl.signin", "onedrive.readwrite"),
				OneDrive.MOBILE_REDIRECT_URI);
		oneDrive.setAuthorizationCode("Md9e15328-b60f-9153-cb95-e8b9579aef18");
		oneDrive.setAccessTokenListener(new IdSerializatorAccessTokenListener("1"));
		Item item = oneDrive.drive().root().fetch();
		System.out.println(item);
	}
	
	public IdSerializatorAccessTokenListener(String id) {
		this.id = id;
	}

	public String getFileName(OneDrive reference){
		return id + ACCESS_TOKEN_EXTENSION;
	}
}
