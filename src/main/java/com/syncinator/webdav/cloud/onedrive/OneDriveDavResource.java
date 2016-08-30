package com.syncinator.webdav.cloud.onedrive;

import java.util.ArrayList;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.springframework.http.HttpMethod;

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
    
	public OneDriveDavResource(DavResourceLocator locator, ResourceConfig config, DavServletRequest request) {
		this(locator, null, config, request);
	}
	public OneDriveDavResource(DavResourceLocator locator, Item item, ResourceConfig config, DavServletRequest request) {
		super(locator, config, request);
		this.item = item;
		if (driveId != null){
			onedrive = OneDriveConnectionRepository.getConnection(driveId);
			if (onedrive != null){
				if (isRoot()) {
					itemRequest = onedrive.drive().root();
				} else {
					itemRequest = onedrive.drive().root().itemByPath(resourcePath);
				}
			}
		}
	}
	@Override
	protected void fetch() {
		if (!retrieved){
			retrieved = true;
			if (item == null) {
				item = itemRequest.fetch();
			}
			if (item != null){
				//properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTLANGUAGE, item.getFile().));
				if (item.getFileSystemInfo() != null){
					modificationTime = item.getFileSystemInfo().getLastModifiedDateTime().getTime();	
					properties.add(new DefaultDavProperty<String>(DavPropertyName.GETLASTMODIFIED, IOUtil.getLastModified(modificationTime)));
					properties.add(new DefaultDavProperty<String>(DavPropertyName.CREATIONDATE, IOUtil.getLastModified(item.getFileSystemInfo().getCreatedDateTime().getTime())));
				}
				String contentType = "application/octet-stream";
				if (item.getFile() != null) {
					contentType = item.getFile().getMimeType();
				} else  if (item.getFolder() != null){
					contentType = "text/directory";
				}
				properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTTYPE, contentType));
				properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTLENGTH, item.getSize() + ""));
				properties.add(new DefaultDavProperty<String>(DavPropertyName.GETETAG, item.geteTag()));
			}
		}
	}
	
	@Override
	public boolean exists() {
		if (onedrive == null){
			return false;
		}
		if (request != null && !request.getMethod().equals(HttpMethod.OPTIONS.name()))
			initProperties();
		return item != null;
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
		ItemCollection collection = itemRequest.children().fetch();
		if (collection != null && collection.getValue() != null){
			for (Item item : collection.getValue()){
				DavResourceLocator resourceLocator = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), workspace, path + "/" + item.getName());
				list.add(new OneDriveDavResource(resourceLocator, item, config, request));	
			}
		}
		return new DavResourceIteratorImpl(list);
	}
}
