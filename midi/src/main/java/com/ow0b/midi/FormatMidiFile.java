package com.ow0b.midi;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FormatMidiFile
{
    public static void main(String[] args)
    {
        File file = new File("D:\\c7b9\\c7b9-server\\midis");
        try
        {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<>()
            {
                @NotNull @Override
                public FileVisitResult visitFile(Path libFile, @NotNull BasicFileAttributes attrs) throws IOException
                {
                    String name = libFile.getFileName().toString();
                    Files.move(libFile, libFile.getParent().resolve(name.substring(0, name.lastIndexOf(",")) + ".mid"));
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
