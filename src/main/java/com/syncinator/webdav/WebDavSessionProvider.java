package com.syncinator.webdav;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;

public class WebDavSessionProvider implements DavSessionProvider {

    public boolean attachSession(WebdavRequest request) throws DavException {
        return true;
    }

    public void releaseSession(WebdavRequest request) {
    }
}
}
