package com.ow0b.c7b9.service.database.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

@ToString
@Entity
@Table(name = "audios")
@NoArgsConstructor
public class Audio
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int aid;

    @Column(nullable = false)
    @Getter
    private int uid;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Getter
    private Timestamp access;

    @Column(columnDefinition = "VARCHAR(255)")
    @Getter(onMethod_ = @Nullable)
    private String secret;

    @Column(columnDefinition = "MEDIUMBLOB")
    @Getter(onMethod_ = @Nullable)
    private byte[] m4a;     //手机端播放需要这个格式，用mp3无法播放

    @Column(columnDefinition = "MEDIUMBLOB")
    @Getter(onMethod_ = @Nullable)
    private byte[] mp3;

    @Column(columnDefinition = "MEDIUMBLOB")
    @Getter(onMethod_ = @Nullable)
    private byte[] mid;

    @Column(columnDefinition = "LONGTEXT")
    @Getter(onMethod_ = @Nullable)
    private String content;

    @Column(columnDefinition = "JSON")
    @ColumnDefault("NULL")
    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(onMethod_ = @Nullable)
    private String analysis;

    @ManyToOne
    @JoinColumn(name = "uid", referencedColumnName = "uid", insertable = false, updatable = false)
    private User user;


    public Audio(int uid, byte[] m4a, byte[] mp3)
    {
        this.uid = uid;
        this.m4a = m4a;
        this.mp3 = mp3;
    }
}
