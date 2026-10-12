package com.example.tradedesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TradedeskApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradedeskApplication.class, args);
	}

}
