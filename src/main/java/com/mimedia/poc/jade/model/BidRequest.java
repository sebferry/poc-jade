package com.mimedia.poc.jade.model;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

public class BidRequest implements Serializable {
    private final Task task;

    public BidRequest(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("task", task)
                          .toString();
    }
}
