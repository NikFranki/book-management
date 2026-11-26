package com.example.bookmanagement.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface AppUserRepository {
    @Select("SELECT id, username, password_hash, user_role AS role FROM app_users WHERE username = #{username}")
    Optional<AppUser> findByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) > 0 FROM app_users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    @Insert("INSERT INTO app_users (username, password_hash, user_role) VALUES (#{username}, #{passwordHash}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AppUser user);

    default AppUser save(AppUser user) { insert(user); return user; }
}
