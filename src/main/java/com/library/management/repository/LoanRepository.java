package com.library.management.repository;

import com.library.management.model.Book;
import com.library.management.model.Loan;
import com.library.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUser(User user);

    List<Loan> findByBook(Book book);

    List<Loan> findByStatus(Loan.LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.returnDate IS NULL AND l.dueDate < CURRENT_DATE")
    List<Loan> findAllOverdueLoans();

    @Query("SELECT l FROM Loan l WHERE l.user = ?1 AND l.status = com.library.management.model.Loan$LoanStatus.BORROWED")
    List<Loan> findCurrentLoansByUser(User user);

    @Query("SELECT l FROM Loan l WHERE l.dueDate BETWEEN ?1 AND ?2")
    List<Loan> findLoansDueBetween(LocalDate startDate, LocalDate endDate);

    // Direct update method for loan status
    @Modifying
    @Transactional
    @Query("UPDATE Loan l SET l.status = :status WHERE l.id = :id")
    int updateLoanStatus(@Param("id") Long id, @Param("status") Loan.LoanStatus status);
}