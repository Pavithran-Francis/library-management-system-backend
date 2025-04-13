package com.library.management.service;

import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Book;
import com.library.management.model.Loan;
import com.library.management.model.User;
import com.library.management.repository.BookRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Autowired
    public LoanService(LoanRepository loanRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public List<Loan> getAllLoans() {
        log.info("Fetching all loans");
        return loanRepository.findAll();
    }

    public Loan getLoanById(Long id) {
        log.info("Fetching loan with ID: {}", id);
        return loanRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Loan not found with ID: {}", id);
                    return new ResourceNotFoundException("Loan not found with ID: " + id);
                });
    }

    public List<Loan> getLoansByUser(Long userId) {
        log.info("Fetching loans for user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return loanRepository.findByUser(user);
    }

    public List<Loan> getCurrentLoansByUser(Long userId) {
        log.info("Fetching current loans for user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return loanRepository.findCurrentLoansByUser(user);
    }

    public List<Loan> getOverdueLoans() {
        log.info("Fetching all overdue loans");
        return loanRepository.findAllOverdueLoans();
    }

    @Transactional
    public Loan borrowBook(Long userId, Long bookId, LocalDate dueDate) {
        log.info("Processing loan: User ID {} borrowing Book ID {}", userId, bookId);

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Check if user is active
        if (!user.isActive()) {
            log.error("User account is inactive: {}", user.getUsername());
            throw new IllegalStateException("Cannot borrow books with an inactive account");
        }

        // Get book
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        // Check if book is available
        if (book.getAvailableCopies() <= 0) {
            log.error("Book is not available for borrowing: {}", book.getTitle());
            throw new IllegalStateException("Book is not available for borrowing: " + book.getTitle());
        }

        // Set default due date if not provided
        if (dueDate == null) {
            dueDate = LocalDate.now().plusWeeks(2); // Default loan period of 2 weeks
        }

        // Create loan
        Loan loan = Loan.builder()
                .user(user)
                .book(book)
                .loanDate(LocalDate.now())
                .dueDate(dueDate)
                .status(Loan.LoanStatus.BORROWED)
                .build();

        // Update book available copies
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Loan savedLoan = loanRepository.save(loan);
        log.info("Book borrowed successfully: '{}' by user '{}'", book.getTitle(), user.getUsername());
        return savedLoan;
    }

    @Transactional
    public Loan returnBook(Long loanId) {
        log.info("Processing book return for loan ID: {}", loanId);

        Loan loan = getLoanById(loanId);

        // Check if already returned
        if (loan.getReturnDate() != null) {
            log.error("Book already returned on: {}", loan.getReturnDate());
            throw new IllegalStateException("Book already returned on: " + loan.getReturnDate());
        }

        // Set return date
        loan.setReturnDate(LocalDate.now());

        // Force status to RETURNED - this is the critical fix
        loan.setStatus(Loan.LoanStatus.RETURNED);

        // Add note about late return if applicable
        if (LocalDate.now().isAfter(loan.getDueDate())) {
            long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            loan.setNotes("Returned " + daysOverdue + " days late");
        }

        // Update book available copies
        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        // Save the loan first
        loanRepository.save(loan);

        // Use the direct update method to ensure status is RETURNED
        loanRepository.updateLoanStatus(loanId, Loan.LoanStatus.RETURNED);

        log.info("Book '{}' returned by user '{}'. Status set to RETURNED",
                book.getTitle(), loan.getUser().getUsername());

        // Return the updated loan
        return getLoanById(loanId);
    }

    @Transactional
    public Loan renewLoan(Long loanId, LocalDate newDueDate) {
        log.info("Renewing loan with ID: {}", loanId);

        Loan loan = getLoanById(loanId);

        // Check if already returned
        if (loan.getReturnDate() != null) {
            log.error("Cannot renew a returned loan");
            throw new IllegalStateException("Cannot renew a returned loan");
        }

        // Check if overdue
        if (LocalDate.now().isAfter(loan.getDueDate())) {
            log.error("Cannot renew an overdue loan");
            throw new IllegalStateException("Cannot renew an overdue loan. Please return the book first");
        }

        // Set new due date
        if (newDueDate == null) {
            newDueDate = loan.getDueDate().plusWeeks(2); // Extend by 2 more weeks
        }

        loan.setDueDate(newDueDate);
        String notes = loan.getNotes();
        loan.setNotes((notes != null ? notes + "\n" : "") + "Loan renewed until: " + newDueDate);

        Loan updatedLoan = loanRepository.save(loan);
        log.info("Loan renewed successfully for book '{}' until {}", loan.getBook().getTitle(), newDueDate);
        return updatedLoan;
    }

    @Transactional
    public void reportLost(Long loanId) {
        log.info("Reporting book as lost for loan ID: {}", loanId);

        Loan loan = getLoanById(loanId);

        // Check if already returned
        if (loan.getReturnDate() != null) {
            log.error("Cannot report a returned book as lost");
            throw new IllegalStateException("Cannot report a returned book as lost");
        }

        // Update loan status
        loan.setStatus(Loan.LoanStatus.LOST);
        String notes = loan.getNotes();
        loan.setNotes((notes != null ? notes + "\n" : "") + "Book reported lost on: " + LocalDate.now());

        // Update book total copies
        Book book = loan.getBook();
        book.setTotalCopies(book.getTotalCopies() - 1);
        bookRepository.save(book);

        loanRepository.save(loan);
        loanRepository.updateLoanStatus(loanId, Loan.LoanStatus.LOST);

        log.warn("Book '{}' reported lost by user '{}'", book.getTitle(), loan.getUser().getUsername());
    }

    public List<Loan> getLoansDueSoon(int days) {
        log.info("Fetching loans due in the next {} days", days);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(days);
        return loanRepository.findLoansDueBetween(startDate, endDate);
    }
}