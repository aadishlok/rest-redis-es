package com.insurance.dao;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.insurance.dao.interfaces.PatchDao;

import redis.clients.jedis.Jedis;

public class PatchDaoImpl implements PatchDao {
	
	private Jedis jedis;
	private InsuranceDaoImpl insuranceDao;
	
	public PatchDaoImpl() {
		// TODO Auto-generated constructor stub
		jedis= new Jedis();
	}

	@Override
	public boolean patch(String patch) throws ParseException, ProcessingException, IOException {
		// TODO Auto-generated method stub
		
		// Convert to JSON Array
		JSONParser parser= new JSONParser();
		JSONArray patchArray= (JSONArray) parser.parse(patch);
		
		// Loop Through all JSON Objects
		for(int i=0; i< patchArray.size(); i++) {
			
			boolean replaceResponse= false;
			// Extract object from array
			JSONObject object= (JSONObject) patchArray.get(i);
			switch(object.get("op").toString()) {
			
			case "replace": 
				replaceResponse= replace(object);
				break;
			}
			
			if( replaceResponse == false )
				return false;
		}
		
		return true;
	}

	@Override
	public boolean replace(JSONObject object) throws ProcessingException, IOException {
		// TODO Auto-generated method stub
		
		System.out.println("Replacing:\n"+object);
		
		// Extract Redis key from Path
		String path= object.get("path").toString();
		String uuid= path.substring(path.indexOf("/")+1, path.lastIndexOf("/"));
		String key= "";
		String condition= "";
		if(uuid.contains(".")) {
			condition= uuid;
			key= uuid.substring(0, uuid.indexOf("."))+"."+path.substring(0, path.indexOf("/")); 
		}else {
			key= uuid+"."+path.substring(0, path.indexOf("/"));
			condition= key;
		}
		String field= path.substring(path.lastIndexOf("/"));
		
		// Check if key and field exists
		long response= 0;
		if( jedis.exists(condition) && jedis.hexists(condition, field)) {
			
			response= jedis.hset(condition, field, object.get("value").toString());
			if( response == 1 ) {
				
				if( key.contains("plan") ) {
					
					JSONObject updatedPlan= insuranceDao.getPlan(key);
					ProcessingReport report;
					report= getPlanSchema().validate(JsonLoader.fromString(updatedPlan.toJSONString()));
					if(report.isSuccess()) {
						System.out.println("Patch for "+path+" successful");
						return true;
					}
				} else if( key.contains("profile") ) {
					
					JSONObject updatedProfile= insuranceDao.getPlan(key);
					ProcessingReport report;
					report= getProfileSchema().validate(JsonLoader.fromString(updatedProfile.toJSONString()));
					if(report.isSuccess()) {
						System.out.println("Patch for "+path+" successful");
						return true;
					}
				}
			}
		}
		
		return false;
	}

	@Override
	public JsonSchema getPlanSchema() throws IOException, ProcessingException {
		// TODO Auto-generated method stub
		
		final JsonNode schemaNode= JsonLoader.fromPath("D:/Courses/ABD/Workspace/Insurance/WebContent/WEB-INF/resources/planSchema.json");
		
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(schemaNode);
		
		return schema;
	}
	
	@Override
	public JsonSchema getProfileSchema() throws IOException, ProcessingException {
		// TODO Auto-generated method stub
		
		final JsonNode schemaNode= JsonLoader.fromPath("D:/Courses/ABD/Workspace/Insurance/WebContent/WEB-INF/resources/profileSchema.json");
		
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(schemaNode);
		
		return schema;
	}

}
