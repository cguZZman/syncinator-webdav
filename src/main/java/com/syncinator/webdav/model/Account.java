package com.syncinator.webdav.model;

public class Account {
	private String provider;
	private String owner;
	private String driveId;
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getDriveId() {
		return driveId;
	}
	public void setDriveId(String driveId) {
		this.driveId = driveId;
	}
}
