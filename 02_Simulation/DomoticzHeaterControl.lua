-- PUT THIS SCRIPT TO YOUR OTHER DOMOTICZ EVENTS IT CONTROLS HEATER
-- Name the script: HeaterControl
-- Set to be run with "Device" update
--
commandArray = {}

print ("All based event fired");
-- loop through all the devices
for deviceName,deviceValue in pairs(otherdevices) do
    if (deviceName=='InsideTemperature') then
        if tonumber(deviceValue)+1 < uservariables['TargetTemperature'] then
            commandArray['Heater'] = "On";
            print("Heater was turned on by LUA scripting.");
        else
            commandArray['Heater'] = "Off";
            print("No reason for heater to run.");
        end
    end
-- Cool the room down if temperature is higher
    if (deviceName=='InsideTemperature') then
        if tonumber(deviceValue)-1 > uservariables['TargetTemperature'] then
            commandArray['Cooler'] = "On";
            print("Cooler was turned on by LUA scripting.");
        else
            commandArray['Cooler'] = "Off";
            print("No reason for cooler to run");
        end
    end
end

-- loop through all the variables
for variableName,variableValue in pairs(uservariables) do
--    if (variableName=='myVariable') then
--        if variableValue == 1 then
--            commandArray['a device name'] = "On"
--            commandArray['Group:My Group'] = "Off AFTER 30"
--        end
--    end
end

return commandArray
