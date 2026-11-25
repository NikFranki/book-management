package com.example.bookmanagement.borrowing;

import com.example.bookmanagement.book.Book;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "borrow_records")
public class BorrowRecord {

    public static final long BORROW_PERIOD_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    private String borrowerName;

    private Instant borrowedAt;

    private Instant dueAt;

    private Instant returnedAt;

    protected BorrowRecord() {
    }

    public BorrowRecord(Book book, String borrowerName, Instant borrowedAt) {
        this.book = book;
        this.borrowerName = borrowerName;
        this.borrowedAt = borrowedAt;
    }

    public BorrowRecord(Book book, String borrowerName, Instant borrowedAt, Instant dueAt) {
        this.book = book;
        this.borrowerName = borrowerName;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        // Existing records predate the dueAt column; preserve their 30-day borrowing period.
        return dueAt != null ? dueAt : borrowedAt.plus(java.time.Duration.ofDays(BORROW_PERIOD_DAYS));
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public BorrowStatus getStatus() {
        if (returnedAt != null) {
            return BorrowStatus.RETURNED;
        }
        return getDueAt().isBefore(Instant.now()) ? BorrowStatus.OVERDUE : BorrowStatus.BORROWING;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }
}
