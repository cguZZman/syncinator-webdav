package com.syncinator.webdav.server;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;	

public class SyncinatorRequest extends WebdavRequestImpl {

	public SyncinatorRequest(HttpServletRequest httpRequest) {
		super(httpRequest, null);
	}

	@Override
	public String getHeader(String s) {
		String value = super.getHeader(s);
		if (s.equals(DavConstants.HEADER_DESTINATION) && value != null) {
			try {
				new URI(value);
			} catch (Exception e) {
				try {
					value = URIUtil.encodePathQuery(value);
				} catch (Exception e1) {
					
				}
			}
		}
		return value;
	}

}
