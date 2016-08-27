 package com.syncinator.webdav;

import java.util.Arrays;

import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import com.onedrive.api.OneDrive;
import com.onedrive.api.support.ClientCredential;
import com.onedrive.api.support.Scope;
import com.syncinator.webdav.cloud.onedrive.IdSerializatorAccessTokenListener;
import com.syncinator.webdav.cloud.onedrive.OneDriveConnectionRepository;
import com.syncinator.webdav.server.SyncinatorWebdavServlet;

@SpringBootApplication
public class SyncinatorWebdavApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyncinatorWebdavApplication.class, args);
	}
	
	@Bean
	public ServletRegistrationBean servletRegistrationBean(){
		String prefix = "/webdav";
		ServletRegistrationBean registration = new ServletRegistrationBean(new SyncinatorWebdavServlet(), prefix + "/*");
		registration.addInitParameter(SimpleWebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, prefix);
		
		OneDrive oneDrive = new OneDrive(new ClientCredential("0000000048145120"),
				Arrays.asList(Scope.OFFLINE_ACCESS, "wl.skydrive", "wl.signin", "onedrive.readwrite"),
				OneDrive.MOBILE_REDIRECT_URI);
		oneDrive.setAuthorizationCode("M62b3f60d-d2af-1aef-2716-d7be4ba4d2e3");
		oneDrive.setAccessTokenListener(new IdSerializatorAccessTokenListener("1"));
		OneDriveConnectionRepository.addConnection("1", oneDrive);
		
	    return registration;
	}
	
}
