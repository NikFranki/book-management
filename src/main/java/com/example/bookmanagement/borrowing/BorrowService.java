package com.example.bookmanagement.borrowing;

import com.example.bookmanagement.book.Book;
import com.example.bookmanagement.book.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
public class BorrowService {

    private static final Logger log = LoggerFactory.getLogger(BorrowService.class);

    private final BorrowRepository borrowRepository;
    private final BookRepository bookRepository;

    public BorrowService(BorrowRepository borrowRepository, BookRepository bookRepository) {
        this.borrowRepository = borrowRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<BorrowResponse> findAll(String username, boolean admin) {
        List<BorrowRecord> records = admin
                ? borrowRepository.findAllByOrderByBorrowedAtDesc()
                : borrowRepository.findAllByBorrowerNameOrderByBorrowedAtDesc(username);
        return records.stream()
                .map(BorrowResponse::from)
                .toList();
    }

    @Transactional
    public BorrowResponse borrow(BorrowRequest request, String borrowerName) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在"));

        if (book.getAvailableCopies() < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前图书没有可借库存");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Instant borrowedAt = Instant.now();
        Instant dueAt = borrowedAt.plus(Duration.ofDays(BorrowRecord.BORROW_PERIOD_DAYS));
        BorrowRecord record = borrowRepository.save(new BorrowRecord(book, borrowerName, borrowedAt, dueAt));
        log.info("借书成功，bookId={}, borrowRecordId={}", book.getId(), record.getId());
        return BorrowResponse.from(record);
    }

    @Transactional
    public BorrowResponse returnBook(Long recordId, String username, boolean admin) {
        BorrowRecord record = borrowRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "借阅记录不存在"));

        if (!admin && !Objects.equals(record.getBorrowerName(), username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能归还自己的借阅记录");
        }

        if (record.getReturnedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "这条借阅记录已经归还");
        }

        record.setReturnedAt(Instant.now());
        Book book = record.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        BorrowRecord savedRecord = borrowRepository.save(record);
        log.info("还书成功，bookId={}, borrowRecordId={}", book.getId(), recordId);
        return BorrowResponse.from(savedRecord);
    }

    @Transactional
    public BorrowResponse updateDueDate(Long recordId, Instant dueAt, boolean admin) {
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员可以调整应还日期");
        }

        BorrowRecord record = borrowRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "借阅记录不存在"));

        if (record.getReturnedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已归还的记录不能调整应还日期");
        }

        if (dueAt == null || !dueAt.isAfter(Instant.now()) || !dueAt.isAfter(record.getBorrowedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "应还日期必须晚于当前时间和借出时间");
        }

        record.setDueAt(dueAt);
        return BorrowResponse.from(borrowRepository.save(record));
    }
}
