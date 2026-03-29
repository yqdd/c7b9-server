package com.ow0b.c7b9.service.database.mapper;

import com.ow0b.c7b9.service.database.model.User;
import org.apache.ibatis.annotations.*;

import java.sql.Timestamp;

@Mapper
public interface UserMapper
{
    @Insert("INSERT INTO users (username, password, conv, practice) VALUES (#{username}, #{password}, '{}', '{}')")
    void insert(@Param("username") String username, @Param("password") String password);

    @Select("SELECT * FROM users WHERE uid = #{uid}")
    User getByUid(int uid);

    @Select("SELECT * FROM users WHERE username = #{username}")
    User getByUsername(String username);

    @Select("SELECT * FROM users WHERE permit = #{permit}")
    User getByPermit(@Param("permit") String permit);

    @Update("UPDATE users SET username = #{username} WHERE uid = #{uid}")
    void setUsername(@Param("uid") int uid, @Param("username") String username);

    @Update("UPDATE users SET password = #{password} WHERE uid = #{uid}")
    void setPassword(@Param("uid") int uid, @Param("password") String password);



    @Select("SELECT random FROM users WHERE uid = #{uid}")
    String getRandomByUID(int uid);

    @Update("UPDATE users SET permit = #{permit}, random = #{random} WHERE uid = #{uid}")
    void setPermit(@Param("uid") int uid, @Param("permit") String permit, @Param("random") String random);

    @Update("UPDATE users SET token = #{token} WHERE uid = #{uid}")
    void setToken(@Param("uid") int uid, @Param("token") int token);

    @Update("UPDATE users SET conv = #{conv} WHERE uid = #{uid}")
    void setConversations(@Param("uid") int uid, @Param("conv") String conv);

    @Update("UPDATE users SET practice = #{practice} WHERE uid = #{uid}")
    void setPractice(@Param("uid") int uid, @Param("practice") String practice);

    @Update("UPDATE users SET attempt = #{attempt} WHERE uid = #{uid}")
    void setAttempt(@Param("uid") int uid, @Param("attempt") int attempt);

    @Update("UPDATE users SET timelimit = #{timelimit} WHERE uid = #{uid}")
    void setLimit(@Param("uid") int uid, @Param("timelimit") Timestamp timelimit);
}