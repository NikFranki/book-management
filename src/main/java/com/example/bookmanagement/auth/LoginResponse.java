package com.example.bookmanagement.auth;

import java.util.List;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, String username, List<String> roles) {
}
