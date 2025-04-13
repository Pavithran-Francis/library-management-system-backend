package com.library.management;

import com.library.management.model.Book;
import com.library.management.model.User;
import com.library.management.repository.BookRepository;
import com.library.management.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

@SpringBootApplication
@Slf4j
public class LibraryManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementApplication.class, args);
		log.info("Library Management System started successfully!");
	}

	/**
	 * If no data.sql or if you want additional programmatic data loading,
	 * this CommandLineRunner will run on startup to add sample data.
	 * Only runs in "dev" profile.
	 */
	@Bean
	@Profile("dev")
	public CommandLineRunner demoData(UserRepository userRepository, BookRepository bookRepository) {
		return args -> {
			log.info("Loading sample data...");

			// Only add sample data if repositories are empty
			if (userRepository.count() == 0) {
				log.info("Adding sample users...");

				User admin = User.builder()
						.name("Admin User")
						.email("admin@library.com")
						.phone("555-ADMIN")
						.username("admin")
						.password("adminpass")
						.membershipType("ADMIN")
						.active(true)
						.build();

				userRepository.save(admin);
				log.info("Added sample admin user");
			}

			if (bookRepository.count() == 0) {
				log.info("Adding sample books...");

				Book book = Book.builder()
						.title("Clean Code")
						.author("Robert C. Martin")
						.isbn("9780132350884")
						.publicationDate(LocalDate.of(2008, 8, 1))
						.description("A handbook of agile software craftsmanship")
						.genre("Technology")
						.totalCopies(3)
						.availableCopies(3)
						.build();

				bookRepository.save(book);
				log.info("Added sample book");
			}

			log.info("Sample data loading complete!");
		};
	}
}