package com.insurance.dao;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import com.insurance.dao.interfaces.ElasticSearchDao;

public class ElasticSearchDaoImpl implements ElasticSearchDao {
	
	private RestHighLevelClient client;
	
	public ElasticSearchDaoImpl() {
		// TODO Auto-generated constructor stub
		RestClient restClient = RestClient.builder(
		        new HttpHost("localhost", 9200, "http"),
		        new HttpHost("localhost", 9201, "http")).build();
		client= new RestHighLevelClient(restClient);
	}

}
