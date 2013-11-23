package com.mscharhag.solr;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@ComponentScan
@EnableSolrRepositories("com.mscharhag.solr.repository")
public class Application {

	@Bean
	public SolrServer solrServer() {
		return new HttpSolrServer("http://localhost:8983/solr");
	}

	@Bean
	public SolrTemplate solrTemplate(SolrServer server) throws Exception {
		return new SolrTemplate(server);
	}
}
