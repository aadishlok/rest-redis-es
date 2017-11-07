package com.insurance.dao.interfaces;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;

public interface PatchDao {
	
	public boolean patch(String patch) throws ParseException, ProcessingException, IOException;
	
	public boolean replace(JSONObject object) throws ProcessingException, IOException;
	
	public JsonSchema getPlanSchema() throws IOException, ProcessingException;
	
	public JsonSchema getProfileSchema() throws IOException, ProcessingException;

}
