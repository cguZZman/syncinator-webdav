 package com.syncinator.webdav;

import java.io.File;

import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import com.syncinator.webdav.cloud.onedrive.IdSerializatorAccessTokenListener;
import com.syncinator.webdav.cloud.onedrive.OneDriveConnectionRepository;
import com.syncinator.webdav.server.SyncinatorWebdavServlet;

@SpringBootApplication
public class SyncinatorWebdavApplication {

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
	
}
