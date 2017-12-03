/*
 * Created by - on 1.12.2017.
 *
 * SIN Project 2017 - Intelligent Building Simulation
 * Faculty of Information Technology, Brno University of Technology
 */
package Agents;

import com.sun.org.apache.xpath.internal.SourceTree;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;


import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import virtual.DomoticzHW;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.json.simple.*;

/**
 * Class responsible for communicating sensor data with central control unit
 * and issuing commands to active devices in the room.
 */
public class SmartBedroomAgent extends Agent
{

    /*
     * Sensor abstraction (properties representing measured values)
     */
    private double temperatureInside;
    private double temperatureOutside;
    private double humidityInside;
    private double humidityOutside;
    private double lightLevelInside;
    private double lightLevelOutside;
    private int wakeupMinutes;
    private int wakeupHours;
    private int sleepHours;
    private int sleepMinutes;

    private int secondInDay;


    /*
     * Active device abstraction (agents acting as active devices)
     */
    private ArrayList<AgentController> activeDevices = new ArrayList<>();


    /*
     * Domoticz hw / device type constants & devices / hw created
     */
    private static final String TEMPERATURE_SENSOR_TYPE = "80";
    private static final String HUMIDITY_SENSOR_TYPE = "81";
    private static final String LUX_SENSOR_TYPE = "246";
    private static final String SWITCH_TYPE = "6";

    private boolean isDay = false;

    private static final String domoticzBaseUrl = "http://127.0.0.1:8888/json.htm?";
    private boolean DEBUG = false;


    // Containers with HW & devices created
    private ArrayList<DomoticzHW> virtualHW = new ArrayList<>();
    private ArrayList<DomoticzHW> virtualDevices = new ArrayList<>();

    private static int temperatureVariableIdx = -1;
    private static int wakeupTimeVariableIdx = -1;
    private static int sleepTimeVariableIdx = -1;

    /*
     * SmartBedroomAgents life cycle methods.
     */

    /**
     * Agent lifecycle's method called when agent is spawned. Called at the
     * beginning of the simulation.
     */
    protected void setup()
    {
        // Create active device agents
        System.out.println("[START] Smart Bedroom Agent started.");
        LogString("[DEBUG] Creating Active Devices.");
        spawnActiveDevices();
        LogString("[DEBUG] Number of devices in activeDevices ArrayList: " + activeDevices.size());

        LogString("[DEBUG] Populating Domoticz server...");
        populateDomoticzServer();

        addBehaviour(new RequestSensorData(this, 1000));
        addBehaviour(new ResolveIncomingCommunication());
        addBehaviour(new UpdateDomoticzServer(this, 1000));
        addBehaviour(new ReadCommandsFromRemote(this, 1000));

        // Should alternate between collection data from sensors & sending them to remote control unit
        // and issuing commands to active devices within the system.
        // => Ticker behavior gathering data from world & sending them to Domaticz
        //      * method of this agent will represent sensor, e.g. getLightSensorValue()
        //      * based on control reply, issue command to active device (active device should probably be an Agent)
    }


    /**
     * Agent lifecycle's method called when agent dies. This method is only
     * called once the simulation ends.
     * Before terminating the agent, clean up remote Domoticz server first.
     */
    protected void takeDown()
    {
        killActiveDevices();

        // Remove existing devices from remote
        String deviceIdxs = "";
        for (DomoticzHW device : virtualDevices)
        {
            deviceIdxs += device.getIdx() + ";";
        }
        removeRemoteDevice(deviceIdxs);

        // Remove virtual HW from remote
        for (DomoticzHW hw : virtualHW)
        {
            // We have to delete HW one by one because REST API does not allow
            // multiple HW to be removed at once. Sadly.
            removeRemoteHW(hw.getIdx());
        }

        removeUserVariables();

        System.out.println("Agents.SmartBedroomAgent died.");
    }


    /**
     * Instantiates active device agents in SmartBedroomAgents scope.
     * List of active devices:
     * - Cooler
     * - Heater
     * - Humidifier
     * - Dehumidifier
     * - Illuminator
     * - Dimmer
     */
    private void spawnActiveDevices()
    {
        ContainerController cc = getContainerController();
        try
        {
            AgentController coolerAgent = cc.createNewAgent("CoolerAgent", "Agents.devices.Cooler", null);
            AgentController heaterAgent = cc.createNewAgent("HeaterAgent", "Agents.devices.Heater", null);
            AgentController humidifierAgent = cc.createNewAgent("HumidifierAgent", "Agents.devices.Humidifier", null);
            AgentController dehumidifierAgent = cc.createNewAgent("DehumidifierAgent", "Agents.devices.Dehumidifier", null);
            AgentController illuminatorAgent = cc.createNewAgent("IlluminatorAgent", "Agents.devices.Illuminator", null);
            AgentController dimmerAgent = cc.createNewAgent("DimmerAgent", "Agents.devices.Dimmer", null);

            coolerAgent.start();
            heaterAgent.start();
            humidifierAgent.start();
            dehumidifierAgent.start();
            illuminatorAgent.start();
            dimmerAgent.start();

            activeDevices.add(coolerAgent);
            activeDevices.add(heaterAgent);
            activeDevices.add(humidifierAgent);
            activeDevices.add(dehumidifierAgent);
            activeDevices.add(illuminatorAgent);
            activeDevices.add(dimmerAgent);
        }
        catch (StaleProxyException e)
        {
            System.out.println("[FATAL-ERROR] Unable to create active devices. This error is unrecoverable.");
        }
    }


    /**
     * Eradicates existing active device Agents.
     */
    private void killActiveDevices()
    {
        for (AgentController controller : activeDevices)
        {
            try
            {
                controller.kill();
            }
            catch (StaleProxyException e)
            {
                System.out.println("[ERROR] Unable to kill active device. Hopefully it'll terminate on its own.");
            }
        }
    }


    /*
     * Domoticz environment methods.
     */


    /**
     * Spawns devices on remote Domoticz server and saves reference to them
     * within SmartBedroomAgent.
     */
    private void populateDomoticzServer()
    {
        // Create hardware
        virtualHW.add(createHardware("TemperatureSensor"));
        virtualHW.add(createHardware("HumiditySensor"));
        virtualHW.add(createHardware("LightSensor"));

        // Create sensors for hardware
        virtualDevices.add(createDevice("InsideTemperature", virtualHW.get(0).getIdx(), TEMPERATURE_SENSOR_TYPE));
        virtualDevices.add(createDevice("OutsideTemperature", virtualHW.get(0).getIdx(), TEMPERATURE_SENSOR_TYPE));

        virtualDevices.add(createDevice("InsideHumidity", virtualHW.get(1).getIdx(), HUMIDITY_SENSOR_TYPE));
        virtualDevices.add(createDevice("OutsideHumidity", virtualHW.get(1).getIdx(), HUMIDITY_SENSOR_TYPE));

        //virtualDevices.add(createDevice("InsideLight", virtualHW.get(2).getIdx(), LUX_SENSOR_TYPE));
        virtualDevices.add(createDevice("OutsideLight", virtualHW.get(2).getIdx(), LUX_SENSOR_TYPE));

        // Create all the indicators (switches) for hardware
        virtualDevices.add(createDevice("Cooler", virtualHW.get(0).getIdx(), SWITCH_TYPE));
        virtualDevices.add(createDevice("Heater", virtualHW.get(0).getIdx(), SWITCH_TYPE));

        virtualDevices.add(createDevice("Humidifier", virtualHW.get(1).getIdx(), SWITCH_TYPE));
        virtualDevices.add(createDevice("Dehumidifier", virtualHW.get(1).getIdx(), SWITCH_TYPE));

        virtualDevices.add(createDevice("Illuminator", virtualHW.get(2).getIdx(), SWITCH_TYPE));
        virtualDevices.add(createDevice("Dimmer", virtualHW.get(2).getIdx(), SWITCH_TYPE));

        defineUserVariables();
    }


    /**
     * Sends HTTP GET request HW to Domoticz creating new HW.
     * @param hwName String name of HW to be created.
     * @return DomoticzHW representation of created HW.
     */
    private DomoticzHW createHardware(String hwName)
    {
        // TODO: Why port = 1?
        String baseHWUrl = domoticzBaseUrl + "type=command&param=addhardware&htype=15&port=1&enabled=true&name=";

        // Parse response for data
        String responseBody = fireGetRequest(baseHWUrl + hwName);

        // Prepare new DomoticzHW instance
        JSONParser parser = new JSONParser();
        try
        {
            Object obj = parser.parse(responseBody);
            JSONObject jsonObject = (JSONObject) obj;

            String idx =  (String) jsonObject.get("idx");
            return new DomoticzHW(idx,hwName,"", true);
        }
        catch (ParseException e)
        {
            System.out.println("[ERROR] Unable to create Domoticz hardware instance.");
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Sends HTTP GET request to Domoticz server creating new Device.
     * @param deviceName String name of device to be created.
     * @param hwIdx String identifier of HW which device should bind to.
     * @param deviceType String type of device to be created.
     * @return DomoticzHW representation of device.
     */
    private DomoticzHW createDevice(String deviceName, String hwIdx, String deviceType)
    {
        String baseUrl = domoticzBaseUrl + "type=createvirtualsensor&idx=";
        String responseBody = fireGetRequest(baseUrl + hwIdx + "&sensorname=" + deviceName + "&sensortype=" + deviceType);

        // Parse JSON response
        JSONParser parser = new JSONParser();
        try
        {
            Object obj = parser.parse(responseBody);
            JSONObject jsonObject = (JSONObject) obj;

            String idx = (String) jsonObject.get("idx");

            // For Dimmer & Illuminator we want to keep the default icon.
            if (deviceType.equals(SWITCH_TYPE) && !deviceName.equals("Dimmer") && !deviceName.equals("Illuminator"))
            {
                String updateSwitchImage = domoticzBaseUrl + "type=setused&idx=" + idx + "&name=" + deviceName
                        + "&description=&strparam1=&strparam2=&protected=false&switchtype=0&customimage=9&used=true&addjvalue=0&addjvalue2=0&options=";

                fireGetRequest(updateSwitchImage);
                LogString("[UPDATED] Images for switch: " + deviceName + " updated.");
            }

            return new DomoticzHW(idx, deviceName, "", false);
        }
        catch (ParseException e)
        {
            System.out.println("[ERROR] Unable to create Domoticz device instance.");
            e.printStackTrace();
        }

        return null;
    }


    private void defineUserVariables()
    {
        removeUserVariables();

        // json.htm?type=command&param=saveuservariable&vname=uservariablename&vtype=uservariabletype&vvalue=uservariablevalue
        String baseUrl = domoticzBaseUrl + "type=command&param=saveuservariable&vname=";

        ArrayList<String> userVariablesIdxs = new ArrayList<>();
        ArrayList<String> variableRequests = new ArrayList<>();
        variableRequests.add(baseUrl + "TargetTemperature" + "&vtype=1&vvalue=20.0");
        variableRequests.add(baseUrl + "WakeUpTime" + "&vtype=4&vvalue=06:00");
        variableRequests.add(baseUrl + "SleepTime" + "&vtype=4&vvalue=23:00");
        int variableCounter = 0;

        for (String request : variableRequests)
        {

            String response = fireGetRequest(request);

            // Parse JSON response
            JSONParser parser = new JSONParser();
            try
            {
                Object obj = parser.parse(response);
                JSONObject jsonObject = (JSONObject) obj;

                String status = (String) jsonObject.get("status");
                if (status.equals("OK"))
                    variableCounter++;
            }
            catch (ParseException e)
            {
                System.out.println("[ERROR] Unable to parse user variable creation request.");
                e.printStackTrace();
            }

        }

        if (variableCounter == 3)
        {
            temperatureVariableIdx = 1;
            wakeupTimeVariableIdx = 2;
            sleepTimeVariableIdx = 3;
        }
        else
        {
            System.out.println("[ERROR] Unable to create user variable.");
        }

    }

    private String getUserVariable(int idx)
    {
        String request = domoticzBaseUrl + "type=command&param=getuservariable&idx="+idx;
        String response = fireGetRequest(request);
        String value = "";

        // Parse JSON response
        JSONParser parser = new JSONParser();
        try
        {
            Object obj = parser.parse(response);

            JSONObject jsonObject = (JSONObject) obj;
            JSONArray resultsArray = (JSONArray) jsonObject.get("result");

            if (resultsArray != null)
            {
                Iterator<JSONObject> iterator = resultsArray.iterator();
                if (iterator.hasNext())
                {
                    value = (String) (iterator.next()).get("Value");
                }
            }
            else
            {
                System.out.println("[ERROR] Results array is null.");
            }

            return value;

        }
        catch (ParseException e)
        {
            System.out.println("[ERROR] Unable to create user variable.");
            e.printStackTrace();
        }

        return null;
    }


    private void removeUserVariables()
    {
        // Hardcoded for win (application will use 3 variables, there is a decent chance that we hit their IDX)
        fireGetRequest(domoticzBaseUrl + "type=command&param=deleteuservariable&idx=1");
        fireGetRequest(domoticzBaseUrl + "type=command&param=deleteuservariable&idx=2");
        fireGetRequest(domoticzBaseUrl + "type=command&param=deleteuservariable&idx=3");
        fireGetRequest(domoticzBaseUrl + "type=command&param=deleteuservariable&idx=4");
        fireGetRequest(domoticzBaseUrl + "type=command&param=deleteuservariable&idx=5");
        fireGetRequest(domoticzBaseUrl + "type=command&param=deleteuservariable&idx=6");

    }


    /**
     * Sends HTTP GET deletion request to remote Domoticz server.
     *
     * Due to design of Domoticz REST API, we can not save the number of
     * requests to remote using multiple identifiers in one request. This is
     * however possible for removing remote device.
     *
     * @param idx String identifier of Domoticz HW to be deleted.
     */
    private void removeRemoteHW(String idx)
    {
        String deleteUrl = domoticzBaseUrl + "type=command&param=deletehardware&idx=" + idx;
        fireGetRequest(deleteUrl);
        System.out.println("[REMOVE] Removing HW using url: " + deleteUrl);
    }


    /**
     * Sends HTTP GET deletion request to remote Domoticz server.
     * @param idxs String identifier(s) of remote Domaticz device(s) to be removed.
     */
    private void removeRemoteDevice(String idxs)
    {
        String deleteUrl = domoticzBaseUrl + "type=deletedevice&idx=" + idxs;
        fireGetRequest(deleteUrl);
        System.out.println("[REMOVE] Removing devices using url: " + deleteUrl);
    }


    /*
     * Behavior definitions.
     */

    /**
     * TickerBehaviour-based class responsible for fetching control commands
     * from remote Domoticz server.
     */
    private class ReadCommandsFromRemote extends TickerBehaviour
    {
        ReadCommandsFromRemote(Agent a, long period) { super(a, period);}

        @Override
        protected void onTick() {
            // Basically just query all the devices and check 'data' property
            // in its response. If it says it should be On, message active
            // device agent to turn on, otherwise to turn Off.

            queryRequestedDeviceStates(virtualDevices);
            notifyDevices(virtualDevices);
        }

        private void notifyDevices(ArrayList<DomoticzHW> devices)
        {
            for (DomoticzHW device : devices)
            {
                // Skip sensor devices, read commands only for active devices
                if(isPassiveDevice(device))
                {
                    continue;
                }

                LogString("[NOTIFY] Notifying device " + device.getName() + " to change its state to " + device.getData());
                ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                message.addReceiver(new AID(device.getName() + "Agent", AID.ISLOCALNAME));
                message.setLanguage("English");
                if (device.getData().equals("On"))
                {
                    message.setOntology("CONTROL-MESSAGE-ACTIVATE");
                }
                else
                {
                    message.setOntology("CONTROL-MESSAGE-DEACTIVATE");
                }
                message.setContent("Could you please execute command I just issued? Thanks in advance!");
                send(message);
            }
        }

        private boolean isPassiveDevice(DomoticzHW device)
        {
            return (
                    device.getName().equals("InsideTemperature") ||
                    device.getName().equals("OutsideTemperature") ||
                    device.getName().equals("InsideHumidity") ||
                    device.getName().equals("OutsideHumidity") ||
                    device.getName().equals("InsideLight") ||
                    device.getName().equals("OutsideLight")
            );
        }

        private void queryRequestedDeviceStates(ArrayList<DomoticzHW> devices)
        {
            String baseUrl = domoticzBaseUrl + "type=devices&rid=";
            for (DomoticzHW device : devices)
            {
                // Skip sensor devices, read commands only for active devices
                if(isPassiveDevice(device))
                {
                    continue;
                }

                String responseBody = fireGetRequest(baseUrl + device.getIdx());

                JSONParser parser = new JSONParser();
                try
                {
                    Object obj = parser.parse(responseBody);

                    JSONObject jsonObject = (JSONObject) obj;
                    JSONArray resultsArray = (JSONArray) jsonObject.get("result");

                    if (resultsArray != null)
                    {
                        Iterator<JSONObject> iterator = resultsArray.iterator();
                        if (iterator.hasNext())
                        {
                            String data = (String) (iterator.next()).get("Data");
                            device.setData(data);
                            LogString("[DEBUG] Found result for idx: " + device.getName() + " value: " + data);
                        }
                    }
                    else
                    {
                        System.out.println("[ERROR] Results array is null.");
                    }
                }
                catch (Exception e) { System.out.println("[ERROR] Unable to parse command from remote."); }
            }
        }
    }


    /**
     * TickerBehaviour-based class regularly informing Domoticz about sensory value updates.
     */
    private class UpdateDomoticzServer extends TickerBehaviour
    {
        UpdateDomoticzServer(Agent a, long period) { super(a, period); }


        @Override
        protected void onTick()
        {
               updateAllSensors(virtualDevices);
        }


        private void updateAllSensors(ArrayList<DomoticzHW> devices)
        {
            String baseUrl = domoticzBaseUrl + "type=command&param=udevice&idx=";
            for (DomoticzHW device : devices)
            {
                String composedUrl = baseUrl + device.getIdx();
                boolean fire = false;
                switch (device.getName())
                {
                    case "InsideTemperature":
                        composedUrl +=  "&nvalue=0&svalue=" + temperatureInside;
                        fire = true;
                        break;
                    case "OutsideTemperature":
                        composedUrl +=  "&nvalue=0&svalue=" + temperatureOutside;
                        fire = true;
                        break;
                    case "InsideHumidity":
                        composedUrl += "&nvalue=" + humidityInside + "&svalue=0";
                        fire = true;
                        break;
                    case "OutsideHumidity":
                        composedUrl += "&nvalue=" + humidityOutside + "&svalue=0";
                        fire = true;
                        break;
                    case "InsideLight":
                        composedUrl += "&svalue=" + ((Number) lightLevelInside).intValue();
                        fire = false;
                        break;
                    case "OutsideLight":
                        composedUrl += "&svalue=" + ((Number) lightLevelOutside).intValue();
                        fire = true;
                        break;
                }

                if (fire)
                {
                    LogString("[REPORTING] Firing request to: " + composedUrl);
                    fireGetRequest(composedUrl);
                }

            }
        }
    }


    /**
     * TickerBehaviour-based class gathering data for sensors from WorldAgent.
     * TODO: Figure out period in which data should be polled.
     */
    private class RequestSensorData extends TickerBehaviour
    {
        RequestSensorData(Agent a, long period) { super(a, period);}


        /**
         * Ask WorldAgent what are current values and set internal sensors.
         */
        @Override
        protected void onTick()
        {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID("WorldAgent", AID.ISLOCALNAME));
            msg.setLanguage("English");
            msg.setOntology("SENSORY-REQUEST");
            msg.setContent("Dear World, would you kindly provide me with current world conditions? Thanks in advance!");
            send(msg);

            ACLMessage timeMessage = new ACLMessage(ACLMessage.REQUEST);
            timeMessage.addReceiver(new AID("WorldAgent", AID.ISLOCALNAME));
            timeMessage.setLanguage("English");
            timeMessage.setOntology("TIME-REQUEST");
            timeMessage.setContent("Hey, world, what's the time?");
            send(timeMessage);
        }


        private void debug_printSensorState()
        {
            LogString("[BEDROOM-SENSORS]");
            LogString("\tTemperature inside: " + temperatureInside);
            LogString("\tTemperature outside: " + temperatureOutside);
            LogString("\tHumidity inside: " + humidityInside);
            LogString("\tHumidity outside: " + humidityOutside);
            LogString("\tLight inside: " + lightLevelInside);
            LogString("\tLight outside: " + lightLevelOutside);
        }
    }


    /**
     * CyclicBehaviour-based class handling all the incoming communication from
     * other Agents within the system.
     */
    private class ResolveIncomingCommunication extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage msg = myAgent.receive();
            if (msg != null)
            {
                switch(msg.getOntology())
                {
                    case "SENSORY-UPDATE":
                        updateInternalSensors(msg.getContent());
                        break;
                    case "TIME-UPDATE":
                        updateTime(msg.getContent());
                        break;
                    default: break;
                }
            }
            else
            {
                // Wait for another message incoming message.
                block();
            }
        }


        private void updateTime(String value)
        {
            LogString("[UPDATE] Received time: " + value);

            List<String> items = Arrays.asList(value.split("\\s*:\\s*"));

            if (items.size() != 3)
            {
                System.out.println("[ERROR] Could not update time. ");
                return;
            }

            int actualHours = Integer.parseInt(items.get(0));
            int actualMinutes = Integer.parseInt(items.get(1)) + (60 * actualHours);

            String wakeupTime;
            String sleepTime;

            wakeupTime = getUserVariable(wakeupTimeVariableIdx);
            items = Arrays.asList(wakeupTime.split("\\s*:\\s*"));
            int wakeupHours = Integer.parseInt(items.get(0));
            int wakeupMinutes = Integer.parseInt(items.get(1)) + (60 * wakeupHours);

            sleepTime = getUserVariable(sleepTimeVariableIdx);
            items = Arrays.asList(sleepTime.split("\\s*:\\s*"));
            int sleepHours = Integer.parseInt(items.get(0));
            int sleepMinutes = Integer.parseInt(items.get(1)) + (60 * sleepHours);

            // Get Iluminator & Dimmer ID
            String dimmerIdx = getIdxByDeviceName("Dimmer");
            String illuminatorIdx = getIdxByDeviceName("Illuminator");

            if (actualMinutes > wakeupMinutes && actualMinutes < sleepMinutes)
            {
                // It's day
                System.out.println("[UPDATE] It's day");
                if (lightLevelInside < 200)
                {
                    if (!isDay)
                    {
                        changeLightSwitchStatus(illuminatorIdx, "On");
                        changeLightSwitchStatus(dimmerIdx, "Off");
                        isDay = true;
                    }
                }
            }
            else
            {
                // It's night
                System.out.println("[UPDATE] It's night");
                if (isDay)
                {
                    changeLightSwitchStatus(illuminatorIdx, "Off");
                    changeLightSwitchStatus(dimmerIdx, "On");
                    isDay = false;
                }
            }
        }


        private String getIdxByDeviceName(String deviceName)
        {
            for (DomoticzHW device : virtualDevices)
            {
                if (device.getName().equals(deviceName))
                    return device.getIdx();
            }

            return "";
        }


        /**
         * Updates status for remote light switch.
         * @param idx String identification of remote device.
         * @param status String desired remote device status.
         */
        private void changeLightSwitchStatus(String idx, String status)
        {
            String url = domoticzBaseUrl + "type=command&param=switchlight&switchcmd=" + status + "&idx=" + idx;
            LogString("[SWITCH] Changing switch status using: " + url);
            fireGetRequest(url);
        }


        private void updateInternalSensors(String values)
        {
            // Assumed order: Temperature In, Out, Humidity, In, Out, Light In, Out
            List<String> items = Arrays.asList(values.split("\\s*;\\s*"));
            LogString("[UPDATE] Received sensors: " + values);

            if (items.size() != 5)
            {
                System.out.println("[ERROR] Could not update sensors. Not enough sensory values contained in reply (" + items.size() + " provided, 5 required).");
                return;
            }

            // Update sensors
            temperatureInside = Double.parseDouble(items.get(0));
            temperatureOutside = Double.parseDouble(items.get(1));
            humidityInside = Double.parseDouble(items.get(2));
            humidityOutside = Double.parseDouble(items.get(3));
            lightLevelOutside = Double.parseDouble(items.get(4));
        }
    }


    /*
     * Helper methods.
     */


    /**
     * Launches HTTP GET request against url & returns response body. Expects
     * utf-8 encoding (sufficient for project purposes).
     * @param url String target url.
     * @return String target url response body on success, empty string on
     * failure.
     */
    private String fireGetRequest(String url)
    {
        try
        {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", "utf-8");
            InputStream response = connection.getInputStream();

            try (Scanner scanner = new Scanner(response))
            {
                String responseBody = scanner.useDelimiter("\\A").next();
                response.close();

                return responseBody;
            }
        }
        catch (Exception e){ System.out.println("Error sending HTTP request to URL: " + url); }

        return "";
    }


    private void LogString(String message)
    {
        if (DEBUG)
            System.out.println(message);
    }

}
