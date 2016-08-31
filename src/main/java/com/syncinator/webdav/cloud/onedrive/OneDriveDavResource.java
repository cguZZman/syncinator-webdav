package com.syncinator.webdav.cloud.onedrive;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;

import com.onedrive.api.OneDrive;
import com.onedrive.api.request.ItemRequest;
import com.onedrive.api.resource.Item;
import com.onedrive.api.resource.support.ItemCollection;
import com.syncinator.webdav.server.SyncinatorDavResource;

public class OneDriveDavResource extends SyncinatorDavResource {

	private OneDrive onedrive;
	private ItemRequest itemRequest;
    private Item item;
	private boolean retrieved;
    
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
	}
	
	@Override
	protected void fetch() {
		if (!retrieved){
			retrieved = true;
			if (item == null) {
				try {
					item = itemRequest.fetch();
				} catch (Exception e){
					log.error(e.getMessage());
				}
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
	}
	
	@Override
	public boolean exists() {
		initProperties();
		return item != null;
	}
	
	@Override
	public void spool(OutputContext outputContext) throws IOException {
		itemRequest.content().download(outputContext.getOutputStream());
		log.info("Done.");
	}
	
	@Override
	public boolean isCollection() {
		return isRoot() || (item != null && item.getFolder() != null); 
	}
	
	@Override
	public DavResourceIterator getMembers() {
		ArrayList<DavResource> list = new ArrayList<DavResource>();
		String path = getLocator().getResourcePath();
		String workspace = getLocator().getWorkspacePath();
		ItemCollection collection = null;
		try {
			collection = itemRequest.children().fetch();
		} catch(Exception e){
			log.error(e.getMessage());
		}
		if (collection != null && collection.getValue() != null){
			for (Item item : collection.getValue()){
				try {
					DavResourceLocator resourceLocator = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), workspace, path + "/" + item.getName());
					list.add(new OneDriveDavResource(resourceLocator, item, config, request, response));
				} catch (DavException e) {
					e.printStackTrace();
				}	
			}
		}
		return new DavResourceIteratorImpl(list);
	}
}
