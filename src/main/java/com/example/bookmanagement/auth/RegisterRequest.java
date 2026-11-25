package com.example.bookmanagement.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度需要在 3 到 50 个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能使用英文字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度需要在 8 到 72 个字符之间")
        String password
) {
}
