/*
 * Created by - on 1.12.2017.
 *
 * SIN Project 2017 - Intelligent Building Simulation
 * Faculty of Information Technology, Brno University of Technology
 */

package Agents;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.concurrent.ThreadLocalRandom;


/**
 * Class represents the simulation World and everything that happens within it.
 */
public class WorldAgent extends Agent
{
    enum TimeOfDay
    {
        DAY,
        NIGHT,
        SUNRISE,
        SUNSET
    }

    private long elapsedTicks = 0;

    // World conditions properties
    private int secondInDay = 0;
    private double maximumInsideTemperature = 34;
    private double minimumInsideTemperature = 12;

    private double maximumHumidityInside = 99;
    private double minimumHumidityInside = 1;

    private double averageTemperature = 0;
    private double averageHumidity = 0;
    private int averageSunRise = 0;
    private int averageSunSet = 0;

    private double currentTemperatureInside = 15.0;
    private double currentTemperatureOutside = 5.0;
    private double currentHumidityInside = 40.0;
    private double currentHumidityOutside = 80.0;
    private double LUXOutside = 1.0;
    private TimeOfDay timeOfDay = TimeOfDay.NIGHT;

    private double temperatureCorrection = 10.0;

    // TODO: Decide whether following accessors are useless or not


    // Agent
    private AgentController SmartBedroomAgent;

    /**
     * Agent lifecycle's method called when Agent comes to life. When
     * WorldAgent is spawned, simulation effectively starts. Worlds response
     * to messages coming from other agents are defined here via
     * CommunicateWorldConditions class. Changes to the simulation world are
     * handled in WorldUpdates class extending from TickerBehavior.
     */
    protected void setup()
    {
        System.out.println("[START] World agent just started. Simulation may begin.");

        // Create SmartBedroomAgent
        spawnBedroomAgent();

        // Define behaviors
        addBehaviour(new CommunicateWorldConditions());
        addBehaviour(new WorldUpdates(this, 1000));

        //SIMULATION BASED DATA

        // setting randomly the month of the year
        int currentMonth = ThreadLocalRandom.current().nextInt(1, 13);
        getAverageValues(currentMonth);

        // setting average values for the selected month
        averageTemperature += ThreadLocalRandom.current().nextInt(-2, 3);
        averageHumidity += ThreadLocalRandom.current().nextInt(-2, 3);
        currentHumidityOutside = averageHumidity - 5;
        currentTemperatureOutside = averageTemperature -5;
        currentHumidityInside = currentHumidityOutside - 20;
        currentTemperatureInside = currentTemperatureOutside + temperatureCorrection;

        System.out.println("\tCurrent outer temperature: " + currentTemperatureOutside + "°");
        System.out.println("\tCurrent inner temperature: " + currentTemperatureInside + "°");
        System.out.println("\tCurrent outer humidity: " + currentHumidityOutside + "%");
        System.out.println("\tCurrent inner humidity: " + currentHumidityInside + "%");
        System.out.println("\tSunrise time: " + getTimeFromSeconds(averageSunRise));
        System.out.println("\tSunset time: " + getTimeFromSeconds(averageSunSet));

        //SIMULATION BASED DATA END

    }



    private void spawnBedroomAgent()
    {
        ContainerController cc = this.getContainerController();
        try
        {
            SmartBedroomAgent = cc.createNewAgent("BedroomAgent", "Agents.SmartBedroomAgent", null);
            SmartBedroomAgent.start();
        } catch (StaleProxyException e)
        {
            System.out.println("[ERROR] Unable to create smart bedroom agent! Sorry.");
            System.exit(1);
            e.printStackTrace();
        }
    }


    /**
     * Agent lifecycle's method called when Agents dies.
     */
    protected void takeDown()
    {
        try { SmartBedroomAgent.kill(); }
        catch (StaleProxyException e) { System.out.println("[ERROR] Unable to kill SmartBedroomAgent. Hopefully it'll kill itself."); }

        System.out.println("[EXIT] World agent just terminated. No further output should be produced.");
    }

    /*
     * World simulation helper methods
     */


    /**
     * Increases temperature inside based on information from Heater.
     */
    private void executeHeaterUpdate()
    {
        if ((currentTemperatureInside + 0.5) <= maximumInsideTemperature)
            currentTemperatureInside += 0.5;
    }


    /**
     * Decreases temperature inside based on information from Cooler.
     */
    private void executeCoolerUpdate()
    {
        if ((currentTemperatureInside - 1) >= minimumInsideTemperature)
            currentTemperatureInside -= 1;
    }


    /**
     * Increases humidity inside based on information from Humidifier.
     */
    private void executeHumidifierUpdate()
    {
        if ((currentHumidityInside + 2) <= maximumHumidityInside)
            currentHumidityInside += 2;
    }


    /**
     * Decreases humidity inside based on information from Dehumidifier
     */
    private void executeDehumidifierUpdate()
    {
        if ((currentHumidityInside - 2) >= minimumHumidityInside)
            currentHumidityInside -= 2;
    }


    /**
     * Converts seconds in day to (h)h:(m)m:(s)s format.
     * @param seconds Number of elapsed seconds in day.
     * @return String time in (h)h:(m)m:(s)s format.
     */
    private String getTimeFromSeconds(int seconds)
    {

        int hours = seconds / 3600;
        if (hours == 24)
            hours = 0;

        int secondsRemainder = seconds % 3600;

        int minutes = secondsRemainder / 60;
        secondsRemainder = secondsRemainder % 60;

        return (hours + ":" + minutes + ":" + secondsRemainder);
    }


    /*
    * World simulation methods
    * */

    private int getSecondsFromHours(double hours) {
        return (int)hours * 60 * 60;
    }

    private void getAverageValues(int currentMonth) {

        switch (currentMonth)
        {
            // January
            case 1:
                System.out.println("[INFO] It's January.");
                averageTemperature = -3;
                averageHumidity = 85;
                averageSunRise = getSecondsFromHours(8);
                averageSunSet = getSecondsFromHours(16);
                temperatureCorrection = 11;
                break;
            // February
            case 2:
                System.out.println("[INFO] It's February.");
                averageTemperature = -1;
                averageHumidity = 82;
                averageSunRise = getSecondsFromHours(7.5);
                averageSunSet = getSecondsFromHours(17);
                temperatureCorrection = 10;
                break;
            // March
            case 3:
                System.out.println("[INFO] It's March.");
                averageTemperature = 3;
                averageHumidity = 76;
                averageSunRise = getSecondsFromHours(7);
                averageSunSet = getSecondsFromHours(17.5);
                temperatureCorrection = 10;
                break;
            //April
            case 4:
                System.out.println("[INFO] It's April.");
                averageTemperature = 7;
                averageHumidity = 70;
                averageSunRise = getSecondsFromHours(7);
                averageSunSet = getSecondsFromHours(19.5);
                temperatureCorrection = 6;
                break;
            // May
            case 5:
                System.out.println("[INFO] It's May.");
                averageTemperature = 13;
                averageHumidity = 70;
                averageSunRise = getSecondsFromHours(6);
                averageSunSet = getSecondsFromHours(20);
                temperatureCorrection = 2;
                break;
            // June
            case 6:
                System.out.println("[INFO] It's June.");
                averageTemperature = 15;
                averageHumidity = 71;
                averageSunRise = getSecondsFromHours(5);
                averageSunSet = getSecondsFromHours(21);
                temperatureCorrection = 1;
                break;
            // July
            case 7:
                System.out.println("[INFO] It's July.");
                averageTemperature = 18;
                averageHumidity = 70;
                averageSunRise = getSecondsFromHours(5);
                averageSunSet = getSecondsFromHours(21.5);
                temperatureCorrection = 2;
                break;
            // August
            case 8:
                System.out.println("[INFO] It's August.");
                averageTemperature = 17;
                averageHumidity = 72;
                averageSunRise = getSecondsFromHours(5.5);
                averageSunSet = getSecondsFromHours(21);
                temperatureCorrection = 3;
                break;
            // September
            case 9:
                System.out.println("[INFO] It's September.");
                averageTemperature = 13;
                averageHumidity = 78;
                averageSunRise = getSecondsFromHours(6.5);
                averageSunSet = getSecondsFromHours(20);
                temperatureCorrection = 4;
                break;
            // October
            case 10:
                System.out.println("[INFO] It's October.");
                averageTemperature = 9;
                averageHumidity = 81;
                averageSunRise = getSecondsFromHours(7);
                averageSunSet = getSecondsFromHours(19);
                temperatureCorrection = 5;
                break;
            // November
            case 11:
                System.out.println("[INFO] It's November.");
                averageTemperature = 3;
                averageHumidity = 85;
                averageSunRise = getSecondsFromHours(7);
                averageSunSet = getSecondsFromHours(16.5);
                temperatureCorrection = 10;
                break;
            // December
            case 12:
                System.out.println("[INFO] It's December.");
                averageTemperature = -1;
                averageHumidity = 85;
                averageSunRise = getSecondsFromHours(7.5);
                averageSunSet = getSecondsFromHours(16);
                temperatureCorrection = 11;
                break;
            default:
                averageTemperature = -3;
                averageHumidity = 85;
                averageSunRise = getSecondsFromHours(8);
                averageSunSet = getSecondsFromHours(16);
                break;
        }

    }


    /*
     * Behavior classes
     */


    /**
     * TickerBehaviour-based class representing updates to the
     * simulation World.
     *
     * On each tick, simulation time, outside temperature, humidity, outside
     * light level may be updated.
     */
    private class WorldUpdates extends TickerBehaviour
    {
        static final int SECONDS_IN_DAY = 86400;


        WorldUpdates(Agent a, long period) {
            super(a, period);
        }


        @Override
        protected void onTick()
        {
            // 10 minutes

            // Update tick counter
            elapsedTicks++;

            // Update time
            updateTime();

            // Update temperature, humidity and light level
            updateWorldValues();

            // Debug info
            debug_showTickInformation();
        }


        /**
         * Patented by Anna Popkova.
         * She says she's completely satisfied with the code.
         */
        private void updateWorldValues()
        {
            if (secondInDay == getSecondsFromHours(1))
            {
                currentTemperatureOutside = averageTemperature + -5;
                currentHumidityOutside = averageHumidity + -5;
            }
            else if (secondInDay == getSecondsFromHours(2))
            {
                currentTemperatureOutside = averageTemperature + -5;
                currentHumidityOutside = averageHumidity + -5;
            }
            else if (secondInDay == getSecondsFromHours(3))
            {
                currentTemperatureOutside = averageTemperature + -6;
                currentHumidityOutside = averageHumidity + -6;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(4))
            {
                currentTemperatureOutside = averageTemperature + -7;
                currentHumidityOutside = averageHumidity + -7;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(5))
            {
                currentTemperatureOutside = averageTemperature + -8;
                currentHumidityOutside = averageHumidity + -8;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(6))
            {
                currentTemperatureOutside = averageTemperature + -7;
                currentHumidityOutside = averageHumidity + -7;
                currentTemperatureInside += 1;
                currentHumidityInside += 1;
            }
            else if (secondInDay == getSecondsFromHours(7))
            {
                currentTemperatureOutside = averageTemperature + -5;
                currentHumidityOutside = averageHumidity + -5;
                currentTemperatureInside += 1;
                currentHumidityInside += 1;
            }
            else if (secondInDay == getSecondsFromHours(8))
            {
                currentTemperatureOutside = averageTemperature + -2;
                currentHumidityOutside = averageHumidity + -2;
                currentTemperatureInside += 1;
                currentHumidityInside += 1;
            }
            else if (secondInDay == getSecondsFromHours(9))
            {
                currentTemperatureOutside = averageTemperature;
                currentHumidityOutside = averageHumidity;
                currentTemperatureInside += 1;
                currentHumidityInside += 1;
            }
            // Best construction in the world. #yoloswag
            else if (secondInDay == getSecondsFromHours(10))
            {
                currentTemperatureOutside = averageTemperature + 2;
                currentHumidityOutside = averageHumidity + 2;
                currentTemperatureInside += 2;
                currentHumidityInside += 2;
            }
            else if (secondInDay == getSecondsFromHours(11))
            {
                currentTemperatureOutside = averageTemperature + 5;
                currentHumidityOutside = averageHumidity + 5;
                currentTemperatureInside += 3;
                currentHumidityInside += 3;
            }
            else if (secondInDay == getSecondsFromHours(12))
            {
                currentTemperatureOutside = averageTemperature + 7;
                currentHumidityOutside = averageHumidity + 7;
                currentTemperatureInside += 2;
                currentHumidityInside += 2;
            }
            else if (secondInDay == getSecondsFromHours(13))
            {
                currentTemperatureOutside = averageTemperature + 8;
                currentHumidityOutside = averageHumidity + 8;
                currentTemperatureInside += 1;
                currentHumidityInside += 1;
            }
            else if (secondInDay == getSecondsFromHours(14))
            {
                currentTemperatureOutside = averageTemperature + 8;
                currentHumidityOutside = averageHumidity + 8;   // I love this line
            }
            else if (secondInDay == getSecondsFromHours(15))
            {
                currentTemperatureOutside = averageTemperature + 7;
                currentHumidityOutside = averageHumidity + 7;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(16))
            {
                currentTemperatureOutside = averageTemperature + 6;
                currentHumidityOutside = averageHumidity + 6;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(17))
            {
                currentTemperatureOutside = averageTemperature + 4;
                currentHumidityOutside = averageHumidity + 4;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(18))
            {
                currentTemperatureOutside = averageTemperature + 2;
                currentHumidityOutside = averageHumidity + 2;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(19))
            {
                currentTemperatureOutside = averageTemperature;
                currentHumidityOutside = averageHumidity;
                currentTemperatureInside += -2;
                currentHumidityInside += -2;
            }
            else if (secondInDay == getSecondsFromHours(20))
            {
                currentTemperatureOutside = averageTemperature + -1;
                currentHumidityOutside = averageHumidity + -1; // so hardcore
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(21))
            {
                currentTemperatureOutside = averageTemperature + -2;
                currentHumidityOutside = averageHumidity + -2;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(22))
            {
                currentTemperatureOutside = averageTemperature + -3;
                currentHumidityOutside = averageHumidity + -3;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }
            else if (secondInDay == getSecondsFromHours(23))
            {
                currentTemperatureOutside = averageTemperature + -4;
                currentHumidityOutside = averageHumidity + -4;
                currentTemperatureInside += -1;
                currentHumidityInside += -1;
            }

            if (secondInDay == averageSunRise - 1800)
            {
                timeOfDay = TimeOfDay.SUNRISE;
                LUXOutside = 40;
                System.out.println("[INFO] It's SUNRISE."); // in case you didn't know
            }
            else if (secondInDay == averageSunRise)
            {
                timeOfDay = TimeOfDay.DAY;
                LUXOutside = 200;
                System.out.println("[INFO] It's DAY.");
            }
            else if (secondInDay == averageSunSet - 1800)
            {
                timeOfDay = TimeOfDay.SUNSET;
                LUXOutside = 40;
                System.out.println("[INFO] It's SUNSET.");
            }
            else if (secondInDay == averageSunSet)
            {
                timeOfDay = TimeOfDay.NIGHT;
                LUXOutside = 1;
                System.out.println("[INFO] It's NIGHT."); // huraaay, we can go to sleep
            }
        }


        /**
         * Shifts time in simulation. Time is represented by number of elapsed
         * seconds within a day. On a break of the day (86400 seconds), number
         * of elapsed seconds is reset back to 0.
         */
        private void updateTime()
        {
            secondInDay += 600;

            // Reset day
            if (secondInDay == SECONDS_IN_DAY)
                secondInDay = 0;
        }


        /**
         * Displays debug information about current tick. Touches properties
         * of the main class.
         */
        private void debug_showTickInformation()
        {
            System.out.println("[TIME] " + getTimeFromSeconds(secondInDay));
            System.out.println("\tTemperature inside: " + currentTemperatureInside);
            System.out.println("\tTemperature outside: " + currentTemperatureOutside);
            System.out.println("\tHumidity inside: " + currentHumidityInside);
            System.out.println("\tHumidity outside: " + currentHumidityOutside);
            System.out.println("\tLight inside: " + LUXOutside);
        }

    }


    /**
     * CyclicBehaviour-based class responsible for handling messages coming
     * from agents in simulation World.
     *
     * Activates only on incoming message from agent, then puts itself to sleep
     * and waits for another message to arrive.
     */
    private class CommunicateWorldConditions extends CyclicBehaviour
    {
        /**
         * Behaviour lifecycle's method called when behaviour action is required.
         */
        public void action()
        {
            ACLMessage msg = myAgent.receive();
            if (msg != null)
            {
                switch (msg.getOntology())
                {
                    case "SENSORY-REQUEST":
                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                        reply.addReceiver(msg.getSender());
                        reply.setOntology("SENSORY-UPDATE");
                        reply.setContent(craftWorldConditionsReply());
                        send(reply);

                        System.out.println("[MESSAGE] Sensory update send to " + msg.getSender().getName());
                        break;

                    case "HEATER-ACTIVE-UPDATE":
                        executeHeaterUpdate();
                        break;

                    case "COOLER-ACTIVE-UPDATE":
                        executeCoolerUpdate();
                        break;

                    case "HUMIDIFIER-ACTIVE-UPDATE":
                        executeHumidifierUpdate();
                        break;

                    case "DEHUMIDIFIER-ACTIVE-UPDATE":
                        executeDehumidifierUpdate();
                        break;

                    case "ILLUMINATOR-ACTIVE-UPDATE":
                        break;

                    case "DIMMER-ACTIVE-UPDATE":
                        break;

                    case "TIME-REQUEST":
                        ACLMessage timeReply = new ACLMessage(ACLMessage.INFORM);
                        timeReply.addReceiver(msg.getSender());
                        timeReply.setOntology("TIME-UPDATE");
                        timeReply.setContent(getTimeFromSeconds(secondInDay));
                        send(timeReply);

                    default:
                        // Do nothing at the moment.
                        break;
                }
            }
            else
            {
                // Wait until new message is received.
                block();
            }
        }

        private String craftWorldConditionsReply()
        {
            return (currentTemperatureInside + ";" + currentTemperatureOutside + ";" + currentHumidityInside + ";" + currentHumidityOutside + ";" + LUXOutside);
        }
    }
}
