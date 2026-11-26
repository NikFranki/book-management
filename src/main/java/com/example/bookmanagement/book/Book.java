package com.example.bookmanagement.book;

import java.math.BigDecimal;

public class Book {

    private Long id;

    private String title;

    private String author;

    private String isbn;

    private BigDecimal price;

    // nullable=true 便于给已有 H2 数据库升级；读取旧记录时按 0 本处理。
    private Integer availableCopies = 0;

    public Book() {
    }

    public Book(String title, String author, String isbn, BigDecimal price, Integer availableCopies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.availableCopies = availableCopies;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getAvailableCopies() {
        return availableCopies == null ? 0 : availableCopies;
    }

    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }
}
