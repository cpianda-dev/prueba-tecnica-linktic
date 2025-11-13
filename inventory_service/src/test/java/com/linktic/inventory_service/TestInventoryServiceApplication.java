package com.linktic.inventory_service;

import org.springframework.boot.SpringApplication;

public class TestInventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(InventoryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
