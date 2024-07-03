/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.schema.provider.ibm.app;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ComponentScan({ "org.opengroup.osdu" })
@PropertySource("classpath:swagger.properties")
public class SchemaIBMApplication {

	@PostConstruct
	void f() {

	}

	public static void main(String[] args) {

		SpringApplication.run(SchemaIBMApplication.class, args);
	}
}
