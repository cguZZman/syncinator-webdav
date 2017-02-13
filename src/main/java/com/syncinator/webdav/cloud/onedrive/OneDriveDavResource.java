package com.syncinator.webdav.cloud.onedrive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.springframework.util.StringUtils;

import com.onedrive.api.OneDrive;
import com.onedrive.api.request.ItemRequest;
import com.onedrive.api.resource.Item;
import com.onedrive.api.resource.support.ItemCollection;
import com.onedrive.api.resource.support.ItemReference;
import com.onedrive.api.resource.support.UploadSession;
import com.syncinator.webdav.server.DownloadManager;
import com.syncinator.webdav.server.SyncinatorDavResource;

public class OneDriveDavResource extends SyncinatorDavResource {

	private OneDrive onedrive;
	private ItemRequest itemRequest;
    private Item item;
	
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
	protected void fetchResource() {
		if (item == null) {
			item = itemRequest.fetch();
		}
		if (item != null){
			if (!StringUtils.isEmpty(item.getWebDavUrl())){
				log.info(">> WebDavUrl of " + getResourcePath() + ": " + item.getWebDavUrl());
			}
			if (item.getFileSystemInfo() != null){
				modificationTime = item.getFileSystemInfo().getLastModifiedDateTime().getTime();
				creationTime = item.getFileSystemInfo().getCreatedDateTime().getTime();
			}
			if (item.getFile() != null) {
				contentType = item.getFile().getMimeType();
			} else  if (item.getFolder() != null){
				contentType = "text/directory";
			}
			//log.info(getResourcePath() + ": "+ contentType);
			size = item.getSize();
			eTag = item.geteTag();
		}
	}
	
	@Override
	public void fetchChildren() throws Exception {
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
	public void download(OutputContext context) throws IOException {
//		Map<String,String> headerMap = new HashMap<String,String>();
//		for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();){
//			String header = e.nextElement();
//			headerMap.put(header.toLowerCase(), request.getHeader(header));
//		}
		log.info("File ["+getDisplayName()+"] requested...");
		DownloadManager.download(item.getId(), item.getSize(), item.getDownloadUrl(), request.getHeader("range"), context);
	}
	
	
	@Override
	public boolean isCollection() {
		return isRoot() || (item != null && item.getFolder() != null); 
	}
	
	@Override
	public DavResource getCollection() {
		DavResourceLocator resourceLocator = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), getLocator().getWorkspacePath(), Text.getRelativeParent( getLocator().getResourcePath(), 1));
		OneDriveDavResource parent = null;
		try {
			parent = new OneDriveDavResource(resourceLocator, item, config, request, response);
		} catch (Exception e) {
			log.error("Error getting parent: " + e.getMessage());
		}
		return parent;
	}
	
	@Override
	public void move(DavResource destination) throws DavException {
		itemRequest.move(Text.getRelativeParent(destination.getResourcePath(), 1), 
				Text.getName(destination.getResourcePath()));
	}
	
	@Override
	public void copy(DavResource destination, boolean shallow) throws DavException {
		ItemReference parent = new ItemReference();
		parent.setPath(Text.getRelativeParent(destination.getResourcePath(), 1));
		log.info("parent: " + parent.getPath());
		itemRequest.copy(parent, Text.getName(destination.getResourcePath()));

	}
	
	@Override
	public void removeMember(DavResource member) throws DavException {
		((OneDriveDavResource)member).itemRequest.delete();
	}
	
	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		long size = inputContext.getContentLength();
		String fileName = Text.getName(resource.getResourcePath());
		if (size > SIMPLE_UPLOAD_LIMIT_SIZE){
			log.info("Big file detected.");
			UploadSession session = itemRequest.itemByPath(fileName).uploadCreateSession().create(Item.CONFLICT_BEHAVIOR_REPLACE);
			InputStream is = inputContext.getInputStream();
			byte[] data = new byte[(int) SIMPLE_UPLOAD_LIMIT_SIZE];
			int n = 0, r = 0, startIndex = 0;
			long remaining = size;
			try {
				while ((n = is.read(data)) != -1 && remaining > 0) {
					while (n < data.length){
						if ((r = is.read(data, n, data.length - n)) == -1) break;
						n += r;
					}
					session = session.uploadFragment(startIndex, startIndex+n-1, size, data);
					startIndex += n;
					remaining = size - startIndex;
					if (remaining < SIMPLE_UPLOAD_LIMIT_SIZE){
						data = new byte[(int) remaining];
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (!session.isComplete()){
				log.error("Upload not completed! Remaining = "+ remaining + ", n = "+n);
				throw new DavException(HttpServletResponse.SC_PRECONDITION_FAILED);
			}
		} else {
			Item item = new Item(onedrive);
			item.setName(fileName);
			item.setConflictBehavior(Item.CONFLICT_BEHAVIOR_REPLACE);
			itemRequest.children().upload(item, inputContext.getInputStream());
		}
	}
	
}
