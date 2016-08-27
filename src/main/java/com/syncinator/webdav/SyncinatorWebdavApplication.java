package com.syncinator.webdav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import com.syncinator.webdav.server.SyncinatorSimpleWebdavServlet;

@SpringBootApplication
public class SyncinatorWebdavApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyncinatorWebdavApplication.class, args);
	}
	
	@Bean
	public ServletRegistrationBean servletRegistrationBean(){
	    return new ServletRegistrationBean(new SyncinatorSimpleWebdavServlet(),"/*");
	}
}
