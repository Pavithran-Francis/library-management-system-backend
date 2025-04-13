package com.library.management.service;

import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Book;
import com.library.management.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class BookService {

    private final BookRepository bookRepository;

    @Autowired
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        log.info("Fetching all books");
        return bookRepository.findAll();
    }

    public List<Book> getAvailableBooks() {
        log.info("Fetching all available books");
        return bookRepository.findAllAvailableBooks();
    }

    public Book getBookById(Long id) {
        log.info("Fetching book with ID: {}", id);
        return bookRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Book not found with ID: {}", id);
                    return new ResourceNotFoundException("Book not found with ID: " + id);
                });
    }

    public Book getBookByIsbn(String isbn) {
        log.info("Fetching book with ISBN: {}", isbn);
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> {
                    log.error("Book not found with ISBN: {}", isbn);
                    return new ResourceNotFoundException("Book not found with ISBN: " + isbn);
                });
    }

    public List<Book> searchBooks(String keyword) {
        log.info("Searching books with keyword: {}", keyword);
        return bookRepository.searchBooks(keyword);
    }

    @Transactional
    public Book createBook(Book book) {
        log.info("Creating new book: {}", book.getTitle());

        // Check if ISBN already exists
        if (bookRepository.findByIsbn(book.getIsbn()).isPresent()) {
            log.error("ISBN already exists: {}", book.getIsbn());
            throw new IllegalArgumentException("ISBN already exists: " + book.getIsbn());
        }

        // Ensure available copies is set correctly
        if (book.getAvailableCopies() == 0) {
            book.setAvailableCopies(book.getTotalCopies());
        }

        Book savedBook = bookRepository.save(book);
        log.info("Book created successfully with ID: {}", savedBook.getId());
        return savedBook;
    }

    @Transactional
    public Book updateBook(Long id, Book bookDetails) {
        log.info("Updating book with ID: {}", id);

        Book book = getBookById(id);

        // Check if ISBN is being changed and already exists
        if (!book.getIsbn().equals(bookDetails.getIsbn()) &&
                bookRepository.findByIsbn(bookDetails.getIsbn()).isPresent()) {
            log.error("ISBN already exists: {}", bookDetails.getIsbn());
            throw new IllegalArgumentException("ISBN already exists: " + bookDetails.getIsbn());
        }

        // Update book details
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setIsbn(bookDetails.getIsbn());
        book.setPublicationDate(bookDetails.getPublicationDate());
        book.setDescription(bookDetails.getDescription());
        book.setGenre(bookDetails.getGenre());

        // Handle copy count updates carefully
        int oldTotal = book.getTotalCopies();
        int oldAvailable = book.getAvailableCopies();
        int newTotal = bookDetails.getTotalCopies();

        // Calculate borrowed copies
        int borrowed = oldTotal - oldAvailable;

        // Ensure we can't reduce total below what's already borrowed
        if (newTotal < borrowed) {
            log.error("Cannot reduce total copies below borrowed amount. {} copies are currently borrowed.", borrowed);
            throw new IllegalArgumentException("Cannot reduce total copies below borrowed amount. " + borrowed + " copies are currently borrowed.");
        }

        book.setTotalCopies(newTotal);
        book.setAvailableCopies(newTotal - borrowed);

        Book updatedBook = bookRepository.save(book);
        log.info("Book updated successfully: {}", updatedBook.getTitle());
        return updatedBook;
    }

    @Transactional
    public void deleteBook(Long id) {
        log.info("Deleting book with ID: {}", id);

        Book book = getBookById(id);

        // Check if there are ongoing loans
        if (book.getTotalCopies() > book.getAvailableCopies()) {
            log.error("Cannot delete book with outstanding loans");
            throw new IllegalStateException("Cannot delete book with outstanding loans. Please ensure all copies are returned first.");
        }

        bookRepository.delete(book);
        log.info("Book deleted successfully: {}", book.getTitle());
    }
}
