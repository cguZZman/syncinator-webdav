package com.syncinator.webdav;

import java.net.CookieHandler;
import java.net.CookieManager;

public class SyncinatorCookieManager {
	private static final CookieManager COOKIE_MANAGER = new CookieManager();
	static {
		CookieHandler.setDefault(COOKIE_MANAGER);
	}
	public static CookieManager getCookieManager() {
		return COOKIE_MANAGER;
	}
}
