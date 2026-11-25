package com.example.bookmanagement.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserDataInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String userPassword;

    public UserDataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin-password}") String adminPassword,
            @Value("${app.security.user-password}") String userPassword
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("admin", adminPassword, "ADMIN");
        createIfMissing("reader", userPassword, "USER");
        createIfMissing("reader1", userPassword, "USER");
    }

    private void createIfMissing(String username, String rawPassword, String role) {
        if (appUserRepository.findByUsername(username).isEmpty()) {
            appUserRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword), role));
        }
    }
}
