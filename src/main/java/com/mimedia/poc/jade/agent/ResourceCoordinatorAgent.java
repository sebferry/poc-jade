package com.mimedia.poc.jade.agent;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Throwables;
import com.mimedia.poc.jade.agent.action.ProcessBidRequests;
import com.mimedia.poc.jade.agent.action.ProcessBidRequestsResult;
import com.mimedia.poc.jade.model.BidResponse;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceCoordinatorAgent extends Agent {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void setup() {
        registerServices();

        int sleepTime = processArgs();
        addBehaviour(new ReceiveRequests(this, sleepTime));
        addBehaviour(new DeleteUnreadMessages(this));
    }

    private int processArgs() {
        int sleepTime = 0;
        Object[] arguments = getArguments();
        if (arguments != null && arguments.length > 0) {
            sleepTime = Integer.parseInt((String) arguments[0]);
        }
        return sleepTime;
    }

    private void registerServices() {
        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(AgentService.RESOURCE_COORDINATION.getType());
        serviceDescription.setName(AgentService.RESOURCE_COORDINATION.getName());
        agentDescription.addServices(serviceDescription);

        try {
            DFService.register(this, agentDescription);
        } catch (FIPAException e) {
            LOGGER.error("Error registering the agent", e);
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            LOGGER.error("Error deregistering the agent", e);
        }
    }

    private static class ReceiveRequests extends AchieveREResponder {
        private final int sleepTime;

        public ReceiveRequests(Agent agent, int sleepTime) {
            super(agent, AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST));
            this.sleepTime = sleepTime;
        }

        @Override
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            ACLMessage reply = request.createReply();
            try {
                if (request.getContentObject() instanceof ProcessBidRequests) {
                    reply.setPerformative(ACLMessage.AGREE);
                } else {
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected error", e);
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent(Throwables.getStackTraceAsString(e));
            }
            return reply;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request,
                                                       ACLMessage response) throws FailureException {
            ACLMessage reply = request.createReply();
            try {
                ProcessBidRequests processBidRequests = (ProcessBidRequests) request.getContentObject();
                ProcessBidRequestsResult processBidRequestsResult = computeResult(processBidRequests);

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContentObject(processBidRequestsResult);
            } catch (Exception e) {
                LOGGER.error("Unexpected error", e);
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent(Throwables.getStackTraceAsString(e));
            }
            return reply;
        }

        private ProcessBidRequestsResult computeResult(ProcessBidRequests processBidRequests) throws InterruptedException, UnreadableException {
            Thread.sleep(sleepTime);

            List<BidResponse> bidResponses
                    = processBidRequests.getBidRequests()
                                        .stream()
                                        .map(bidRequest -> new BidResponse(bidRequest, "Slot available"))
                                        .collect(Collectors.toList());
            String serverId = getAgent().getName();
            return new ProcessBidRequestsResult(serverId, bidResponses);
        }
    }
}