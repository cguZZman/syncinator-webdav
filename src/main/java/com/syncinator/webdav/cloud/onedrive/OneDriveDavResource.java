package com.syncinator.webdav.cloud.onedrive;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.ehcache.Cache;

import com.onedrive.api.OneDrive;
import com.onedrive.api.request.ItemRequest;
import com.onedrive.api.resource.Item;
import com.onedrive.api.resource.support.ItemCollection;
import com.syncinator.webdav.SyncinatorCacheManager;
import com.syncinator.webdav.server.SyncinatorDavResource;

public class OneDriveDavResource extends SyncinatorDavResource {

	private OneDrive onedrive;
	private ItemRequest itemRequest;
    private Item item;
	private Cache<String, SyncinatorDavResource> resourceCache;
	
	public OneDriveDavResource(DavResourceLocator locator, ResourceConfig config, DavServletRequest request, DavServletResponse response) throws DavException {
		this(locator, null, config, request, response);
	}
	public OneDriveDavResource(DavResourceLocator locator, Item item, ResourceConfig config, DavServletRequest request, DavServletResponse response) throws DavException {
		super(locator, config, request, response);
		this.item = item;
		onedrive = OneDriveConnectionRepository.getConnection(localAccountId);
		if (onedrive != null){
			if (isRoot()) {
				itemRequest = onedrive.drive().root();
			} else {
				itemRequest = onedrive.drive().root().itemByPath(resourcePath);
			}
		} else {
			throw new DavException(HttpServletResponse.SC_NOT_FOUND);
		}
		resourceCache = SyncinatorCacheManager.getResourceCache();
	}
	
	@Override
	protected void fetchResource() {
		if (item == null) {
			item = itemRequest.fetch();
		}
		if (item != null){
			if (item.getFileSystemInfo() != null){
				modificationTime = item.getFileSystemInfo().getLastModifiedDateTime().getTime();
				creationTime = item.getFileSystemInfo().getCreatedDateTime().getTime();
			}
			if (item.getFile() != null) {
				contentType = item.getFile().getMimeType();
			} else  if (item.getFolder() != null){
				contentType = "text/directory";
			}
			size = item.getSize();
			eTag = item.geteTag();
		}
	}
	
	@Override
	public void fetchChildren() throws Exception {
		children.clear();
		String path = getLocator().getResourcePath();
		String workspace = getLocator().getWorkspacePath();
		ItemCollection collection = null;
		collection = itemRequest.children().fetch();
		if (collection != null && collection.getValue() != null){
			for (Item item : collection.getValue()){
				try {
					DavResourceLocator resourceLocator = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), workspace, path + "/" + item.getName());
					OneDriveDavResource resource = new OneDriveDavResource(resourceLocator, item, config, request, response);
					children.add(resource);
					resourceCache.putIfAbsent(resourceLocator.getResourcePath(), resource);
				} catch (DavException e) {
					e.printStackTrace();
				}	
			}
		}
	}

	
	@Override
	public boolean exists() {
		initProperties();
		return item != null;
	}
	
	@Override
	public void download(OutputStream os) throws IOException {
		try {
			if (!response.isCommitted()) {
				response.sendRedirect(item.getDownloadUrl());
				//response.setHeader("Location", item.getDownloadUrl());
				//response.sendError(HttpServletResponse.SC_FOUND);
			} else {
				log.info("commited??");
			}
			
		} catch (Exception e){
			log.error("Error downloading: " + e.getMessage());
		}
	}
	
	
	@Override
	public boolean isCollection() {
		return isRoot() || (item != null && item.getFolder() != null); 
	}
	
}
