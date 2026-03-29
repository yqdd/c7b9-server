package com.ow0b.c7b9.service.database.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

@ToString
@Entity
@Table(name = "users")
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private int uid;

    @Column(nullable = false, length = 16)
    @Getter(onMethod_ = @NotNull)
    private String username;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    @Getter(onMethod_ = @NotNull)
    private String password;

    @Column(columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(onMethod_ = @NotNull)
    private String conv;

    @Column(columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(onMethod_ = @NotNull)
    private String practice;

    @Column
    @ColumnDefault("0")
    @Getter
    private int token;

    @Column(columnDefinition = "VARCHAR(255)")
    @ColumnDefault("''")
    @Getter(onMethod_ = @Nullable)
    private String permit;

    @Column(columnDefinition = "VARCHAR(255)")
    @ColumnDefault("''")
    @Getter(onMethod_ = @Nullable)
    private String random;

    @Column(columnDefinition = "TIMESTAMP")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Getter
    private Timestamp timelimit;

    @Column(columnDefinition = "TINYINT")
    @ColumnDefault("0")
    @Getter
    private int attempt;
}