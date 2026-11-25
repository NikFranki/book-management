package com.example.bookmanagement.borrowing;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/borrow-records")
@Tag(name = "借阅管理")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @GetMapping
    public List<BorrowResponse> listBorrowRecords(Authentication authentication) {
        return borrowService.findAll(authentication.getName(), isAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowResponse borrow(@Valid @RequestBody BorrowRequest request, Authentication authentication) {
        return borrowService.borrow(request, authentication.getName());
    }

    @PostMapping("/{id}/return")
    public BorrowResponse returnBook(@PathVariable Long id, Authentication authentication) {
        return borrowService.returnBook(id, authentication.getName(), isAdmin(authentication));
    }

    @PutMapping("/{id}/due-date")
    public BorrowResponse updateDueDate(
            @PathVariable Long id,
            @Valid @RequestBody DueDateRequest request,
            Authentication authentication
    ) {
        return borrowService.updateDueDate(id, request.dueAt(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
