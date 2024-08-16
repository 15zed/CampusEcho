package com.hgc.campusechogateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.hgc")
public class CampusEchoGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEchoGatewayApplication.class, args);
    }

}
