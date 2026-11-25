package com.example.bookmanagement.borrowing;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record DueDateRequest(
        @NotNull(message = "应还日期不能为空")
        @Future(message = "应还日期必须晚于当前时间")
        Instant dueAt
) {
}
