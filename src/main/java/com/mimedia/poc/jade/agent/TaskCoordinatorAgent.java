package com.mimedia.poc.jade.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;
import java.util.stream.Collectors;

import com.google.common.base.Throwables;
import com.mimedia.poc.jade.agent.action.ProcessBidRequests;
import com.mimedia.poc.jade.agent.action.ProcessBidRequestsResult;
import com.mimedia.poc.jade.agent.action.ProcessTasks;
import com.mimedia.poc.jade.agent.action.ProcessTasksResult;
import com.mimedia.poc.jade.model.BidRequest;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskCoordinatorAgent extends Agent {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void setup() {
        registerServices();

        addBehaviour(new ReceiveRequestsBehavior(this));
        addBehaviour(new DeleteUnreadMessages(this));
    }

    private void registerServices() {
        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(AgentService.TASK_COORDINATION.getType());
        serviceDescription.setName(AgentService.TASK_COORDINATION.getName());
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

    private interface Callback {
        void execute();
    }

    private static class ReceiveRequestsBehavior extends AchieveREResponder {
        public ReceiveRequestsBehavior(Agent agent) {
            super(agent, AchieveREResponder.createMessageTemplate(FIPA_REQUEST));
        }

        @Override
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            ACLMessage reply = request.createReply();
            try {
                if (request.getContentObject() instanceof ProcessTasks) {
                    handleProcessTasks(reply, request, (ProcessTasks) request.getContentObject());
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

        private void handleProcessTasks(ACLMessage reply,
                                        ACLMessage request,
                                        ProcessTasks processTasks) throws UnreadableException, IOException {
            Collection<AID> participants = findProcessTasksParticipants();
            if (participants.isEmpty()) {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("No participants are available");
            } else {
                reply.setPerformative(ACLMessage.AGREE);

                Date replyByDate = new Date(System.currentTimeMillis() + 500);
                ACLMessage initialMessage = buildProcessTasksInitialMessage(participants, processTasks, replyByDate);
                registerPrepareResultNotification(new ProcessTasksBehavior(getAgent(),
                                                                           request,
                                                                           RESULT_NOTIFICATION_KEY,
                                                                           initialMessage,
                                                                           replyByDate));
            }
        }

        private Collection<AID> findProcessTasksParticipants() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription serviceDescription = new ServiceDescription();
            serviceDescription.setType(AgentService.RESOURCE_COORDINATION.getType());
            serviceDescription.setName(AgentService.RESOURCE_COORDINATION.getName());
            template.addServices(serviceDescription);

            Collection<AID> participants = new LinkedList<>();
            try {
                DFAgentDescription[] result = DFService.search(getAgent(), template);
                for (DFAgentDescription agentDescription : result) {
                    participants.add(agentDescription.getName());
                }
            } catch (FIPAException e) {
                LOGGER.error("Error searching for participant agents", e);
            }
            return participants;
        }

        private ACLMessage buildProcessTasksInitialMessage(Collection<AID> participants,
                                                           ProcessTasks processTasks,
                                                           Date replyByDate) throws IOException {
            ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
            message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            message.setReplyByDate(replyByDate);
            participants.forEach(message::addReceiver);

            Collection<BidRequest> bidRequests = processTasks.getTasks()
                                                             .stream()
                                                             .map(BidRequest::new)
                                                             .collect(Collectors.toList());
            message.setContentObject(new ProcessBidRequests(bidRequests));

            return message;
        }
    }

    private static class ProcessTasksBehavior extends ParallelBehaviour {
        private final ACLMessage request;

        private final String retryKey;

        private final SendProcessBids sendProcessBids;

        public ProcessTasksBehavior(Agent agent,
                                    ACLMessage request,
                                    String retryKey,
                                    ACLMessage initialMessage,
                                    Date replyByDate) throws IOException {
            super(agent, ParallelBehaviour.WHEN_ANY);
            this.request = request;
            this.retryKey = retryKey;

            sendProcessBids = new SendProcessBids(getAgent(), initialMessage, this::sendReplyMessage);
            sendProcessBids.setDataStore(getDataStore());
            addSubBehaviour(sendProcessBids);

            addSubBehaviour(new TimeoutBehavior(getAgent(), replyByDate, this::sendReplyMessage));
        }

        private void sendReplyMessage() {
            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            try {
                reply.setContentObject(new ProcessTasksResult(sendProcessBids.getProcessBidResponses()));
            } catch (IOException e) {
                LOGGER.error("Unexpected error", e);
                reply.setPerformative(ACLMessage.FAILURE);
            }
            getDataStore().put(retryKey, reply);
        }
    }

    private static class TimeoutBehavior extends WakerBehaviour {
        private final transient Callback callback;

        public TimeoutBehavior(Agent a, Date timeoutDate, Callback callback) {
            super(a, timeoutDate);
            this.callback = callback;
        }

        @Override
        protected void onWake() {
            callback.execute();
        }
    }

    private static class SendProcessBids extends AchieveREInitiator {
        private final Collection<ProcessBidRequestsResult> processBidResponses = new LinkedList<>();

        private final transient Callback callback;

        public SendProcessBids(Agent agent, ACLMessage initialMessage, Callback callback) {
            super(agent, initialMessage);
            this.callback = callback;
        }

        public Collection<ProcessBidRequestsResult> getProcessBidResponses() {
            return processBidResponses;
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            try {
                processBidResponses.add((ProcessBidRequestsResult) inform.getContentObject());
            } catch (UnreadableException e) {
                LOGGER.error("Unable to extract the result", e);
            }
        }

        @Override
        protected void handleAllResultNotifications(Vector resultNotifications) {
            callback.execute();
        }
    }
}
