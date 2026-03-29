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

import java.sql.Timestamp;

@ToString
@Entity
@Table(name = "contexts")
@NoArgsConstructor
public class Context
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int sid;

    @Column(nullable = false)
    @Getter
    private int uid;

    @Column(nullable = false, columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(onMethod_ = @NotNull)
    private String data;

    @Column(columnDefinition = "TIMESTAMP")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Getter
    private Timestamp timestamp;

    @ManyToOne
    @JoinColumn(name = "uid", referencedColumnName = "uid", insertable = false, updatable = false)
    private User user;

    public Context(int uid)
    {
        this.uid = uid;
    }
}