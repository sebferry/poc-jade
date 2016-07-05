package com.mimedia.poc.jade.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import com.mimedia.poc.jade.agent.action.ProcessBidRequestsResult;
import com.mimedia.poc.jade.agent.action.ProcessTasks;
import com.mimedia.poc.jade.agent.action.ProcessTasksResult;
import com.mimedia.poc.jade.model.Task;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessTasksAgent extends Agent {
    private static final Logger LOGGER = LogManager.getLogger();

    private static int nextTaskId = 1;

    private ProcessTasksGui processTasksGui;

    @Override
    protected void setup() {
        processTasksGui = new ProcessTasksGui(this);
        processTasksGui.show();
    }

    @Override
    protected void takeDown() {
        processTasksGui.dispose();
    }

    public void processNextTasks() throws IOException {
        AID participant = findParticipant();
        if (participant == null) {
            LOGGER.error("Unable to find any participant");
            return;
        }

        ACLMessage message = buildInitialMessage(participant);
        addBehaviour(new SimpleAchieveREInitiator(this, message) {
            @Override
            protected void handleAllResultNotifications(Vector resultNotifications) {
                for (Object resultNotification : resultNotifications) {
                    ACLMessage message = (ACLMessage) resultNotification;
                    try {
                        ProcessTasksResult processTasksResult = (ProcessTasksResult) message.getContentObject();
                        for (ProcessBidRequestsResult processBidRequestsResult : processTasksResult.getBidResponses()) {
                            LOGGER.info("Received from {}", processBidRequestsResult.getServer());
                            processBidRequestsResult.getBidResponses().forEach(LOGGER::info);
                        }
                    } catch (UnreadableException e) {
                        LOGGER.error("Unexpected error", e);
                    }
                }
            }
        });
    }

    private AID findParticipant() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(AgentService.TASK_COORDINATION.getType());
        serviceDescription.setName(AgentService.TASK_COORDINATION.getName());
        template.addServices(serviceDescription);

        try {
            DFAgentDescription[] result = DFService.search(ProcessTasksAgent.this, template);
            if (result.length > 0) {
                return result[0].getName();
            }
        } catch (FIPAException e) {
            LOGGER.error("Error searching for participant agents", e);
        }
        return null;
    }

    private ACLMessage buildInitialMessage(AID participant) throws IOException {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        message.addReceiver(participant);
        Collection<Task> taskElemets = new LinkedList<>();
        for (int i = 0; i < 3; i++) {
            taskElemets.add(new Task(nextTaskId++));
        }
        ProcessTasks processTasks = new ProcessTasks(taskElemets);
        message.setContentObject(processTasks);
        LOGGER.info("Sending {}", processTasks);
        return message;
    }
}
