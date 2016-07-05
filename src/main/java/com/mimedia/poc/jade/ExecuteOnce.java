package com.mimedia.poc.jade;

import jade.core.*;
import jade.core.Runtime;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecuteOnce extends Agent {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws ControllerException, InterruptedException {
        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN, "false");
        profile.setParameter(Profile.LOCAL_HOST, "localhost");
        jade.wrapper.AgentContainer agentContainer = Runtime.instance().createAgentContainer(profile);
        AgentController agentController = agentContainer.acceptNewAgent("chris", new ExecuteOnce());
        agentController.start();
    }

    @Override
    protected void setup() {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.setProtocol("ExecuteOnce");
                message.setSender(getAID());
                message.addReceiver(new AID("richard", AID.ISLOCALNAME));
                send(message);
                LOGGER.info("sent\n{}", message);
                try {
                    getContainerController().kill();
                } catch (StaleProxyException e) {
                    LOGGER.error("Error while stopping the container", e);
                }
            }
        });
    }
}
