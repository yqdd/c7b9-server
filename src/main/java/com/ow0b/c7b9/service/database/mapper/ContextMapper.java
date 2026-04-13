package com.ow0b.c7b9.service.database.mapper;

import com.ow0b.c7b9.service.database.model.Context;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ContextMapper
{
    @Insert("INSERT INTO contexts (uid, data) VALUES (#{uid}, '[]')")
    @Options(useGeneratedKeys = true, keyProperty = "sid", keyColumn = "sid")   //返回插入的主键
    void insert(Context context);

    @Update("""
        UPDATE users SET conv = JSON_INSERT(conv, CONCAT('$."', #{sid}, '"'), '新对话') WHERE uid = #{uid}
        """)
    void updateConversation(@Param("uid") int uid, @Param("sid") int sid);

    @Select("SELECT * FROM contexts WHERE sid = #{sid}")
    Context getContext(int sid);

    @Delete("DELETE FROM contexts WHERE sid = #{sid}")
    void deleteContext(int sid);

    @Select("SELECT * FROM contexts WHERE uid = #{uid} ORDER BY timestamp ASC")
    List<Context> getContexts(int uid);

    @Select("SELECT conv FROM users WHERE uid = #{uid}")
    String getConversations(int uid);

    @Update("UPDATE contexts SET data = #{data} WHERE sid = #{sid}")
    void setContextData(@Param("sid") int sid, @Param("data") String data);

    @Update("UPDATE users SET conv = #{conv} WHERE uid = #{uid}")
    void setConversation(@Param("uid") int uid, @Param("conv") String conv);
}