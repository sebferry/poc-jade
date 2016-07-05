package com.mimedia.poc.jade.agent.action;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.base.MoreObjects;

public class ProcessTasksResult implements Serializable {
    private final Collection<ProcessBidRequestsResult> processBidResponses;

    public ProcessTasksResult(Collection<ProcessBidRequestsResult> processBidResponses) {
        this.processBidResponses = processBidResponses;
    }

    public Collection<ProcessBidRequestsResult> getBidResponses() {
        return processBidResponses;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("bidResponses", processBidResponses)
                          .toString();
    }
}
