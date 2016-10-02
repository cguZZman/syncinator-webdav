package com.syncinator.webdav.cloud.onedrive;

import com.onedrive.api.support.AccessToken;

public class SyncinatorAccessToken extends AccessToken{

	private static final long serialVersionUID = 1395073980193234216L;
	private String owner;
	private String driveId;
	
	public SyncinatorAccessToken() {
	}
	public SyncinatorAccessToken(AccessToken accessToken) {
		setAccessToken(accessToken.getAccessToken());
		setExpiration(accessToken.getExpiration());
		setRefreshToken(accessToken.getRefreshToken());
		setScope(accessToken.getScope());
		setTokenType(accessToken.getTokenType());
	}
	public String getDriveId() {
		return driveId;
	}
	public void setDriveId(String driveId) {
		this.driveId = driveId;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}

}
