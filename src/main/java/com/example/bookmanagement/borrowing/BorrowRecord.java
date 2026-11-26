package com.example.bookmanagement.borrowing;

import com.example.bookmanagement.book.Book;

import java.time.Instant;

public class BorrowRecord {

    public static final long BORROW_PERIOD_DAYS = 30;

    private Long id;

    private Book book;

    private String borrowerName;

    private Instant borrowedAt;

    private Instant dueAt;

    private Instant returnedAt;

    public BorrowRecord() {
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
