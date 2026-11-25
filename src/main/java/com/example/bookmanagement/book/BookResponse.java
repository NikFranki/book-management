package com.example.bookmanagement.book;

import java.math.BigDecimal;

public record BookResponse(Long id, String title, String author, String isbn, BigDecimal price, Integer availableCopies) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPrice(),
                book.getAvailableCopies()
        );
    }
}
