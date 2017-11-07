package com.insurance.controllers.interfaces;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;

public interface AuthorizationController {

	public Response getAuthToken(@Context HttpRequest request);
	
}
