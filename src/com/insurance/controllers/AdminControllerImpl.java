package com.insurance.controllers;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.insurance.controllers.interfaces.AdminController;
import com.insurance.dao.InsuranceDaoImpl;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminControllerImpl implements AdminController {

	private InsuranceDaoImpl dao;
	
	public AdminControllerImpl() {
		// TODO Auto-generated constructor stub
		dao= new InsuranceDaoImpl();
	}
	
	@Override
	@POST
	@Path("/planSchema")
	public Response savePlanSchema(String schema) throws IOException {
		// TODO Auto-generated method stub
		
		if(schema.isEmpty() || schema == null)
			return Response.notAcceptable(null).entity("Please provide valid data").build();
		
		String response= dao.savePlanSchema(schema);
		
		return Response.ok().status(Status.OK).entity(response).build();
	}

	@Override
	@POST
	@Path("/profileSchema")
	public Response saveProfileSchema(String schema) throws IOException {
		// TODO Auto-generated method stub
		
		if(schema.isEmpty() || schema == null)
			return Response.notAcceptable(null).entity("Please provide valid data").build();
		
		String response= dao.saveProfileSchema(schema);
		
		return Response.ok().status(Status.OK).entity(response).build();
	}
	
	@Override
	@GET
	@Path("/{type}")
	public Response getSchema(@PathParam("type") String type) {
		
		
		if( type.equals("plan") || type.equals("profile") ) {
			
			JSONObject schema= dao.getSchema(type);
			return Response.ok().status(Status.OK).entity(schema.toJSONString()).build();
		}
		
		return Response.status(Status.NOT_ACCEPTABLE).entity("Please provide valid type").build();
	}

}
