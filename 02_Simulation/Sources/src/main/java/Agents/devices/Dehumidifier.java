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

public class Dehumidifier extends Agent
{
    private static final String CONTROL_MESSAGE_TURNON = "CONTROL-MESSAGE-ACTIVATE";
    private static final String CONTROL_MESSAGE_TURNOFF = "CONTROL-MESSAGE-DEACTIVATE";
    private boolean isActive = false;
    private ActiveDrying activeDryingBehaviour;

    protected void setup()
    {
        System.out.println("[START] DehumidifierAgent started.");
        addBehaviour(new ResolveIncomingCommunication());
    }

    protected void takeDown()
    {
        System.out.println("[EXIT] DehumidifierAgent terminated.");
    }

    private class ResolveIncomingCommunication extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage msg = myAgent.receive();
            if (msg != null)
            {
               System.out.println("[MESSAGE] Received message in DehumidifierAgent. Ontology: " + msg.getOntology());
               switch(msg.getOntology())
               {
                   case CONTROL_MESSAGE_TURNOFF:
                       if (isActive && activeDryingBehaviour != null)
                       {
                           myAgent.removeBehaviour(activeDryingBehaviour);
                           isActive = false;
                       }
                       break;
                   case CONTROL_MESSAGE_TURNON:
                       if (!isActive)
                       {
                           activeDryingBehaviour = new ActiveDrying(myAgent, 3000);
                           myAgent.addBehaviour(activeDryingBehaviour);
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

    private class ActiveDrying extends TickerBehaviour
    {
        ActiveDrying(Agent a, long period) { super(a, period); }

        @Override
        protected void onTick()
        {
            System.out.println("[MESSAGE] Dehumidifier sending message to WorldAgent.");
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(new AID("WorldAgent", AID.ISLOCALNAME));
            message.setLanguage("English");
            message.setOntology("DEHUMIDIFIER-ACTIVE-UPDATE");
            message.setContent("Start drying process, please, World.");
            send(message);
        }
    }


}
