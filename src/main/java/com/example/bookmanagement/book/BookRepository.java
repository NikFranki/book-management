package com.example.bookmanagement.book;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BookRepository {
    @Select("SELECT id, title, author, isbn, price, COALESCE(available_copies, 0) AS available_copies FROM books ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<Book> findPage(@Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM books")
    long countAll();

    @Select("SELECT id, title, author, isbn, price, COALESCE(available_copies, 0) AS available_copies FROM books WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%')) OR LOWER(author) LIKE LOWER(CONCAT('%', #{keyword}, '%')) ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<Book> searchPage(@Param("keyword") String keyword, @Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM books WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%')) OR LOWER(author) LIKE LOWER(CONCAT('%', #{keyword}, '%'))")
    long countSearch(@Param("keyword") String keyword);

    @Select("SELECT id, title, author, isbn, price, COALESCE(available_copies, 0) AS available_copies FROM books WHERE id = #{id}")
    Optional<Book> findById(@Param("id") Long id);

    @Insert("INSERT INTO books (title, author, isbn, price, available_copies) VALUES (#{title}, #{author}, #{isbn}, #{price}, #{availableCopies})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Book book);

    @Update("UPDATE books SET title=#{title}, author=#{author}, isbn=#{isbn}, price=#{price}, available_copies=#{availableCopies} WHERE id=#{id}")
    int update(Book book);

    @Delete("DELETE FROM books WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) > 0 FROM books WHERE id = #{id}")
    boolean existsById(@Param("id") Long id);

    @Delete("DELETE FROM books")
    int deleteAll();

    @Select("SELECT COUNT(*) FROM books")
    long count();

    default Book save(Book book) {
        if (book.getId() == null) insert(book); else update(book);
        return book;
    }

    default Book saveAndFlush(Book book) { return save(book); }
}
