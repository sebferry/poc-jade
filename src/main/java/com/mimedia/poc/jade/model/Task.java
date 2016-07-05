package com.mimedia.poc.jade.model;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

public class Task implements Serializable {
    private final Object id;

    public Task(Object id) {
        this.id = id;
    }

    public Object getId() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("id", id)
                          .toString();
    }
}
