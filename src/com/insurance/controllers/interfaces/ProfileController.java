package com.insurance.controllers.interfaces;

import java.io.IOException;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public interface ProfileController {
	
	public Response createProfile(String newUser, @Context UriInfo uriInfo) throws IOException, ProcessingException, ParseException;
	
	public Response getProfile( @Context HttpHeaders headers);
	
	public Response updateProfile(String changedUser,  @Context HttpHeaders headers) throws ProcessingException, IOException, ParseException;
	
	public Response subscribe(@PathParam("profileId") String profileId, @PathParam("planId") String planId) throws IOException, ProcessingException;
	
	public Response deactivateProfile( @Context HttpHeaders headers);
	
	public Response unsubscribePlan(@PathParam("planId") String planId,  @Context HttpHeaders headers);
	
	public String getIdFromHeaders(HttpHeaders httpHeaders);

}
