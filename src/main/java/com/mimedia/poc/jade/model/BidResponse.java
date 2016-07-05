package com.mimedia.poc.jade.model;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

public class BidResponse implements Serializable {
    private final BidRequest bidRequest;

    private final String response;

    public BidResponse(BidRequest bidRequest, String response) {
        this.bidRequest = bidRequest;
        this.response = response;
    }

    public BidRequest getBidRequest() {
        return bidRequest;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("bidRequest", bidRequest)
                          .add("response", response)
                          .toString();
    }
}
