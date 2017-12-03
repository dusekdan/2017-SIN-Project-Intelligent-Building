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

public class Cooler extends Agent
{
    private static final String CONTROL_MESSAGE_TURNON = "CONTROL-MESSAGE-ACTIVATE";
    private static final String CONTROL_MESSAGE_TURNOFF = "CONTROL-MESSAGE-DEACTIVATE";
    private boolean isActive = false;
    private ActiveCooling activeCoolerBehavior;

    protected void setup()
    {
        System.out.println("[START] CoolerAgent started.");
        addBehaviour(new ResolveIncomingCommunication());
    }

    private class ResolveIncomingCommunication extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage msg = myAgent.receive();
            if (msg != null)
            {
                System.out.println("[MESSAGE] Received message in CoolerAgent. Ontology: " + msg.getOntology());
                switch (msg.getOntology())
                {
                    case CONTROL_MESSAGE_TURNOFF:
                        if (isActive && activeCoolerBehavior != null)
                        {
                            myAgent.removeBehaviour(activeCoolerBehavior);
                            isActive = false;
                        }
                        break;
                    case CONTROL_MESSAGE_TURNON:
                        if (!isActive)
                        {
                            activeCoolerBehavior = new ActiveCooling(myAgent, 1500);
                            myAgent.addBehaviour(activeCoolerBehavior);
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

    private class ActiveCooling extends TickerBehaviour
    {
        ActiveCooling(Agent a, long period) { super(a, period); }

        @Override
        protected void onTick()
        {
            System.out.println("[MESSAGE] Cooler sending message to WorldAgent");
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(new AID("WorldAgent", AID.ISLOCALNAME));
            message.setLanguage("English");
            message.setOntology("COOLER-ACTIVE-UPDATE");
            message.setContent("Yo, wat up? We coolin' now!");
            send(message);
        }
    }
}
