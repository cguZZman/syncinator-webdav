package com.syncinator.webdav.cloud.onedrive;

import com.onedrive.api.OneDrive;
import com.onedrive.api.support.AccessToken;
import com.onedrive.api.support.AccessTokenListener;

public class InMemoryAccessTokenListener implements AccessTokenListener {
	private SyncinatorAccessToken accessToken;
	
	public void onAccessTokenReceived(OneDrive reference, AccessToken accessToken) {
		this.accessToken = new SyncinatorAccessToken(accessToken);
	}

	public AccessToken onAccessTokenRequired(OneDrive reference) {
		return accessToken;
	}
}
