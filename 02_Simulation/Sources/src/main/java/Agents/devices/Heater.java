/*
 * Created by - on 2.12.2017.
 *
 * SIN Project 2017 - Intelligent Building Simulation
 * Faculty of Information Technology, Brno University of Technology
 */

package Agents.devices;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class Heater extends Agent
{
    private static final String CONTROL_MESSAGE_TURNON = "CONTROL-MESSAGE-ACTIVATE";
    private static final String CONTROL_MESSAGE_TURNOFF = "CONTROL-MESSAGE-DEACTIVATE";

    private boolean isActive = false;

    private ActiveHeating activeHeaterBehaviour;

    protected void setup()
    {
        System.out.println("[START] HeaterAgent started.");
        addBehaviour(new ResolveIncomingCommunication());
    }

    protected void takeDown()
    {
        System.out.println("[EXIT] HeaterAgent terminated.");
    }

    private class ResolveIncomingCommunication extends CyclicBehaviour
    {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null)
            {
                System.out.println("[MESSAGE] Received message in HeaterAgent. Ontology: " + msg.getOntology());
                switch (msg.getOntology())
                {
                    case CONTROL_MESSAGE_TURNOFF:
                        // Check whether agent is active, if yes, deactivate
                        if (isActive && activeHeaterBehaviour != null)
                        {
                            myAgent.removeBehaviour(activeHeaterBehaviour);
                            isActive = false;
                        }
                        break;
                    case CONTROL_MESSAGE_TURNON:
                        // Check whether agent is inactive, if yes, activate
                        if (!isActive)
                        {
                            activeHeaterBehaviour = new ActiveHeating(myAgent, 1000);
                            myAgent.addBehaviour(activeHeaterBehaviour);
                            isActive = true;
                        }
                        break;
                    default: break;
                }
            }
            else
            {
                block();
            }
        }
    }

    private class ActiveHeating extends TickerBehaviour
    {
        ActiveHeating(Agent a, long period) { super(a, period); }

        @Override
        protected void onTick() {
            System.out.println("[MESSAGE] Heater sending message to WorldAgent");
            // Send message to WorldAgent
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(new AID("WorldAgent", AID.ISLOCALNAME));
            message.setLanguage("English");
            message.setOntology("HEATER-ACTIVE-UPDATE");
            message.setContent("Hello, sir, I would like to take a moment to inform you about me being now active. Sincere: The Heater.");
            send(message);
        }
    }
}
