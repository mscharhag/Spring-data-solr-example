package com.mscharhag.solr.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.repository.Boost;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;

import com.mscharhag.solr.document.Book;

public interface BookRepository extends SolrCrudRepository<Book, String> {

	List<Book> findByName(String name);
	
	Page<Book> findByNameOrDescription(@Boost(2) String name, String description, Pageable pageable);

	@Query("name:?0")
	@Facet(fields = { "categories_txt" }, limit = 5)
	FacetPage<Book> findByNameAndFacetOnCategories(String name, Pageable page);
	
	@Highlight(prefix = "<highlight>", postfix = "</highlight>")
	HighlightPage<Book> findByDescription(String description, Pageable pageable);
	
}
