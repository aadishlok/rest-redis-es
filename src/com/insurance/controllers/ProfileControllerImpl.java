package com.insurance.controllers;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.insurance.controllers.interfaces.ProfileController;
import com.insurance.dao.InsuranceDaoImpl;
import com.insurance.util.AES;

@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileControllerImpl implements ProfileController{
	
	private InsuranceDaoImpl dao;
	private static final String AUTHORIZATION_HEADER= "authorization";
	private static final String encryptionKey= "encryptionkeyforinsuranceproject";
	private static final String AUTHORIZATION_HEADER_PREFIX= "Bearer ";
	
	public ProfileControllerImpl() {
		// TODO Auto-generated constructor stub
		
		dao= new InsuranceDaoImpl();
	}
	
	@Override
	@POST
	public Response createProfile(String newProfile, @Context UriInfo uriInfo) throws IOException, ProcessingException, ParseException {
		// TODO Auto-generated method stub
		
		System.out.println("Profile received:\n"+newProfile);
		
		final JsonNode profileSchema= JsonLoader.fromPath("C:/Users/antri/Documents/workspace-sts-3.9.1.RELEASE/Insurance/WebContent/WEB-INF/resources/profileSchema.json");
		
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(profileSchema);
		
		ProcessingReport report;
		report= schema.validate(JsonLoader.fromString(newProfile));
		
		System.out.println("Report: "+report.isSuccess());
		
		// Check if validation is successful. Call DAO
		String profileId= "";
		if(report.isSuccess()) 
			profileId= dao.createProfile(newProfile);
		else
			return Response.ok().status(422).entity("Please provide valid data").build();
			
		// Get Etag
		EntityTag etag= dao.getEtag(profileId, "profile");
		System.out.println("ETAG: "+etag);
		
		// Create Response
		URI uri= uriInfo.getAbsolutePathBuilder().path(profileId).build();
		return Response.created(uri).tag(etag)
				.header("ID", profileId)
				.entity(profileId).build();
	}

	@Override
	@GET
	public Response getProfile( @Context HttpHeaders headers) {
		// TODO Auto-generated method stub
		
		String id= getIdFromHeaders(headers);
		System.out.println("Get Profile "+id);
		
		JSONObject profile= dao.getProfile(id);
		if(profile.isEmpty()) 
			return Response.ok().status(Status.NOT_FOUND).entity("Please provide a valid ID").build();
		
		System.out.println("Retrieved Profile: "+profile.toJSONString());
		
		// Get ETAG
		EntityTag etag= dao.getEtag(id, "profile");
		/*System.out.println("Headers: "+headers.getRequestHeader("if-none-match").get(0));
		System.out.println("Etag: "+etag.getValue());
		System.out.println(headers.getRequestHeaders().containsKey("if-none-match"));
		System.out.println("Equals: "+headers.getRequestHeader("if-none-match").get(0).equals(etag.getValue()));*/
		if( headers.getRequestHeaders().containsKey("if-none-match") && headers.getRequestHeader("if-none-match").get(0).equals(etag.getValue()))
			return Response.notModified(etag).build();		
		
		return Response.ok().status(Status.OK).tag(etag).entity(profile.toJSONString()).build();
	}

	@Override
	@PUT
	public Response updateProfile(String changedUser,  @Context HttpHeaders headers) throws ProcessingException, IOException, ParseException {
		// TODO Auto-generated method stub
		
		String id= getIdFromHeaders(headers);
		System.out.println("Update Profile "+id+" with values:\n"+changedUser);
		
		// Validate against schema
		
		final JsonNode profileSchema= JsonLoader.fromPath("C:/Users/antri/Documents/workspace-sts-3.9.1.RELEASE/Insurance/WebContent/WEB-INF/resources/profileSchema.json");
		
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(profileSchema);
		
		ProcessingReport report;
		report= schema.validate(JsonLoader.fromString(changedUser));
		
		System.out.println("Report: "+report.isSuccess());
		
		// Check if validation is successful. Call DAO
		boolean profileId= false;
		if(report.isSuccess())
			profileId= dao.updateProfile(id, changedUser);
		else
			return Response.ok().status(422).entity("Please provide valid data").build();
		
		
		// Conditional return to POSTMAN
		if(profileId) { 
		
			EntityTag etag= dao.getEtag(id, "profile");
			return Response.ok().status(Status.OK).tag(etag).entity(id+" updated").build();
		}
		
		return Response.ok().status(Status.NOT_FOUND).entity("Please provide a valid ID").build();
	}

	@Override
	@DELETE
	public Response deactivateProfile( @Context HttpHeaders headers) {
		// TODO Auto-generated method stub
		
		String id= getIdFromHeaders(headers);
		System.out.println("Deactivate Profile: "+id);
		
		// Call DAO
		boolean result= dao.deactivateProfile(id);
		
		// Conditional return to POSTMAN
		if(result)
			return Response.ok().status(Status.OK).entity(id+" successfully removed").build();
		
		return Response.ok().status(Status.NOT_FOUND).entity("Please provide a valid ID").build();
	}
	
	@Override
	@DELETE
	@Path("/{planId}")
	public Response unsubscribePlan(@PathParam("planId") String planId,  @Context HttpHeaders headers) {
		
		String id= getIdFromHeaders(headers);
		System.out.println("Unsubscribe Plan");
		
		boolean response= dao.unsubscribePlan(id, planId);
		if(response)
			return Response.ok().status(Status.OK).entity("Plan with ID "+planId+" successfully removed").build();
		
		return Response.status(Status.NOT_FOUND).entity("Please provide valid data").build();
	}
	
	@Override
	@PUT
	@Path("/{profileId}/{planId}")
	public Response subscribe(@PathParam("profileId") String profileId, @PathParam("planId") String planId) throws IOException, ProcessingException {
		
		System.out.println("Subscribe to Plan: "+profileId+"\n"+planId);
		
		// Call DAO
		JSONObject updatedProfile= dao.subscribe(profileId, planId);
		
		// Conditional Return to postman
		if( updatedProfile == null )
			return Response.ok().status(Status.NOT_ACCEPTABLE).entity("Please provide valid data").build();
		
		return Response.ok().status(Status.OK).entity(updatedProfile.toJSONString()).build();
	}
	
	@Override
	public String getIdFromHeaders(HttpHeaders httpHeaders) {
		
		System.out.println("Headers: "+httpHeaders);
		List<String> authHeaders= httpHeaders.getRequestHeader(AUTHORIZATION_HEADER);
		System.out.println("Auth Header: "+authHeaders);
		
		// Extract Authorization Header
		String authToken= authHeaders.get(0);
		authToken= authToken.replaceFirst(AUTHORIZATION_HEADER_PREFIX, "");
		AES.setKey(encryptionKey);
		AES.decrypt(authToken);
		String decodedHeader= AES.getDecryptedString();
		
		// Extract Credentials
		StringTokenizer tokenizer= new StringTokenizer(decodedHeader, ":");
		String id= tokenizer.nextToken();
		
		return id;
	}

}
