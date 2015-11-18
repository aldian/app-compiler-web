package com.aldianfazrihady.model;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.io.Serializable;

/**
 * Created by AldianFazrihady on 11/13/15.
 */
@Entity
public class CompilationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Lob
    private String message;

    public CompilationResult() {}

    public CompilationResult(String message) {
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("CompilationResult[id=%d, message='%s']", id, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompilationResult that = (CompilationResult) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
