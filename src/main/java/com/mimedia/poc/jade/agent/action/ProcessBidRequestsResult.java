package com.mimedia.poc.jade.agent.action;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.base.MoreObjects;
import com.mimedia.poc.jade.model.BidResponse;

public class ProcessBidRequestsResult implements Serializable {
    private final Object server;

    private final Collection<BidResponse> bidResponses;

    public ProcessBidRequestsResult(Object server, Collection<BidResponse> bidResponses) {
        this.server = server;
        this.bidResponses = bidResponses;
    }

    public Object getServer() {
        return server;
    }

    public Collection<BidResponse> getBidResponses() {
        return bidResponses;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("server", server)
                          .add("bidResponses", bidResponses)
                          .toString();
    }
}
