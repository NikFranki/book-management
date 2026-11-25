package com.example.bookmanagement.borrowing;

import java.time.Instant;

public record BorrowResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String borrowerName,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        BorrowStatus status
) {

    public static BorrowResponse from(BorrowRecord record) {
        return new BorrowResponse(
                record.getId(),
                record.getBook().getId(),
                record.getBook().getTitle(),
                record.getBorrowerName(),
                record.getBorrowedAt(),
                record.getDueAt(),
                record.getReturnedAt(),
                record.getStatus()
        );
    }
}
