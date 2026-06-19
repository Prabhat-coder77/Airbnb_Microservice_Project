package com.auth_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth_service.entity.User;

@RestController
@RequestMapping("/api/v1/welcome")
public class WelcomeController {
	
	//http://localhost:8083/api/v1/welcome/message
	
	@GetMapping("/message")
	public String welcome(@AuthenticationPrincipal User user) { 
		System.out.println(user.getName());
		return "welcome ajay";
	}

}
