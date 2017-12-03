--
-- Domoticz passes information to scripts through a number of global tables
--
-- otherdevices, otherdevices_lastupdate and otherdevices_svalues are arrays for all devices: 
--   otherdevices['yourotherdevicename'] = "On"
--   otherdevices_lastupdate['yourotherdevicename'] = "2015-12-27 14:26:40"
--   otherdevices_svalues['yourotherthermometer'] = string of svalues
--
-- uservariables and uservariables_lastupdate are arrays for all user variables: 
--   uservariables['yourvariablename'] = 'Test Value'
--   uservariables_lastupdate['yourvariablename'] = '2015-12-27 11:19:22'
--
-- other useful details are contained in the timeofday table
--   timeofday['Nighttime'] = true or false
--   timeofday['SunriseInMinutes'] = number
--   timeofday['Daytime'] = true or false
--   timeofday['SunsetInMinutes'] = number
--   globalvariables['Security'] = 'Disarmed', 'Armed Home' or 'Armed Away'
--
-- To see examples of commands see: http://www.domoticz.com/wiki/LUA_commands#General
-- To get a list of available values see: http://www.domoticz.com/wiki/LUA_commands#Function_to_dump_all_variables_supplied_to_the_script
--
-- Based on your logic, fill the commandArray with device commands. Device name is case sensitive. 
--
--
--
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
--    if (timeofday['SunriseInMinutes'] <= 20) then
--        commandArray['Illuminator'] = "On";
--    else
--        commandArray['Illuminator'] = "Off";
--   end
--    if (timeofday['SunsetInMinutes'] <= 30) then
--        commandArray['Dimmer'] = "On";
--    else
--        commandArray['Dimmer'] = "Off";
--    end
    
--    if (deviceName=='myDevice') then
--        if deviceValue == "On" then
--            print("Device is On")
--        elseif deviceValue == "Off" then
--            commandArray['a device name'] = "On"
--            commandArray['Scene:MyScene'] = "Off"
--        end
--    end
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
