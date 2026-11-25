package com.example.bookmanagement.book;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record BookRequest(
        @NotBlank(message = "书名不能为空")
        @Size(max = 200, message = "书名不能超过 200 个字符")
        String title,

        @NotBlank(message = "作者不能为空")
        @Size(max = 100, message = "作者不能超过 100 个字符")
        String author,

        @NotBlank(message = "ISBN 不能为空")
        @Size(max = 20, message = "ISBN 不能超过 20 个字符")
        String isbn,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,

        @NotNull(message = "可借数量不能为空")
        @Min(value = 0, message = "可借数量不能小于 0")
        Integer availableCopies
) {

    public Book toEntity() {
        return new Book(title, author, isbn, price, availableCopies);
    }
}
