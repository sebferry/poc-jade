package com.mimedia.poc.jade.agent.action;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.base.MoreObjects;
import com.mimedia.poc.jade.model.Task;

public class ProcessTasks implements Serializable {
    private final Collection<Task> tasks;

    public ProcessTasks(Collection<Task> tasks) {
        this.tasks = tasks;
    }

    public Collection<Task> getTasks() {
        return tasks;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("tasks", tasks)
                          .toString();
    }
}
