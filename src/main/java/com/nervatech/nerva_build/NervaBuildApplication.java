package com.nervatech.nerva_build;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class NervaBuildApplication {

	public static void main(String[] args) {
		SpringApplication.run(NervaBuildApplication.class, args);
	}

}
