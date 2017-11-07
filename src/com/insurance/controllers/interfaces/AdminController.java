package com.insurance.controllers.interfaces;

import java.io.IOException;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

public interface AdminController {
	
	public Response savePlanSchema(String schema) throws IOException;
	
	public Response saveProfileSchema(String schema) throws IOException;
	
	public Response getSchema(@PathParam("type") String type);

}
