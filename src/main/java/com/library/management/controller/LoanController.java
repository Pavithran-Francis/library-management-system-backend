package com.library.management.controller;

import com.library.management.model.Loan;
import com.library.management.service.LoanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class LoanController {

    private final LoanService loanService;

    @Autowired
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public ResponseEntity<List<Loan>> getAllLoans() {
        log.debug("REST request to get all loans");
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        log.debug("REST request to get loan by ID: {}", id);
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Loan>> getLoansByUser(@PathVariable Long userId) {
        log.debug("REST request to get loans for user ID: {}", userId);
        return ResponseEntity.ok(loanService.getLoansByUser(userId));
    }

    @GetMapping("/user/{userId}/current")
    public ResponseEntity<List<Loan>> getCurrentLoansByUser(@PathVariable Long userId) {
        log.debug("REST request to get current loans for user ID: {}", userId);
        return ResponseEntity.ok(loanService.getCurrentLoansByUser(userId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Loan>> getOverdueLoans() {
        log.debug("REST request to get overdue loans");
        return ResponseEntity.ok(loanService.getOverdueLoans());
    }

    @GetMapping("/due-soon")
    public ResponseEntity<List<Loan>> getLoansDueSoon(@RequestParam(defaultValue = "7") int days) {
        log.debug("REST request to get loans due in the next {} days", days);
        return ResponseEntity.ok(loanService.getLoansDueSoon(days));
    }

    @PostMapping("/borrow")
    public ResponseEntity<Loan> borrowBook(
            @RequestParam Long userId,
            @RequestParam Long bookId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        log.debug("REST request to borrow book: userID={}, bookID={}, dueDate={}", userId, bookId, dueDate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.borrowBook(userId, bookId, dueDate));
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<Loan> returnBook(@PathVariable Long id) {
        log.debug("REST request to return book for loan ID: {}", id);
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    @PutMapping("/{id}/renew")
    public ResponseEntity<Loan> renewLoan(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDueDate) {
        log.debug("REST request to renew loan ID: {} until {}", id, newDueDate);
        return ResponseEntity.ok(loanService.renewLoan(id, newDueDate));
    }

    @PutMapping("/{id}/lost")
    public ResponseEntity<Void> reportLost(@PathVariable Long id) {
        log.debug("REST request to report book as lost for loan ID: {}", id);
        loanService.reportLost(id);
        return ResponseEntity.ok().build();
    }
}