package com.auth_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth_service.entity.User;
import com.auth_service.payload.APIResponse;
import com.auth_service.payload.LoginDto;
import com.auth_service.payload.UserDto;
import com.auth_service.repository.UserRepository;
import com.auth_service.service.AuthService;
import com.auth_service.service.JWTService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JWTService jwtService;
	
	@Autowired
	private UserRepository userRepository;

	
	
	
	
	 @PostMapping("/register")
	    public ResponseEntity<APIResponse<String>> register(@RequestBody UserDto dto) {
	        APIResponse<String> response = authService.register(dto);
	        return new ResponseEntity<>(response, HttpStatusCode.valueOf(response.getStatus()));
	    }
	 
	 
	 @PostMapping("/login")
	 public ResponseEntity<APIResponse<String>> login(@RequestBody LoginDto loginDto){
		 
		 APIResponse<String> response = new APIResponse<>();
		 
		 UsernamePasswordAuthenticationToken token = 
				 new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());
		 
		try {
			 Authentication authenticate = authenticationManager.authenticate(token);
			 
			 if(authenticate.isAuthenticated()) {
				 
				 String jwtToken = jwtService.generateToken(loginDto.getUsername(),  // for generate JWT 
			      authenticate.getAuthorities().iterator().next().getAuthority());
				 
				 response.setMessage("Login Sucessful");
				 response.setStatus(200);
				 response.setData(jwtToken);// return JWT
				 return new ResponseEntity<>(response, HttpStatusCode.valueOf(response.getStatus()));
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		 response.setMessage("Failed");
		 response.setStatus(401);
		 response.setData("Un-Authorized Access");
		 return new ResponseEntity<>(response, HttpStatusCode.valueOf(response.getStatus()));
	 }
	 
	 @GetMapping("/get-user")
	 public User getUserByUserName(@RequestParam String username) {
		 
		 User user = userRepository.findByUsername(username);
		 return user;
		 
	 }


}
