package com.ow0b.c7b9.service.database.mapper;

import com.ow0b.c7b9.service.database.model.Audio;
import org.apache.ibatis.annotations.*;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface AudioMapper
{
    @Insert("INSERT INTO audios (uid, m4a, mp3) VALUES (#{uid}, #{m4a, jdbcType=BLOB}, #{mp3, jdbcType=BLOB})")
    @Options(useGeneratedKeys = true, keyProperty = "aid", keyColumn = "aid")   //返回插入的主键
    void insert(Audio audio);

    @Delete("DELETE FROM audios WHERE aid = #{aid}")
    void delete(int aid);

    @Select("SELECT * FROM audios WHERE aid = #{aid}")
    Audio getByAid(int aid);

    @Select("SELECT * FROM audios WHERE secret = #{secret}")
    Audio getBySecret(String secret);

    @Select("SELECT aid FROM audios WHERE secret = #{secret}")
    Integer getSecretAid(String secret);

    @Select("SELECT * FROM audios WHERE uid = #{uid} ORDER BY aid DESC")
    List<Audio> getAudios(int uid);

    @Update("UPDATE audios SET access = #{access}, secret = #{secret} WHERE aid = #{aid}")
    void setAccess(@Param("aid") int aid, @Param("access") Timestamp access, @Param("secret") String secret);

    @Update("UPDATE audios SET mid = #{mid, jdbcType=BLOB} WHERE aid = #{aid}")
    void setMid(@Param("aid") int aid, @Param("mid") byte[] mid);

    @Update("UPDATE audios SET content = #{content} WHERE aid = #{aid}")
    void setContent(@Param("aid") int aid, @Param("content") String content);

    @Update("UPDATE audios SET analysis = #{analysis} WHERE aid = #{aid}")
    void setAnalysis(@Param("aid") int aid, @Param("analysis") String analysis);
}
