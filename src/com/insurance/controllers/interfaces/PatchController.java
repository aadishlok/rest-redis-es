package com.insurance.controllers.interfaces;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public interface PatchController {
	
	public Response patch(String patch) throws ParseException, IOException, ProcessingException;

}
