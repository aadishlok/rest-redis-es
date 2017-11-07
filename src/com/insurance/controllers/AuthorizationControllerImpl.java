package com.insurance.controllers;

import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.Base64;
import org.jboss.resteasy.spi.HttpRequest;

import com.insurance.controllers.interfaces.AuthorizationController;
import com.insurance.util.AES;
import com.insurance.util.Credentials;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthorizationControllerImpl implements AuthorizationController {

	
	private static final String AUTHORIZATION_HEADER= "authorization";
	private static final String AUTHORIZATION_HEADER_PREFIX= "Basic ";
	private static final String encryptionKey= "encryptionkeyforinsuranceproject";
	
	@Override
	@GET
	public Response getAuthToken(@Context HttpRequest request) {
		// TODO Auto-generated method stub
		
		List<String> authHeader= request.getHttpHeaders().getRequestHeader(AUTHORIZATION_HEADER);
		String authToken= authHeader.get(0).replaceFirst(AUTHORIZATION_HEADER_PREFIX, "");
		
		// Extract Authorization Header
		String decodedHeader= Base64.decodeAsString(authToken);
		
		// Extract Credentials
		StringTokenizer tokenizer= new StringTokenizer(decodedHeader, ":");
		String id= tokenizer.nextToken();
		String password= tokenizer.nextToken();
		
		System.out.println("Credentials: "+authToken);
		System.out.println("ID: "+id);
		System.out.println("Password: "+password);
		
		// Generate credential string to encrypt
		Credentials credentials= new Credentials(id, password);
		System.out.println("Encrytion Data: "+credentials.toString());
		
		// Set Encryption Key
		AES.setKey(encryptionKey.trim());
		
		// Encrypt
		AES.encrypt(credentials.toString());
		System.out.println(AES.getEncryptedString());
		
		// Decrypt
		AES.decrypt(AES.getEncryptedString());
		System.out.println(AES.getDecryptedString());
		
		return Response.ok().entity(AES.getEncryptedString()).build();
	}
	
}
