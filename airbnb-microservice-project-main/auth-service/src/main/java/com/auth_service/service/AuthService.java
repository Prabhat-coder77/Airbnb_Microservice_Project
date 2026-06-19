package com.auth_service.service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth_service.entity.User;
import com.auth_service.payload.APIResponse;
import com.auth_service.payload.UserDto;
import com.auth_service.repository.UserRepository;

@Service
public class AuthService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	public APIResponse<String> register(UserDto userDto) {
		
        // API Response object
		APIResponse<String> response = new APIResponse<>();

	    // Check whether USERNAME exits
	    if (userRepository.existsByUsername(userDto.getUsername())) {
	        response.setMessage("Registration Failed");
	        response.setStatus(400);
	        response.setData("Username already exists");
	        return response;
	    }

	    // Check whether email exists
	    if (userRepository.existsByEmail(userDto.getEmail())) {
	        response.setMessage("Registration Failed");
	        response.setStatus(400);
	        response.setData("Email already registered");
	        return response;
	    }

	    // Encode password
	    User user = new User();
	    BeanUtils.copyProperties(userDto, user);
	    user.setPassword(passwordEncoder.encode(userDto.getPassword())); // password encoded
	    user.setRole("ROLE_ADMIN"); // for role based(added)

	    User savedUser = userRepository.save(user);

	    if (savedUser == null) {
	        response.setMessage("Registration Failed");
	        response.setStatus(500);
	        response.setData("Unexpected server error");
	        return response;
	    }

	    // Success response
	    response.setMessage("Registration Successful");
	    response.setStatus(201);
	    response.setData("User registered successfully");

	    return response;
	}

}
