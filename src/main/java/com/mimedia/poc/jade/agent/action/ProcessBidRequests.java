package com.mimedia.poc.jade.agent.action;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.base.MoreObjects;
import com.mimedia.poc.jade.model.BidRequest;

public class ProcessBidRequests implements Serializable {
    private final Collection<BidRequest> bidRequests;

    public ProcessBidRequests(Collection<BidRequest> bidRequests) {
        this.bidRequests = bidRequests;
    }

    public Collection<BidRequest> getBidRequests() {
        return bidRequests;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("bidRequests", bidRequests)
                          .toString();
    }
}
