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

public class Humidifier extends Agent
{
    private static final String CONTROL_MESSAGE_TURNON = "CONTROL-MESSAGE-ACTIVATE";
    private static final String CONTROL_MESSAGE_TURNOFF = "CONTROL-MESSAGE-DEACTIVATE";
    private boolean isActive = false;
    private ActiveHumidification activeHumidifierBehaviour;

    protected void setup()
    {
        System.out.println("[START] HumidifierAgent started.");
        addBehaviour(new ResolveIncomingCommunication());
    }

    protected void takeDown()
    {
        System.out.println("[EXIT] HumidifierAgent terminated.");
    }

    private class ResolveIncomingCommunication extends CyclicBehaviour
    {

        @Override
        public void action()
        {
            ACLMessage msg = myAgent.receive();
            if (msg != null)
            {
                System.out.println("[MESSAGE] Received message in HumidifierAgent. Ontology: " + msg.getOntology());
                switch(msg.getOntology())
                {
                    case CONTROL_MESSAGE_TURNOFF:
                        if (isActive && activeHumidifierBehaviour != null)
                        {
                            myAgent.removeBehaviour(activeHumidifierBehaviour);
                            isActive = false;
                        }
                        break;
                    case CONTROL_MESSAGE_TURNON:
                        if (!isActive)
                        {
                            activeHumidifierBehaviour = new ActiveHumidification(myAgent, 3000);
                            myAgent.addBehaviour(activeHumidifierBehaviour);
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

    private class ActiveHumidification extends TickerBehaviour
    {
        public ActiveHumidification(Agent a, long period) { super(a, period); }

        @Override
        protected void onTick()
        {
            System.out.println("[MESSAGE] Humidifier sending message to WorldAgent.");
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(new AID("WorldAgent", AID.ISLOCALNAME));
            message.setLanguage("English");
            message.setOntology("HUMIDIFIER-ACTIVE-UPDATE");
            message.setContent("Start humidification process, please, World.");
            send(message);
        }
    }
}
