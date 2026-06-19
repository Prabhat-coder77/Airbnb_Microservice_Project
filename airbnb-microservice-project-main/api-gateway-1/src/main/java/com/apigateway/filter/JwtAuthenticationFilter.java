package com.apigateway.filter;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import reactor.core.publisher.Mono;

@Component // which ever URL will come with token first come to this class for verification
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
	
	 private static final String SECRET_KEY = "my-super-secret-key"; // should match signature

	    private static final List<String> openApiEndpoints = List.of( // open URL
	            "/auth/api/v1/auth/login",
	            "/auth/api/v1/auth/register",
	            "/property/api/v1/property/search-property"
	    );

	    private static final Map<String, List<String>> protectedEndpointsWithRoles = Map.of(
	    	    //"/auth/api/v1/welcome/message", List.of("ROLE_ADMIN"), // protected URL/auth written from api-gateway-1 YML file
	    	    "/micro2/message", List.of("ROLE_ADMIN")               // protected URL/micro2 written from api-gateway-1 YML file
	    	);


	    @Override
	    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
	        String requestPath = exchange.getRequest().getURI().getPath();

	        // Allow public ENDPOINTS 
	        if (isPublicEndpoint(requestPath)) {
	            return chain.filter(exchange);
	        }

	        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
	        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
	            return exchange.getResponse().setComplete();
	        }

	        String token = authHeader.substring(7);

	        try {
	            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET_KEY))
	                    .build()
	                    .verify(token);

	            String role = jwt.getClaim("role").asString();

	            System.out.println("Request path: " + requestPath);
	            System.out.println("Role from token: " + role);

	            if (!isAuthorized(requestPath, role)) {
	                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
	                return exchange.getResponse().setComplete();
	            }

	            // Pass role to downstream services (optional)
	            exchange = exchange.mutate()
	                    .request(r -> r.header("X-User-Role", role))
	                    .build();

	        } catch (JWTVerificationException e) {
	            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
	            return exchange.getResponse().setComplete();
	        }

	        return chain.filter(exchange);
	    }

	    private boolean isPublicEndpoint(String path) {
	        return openApiEndpoints.stream().anyMatch(path::equalsIgnoreCase);
	    }

	    private boolean isAuthorized(String path, String role) {
	        for (Map.Entry<String, List<String>> entry : protectedEndpointsWithRoles.entrySet()) {
	            String protectedPath = entry.getKey();
	            List<String> allowedRoles = entry.getValue();

	            if (path.startsWith(protectedPath)) {
	                System.out.println("Matched protected path: " + protectedPath + " | Allowed roles: " + allowedRoles);
	                return allowedRoles.contains(role);
	            }
	        }
	        return true; // Allow access if path is not protected (can be changed to false to deny by default)
	    }

	    @Override
	    public int getOrder() {
	        return -1;
	    }
	}

