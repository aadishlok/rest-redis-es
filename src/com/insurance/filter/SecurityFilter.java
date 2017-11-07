package com.insurance.filter;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.insurance.dao.InsuranceDaoImpl;
import com.insurance.util.AES;

public class SecurityFilter implements ContainerRequestFilter {

	private static final String AUTHORIZATION_HEADER= "authorization";
	private static final String AUTHORIZATION_HEADER_PREFIX= "Bearer ";
	private static final String ROLE_ADMIN= "ADMIN";
	private static final String encryptionKey= "encryptionkeyforinsuranceproject";
	
	private enum Methods{
		GET,
		POST,
		PUT,
		DELETE
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// TODO Auto-generated method stub
		
		boolean flag= true;
		String requestMethod= requestContext.getMethod();
		System.out.println("FILTER: "+requestContext.getHeaders());
		
		// Loop through publicly available URIs to finalize flag
		if( requestContext.getUriInfo().getPath().contains("test") )
			flag= true;
		else if(requestContext.getMethod().equals(Methods.GET.name()) && 
				( requestContext.getUriInfo().getPath().contains("plan") || requestContext.getUriInfo().getPath().contains("auth"))) 
			flag= false;
		else if( requestContext.getMethod().equals(Methods.POST.name()) &&
				requestContext.getUriInfo().getPath().contains("profile") )
			flag= false;
		

		
		// Filter if authorization required
		System.out.println("Flag: "+flag);
		if(flag) {
			
			List<String> authHeader= requestContext.getHeaders().get(AUTHORIZATION_HEADER);
			System.out.println("Auth Header: "+authHeader);
			if( authHeader == null ) {
				
				Response unauthorizedResponse= Response.ok().status(Status.FORBIDDEN).entity("Please provide credentials").build();
				requestContext.abortWith(unauthorizedResponse);
			}else {
				
				InsuranceDaoImpl dao= new InsuranceDaoImpl();
				// Extract Authorization Header
				String authToken= authHeader.get(0);
				authToken= authToken.replaceFirst(AUTHORIZATION_HEADER_PREFIX, "");
				AES.setKey(encryptionKey);
				AES.decrypt(authToken);
				String decodedHeader= AES.getDecryptedString();
				
				// Extract Credentials
				StringTokenizer tokenizer= new StringTokenizer(decodedHeader, ":");
				String id= tokenizer.nextToken();
				String password= tokenizer.nextToken();
				
				// Check Role
				String role= dao.getRole(id);
				System.out.println("Role: "+role);
				if(role == null || role.isEmpty()) {
					
					Response unauthorizedResponse= Response.ok().status(Status.FORBIDDEN).entity("Please provide credentials").build();
					requestContext.abortWith(unauthorizedResponse);
				} else if ( role.equals(ROLE_ADMIN) )
					return;
				
				// Validate Credentials
				if(dao.validateCredentials(id, password) && validateRole(requestMethod, role, requestContext.getUriInfo().getPath()))
					return;
				
				Response unauthorizedResponse= Response.ok().status(Status.UNAUTHORIZED).entity("Invalid credentials").build();
				requestContext.abortWith(unauthorizedResponse);
			}
		} else {
			return;
		}
	}
	
	public boolean validateRole(String requestMethod, String role, String path) {
		System.out.println("Validate Role: "+requestMethod);
		
		switch(requestMethod) {
		
		case "GET": 
			if( path.contains("profile") || path.contains("test") )
				return true;
			break;
		
		case "POST":
			break;
			
		case "PUT":
			return true;
			
		case "DELETE":
			if(path.contains("plan") )
				return false;
			else if( path.contains("profile") )
				return true;
		}
			
		return false;
	}

	
}
