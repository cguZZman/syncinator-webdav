package com.syncinator.webdav.cloud.onedrive;

import java.util.ArrayList;

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;

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
    
	public OneDriveDavResource(DavResourceLocator locator) {
		this(locator, null);
	}
	public OneDriveDavResource(DavResourceLocator locator, Item item) {
		super(locator);
		onedrive = OneDriveConnectionRepository.getConnection(driveId);
		if (isRoot()) {
			itemRequest = onedrive.drive().root();
		} else {
			itemRequest = onedrive.drive().root().itemByPath(resourcePath);
		}
		this.item = item;
	}
	@Override
	protected void fetch() {
		if (!retrieved){
			retrieved = true;
			if (item == null) {
				item = itemRequest.fetch();
			}
		}
	}
	
	@Override
	public boolean isCollection() {
		return isRoot() || item.getFolder() != null; 
	}
	
	@Override
	public DavResourceIterator getMembers() {
		ArrayList<DavResource> list = new ArrayList<DavResource>();
		String path = getLocator().getResourcePath();
		String workspace = getLocator().getWorkspacePath();
		ItemCollection collection = itemRequest.children().fetch();
		if (collection != null && collection.getValue() != null){
			for (Item item : collection.getValue()){
				DavResourceLocator resourceLocator = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), workspace, path + "/" + item.getName());
				list.add(new OneDriveDavResource(resourceLocator, item));	
			}
		}
		return new DavResourceIteratorImpl(list);
	}
}
