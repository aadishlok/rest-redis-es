package com.insurance.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.insurance.controllers.AdminControllerImpl;
import com.insurance.controllers.AuthorizationControllerImpl;
import com.insurance.controllers.PatchControllerImpl;
import com.insurance.controllers.PlanControllerImpl;
import com.insurance.controllers.ProfileControllerImpl;
import com.insurance.filter.SecurityFilter;

public class MyApp extends Application{
	
	private Set<Object> singletons= new HashSet<Object>();
	
	public MyApp() {
		singletons.add(new PlanControllerImpl());
		singletons.add(new ProfileControllerImpl());
		singletons.add(new PatchControllerImpl());
		singletons.add(new SecurityFilter());
		singletons.add(new AuthorizationControllerImpl());
		singletons.add(new AdminControllerImpl());
	}
	
	@Override
	public Set<Object> getSingletons(){
		return singletons;
	}

}
