package com.example.bookmanagement.book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public Page<Book> search(String keyword, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 不能小于 0，size 必须在 1 到 100 之间");
        }

        log.debug("开始执行分页查询，page={}, size={}", page, size);
        log.info("分页查询图书，keyword={}, page={}, size={}", keyword, page, size);
        var pageable = PageRequest.of(page, size);
        if (keyword == null || keyword.isBlank()) {
            return new PageImpl<>(bookRepository.findPage((long) page * size, size), pageable, bookRepository.countAll());
        }
        return new PageImpl<>(bookRepository.searchPage(keyword, (long) page * size, size), pageable,
                bookRepository.countSearch(keyword));
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        log.info("按 id 查询图书，id={}", id);
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在"));
    }

    @Transactional
    public Book create(Book book) {
        Book savedBook = bookRepository.save(book);
        log.info("新增图书成功，id={}", savedBook.getId());
        return savedBook;
    }

    @Transactional
    public Book update(Long id, Book updatedBook) {
        Book existingBook = findById(id);
        existingBook.setTitle(updatedBook.getTitle());
        existingBook.setAuthor(updatedBook.getAuthor());
        existingBook.setIsbn(updatedBook.getIsbn());
        existingBook.setPrice(updatedBook.getPrice());
        existingBook.setAvailableCopies(updatedBook.getAvailableCopies());

        Book savedBook = bookRepository.save(existingBook);
        log.info("修改图书成功，id={}", id);
        return savedBook;
    }

    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在");
        }

        bookRepository.deleteById(id);
        log.info("删除图书成功，id={}", id);
    }
}
