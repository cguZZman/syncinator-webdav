 package com.syncinator.webdav;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.syncinator.webdav.cloud.onedrive.IdSerializatorAccessTokenListener;
import com.syncinator.webdav.cloud.onedrive.OneDriveConnectionRepository;
import com.syncinator.webdav.server.DownloadManager;
import com.syncinator.webdav.server.DownloadManager.DownloadItem;
import com.syncinator.webdav.server.DownloadManager.DownloadItemPart;
import com.syncinator.webdav.server.SyncinatorWebdavServlet;

@SpringBootApplication
@EnableScheduling
public class SyncinatorWebdavApplication {
	private Logger log = LoggerFactory.getLogger(getClass());
	public static final String APP_BASE_DIR = System.getProperty("user.home", "") + File.separator + ".syncinator"; 
	
	public static void main(String[] args) {
		SpringApplication.run(SyncinatorWebdavApplication.class, args);
	}
	
	@Bean
	public ServletRegistrationBean servletRegistrationBean(){
		String prefix = "/webdav";
		ServletRegistrationBean registration = new ServletRegistrationBean(new SyncinatorWebdavServlet(), prefix + "/*");
		registration.addInitParameter(SimpleWebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, prefix);

		//temporal
		File dir = new File(new IdSerializatorAccessTokenListener(null).getApplicationFolder());
		if (dir.exists() && dir.isDirectory()){
			for (File file : dir.listFiles()){
				if (file.getName().endsWith(IdSerializatorAccessTokenListener.ACCESS_TOKEN_EXTENSION)){
					String id = file.getName().substring(0, file.getName().length() - IdSerializatorAccessTokenListener.ACCESS_TOKEN_EXTENSION.length());
					OneDriveConnectionRepository.addConnectionFromDisk(id);
				}
			}
		}
		
	    return registration;
	}
	
	@Scheduled(fixedDelay = 5000)
	public void downloadPartCleaner(){
		Map<String, DownloadItem> itemMap = DownloadManager.itemMap;
		synchronized (itemMap) {
			List<String> itemsToDelete = new ArrayList<String>();
			for (Entry<String,DownloadItem> e : itemMap.entrySet()){
				DownloadItem item = e.getValue();
				synchronized (item) {
					TreeSet<DownloadItemPart> parts = item.getParts();
					synchronized (parts) {
						List<DownloadItemPart> partsToDelete = new ArrayList<DownloadItemPart>();
						for (DownloadItemPart part : parts) {
							synchronized (part) {
								if (part.getExpirationTime() < System.currentTimeMillis() && part.getStatus() != DownloadItemPart.STATUS_STARTED) {
									File file = part.getFile();
									synchronized (file) {
										file.delete();
									}
									partsToDelete.add(part);
								}
							}
						}
						parts.removeAll(partsToDelete);
						if (parts.size() == 0) {
							itemsToDelete.add(e.getKey());
						}
					}
				}
			}
			for (String key : itemsToDelete) {
				itemMap.remove(key);
			}
			List<DownloadItemPart> deleted = new ArrayList<DownloadItemPart>();
			synchronized (DownloadManager.unableToDelete){
				for (DownloadItemPart part : DownloadManager.unableToDelete) {
					File file = part.getFile();
					synchronized (file) {
						log.info("Unable to delete file ["+part.getFile().getName()+"]. Deleted now?: " + part.getFile().delete());
						if (!part.getFile().exists()) {
							deleted.add(part);
						}
					}
				}
				DownloadManager.unableToDelete.removeAll(deleted);
			}
		}
	}
}
