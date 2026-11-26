package com.example.bookmanagement.borrowing;

import com.example.bookmanagement.book.Book;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BorrowRepository {
    String COLUMNS = "SELECT br.id, br.book_id, br.borrower_name, br.borrowed_at, br.due_at, br.returned_at, b.id AS b_id, b.title AS b_title, b.author AS b_author, b.isbn AS b_isbn, b.price AS b_price, COALESCE(b.available_copies, 0) AS b_available_copies FROM borrow_records br JOIN books b ON b.id = br.book_id ";

    @Select(COLUMNS + "ORDER BY br.borrowed_at DESC")
    @Results(id = "borrowRecord", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "borrowerName", column = "borrower_name"),
            @Result(property = "borrowedAt", column = "borrowed_at"),
            @Result(property = "dueAt", column = "due_at"),
            @Result(property = "returnedAt", column = "returned_at"),
            @Result(property = "book.id", column = "b_id"),
            @Result(property = "book.title", column = "b_title"),
            @Result(property = "book.author", column = "b_author"),
            @Result(property = "book.isbn", column = "b_isbn"),
            @Result(property = "book.price", column = "b_price"),
            @Result(property = "book.availableCopies", column = "b_available_copies")
    })
    List<BorrowRecord> findAllByOrderByBorrowedAtDesc();

    @Select(COLUMNS + "WHERE br.borrower_name = #{borrowerName} ORDER BY br.borrowed_at DESC")
    @ResultMap("borrowRecord")
    List<BorrowRecord> findAllByBorrowerNameOrderByBorrowedAtDesc(@Param("borrowerName") String borrowerName);

    @Select(COLUMNS + "WHERE br.id = #{id}")
    @ResultMap("borrowRecord")
    Optional<BorrowRecord> findById(@Param("id") Long id);

    @Insert("INSERT INTO borrow_records (book_id, borrower_name, borrowed_at, due_at, returned_at) VALUES (#{book.id}, #{borrowerName}, #{borrowedAt}, #{dueAt}, #{returnedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BorrowRecord record);

    @Update("UPDATE borrow_records SET due_at=#{dueAt}, returned_at=#{returnedAt} WHERE id=#{id}")
    int update(BorrowRecord record);

    @Select("SELECT COUNT(*) FROM borrow_records")
    long count();

    @Delete("DELETE FROM borrow_records")
    int deleteAll();

    default BorrowRecord save(BorrowRecord record) {
        if (record.getId() == null) insert(record); else update(record);
        return record;
    }

    default BorrowRecord saveAndFlush(BorrowRecord record) { return save(record); }
}
