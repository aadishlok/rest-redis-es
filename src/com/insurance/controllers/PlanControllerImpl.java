package com.insurance.controllers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.insurance.controllers.interfaces.PlanController;
import com.insurance.dao.InsuranceDaoImpl;

@Path("/plan")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlanControllerImpl implements PlanController {
	
	// private static final String LOCATION_URI= "http://localhost:8080/Insurance/insurance";
	
	private InsuranceDaoImpl dao;
	
	public PlanControllerImpl() {
		// TODO Auto-generated constructor stub
		
		dao= new InsuranceDaoImpl();
	}

	@Override
	@GET
	@Path("/test")
	public Response test() {
		// TODO Auto-generated method stub
		
		return Response.ok().status(Status.OK).entity("Hello World").build();
	}

	@Override
	@POST
	public Response createPlan(String newPlan, @Context UriInfo uriInfo) throws IOException, ProcessingException, ParseException, URISyntaxException {
		// TODO Auto-generated method stub
		System.out.println("Plan received:\n"+newPlan);
		
		final JsonNode planSchema= JsonLoader.fromPath("C:/Users/antri/Documents/workspace-sts-3.9.1.RELEASE/Insurance/WebContent/WEB-INF/resources/planSchema.json");
		
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(planSchema);
		
		ProcessingReport report;
		report= schema.validate(JsonLoader.fromString(newPlan));
		
		System.out.println("Report: "+report);
		
		// Check if validation is successful. Call DAO
		String planId= "";
		if(report.isSuccess()) {
			planId= dao.createPlan(newPlan);
		}else {
			return Response.ok().status(422).entity("Please provide valid data").build();
		}
		
		// Get Etag
		EntityTag etag= dao.getEtag(planId.toString(), "plan");
		
		// Create Response
		URI uri= uriInfo.getAbsolutePathBuilder().path(planId).build();
		return Response.created(uri).tag(etag)
				.header("ID", planId)
				.entity(planId).build();
	}

	@Override
	@GET
	@Path("/{id}")
	public Response getPlan(@PathParam("id") String id, @Context HttpHeaders headers) {
		// TODO Auto-generated method stub
		
		System.out.println("Get Plan "+id);
		
		JSONObject plan= dao.getPlan(id);
		System.out.println("Retrieved Plan: "+plan);
		
		if( plan.isEmpty() || plan == null ) 
			return Response.ok().status(Status.NOT_FOUND).entity("Please provide valid data").build();
		
		EntityTag etag= dao.getEtag(id, "plan");
		if( headers.getRequestHeaders().containsKey("if-none-match") && headers.getRequestHeader("if-none-match").get(0).equals(etag.getValue()))
			return Response.notModified(etag).build();
			
		return Response.ok().status(Status.OK).entity(plan.toJSONString()).build();
	}

	@Override
	@PUT
	@Path("/{id}")
	public Response updatePlan(@PathParam("id") String id, String changedPlan) throws IOException, ProcessingException, ParseException {
		// TODO Auto-generated method stub
		
		System.out.println("Update Plan "+id+" with values:\n"+changedPlan);
		
		// Validate against schema
		
		final JsonNode planSchema= JsonLoader.fromPath("C:/Users/antri/Documents/workspace-sts-3.9.1.RELEASE/Insurance/WebContent/WEB-INF/resources/planSchema.json");
		
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(planSchema);
		
		ProcessingReport report;
		report= schema.validate(JsonLoader.fromString(changedPlan));
		
		System.out.println("Report: "+report.isSuccess());
		
		// Check if validation is successful. Call DAO
		boolean planId= false;
		if(report.isSuccess()) {
			planId= dao.updatePlan(id, changedPlan);
		}else {
			return Response.ok().status(422).entity("Please provide valid data").build();
		}
		
		// Conditional return to POSTMAN
		if(planId) {
			EntityTag etag= dao.getEtag(id, "plan");
			return Response.ok().status(Status.OK).tag(etag).entity(id+" updated").build();
		}
		
		return Response.ok().status(Status.NOT_FOUND).entity("Please provide a valid ID").build();
	}

	@Override
	@DELETE
	@Path("/{id}")
	public Response deactivatePlan(@PathParam("id") String id) {
		// TODO Auto-generated method stub
		
		System.out.println("Deactivate Plan: "+id);
		
		// Call DAO
		boolean result= dao.deactivatePlan(id);
		
		// Conditional return to POSTMAN
		if(result) {
			return Response.ok().status(Status.OK).entity(id+" successfully removed").build();
		}
		
		return Response.ok().status(Status.NOT_FOUND).entity("Please provide a valid ID").build();
	}

	@Override
	@GET
	@Path("/available")
	public Response getAllPlanKeys() {
		// TODO Auto-generated method stub
		System.out.println("Get All Plan Keys");
		
		return Response.ok().status(Status.OK).entity(dao.getAllPlanKeys()).build();
	}

	@Override
	@GET
	@Path("/search/{criteria}")
	public Response search(@PathParam("criteria") String criteria) {
		// TODO Auto-generated method stub
		System.out.println("Search Plans");
		
		if(criteria.isEmpty()) 
			return Response.ok().status(422).entity("Please provide a valid search criteria").build();
		
		Set<String> searchResults= dao.search(criteria);
		if( !searchResults.isEmpty() )
			return Response.ok().status(Status.OK).entity(searchResults).build();
		
		return Response.ok().status(Status.NO_CONTENT).entity("Search criteria doesnt match any entities").build();
	}

}
