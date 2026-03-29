package com.ow0b.c7b9.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface TempDir
{
    Path temp = Paths.get("").toAbsolutePath().resolve("temp");

    default void saveCache(File file, byte @NotNull [] data)
    {
        //从数据库拷贝音频文件到本地
        try(FileOutputStream output = new FileOutputStream(file))
        {
            output.write(data);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    default void deleteCache(File file) throws RuntimeException, IOException
    {
        if(!file.delete()) LoggerFactory.getLogger(TempDir.class).info("缓存文件删除失败：{}", file);
    }
}
