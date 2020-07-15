package bookstore.web;

import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import bookstore.service.rest.BookStoreService;

@SpringBootApplication
public class BookStoreWebApplication {
	public static void main(String[] args) {
		SpringApplication.run(BookStoreWebApplication.class, args);
	}

	@Bean
	public BookStoreService bookStoreService(@Value("${bookstore.rest.url}") String bookStoreServiceUrl)
			throws IllegalArgumentException, URISyntaxException {
		BookStoreService bookStoreService = new BookStoreService(bookStoreServiceUrl);

		return bookStoreService;
	}

}
