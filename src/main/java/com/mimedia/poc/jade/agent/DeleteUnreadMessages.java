package com.mimedia.poc.jade.agent;

import java.util.Collection;
import java.util.HashSet;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteUnreadMessages extends TickerBehaviour {
    private static final Logger LOGGER = LogManager.getLogger();

    private Collection<ACLMessage> oldMessages = new HashSet<>();

    public DeleteUnreadMessages(Agent agent) {
        super(agent, 5000);
        setBehaviourName("DeleteUnreadMessages");
    }

    @Override
    protected void onTick() {
        Collection<ACLMessage> newMessages = new HashSet<>();
        ACLMessage currentMessage;
        while ((currentMessage = getAgent().receive()) != null) {
            if (!oldMessages.remove(currentMessage)) {
                newMessages.add(currentMessage);
            } else {
                LOGGER.debug("Removing old message {}", currentMessage);
            }
        }
        newMessages.stream().forEach(message -> getAgent().putBack(message));
        oldMessages = newMessages;
    }
}
