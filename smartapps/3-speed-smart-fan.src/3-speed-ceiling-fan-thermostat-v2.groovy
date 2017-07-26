/*
	 Virtual Thermostat for 3 Speed Ceiling Fan Control
	 Copyright 2016 SmartThings, Dale Coffing
	 
	 This smartapp provides automatic control of Low, Medium, High speeds of a ceiling fan using 
	 any temperature sensor with optional motion override. 
	 It requires two hardware devices; any temperature sensor and a dimmer type smart fan controller
	 such as the GE 12730 or Leviton VRF01-1LX
	 
	Change Log
	2017-06-05 Added ability for manually overriding the app from the switch for configured time.
	2016-08-02 Added the ability to make this a parent / child app.
	2016-06-30 added dynamic temperature display on temperature setpoint input text
	2016-06-28 x.1 version update
				added submitOnChange for motion so to skip minutes input next if no motion selected
			changed order of inputs for better logic flow
						added separate input page for Configuring Settings to reduce clutter on required inputs
						change to other mode techinque to see if it will force a reevaluate of methods
						renamed fanHiSpeed to fanSpeed for more generic use, added 0.0 on timer selection
						changed motion detector minutes input only if motion selected submitOnChange
	2016-06-03 modified the 3 second startup to 1 for low speed
	2016-5-30 added dynamicPages for user guide, combined version data with aboutPage parameters which
				gives a larger icon image then if used alone in paragraph mode.
	2016-5-19 code clean up only
	2016-5-17 fanDiffTemp input changed to use enum with preselected values to overcome range:"0.1..2.0" bug
	2016-5-16 fixed typo with motion to motionSensor in hasBeenRecentMotion()
						fixed IDE integration with ST by making another change to file name specifics.
	2016-5-15 fixed fan differenial decimal point error by removing range: "1..99", removed all fanDimmer.setLevel(0)
					 added iconX3Url, reworded preferences, rename evaluate to tempCheck for clarity,
					 best practices to utilize initialize() method & replace motion with motionSensor,
	2016-5-14 Fan temperature differential variable added, best practices to change sensor to tempSensor,
	2016-5-13 best practices to replace ELSE IF for SWITCH statements on fan speeds, removed emergency temp control
	2016-5-12 added new icons for 3SFC, colored text in 3SFC125x125.png and 3sfc250x250.png
	2016-5-6  (e)minor changes to text, labels, for clarity, (^^^e)default to NO-Manual for thermostat mode 
	2016-5-5c clean code, added current ver section header, allow for multiple fan controllers,
						replace icons to ceiling fan, modify name from Control to Thermostat
	2016-5-5b @krlaframboise change to bypasses the temperatureHandler method and calls the tempCheck method
						with the current temperature and setpoint setting
	2016-5-5  autoMode added for manual override of auto control/*
	2016-5-4b cleaned debug logs, removed heat-cool selection, removed multiple stages
	2016-5-3  fixed error on not shutting down, huge shout out to my bro Stephen Coffing in the logic formation 
	
	I modified the SmartThngs original Virtual Thermostat code which is buggy. Known issues
	-[Fixed] when SP is updated, temp control isn't evaluated immediately, an event must trigger like change in temp, motion
	- if load is previously running when smartapp is loaded, it isn't evaluated immediately to turn off when SetPt>CurrTemp
	- temperature control is not evaluated when making a mode change, have to wait for something to change like temp
 
	Thanks to @krlaframboise, @MikeMaxwell for help in solving issues for a first time coder. @MichaelS for icon background
 
	 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
	 in compliance with the License. You may obtain a copy of the License at: www.apache.org/licenses/LICENSE-2.0
	 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
	 on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
	 for the specific language governing permissions and limitations under the License.
	
 */
definition(
	name: "${appName()}",
	namespace: "dcoffing",
	author: "Dale Coffing",
	description: "Automatic control for 3 Speed Ceiling Fan using Low, Medium, High speeds with any temperature sensor.",
	category: "My Apps",
	iconUrl: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png", 
	iconX2Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
	iconX3Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
)

preferences {
	page(name: "startPage")
	page(name: "parentPage")
	page(name: "childStartPage")
	page(name: "optionsPage")
	page(name: "aboutPage")
}

def startPage() {
	if (parent) {
		childStartPage()
	} else {
		parentPage()
	}
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: false, uninstall: true) {
		section("Create a new fan automation.") {
			app(name: "childApps", appName: appName(), namespace: "dcoffing", title: "New Fan Automation", multiple: true)
		}
	}
}

def childStartPage() {
	return dynamicPage(name: "childStartPage", title: "Select your devices and settings", install: true, uninstall: true) {
		
		section("Select a room temperature sensor to control the fan..."){
			input "tempSensor", "capability.temperatureMeasurement", multiple:false, title: "Temperature Sensor", required: true, submitOnChange: true  
		}
		
		if (tempSensor) {  //protects from a null error
			section("Enter the desired room temperature setpoint...\n" + "NOTE: ${tempSensor.displayName} room temp is ${tempSensor.currentTemperature}° currently"){
				input "setpoint", "decimal", title: "Room Setpoint Temp", defaultValue: tempSensor.currentTemperature, required: true
			}
		}	else 
			section("Enter the desired room temperature setpoint..."){
				input "setpoint", "decimal", title: "Room Setpoint Temp", required: true
			}       
	
			section("Select the ceiling fan control hardware..."){
				input "fanDimmer", "capability.switchLevel", 
				multiple:false, title: "Fan Control device", required: true
			}
				
			section("App Name") {
				label(title: "Assign a name", required: false)
			}
				
			section("Optional Settings (Diff Temp, Timers, Motion, etc)") {
				href (
					name: "optionsPage", 
					title: "Configure Optional settings", 
					description: none,
					image: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/evap-cooler-thermostat.src/settings250x250.png",
					required: false,
					page: "optionsPage"
				)
			}
			section("Version Info, User's Guide") {
				// VERSION
				href (
					name: "aboutPage", 
					title: "3 Speed Ceiling Fan Thermostat \n"+"Version:3.0.170605 \n"+"Copyright © 2016 Dale Coffing", 
					description: "Tap to get user's guide.",
					image: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png",
					required: false,
					page: "aboutPage"
				)
			}
		
	}
}      

def optionsPage() {
	dynamicPage(name: "optionsPage", title: "Configure Optional Settings", install: false, uninstall: false) {
		section("Enter the desired differential temp between fan speeds (default=1.0)..."){
			input "fanDiffTempString", "enum", title: "Fan Differential Temp", options: ["0.5","1.0","1.5","2.0", "3.0"], required: false
		}
		section("Enable ceiling fan thermostat only if motion is detected at (optional, leave blank to not require motion)..."){
			input "motionSensor", "capability.motionSensor", title: "Select Motion device", required: false, submitOnChange: true
		}
		if (motionSensor) {
			section("Turn off ceiling fan thermostat when there's been no motion detected for..."){
				input "motionInactivityTimeout", "number", title: "Minutes?", required: true
			}
		}
		section("Select ceiling fan operating mode desired (default to 'YES-Auto'..."){
			input "autoMode", "enum", title: "Enable Ceiling Fan Thermostat?", options: ["NO-Manual","YES-Auto"], required: false
		}
		section("Override app if fan speed changed from switch"){
			input(name: "override", title: "Override from switch?", type: "bool", required: true, defaultValue: false, description: "Allows temporary overriding of the app processing if the fan is controlled from the switch.")
			input(name: "overrideTimeout", title: "Override time (minutes)", type: "number", required: false, description: "How long should the app stop processing?")
		}
		section ("Mode selector") {
			mode title: "Set for specific mode(s)", required: false
		}
		section ("Debug logging") {
			input "debugOutput", "boolean",	title: "Enable debug logging?",	defaultValue: false, displayDuringSetup: true
		}
	}
}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none, install: true, uninstall: true) {
		section("User's Guide; 3 Speed Ceiling Fan Thermostat") {
			paragraph textHelp()
		}
	}
}

private def appName() { return "${parent ? "Fan Automation" : "3 Speed Ceiling Fan Thermostat v3"}" }

def installed() {
	if (state.debug) log.debug "def INSTALLED with settings: ${settings}"
	initialize()
}

def updated() {
	if (state.debug) log.debug "def UPDATED with settings: ${settings}"
	state.debug = ("true" == debugOutput)
	unsubscribe()
	initialize()
	handleTemperature(tempSensor.currentTemperature) //call handleTemperature to bypass temperatureHandler method 
}

def initialize() {
	if(parent) { 
		initChild() 
	} else {
		initParent() 
	}
}

def initParent() {
}

def initChild() {
	if (state.debug) log.debug "def INITIALIZE with settings: ${settings}"
	subscribe(tempSensor, "temperature", temperatureHandler) //call temperatureHandler method when any reported change to "temperature" attribute
	runEvery5Minutes(handleTemperature, [data: tempSensor.currentTemperature])
	if (motionSensor) {
		subscribe(motionSensor, "motion", motionHandler) //call the motionHandler method when there is any reported change to the "motion" attribute
		state.hasRecentMotion = true
	}
	if (override) {
		subscribe(fanDimmer, "switch", fanLevelChangedHandler)
		subscribe(fanDimmer, "level", fanLevelChangedHandler)
		subscribeToCommand(fanDimmer, "lowSpeed", fanLevelChangedHandler)
		subscribeToCommand(fanDimmer, "medSpeed", fanLevelChangedHandler)
		subscribeToCommand(fanDimmer, "highSpeed", fanLevelChangedHandler)
		state.lastAutoLevelSetTime = 0 // record the last time the app set the fan level
		state.lastSwitchLevelSetTime = 0 // record te timestamp of the last level change at the switch (regardless of source)
		state.hasRecentOverride = false
	}
}

//Event Handler Methods
def fanLevelChangedHandler(evt) {
	state.lastSwitchLevelSetTime = evt.date.getTime()
	if (wasOverride()) {
		runIn(overrideTimeout * 60, recentOverride)
	}
	if (state.debug) log.debug "fanLevelChangedHandler called at $state.lastSwitchLevelSetTime"
}

def temperatureHandler(evt) { 
	handleTemperature(evt.doubleValue)
	if (state.debug) log.debug "temperatureHandler evt.doubleValue : $evt"
}

def handleTemperature(temp) {   // 
	if (motion && !state.hasRecentMotion) {
		fanDimmer.off()
	}
	else {
		if (state.debug) log.debug "handler running: $temp"
		//motion detected recently
		tempCheck(temp, setpoint)
	}
}

def motionHandler(evt) {
	if (evt.value == "active") {
		//motion detected
		if(motionInactivityTimeout) {
			// reset the clock for no motion counter
			if (state.debug) log.debug "motion was detected; resetting noMotion timer"
			state.hasRecentMotion = true
			runIn(motionInactivityTimeout * 60, recentMotion)
		}
		def lastTemp = tempSensor.currentTemperature
		if (lastTemp != null) {
			tempCheck(lastTemp, setpoint)
		}
	} else if (evt.value == "inactive") {   //testing to see if evt.value is indeed equal to "inactive" (vs evt.value to "active")
		//motion stopped
		if (state.debug) log.debug "motion sensor claimed to be inactive"
		if (state.hasRecentMotion) {
			def lastTemp = tempSensor.currentTemperature
			if (lastTemp != null) {       //lastTemp not equal to null (value never been set) 
				tempCheck(lastTemp, setpoint)
			}
		}
		else {
			fanDimmer.off()
		}
	}
}

def recentMotion() {
	state.hasRecentMotion = false
}

def recentOverride() {
	state.hasRecentOverride = false
}

private wasOverride() {
	def diff = state.lastSwitchLevelSetTime - state.lastAutoLevelSetTime
	if (diff < 500 && diff > -500) {
		state.hasRecentOverride = true
		if (state.debug) log.debug "the last event from the switch and the app were probably the same; the difference was $diff"
		return false
	} else {
		if (state.debug) log.debug "the last event from the switch and the app were probably different; the difference was $diff"
		return true
	}
}

private def tempCheck(currentTemp, desiredTemp)
{
	if (state.debug) log.debug "TEMPCHECK#1(CT=$currentTemp,SP=$desiredTemp,FD=$fanDimmer.currentSwitch,FD_LVL=$fanDimmer.currentLevel, automode=$autoMode,FDTstring=$fanDiffTempString, FDTvalue=$fanDiffTempValue)"
		
	//convert Fan Diff Temp input enum string to number value and if user doesn't select a Fan Diff Temp default to 1.0 
	def fanDiffTempValue = (settings.fanDiffTempString != null && settings.fanDiffTempString != "") ? Double.parseDouble(settings.fanDiffTempString): 1.0

	//if user doesn't select autoMode then default to "YES-Auto"
	def autoModeValue = (settings.autoMode != null && settings.autoMode != "") ? settings.autoMode : "YES-Auto" 
	
	def LowDiff = fanDiffTempValue*1 
	def MedDiff = fanDiffTempValue*2
	def HighDiff = fanDiffTempValue*3
	
	if (autoModeValue == "YES-Auto" && !state.hasRecentOverride) {
		def currentLevel = fanDimmer.currentLevel
		switch (currentTemp - desiredTemp) {
			case { it >= HighDiff }:
				if (shouldAutoAdjust("high")) {
					// turn on fan high speed
					fanDimmer.setLevel(90)
					state.lastAutoLevelSetTime = now()
					if (state.debug) log.debug "HI speed(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, HighDiff=$HighDiff)"
				}
				break  //exit switch statement 
			case { it >= MedDiff }:
				if (shouldAutoAdjust("medium"))
				// turn on fan medium speed
				fanDimmer.setLevel(60)
				state.lastAutoLevelSetTime = now()
				if (state.debug) log.debug "MED speed(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, MedDiff=$MedDiff)"
				break
			case { it >= LowDiff }:
				if (shouldAutoAdjust("low")) {
					// turn on fan low speed
					if (fanDimmer.currentSwitch == "off") {   // if fan is OFF to make it easier on motor by   
						fanDimmer.setLevel(90) // starting fan in High speed temporarily then
						fanDimmer.setLevel(30, [delay: 1000]) // change to Low speed after 1 second
						if (state.debug) log.debug "LO speed after HI 3secs(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, LowDiff=$LowDiff)"
					} else {
						fanDimmer.setLevel(30)  //fan is already running, not necessary to protect motor
					}             //set Low speed immediately
					state.lastAutoLevelSetTime = now()
					if (state.debug) log.debug "LO speed immediately(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, LowDiff=$LowDiff)"
				}
				break
			default:
				// check to see if fan should be turned off
				if (desiredTemp - currentTemp >= 0 && shouldAutoAdjust("off")) {  //below or equal to setpoint, turn off fan, zero level
					fanDimmer.off()
					state.lastAutoLevelSetTime = now()
					if (state.debug) log.debug "below SP+Diff=fan OFF (CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, FD=$fanDimmer.currentSwitch,autoMode=$autoMode,)"
				} 
				if (state.debug) log.debug "autoMode YES-MANUAL? else OFF(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, FD=$fanDimmer.currentSwitch,autoMode=$autoMode,)"
		}
	} 
}

private def shouldAutoAdjust(level) {
	def currentLevel = fanDimmer.currentLevel
  def currentState = fanDimmer.currentSwitch
  if (state.debug) log.debug "currentLevel: $currentLevel"
  if (state.debug) log.debug "currentState: $currentState"
	switch (level) {
		case "high":
			if (currentLevel < 68 || currentState == "off") {
				return true
			} else {
				if (state.debug) log.debug "fan was already on high, no need to send a new setting"
				return false
			}
			break
		case "medium":
			if (currentLevel < 34 || currentLevel > 67 || currentState == "off") {
				return true
			} else {
				if (state.debug) log.debug "fan was already on medium, no need to send a new setting"
				return false
			}
			break
		case "low":
			if (currentLevel > 33 || currentState == "off") {
				return true
			} else {
				if (state.debug) log.debug "fan was already on low, no need to send a new setting"
				return false
			}
			break
		default:
			if (currentState == "off") {
				if (state.debug) log.debug "fan was already off, no need to send a new setting"
				return false
			} else {
				return true
			}
	}
}

private def textHelp() {
	def text =
		"This smartapp provides automatic control of Low, Medium, High speeds of a"+
		" ceiling fan using any temperature sensor based on its' temperature setpoint"+
		" turning on each speed automatically in 1 degree differential increments."+
		" For example, if the desired room temperature setpoint is 72, the low speed"+
		" turns on first at 73, the medium speed turns on at 74, the high speed turns"+
		" on at 75. And vice versa on decreasing temperature until at 72 the ceiling"+
		" fan turns off. The differential is adjustable from 0.5 to 2.0 in half degree increments. \n\n" +
		"A notable feature is when low speed is initially requested from"+
		" the off condition, high speed is turned on briefly to overcome the startup load"+
		" then low speed is engaged. This mimics the pull chain switches that most"+
		" manufacturers use by always starting in high speed. \n\n"+
		"A motion option turns off automatic mode when no motion is detected. A thermostat"+
		" mode option will disable the smartapp and pass control to manual control.\n\n"+
		"@ChadCK's 'Z-Wave Smart Fan Control Custom Device Handler' along with hardware"+
		" designed specifically for motor control such as the GE 12730 Z-Wave Smart Fan Control or"+
		" Leviton VRF01-1LX works well together with this smartapp."
}