package com.example.bookmanagement.borrowing;

import com.example.bookmanagement.book.Book;
import com.example.bookmanagement.book.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:borrow-service-tests;DB_CLOSE_DELAY=-1",
        "logging.file.name="
})
class BorrowServiceIntegrationTests {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @BeforeEach
    void clearBorrowingData() {
        borrowRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    void borrowingBookReducesAvailableCopiesAndCreatesActiveRecord() {
        Book book = saveBook(2);

        BorrowResponse response = borrowService.borrow(new BorrowRequest(book.getId()), "reader-a");

        assertEquals(book.getId(), response.bookId());
        assertEquals("reader-a", response.borrowerName());
        assertNull(response.returnedAt());
        assertNotNull(response.borrowedAt());
        assertEquals(response.borrowedAt().plus(Duration.ofDays(BorrowRecord.BORROW_PERIOD_DAYS)), response.dueAt());
        assertEquals(BorrowStatus.BORROWING, response.status());
        assertEquals(1, bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies());
        assertEquals(1, borrowRepository.count());
    }

    @Test
    void returningBookRestoresAvailableCopiesAndMarksRecordReturned() {
        Book book = saveBook(1);
        BorrowResponse borrowed = borrowService.borrow(new BorrowRequest(book.getId()), "reader-a");

        BorrowResponse returned = borrowService.returnBook(borrowed.id(), "reader-a", false);

        assertNotNull(returned.returnedAt());
        assertEquals(BorrowStatus.RETURNED, returned.status());
        assertEquals(1, bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies());
        assertNotNull(borrowRepository.findById(borrowed.id()).orElseThrow().getReturnedAt());
    }

    @Test
    void borrowingBookWithNoAvailableCopiesReturnsConflictWithoutCreatingRecord() {
        Book book = saveBook(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> borrowService.borrow(new BorrowRequest(book.getId()), "reader-a")
        );

        assertEquals(CONFLICT, exception.getStatusCode());
        assertEquals(0, borrowRepository.count());
        assertEquals(0, bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies());
    }

    @Test
    void returningAnotherReadersRecordIsForbiddenAndDoesNotChangeInventory() {
        Book book = saveBook(1);
        BorrowResponse borrowed = borrowService.borrow(new BorrowRequest(book.getId()), "reader-a");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> borrowService.returnBook(borrowed.id(), "reader-b", false)
        );

        assertEquals(FORBIDDEN, exception.getStatusCode());
        assertEquals(0, bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies());
        assertNull(borrowRepository.findById(borrowed.id()).orElseThrow().getReturnedAt());
    }

    @Test
    void returningSameRecordTwiceDoesNotIncreaseInventoryTwice() {
        Book book = saveBook(1);
        BorrowResponse borrowed = borrowService.borrow(new BorrowRequest(book.getId()), "reader-a");
        borrowService.returnBook(borrowed.id(), "reader-a", false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> borrowService.returnBook(borrowed.id(), "reader-a", false)
        );

        assertEquals(CONFLICT, exception.getStatusCode());
        assertEquals(1, bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies());
        assertTrue(borrowRepository.findById(borrowed.id()).orElseThrow().getReturnedAt() != null);
    }

    @Test
    void activeRecordPastDueDateIsMarkedOverdue() {
        Book book = saveBook(0);
        Instant borrowedAt = Instant.now().minus(Duration.ofDays(BorrowRecord.BORROW_PERIOD_DAYS + 1));
        BorrowRecord record = borrowRepository.saveAndFlush(new BorrowRecord(
                book,
                "reader-a",
                borrowedAt,
                borrowedAt.plus(Duration.ofDays(BorrowRecord.BORROW_PERIOD_DAYS))
        ));

        BorrowResponse response = BorrowResponse.from(record);

        assertEquals(BorrowStatus.OVERDUE, response.status());
    }

    @Test
    void legacyRecordWithoutStoredDueDateUsesTheDefaultBorrowingPeriod() {
        Book book = saveBook(0);
        Instant borrowedAt = Instant.now().minus(Duration.ofDays(5));
        BorrowRecord legacyRecord = borrowRepository.saveAndFlush(new BorrowRecord(book, "reader-a", borrowedAt));

        assertEquals(borrowedAt.plus(Duration.ofDays(BorrowRecord.BORROW_PERIOD_DAYS)), legacyRecord.getDueAt());
    }

    @Test
    void adminCanEditDueDateAndAnOverdueRecordBecomesActiveAgain() {
        Book book = saveBook(1);
        Instant borrowedAt = Instant.now().minus(Duration.ofDays(BorrowRecord.BORROW_PERIOD_DAYS + 2));
        BorrowRecord record = borrowRepository.saveAndFlush(new BorrowRecord(
                book,
                "reader-a",
                borrowedAt,
                borrowedAt.plus(Duration.ofDays(BorrowRecord.BORROW_PERIOD_DAYS))
        ));
        assertEquals(BorrowStatus.OVERDUE, record.getStatus());
        Instant newDueAt = Instant.now().plus(Duration.ofDays(14));

        BorrowResponse updated = borrowService.updateDueDate(record.getId(), newDueAt, true);

        assertEquals(newDueAt, updated.dueAt());
        assertEquals(BorrowStatus.BORROWING, updated.status());
    }

    @Test
    void readerCannotEditDueDate() {
        Book book = saveBook(1);
        BorrowResponse borrowed = borrowService.borrow(new BorrowRequest(book.getId()), "reader-a");
        Instant originalDueAt = borrowRepository.findById(borrowed.id()).orElseThrow().getDueAt();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> borrowService.updateDueDate(borrowed.id(), Instant.now().plus(Duration.ofDays(60)), false)
        );

        assertEquals(FORBIDDEN, exception.getStatusCode());
        assertEquals(originalDueAt, borrowRepository.findById(borrowed.id()).orElseThrow().getDueAt());
    }

    @Test
    void returnedRecordCannotHaveDueDateEdited() {
        Book book = saveBook(1);
        BorrowResponse borrowed = borrowService.borrow(new BorrowRequest(book.getId()), "reader-a");
        borrowService.returnBook(borrowed.id(), "reader-a", false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> borrowService.updateDueDate(borrowed.id(), Instant.now().plus(Duration.ofDays(60)), true)
        );

        assertEquals(CONFLICT, exception.getStatusCode());
    }

    @Test
    void dueDateMustBeInTheFuture() {
        Book book = saveBook(1);
        BorrowResponse borrowed = borrowService.borrow(new BorrowRequest(book.getId()), "reader-a");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> borrowService.updateDueDate(borrowed.id(), Instant.now().minusSeconds(1), true)
        );

        assertEquals(BAD_REQUEST, exception.getStatusCode());
    }

    private Book saveBook(int availableCopies) {
        return bookRepository.saveAndFlush(new Book(
                "测试书籍",
                "测试作者",
                "ISBN-TEST-001",
                new BigDecimal("39.90"),
                availableCopies
        ));
    }
}
