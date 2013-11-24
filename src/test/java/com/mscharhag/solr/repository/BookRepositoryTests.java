package com.mscharhag.solr.repository;

import static com.mscharhag.solr.document.Category.ADVENTURE;
import static com.mscharhag.solr.document.Category.EDUCATION;
import static com.mscharhag.solr.document.Category.HISTORY;
import static com.mscharhag.solr.document.Category.HUMOR;
import static com.mscharhag.solr.document.Category.ROMANCE;
import static com.mscharhag.solr.document.Category.TECHNOLOGY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.query.result.FacetEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mscharhag.solr.Application;
import com.mscharhag.solr.document.Book;
import com.mscharhag.solr.document.Category;
import com.mscharhag.solr.repository.BookRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class, loader=SpringApplicationContextLoader.class)
public class BookRepositoryTests {
	
	@Autowired
	private BookRepository bookRepository;
	
	
	@Before
	public void beforeClass() {
		createSampleData();
	}
	
	
	@Test
	public void findByName() {
		List<Book> books = bookRepository.findByName("Island");
		
		// the following book names contain the string "Island": 01, 02, 05, 08
		assertEquals(books.size(), 4);
		assertTrue(containsBooksWithIds(books, "01", "02", "05", "08"));
	}
	
	
	@Test
	public void findByNameOrDescription() {
		Page<Book> booksPage = bookRepository.findByNameOrDescription("Island", "Island", new PageRequest(0, 10));
		
		// the following books contain the string "island" in their name or description: 01, 02, 05, 07, 08
		List<Book> books = booksPage.getContent();
		assertEquals(5, books.size());
		assertTrue(containsBooksWithIds(books, "01", "02", "05", "07", "08"));
		
		// If the book name contains the string "Island" within the name, the book ranking is boosted by 2 (see @Boost in repository method)
		// Book with ID 07 is the only book which name does not contain "Island" so it should be ranked last
		assertEquals("07", books.get(4).getId());
	}
	
	
	@Test
	public void findByNameAndFacetOnCategories() {
		FacetPage<Book> booksFacetPage = bookRepository.findByNameAndFacetOnCategories("Island", new PageRequest(0, 2));

		// There are 4 books which contain "Island" in their name. However, only the first 2 books are returned because 
		// a page with size 2 is requested. The next two books could be requested using "new PageRequest(1, 2)" 
		
		assertEquals(2, booksFacetPage.getNumberOfElements());
		
		// 3 of these 4 books are categorized as Adventure
		// 2 of these 4 books are categorized as Humor
		// 1 of these 4 books is categorized as Romance
		Map<Category, Long> categoryFacetCounts = getCategoryFacetCounts(booksFacetPage);
		assertEquals(new Long(3), categoryFacetCounts.get(ADVENTURE));
		assertEquals(new Long(2), categoryFacetCounts.get(HUMOR));
		assertEquals(new Long(1), categoryFacetCounts.get(ROMANCE));
	}
	
	
	@Test
	public void findByDescription() {
		HighlightPage<Book> booksHighlightPage = bookRepository.findByDescription("cookies", new PageRequest(0, 10));
		booksHighlightPage.getContent();
		assertTrue(containsSnipplet(booksHighlightPage, "How to handle <highlight>cookies</highlight> in web applications"));
		assertTrue(containsSnipplet(booksHighlightPage, "Bake your own <highlight>cookies</highlight>, on a secret island!"));
	}
	
	
	private boolean containsSnipplet(HighlightPage<Book> booksHighlightPage, String snippletToCheck) {
		for (HighlightEntry<Book> he : booksHighlightPage.getHighlighted()) {
			// A HighlightEntry belongs to an Entity (Book) and may have multiple highlighted fields (description)
			for (Highlight highlight : he.getHighlights()) {
				for (String snipplet : highlight.getSnipplets()) {
					
					if (snipplet.equals(snippletToCheck)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	
	private void createSampleData() {
		if (bookRepository.findOne("01") != null) {
			System.out.println("Not adding test data to solr index. Data already exists");
			return;
		}
		addBookToIndex("01", "Treasure Island",     "Best seller by R.L.S.",                             ADVENTURE);
		addBookToIndex("02", "Treasure Island 2.0", "Humorous remake of the famous best seller",         ADVENTURE, HUMOR);
		addBookToIndex("03", "Solr for dummies",    "Get started with solr",                             EDUCATION, HUMOR, TECHNOLOGY);
		addBookToIndex("04", "Moon landing",        "All facts about Apollo 11, a best seller",          HISTORY, EDUCATION);
		addBookToIndex("05", "Spring Island",       "The perfect island romance..",                      ROMANCE);
		addBookToIndex("06", "Refactoring",         "It's about improving the design of existing code.", TECHNOLOGY);
		addBookToIndex("07", "Baking for dummies",  "Bake your own cookies, on a secret island!",        EDUCATION, HUMOR);
		addBookToIndex("08", "The Pirate Island",   "Oh noes, the pirates are coming!",                  ADVENTURE, HUMOR);
		addBookToIndex("09", "Blackbeard",          "It's the pirate Edward Teach!",                     ADVENTURE, HISTORY);
		addBookToIndex("10", "Handling Cookies",    "How to handle cookies in web applications",         TECHNOLOGY);
	}
	
	
	private void addBookToIndex(String id, String name, String description, Category... categories) {
		Book book = new Book();
		book.setName(name);
		book.setDescription(description);
		book.setCategories(Arrays.asList(categories));
		book.setId(id);
		bookRepository.save(book);
		System.out.println("Added book with id " + id + " to index");
	}
	
	
	private Map<Category, Long> getCategoryFacetCounts(FacetPage<Book> booksFacetPage) {
		Map<Category, Long> facetCounts = new HashMap<Category, Long>();
		for (Page<? extends FacetEntry> p : booksFacetPage.getAllFacets()) {
			for (FacetEntry facetEntry : p.getContent()) {
				Category category = Category.valueOf(facetEntry.getValue().toUpperCase());
				facetCounts.put(category, facetEntry.getValueCount());
			}
		}
		return facetCounts;
	}

	
	private boolean containsBooksWithIds(List<Book> books, String... idsToCheck) {
		String[] bookIds = new String[books.size()];
		for (int i = 0; i < books.size(); i++) {
			bookIds[i] = books.get(i).getId();
		}
		Arrays.sort(bookIds);
		Arrays.sort(idsToCheck);
		return Arrays.equals(bookIds, idsToCheck);
	}

}
