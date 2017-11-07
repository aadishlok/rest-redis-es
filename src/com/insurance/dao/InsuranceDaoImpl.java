package com.insurance.dao;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.ws.rs.core.EntityTag;

import org.apache.commons.codec.digest.DigestUtils;
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
import com.insurance.dao.interfaces.InsuranceDao;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class InsuranceDaoImpl implements InsuranceDao {

	private static final String MASTER_NAME= "mymaster";
	
	private Jedis jedis;
	private Jedis slave;
	private JedisPool pool;
	
	public InsuranceDaoImpl() {
		// TODO Auto-generated constructor stub
		pool= new JedisPool(new JedisPoolConfig(), "localhost");
		jedis= pool.getResource();
		jedis.slaveofNoOne();
		slave= new Jedis("localhost", 6380);
		slave.slaveof("localhost", 6379);
		
		System.out.println("Info Master:\n "+pool.getNumActive());
	}
	
	
	@Override
	public String createPlan(String newPlan) throws IOException, ParseException {
		// TODO Auto-generated method stub
		System.out.println("Create Plan DAO");

		// Create UUID and put in plan
		UUID id= UUID.randomUUID();
		
		// Convert to JSONObject
		JSONParser parser= new JSONParser();
		JSONObject newPlanObject= (JSONObject) parser.parse(newPlan);
		
		// Add Creation Date parameter
		TimeZone tz= TimeZone.getTimeZone("UTC");
		DateFormat df= new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss'Z'");
		df.setTimeZone(tz);
		newPlanObject.put("creationDate", df.format(new Date()));
		newPlanObject.put("lastModifiedDate", df.format(new Date()));
		
		System.out.println("Map: "+newPlanObject);
	
		// Recursion to create stand-alone objects. 
		saveToRedis(id.toString(), "plan", JsonLoader.fromString(newPlanObject.toJSONString()));
		
		return id.toString();
	}
	
	public EntityTag getEtag(String id, String type) {
		
		JSONObject data= new JSONObject();
		// Get Plan/Profile
		if(type.equals("plan"))
			data= getPlan(id);
		else
			data= getProfile(id);
		System.out.println("ETAG Profile: "+data);
		
		// Create Hash
		String hash= DigestUtils.md5Hex(data.toJSONString());
		System.out.println("Hash:\n"+hash);
		
		return EntityTag.valueOf(hash);
	}
	
	public void saveSchema(String id, String type, JsonNode schema) {
		
		System.out.println("Save Schema to REDIS");
		
		Map<String, String> map= new HashMap<>();
		Iterator<String> fieldNames= schema.fieldNames();
		while(fieldNames.hasNext()) {
			
			String fieldName= fieldNames.next();
			JsonNode fieldValue= schema.get(fieldName);
			if( fieldValue.isObject() ) {
				
				map.put(fieldName, id+"."+type+"."+fieldName);
				saveSchema(id+"."+type, fieldName, fieldValue);
			} else if( fieldValue.isArray() ) {
				
				if(fieldName.equals("enum") || fieldName.equals("required")) {
					
					map.put(fieldName, id+"."+type+"."+fieldName);
					for( int i= 0; i< fieldValue.size(); i++ )
						jedis.sadd(id+"."+type+"."+fieldName, fieldValue.get(i).asText());
				}else {
					
					map.put(fieldName, id+"."+type+"."+fieldName);
					for( int i=0; i< fieldValue.size(); i++ ) {
						
						jedis.sadd(id+"."+type+"."+fieldName, fieldName+".members."+i);
						saveSchema(fieldName+".members", String.valueOf(i), fieldValue.get(i));
					}
						
				}
			} else {
				
				if(fieldValue.isTextual()) 
					map.put(fieldName, fieldValue.textValue());
				else 
					map.put(fieldName, fieldValue.toString());
			}
		}
		
		System.out.println("Key: "+id+"."+type);
		System.out.println("Saved Map:"+map);
		String r= jedis.hmset(id+"."+type, map);
		System.out.println("Redis Response: "+r);
	}
	
	@Override
	public void saveToRedis(String id, String type, JsonNode newPlan) {
		
		System.out.println("Save To REDIS:");
		
		Map<String, String> map= new HashMap<>();
		map.put("_id", id);
		map.put("objectId", id);
		map.put("_type", type);
		map.put("objectType", type);
		Iterator<String> fieldNames= newPlan.fieldNames();
		while(fieldNames.hasNext()) {
			
			String fieldName= fieldNames.next();
			JsonNode fieldValue= newPlan.get(fieldName);
			if(fieldValue.isObject()) {
				
				System.out.println(fieldName+" is an object");
				map.put(fieldName, id+"."+fieldName);
				saveToRedis(id+"."+type, fieldName, fieldValue);
			} else if( fieldValue.isArray() ) {
				
				System.out.println(fieldName+" is an array with values:\n"+fieldValue);
				System.out.println("Size: "+fieldValue.size());
				map.put(fieldName, id+"."+fieldName);
				for(int i= 0; i< fieldValue.size(); i++) { 
					
					boolean checkPlanReplaced= checkExistingSubscribedPlans(id, fieldValue.get(i));
					if(!checkPlanReplaced)
						saveNewSubscribedPlanIfExists(id, fieldName, fieldValue.get(i));
				}
				
			} else {
				if(fieldValue.isTextual()) 
					map.put(fieldName, fieldValue.textValue());
				else 
					map.put(fieldName, fieldValue.toString());
				
				
			}
		}
		// Save Map To Redis
		System.out.println("Saved Map: \n"+map);
		String r= jedis.hmset(id+"."+type, map);
		System.out.println("Redis Response: "+r);
		System.out.println("SAVED");
	}
	
	public void saveNewSubscribedPlanIfExists(String id, String fieldName, JsonNode newSubscription) {
		
		System.out.println("New Subscribed Plan Object");
		Iterator<String> fNames= newSubscription.fieldNames();
		while(fNames.hasNext()) {
			
			String fName= fNames.next();
			JsonNode fValue= newSubscription.get(fName);
			System.out.println("Field: "+fName+"\nValue: "+fValue.asText()+"\nExistence: "+jedis.exists(fValue.asText()+".plan"));
			if(fName.equals("planId") && jedis.exists(fValue.asText()+".plan")) {
				
				UUID subscribedPlanObjectKey= UUID.randomUUID();
				jedis.sadd(id+"."+fieldName, subscribedPlanObjectKey.toString()+".subscribedPlan");
				saveToRedis(subscribedPlanObjectKey.toString(), "subscribedPlan", newSubscription);
			}
			
		}
		
	}
	
	@Override
	public boolean checkExistingSubscribedPlans(String id, JsonNode changedSubscription) {
		
		System.out.println("Check for existing subscriptions");
		
		if( jedis.exists(id+".subscribedPlans")) {
			
			// Get Keys for the subscribed plans already saved as separate objects.
			// Subscribed Plans data from Profile object will contain a set of keys
			// Those keys contain the data about the subscriptions user already has to his profile.
			Set<String> subscribedPlansObjectKeys= jedis.smembers(id+".subscribedPlans");
			System.out.println("Subscription Object Keys: "+subscribedPlansObjectKeys);
			for( String key: subscribedPlansObjectKeys ) {
				
				Map<String, String> subscribedPlan= getSubscribedPlan(key);
				JSONObject subscribedPlanObject= new JSONObject(subscribedPlan);
				System.out.println("Subscribed Plan: "+subscribedPlanObject);
				
				JSONObject changedSubscriptionObject= new JSONObject();
				Iterator<String> fieldNames= changedSubscription.fieldNames();
				while( fieldNames.hasNext() ) {
					
					String fieldName= fieldNames.next();
					changedSubscriptionObject.put(fieldName, changedSubscription.get(fieldName).asText());
				}
				System.out.println("Changed Plan: "+changedSubscriptionObject);
				
				// Replace Old subscription plan values with new ones if plans are the same.
				if( changedSubscriptionObject.get("planId").equals(subscribedPlanObject.get("planId")) ) {
					System.out.println("Subscription Exists. Updating Values");
					jedis.hset(key, "startDate", changedSubscription.get("startDate").asText());
					jedis.hset(key, "endDate", changedSubscription.get("endDate").asText());
					return true;
				}
					
			}
		}
		
		return false;
	}
	
	@Override
	public Map<String, String> getSubscribedPlan(String id){
		
		return jedis.hgetAll(id);
	}

	@Override
	public JSONObject getPlan(String id) {
		// TODO Auto-generated method stub
		
		Map<String, String> plan= jedis.hgetAll(id+".plan");
		System.out.println("Stored Plan:\n"+plan);
		
		return convertToJson(plan);
	}
	
	@Override
	public JSONObject convertToJson(Map<String, String> storedPlan) {
		
		JSONObject storedPlanObject= new JSONObject();
		for(String key: storedPlan.keySet()) {
			System.out.println("Key: "+key+"\nValue: "+storedPlan.get(key));
			if( storedPlan.get(key).contains(key) ) {
				
				if( storedPlan.get(key).contains("subscribedPlans")) {
					
					JSONArray subscribedPlansArray= new JSONArray();
					Set<String> subscribedPlanObjectKeySet= jedis.smembers(storedPlan.get(key));
					System.out.println("Subscribed Plans: "+subscribedPlanObjectKeySet);
					for( String subscribedPlanKey: subscribedPlanObjectKeySet ) {
						subscribedPlansArray.add(convertToJson(jedis.hgetAll(subscribedPlanKey)));
					}
					storedPlanObject.put(key, subscribedPlansArray);
				} else if( storedPlan.get(key).contains("enum") || storedPlan.get(key).contains("required") ) {
					
					JSONArray values= new JSONArray();
					Set<String> data= jedis.smembers(storedPlan.get(key));
					for( String d: data )
						values.add(d);
					storedPlanObject.put(key, values);
				}
				  else
					storedPlanObject.put(key, convertToJson(jedis.hgetAll(storedPlan.get(key))));
			}else 
				storedPlanObject.put(key, storedPlan.get(key));			
		}
		
		return storedPlanObject;
	}

	@Override
	public boolean updatePlan(String id, String changedPlan) throws IOException, ParseException {
		// TODO Auto-generated method stub
		
		if(jedis.exists(id+".plan")) {
			
			// Delete previous version
			/* Set<String> planKeys= jedis.keys(id);
			for(String key: planKeys){
				jedis.del(key);
			}
			System.out.println("Previous version deleted");
			*/
			// Create new instance in Redis
			
			// Convert to JSONObject
			JSONParser parser= new JSONParser();
			JSONObject newPlanObject= (JSONObject) parser.parse(changedPlan);
			
			// Add Creation Date parameter
			TimeZone tz= TimeZone.getTimeZone("UTC");
			DateFormat df= new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss'Z'");
			df.setTimeZone(tz);
			newPlanObject.put("lastModifiedDate", df.format(new Date()));
			
			System.out.println("Map: "+newPlanObject);
			
			// Recursion to create stand-alone objects. 
			saveToRedis(id.toString(), "plan", JsonLoader.fromString(newPlanObject.toJSONString()));
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean deactivatePlan(String id) {
		// TODO Auto-generated method stub

		// Check presence id REDIS
		if(jedis.exists(id+".plan")) {
			
			// Remove from REDIS
			Set<String> planKeys= jedis.keys(id);
			for(String key: planKeys) {
				jedis.del(key);
			}
			System.out.println("Plan removed from REDIS");
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public String createProfile(String newProfile) throws ParseException, IOException {
		
		System.out.println("Create Profile DAO: "+newProfile);
		
		// Create UUID
		UUID id= UUID.randomUUID();
		
		// Convert to JSONObject
		JSONParser parser= new JSONParser();
		JSONObject newProfileObject= (JSONObject) parser.parse(newProfile);
		
		// Add Creation Date parameter
		TimeZone tz= TimeZone.getTimeZone("UTC");
		DateFormat df= new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss'Z'");
		df.setTimeZone(tz);
		newProfileObject.put("creationDate", df.format(new Date()));
		newProfileObject.put("lastModifiedDate", df.format(new Date()));
		
		System.out.println("Map: "+newProfileObject); 
		
		// Save profile to redis
		saveToRedis(id.toString(), "profile", JsonLoader.fromString(newProfileObject.toJSONString()));
		
		return id.toString();
	}
	
	@Override
	public JSONObject getProfile(String id) {
		
		// Check if ID exists
		if(jedis.exists(id+".profile")) {
			
			// Get Profile, Convert to JSON Object
			Map<String, String> profile= jedis.hgetAll(id+".profile");
			System.out.println("Profile: "+profile);
			
			return convertToJson(profile);
		}
		
		return null;
	}


	@Override
	public boolean updateProfile(String id, String changedProfile) throws IOException, ParseException {
		// TODO Auto-generated method stub
		
		if(jedis.exists(id+".profile")) {
					
			// Delete previous version
			/*Set<String> profileKeys= jedis.keys(id);
			for(String key: profileKeys){
				jedis.del(key);
			}
			System.out.println("Previous version deleted");*/
			
			// Create new instance in Redis
			
			// Convert to JSONObject
			JSONParser parser= new JSONParser();
			JSONObject newProfileObject= (JSONObject) parser.parse(changedProfile);
			
			// Add Creation Date parameter
			TimeZone tz= TimeZone.getTimeZone("America/New_York");
			DateFormat df= new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
			df.setTimeZone(tz);
			newProfileObject.put("lastModifiedDate", df.format(new Date()));
			
			System.out.println("Map: "+newProfileObject);
		
			// Recursion to create stand-alone objects. 
			saveToRedis(id.toString(), "profile", JsonLoader.fromString(newProfileObject.toJSONString()));
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean deactivateProfile(String id) {
		// TODO Auto-generated method stub
		
		if(jedis.exists(id+".profile")) {
			
			// Remove from REDIS
			Set<String> profileKeys= jedis.keys(id);
			for(String key: profileKeys) {
				jedis.del(key);
			}
			System.out.println("Profile removed from REDIS");
			
			return true;
		}
		
		return false;
	}


	@Override
	public Set<String> getAllPlanKeys() {
		// TODO Auto-generated method stub
		
		System.out.println("Get All Plan Keys DAO");
		
		Set<String> planKeys= jedis.keys("*.plan");
			
		return planKeys;
	}
	
	@Override
	public Set<String> getAllProfileKeys() {
		// TODO Auto-generated method stub
		
		System.out.println("Get All Profile Keys DAO");
		
		Set<String> profileKeys= jedis.keys("*.profile");
			
		return profileKeys;
	}

	@Override
	public Set<String> search(String criteria) {
		// TODO Auto-generated method stub
		
		Set<String> plans= getAllPlanKeys();
		JSONObject plan= new JSONObject();
		
		Set<String> results= new HashSet<>();
		// Loop through each plan to match criteria
		for(String p: plans) {
			
			plan= getPlan(p);
			if(plan.containsValue(criteria))
				results.add(p);
		}
		
		return results;
	}


	@Override
	public boolean validateCredentials(String id, String password) {
		// TODO Auto-generated method stub
		
		if( jedis.exists(id+".profile") && jedis.hget(id+".profile", "password").equals(password) )
			return true;
		
		return false;
	}
	
	@Override
	public String getRole(String id) {
		
		if(jedis.exists(id+".profile"))
			return jedis.hget(id+".profile", "role");
		
		return null;
	}
	
	@Override
	public boolean unsubscribePlan(String id, String planId) {
		
		if( jedis.exists(id+".profile") && jedis.hexists(id+".profile", "subscribedPlans") ) {
			
			// Get All subscribed plan object keys
			Set<String> subscribedPlanObjectKeys= jedis.smembers(id+".subscribedPlans");
			
			// Loop through all keys
			if( !subscribedPlanObjectKeys.isEmpty() ) {
				
				for(String subscribedPlanObjectKey: subscribedPlanObjectKeys) {
					
					// Get Subscribed Plan object for given key
					Map<String, String> subscribedPlan= jedis.hgetAll(subscribedPlanObjectKey);
					JSONObject subscribedPlanObject= new JSONObject(subscribedPlan);
					
					// check if provided plan id is present in stored data
					if( subscribedPlanObject.get("planId").equals(planId) ) {
					
						// delete object from REDIS
						for(String key: subscribedPlan.keySet())
							jedis.hdel(subscribedPlanObjectKey, key);
						
						// delete key from subscribed plans set
						jedis.srem(id+".subscribedPlans", subscribedPlanObjectKey);
						return true;
					}
				}
			}
			
		}
		
		return false;
	}
	
	@Override
	public JSONObject subscribe(String profileId, String planId) throws IOException, ProcessingException {
		
		// Get Profile from Profile ID
		JSONObject profile= getProfile(profileId);
		JSONArray subscribedPlansArray= (JSONArray) profile.get("subscribedPlans");
		
		// Validate that Profile doesn't contain the provided planId
		if( subscribedPlansArray.isEmpty() || subscribedPlansArray == null ) {
			
			System.out.println("Profile has no subscriptions");
		} else {
			
			Iterator<String> iterator= subscribedPlansArray.iterator();
			while( iterator.hasNext() ) {
				
				String plans= iterator.next();
				if( planId.equals(plans) )
					return null;
			}
		}
			
			
		// Get Plan from Plan ID
		JSONObject plan= getPlan(planId);
		
		// Verify Plan and Put Plan ID in the Profile
		if(plan.isEmpty() || plan == null) 
			return null;
		else {
			
			subscribedPlansArray.add(planId);
			profile.put("subscribedPlans", subscribedPlansArray);
		}
		
		// Validate new object with Profile Schema
		final JsonNode profileSchema= JsonLoader.fromPath("D:/Courses/ABD/Workspace/Insurance/WebContent/WEB-INF/resources/profileSchema.json");
		
		final JsonSchemaFactory factory= JsonSchemaFactory.byDefault();
		final JsonSchema schema= factory.getJsonSchema(profileSchema);
		
		ProcessingReport report;
		report= schema.validate(JsonLoader.fromResource(profile.toJSONString()));
		
		if( report.isSuccess() ) {
			System.out.println("Report Successful");
			// Save new data to REDIS
			
			saveToRedis(profileId, "profile", JsonLoader.fromString(profile.toJSONString()));
			return profile;
		} else
			System.out.println(" FAILURE\n "+report);
		
		return null;
	}
	
	@Override
	public String savePlanSchema(String schema) throws IOException {
		
		System.out.println("Save Plan Schema:\n"+schema);
		Set<String> keys= jedis.keys("*.plan.schema");
		if( keys.size() > 0 ) {
			
			String uuid= "";
			for( String k: keys )
				uuid= k.substring(0, k.indexOf("."));
			
			Set<String> subKeys= jedis.keys(uuid+"*");
			for( String key: subKeys )
				jedis.del(key);
		}
		
		// Generate ID
		UUID id= UUID.randomUUID();
		
		// Save To REDIS
		final JsonNode planSchema= JsonLoader.fromPath("C:/Users/antri/Documents/workspace-sts-3.9.1.RELEASE/Insurance/WebContent/WEB-INF/resources/planSchema.json");
		saveSchema(id.toString(), "plan.schema", planSchema);
		
		return id.toString();
	}
	
	@Override
	public String saveProfileSchema(String schema) throws IOException {
		
		System.out.println("Save Profile Schema:\n"+schema);
		Set<String> keys= jedis.keys("*.profile.schema");
		if( keys.size() > 0 ) {
			
			String uuid= "";
			for( String k: keys )
				uuid= k.substring(0, k.indexOf("."));
			
			Set<String> subKeys= jedis.keys(uuid+"*");
			for( String key: subKeys )
				jedis.del(key);
		}
		
		// Generate ID
		UUID id= UUID.randomUUID();
		
		// Save To REDIS
		saveSchema(id.toString(), "profile.schema", JsonLoader.fromResource(schema));
		
		return id.toString();
	}
	
	@Override
	public JSONObject getSchema(String type) {
	
		System.out.println("Get Schema: "+type);
		
		Map<String, String> schema= new HashMap<>();
		if( type.equals("profile") ){
			
			Set<String> schemaKey= jedis.keys("*.profile.schema");
			System.out.println("Keys: "+schemaKey);
			for(String key: schemaKey)
				schema= jedis.hgetAll(key);
		} else {
			
			Set<String> schemaKey= jedis.keys("*.plan.schema");
			System.out.println("Keys: "+schemaKey);
			for(String key: schemaKey)
				schema= jedis.hgetAll(key);
		}
		System.out.println("Schema: "+schema);
		return convertToJson(schema);
	}

}
