package com.insurance.controllers.interfaces;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public interface PlanController {
	
	public Response test();
	
	public Response createPlan(String newPlan, @Context UriInfo uriInfo) throws IOException, ProcessingException, ParseException, URISyntaxException;
	
	public Response getPlan(@PathParam("id") String id, @Context HttpHeaders headers);
	
	public Response updatePlan(@PathParam("id") String id, String changedPlan) throws IOException, ProcessingException, ParseException;
	
	public Response deactivatePlan(@PathParam("id") String id);
	
	public Response getAllPlanKeys();

	public Response search(@PathParam("criteria") String criteria);
}
