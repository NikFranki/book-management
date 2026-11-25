package com.example.bookmanagement.borrowing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BorrowRequest(
        @NotNull(message = "图书 ID 不能为空")
        @Positive(message = "图书 ID 必须大于 0")
        Long bookId
) {
}
