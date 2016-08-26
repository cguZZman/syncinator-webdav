package com.syncinator.webdav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SyncinatorWebdavApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyncinatorWebdavApplication.class, args);
	}
	
	@Bean
	public ServletRegistrationBean servletRegistrationBean(){
	    return new ServletRegistrationBean(new WebdavServlet(),"/onedrive/*");
	}
}
