package com.example.bookmanagement.borrowing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findAllByOrderByBorrowedAtDesc();

    List<BorrowRecord> findAllByBorrowerNameOrderByBorrowedAtDesc(String borrowerName);
}
