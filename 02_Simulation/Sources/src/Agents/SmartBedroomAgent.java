/**
 * Created by Daniel Dusek on 30.11.2017.
 */

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class SmartBedroomAgent extends Agent{


    protected void setup()
    {
        String nick = "Daniel";
        AID id = new AID(nick, AID.ISLOCALNAME);

        System.out.println("SmartBedroomAgent initialized. This will be the 'world' container.");
        System.out.println("Some stats\n\tName:" + getAID());
        // Add some behaviors
        // - Outside daylight simulation
        // - Outside && inside temperature changing
        // - Humidity changing
        //
    }

}
