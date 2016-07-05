package com.mimedia.poc.jade.agent;

public enum AgentService {
    TASK_COORDINATION("task-allocation", "task-coordination"),
    RESOURCE_COORDINATION("task-allocation", "resource-coordination");

    private final String type;

    private final String name;

    AgentService(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
