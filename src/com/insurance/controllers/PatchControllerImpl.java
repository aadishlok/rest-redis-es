package com.insurance.controllers;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.insurance.controllers.interfaces.PatchController;
import com.insurance.dao.PatchDaoImpl;

@Path("/patch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatchControllerImpl implements PatchController {

	private PatchDaoImpl patchDao;
	
	public PatchControllerImpl() {
		// TODO Auto-generated constructor stub
		patchDao= new PatchDaoImpl();
	}
	
	@Override
	@PUT
	public Response patch(String patch) throws ParseException, IOException, ProcessingException {
		// TODO Auto-generated method stub
		
		System.out.println("Patch Controller:\n "+patch);
		
		// Validate with Patch Schema
		final JsonNode patchSchema= JsonLoader.fromPath("D:/Courses/ABD/Workspace/Insurance/WebContent/WEB-INF/resources/patchSchema.json");
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(patchSchema);
		
		ProcessingReport report;
		report= schema.validate(JsonLoader.fromString(patch));
		
		// Check Report and Call Patch DAO
		
		boolean result= false;
		if(report.isSuccess()) {
			result= patchDao.patch(patch);
		}
		
		if(result) {
			return Response.ok().status(Status.OK).entity("Patch Successful").build();
		}
		
		return Response.ok().status(422).entity("Please provide valid data").build();
	}

}
