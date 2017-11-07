package com.insurance.dao.interfaces;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public interface InsuranceDao {
	
	// Plan Functions
	
	public String createPlan(String newPlan) throws IOException, ParseException;
	
	public JSONObject getPlan(String id);
	
	public boolean updatePlan(String id, String changedPlan) throws IOException, ParseException;
	
	public boolean deactivatePlan(String id);
	
	// Profile Functions
	
	public String createProfile(String newProfile) throws ParseException, IOException;
	
	public JSONObject getProfile(String id);
	
	public boolean updateProfile(String id, String changedProfile) throws IOException, ParseException;
	
	public boolean deactivateProfile(String id);
	
	public boolean unsubscribePlan(String id, String planId);
	
	public JSONObject subscribe(String id, String planId) throws IOException, ProcessingException;
	
	public String getRole(String id);
	
	// Utility Functions
	
	public JSONObject convertToJson(Map<String, String> storedPlan);
	
	public void saveToRedis(String id, String type, JsonNode newPlan);
	
	public boolean checkExistingSubscribedPlans(String id, JsonNode changedSubscription);
	
	public Map<String, String> getSubscribedPlan(String id);
	
	// Public Functions
	
	public Set<String> getAllPlanKeys();
	
	public Set<String> search(String criteria);
	
	public boolean validateCredentials(String username, String password);

	public Set<String> getAllProfileKeys();
	
	// Admin Functions
	
	public String savePlanSchema(String schema) throws IOException;
	
	public String saveProfileSchema(String schema) throws IOException;
	
	public JSONObject getSchema(String type);
}
