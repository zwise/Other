definition (
  name: 'Automatic Fan',
  namespace: 'zwise',
  author: 'Zach Wise',
  description: 'Automatic control for 3 Speed Ceiling Fan using Low, Medium, High speeds with any temperature sensor. This is a child app',
  category: 'My Apps',
  parent: 'zwise:Automatic Fan Manager',
  iconUrl: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png", 
  iconX2Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
  iconX3Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png"
)

preferences {
  page(name: "mainPage")
  page(name: "optionsPage")
  page(name: "aboutPage")
}

def installed() {
  if (state.debug) log.debug "def INSTALLED with settings: ${settings}"
  if (autoMode) {
    initialize()
  }
}

def updated() {
  state.debug = ("true" == debugOutput)
  unsubscribe()
  if (autoMode) {
    initialize()
    handleTemperature("initialize") //call handleTemperature to bypass temperatureHandler method
  }
}

def initialize() {
  if (state.debug) log.debug "def INITIALIZE with settings: ${settings}"
  if (autoMode) {
    subscribe(tempSensor, "temperature", temperatureHandler) //call temperatureHandler method when any reported change to "temperature" attribute
    // runEvery1Minute(handleTemperature) // may consider re-enabling this later, but right now it just doesn't make sense; better to rely on the temperature sensors to do their job
    state.hasRecentMotion = false
    state.hasRecentOverride = false
    if (motionSensor) {
      subscribe(motionSensor, "motion", motionHandler) //call the motionHandler method when there is any reported change to the "motion" attribute
      state.hasRecentMotion = true
      runIn(motionInactivityTimeout * 60, clearRecentMotion)
      if (state.debug) log.debug "initializing: setting hasRecentMotion to ${state.hasRecentMotion}"
    }
    if (override) {
      subscribe(fanDimmer, "switch", fanLevelChangedHandler)
      subscribe(fanDimmer, "level", fanLevelChangedHandler)
      state.lastAutoLevelSetTime = now() // record the last time the app set the fan level
      state.lastSwitchLevelSetTime = now() // record the timestamp of the last level change at the switch (regardless of source)
      state.overrideCounter = 0
    }
  }
}

def mainPage() {
  return dynamicPage(name: "mainPage", title: "Select your devices and settings", install: true, uninstall: true) {
    
    section("Enable ceiling fan thermostat?"){
      input "autoMode", "boolean", title: "Enable Ceiling Fan Thermostat?", defaultValue: true, displayDuringSetup: true
    }

    section("Select a temperature sensor to control the fan...") {
      input "tempSensor", "capability.temperatureMeasurement", multiple:false, title: "Temperature Sensor", required: true, submitOnChange: true  
    }

    if (tempSensor) {  //protects from a null error
      section("Enter the desired room temperature setpoint...\n" + "NOTE: ${tempSensor.displayName} room temp is ${tempSensor.currentTemperature}° currently"){
        input "setpoint", "decimal", title: "Room Setpoint Temp", defaultValue: tempSensor.currentTemperature, required: true
      }
    } else {
      section("Enter the desired room temperature setpoint...") {
        input "setpoint", "decimal", title: "Room Setpoint Temp", required: true
      }
    }

    section("Select the ceiling fan switch...") {
      input "fanDimmer", "capability.switchLevel", multiple:false, title: "Fan Switch", required: true
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
  }
}      

def optionsPage() {
  dynamicPage(name: "optionsPage", title: "Configure Optional Settings", install: false, uninstall: false) {
    section("Select differential temp between fan speeds..."){
      input "fanDiffTempString", "enum", title: "Fan Differential Temp", options: ["0.5","1.0","1.5","2.0", "3.0"], required: false
    }
    section("Allow room presence to control fan..."){
      input "motionSensor", "capability.motionSensor", title: "Select Motion device", required: false, submitOnChange: true
      if (motionSensor) {
        input "motionInactivityTimeout", "number", title: "Inactive time (minutes)", required: true
      }
    }
    section("Override"){
      input(name: "override", title: "Override from switch?", type: "bool", submitOnChange: true, required: true, defaultValue: false, description: "Allows temporary overriding of the app processing if the fan is controlled from the switch.")
      if (override) {
        input(name: "overrideTimeout", title: "Override time (minutes)", type: "number", required: false)
      }
    }
    section ("Mode selector") {
      mode(title: "Set for specific mode(s)", multiple: true)
    }
    section ("Debug logging") {
      input "debugOutput", "boolean", title: "Enable debug logging?", defaultValue: false, displayDuringSetup: true
    }
  }
}

//Event Handler Methods
def temperatureHandler(evt) {
  // if (state.debug) log.debug "temperatureHandler: displayname=${evt.displayName}, name=${evt.name}, source=${evt.source}, value=${evt.value}, appid=${evt.installedSmartAppId}, isDigital=${evt.isDigital()}, isPhysical=${evt.isPhysical()}"
  handleTemperature("temperatureHandler")
}

def handleTemperature(caller) {
  if (state.debug) log.debug "handleTemperature called by $caller; motionSensor=$motionSensor, hasRecentMotion=${state.hasRecentMotion}, override=$override, hasRecentOverride=${state.hasRecentOverride}, autoMode=$autoMode"
  if (!autoMode) {
    if (state.debug) log.debug "autoMode=$autoMode: app services are not desired right now"
  } else if (override && state.hasRecentOverride) {
    if (state.debug) log.debug "hasRecentOverride=${state.hasRecentOverride}: waiting for the override to clear"
  } else if (motionSensor && !state.hasRecentMotion) {
    if (state.debug) log.debug "hasRecentMotion=${state.hasRecentMotion}: no need to turn on the fan"
  } else {
    def temp = tempSensor.currentTemperature
    if (state.debug) log.debug "no harm in trying: see if temp=$temp is in range"
    //motion detected recently
    if (temp != null) {
      tempCheck(temp, setpoint)
    }
  }
}

def fanLevelChangedHandler(evt) {
  state.lastSwitchLevelSetTime = now();
  // if (state.debug) log.debug "fanLevelChangedHandler: displayname=${evt.displayName}, name=${evt.name}, source=${evt.source}, time=${evt.date}, timevalue=${evt.dateValue}, timevalueinstance=${evt.dateValue instanceof Date}, isoDate=${evt.isoDate}, value=${evt.value}, appid=${evt.installedSmartAppId}, isDigital=${evt.isDigital()}, isPhysical=${evt.isPhysical()}"
  if (wasOverride(evt) && autoMode) {
    state.overrideCounter = state.overrideCounter + 1
    if (state.debug) log.debug "override detected; resetting override timer for another $overrideTimeout minutes; overrideCounter=${state.overrideCounter}"
    runIn(overrideTimeout * 60, clearOverride)
  }
}

def motionHandler(evt) {
  if (evt.value == "active") {
    //motion detected
    if(motionInactivityTimeout) {
      // reset the clock for no motion counter
      if (state.debug) log.debug "motion detected; resetting motion timer for another $motionInactivityTimeout minutes"
      state.hasRecentMotion = true
      runIn(motionInactivityTimeout * 60, clearRecentMotion)
    }
    handleTemperature("motionHandler")
  }
}

def clearRecentMotion() {
  if (!state.hasRecentOverride) {
    state.hasRecentMotion = false
    if (state.debug) log.debug "motionTimeout reached: setting hasRecentMotion to ${state.hasRecentMotion} and turning off fan"
    state.lastAutoLevelSetTime = now()
    fanDimmer.off()
  } else {
    def runSoon = ((motionInactivityTimeout / 2) * 60)
    if (state.debug) log.debug "motionTimeout reached, but hasRecentOverride= ${state.hasRecentOverride}; will check again in $runSoon seconds"
    runIn(runSoon, clearRecentMotion)
  }
}

def clearOverride() {
  state.hasRecentOverride = false
  if (state.debug) log.debug "overrideTimeout reached: setting hasRecentOverride to ${state.hasRecentOverride}"
  handleTemperature("clearOverride")
}

private def wasOverride(evt) {
  // def eventTime = evt.isoDate ? timeToday(evt.isoDate.replace('Z', '-0000')).getTime() : now() // may consider using this later, but doesn't seem to be very accurate
  def diff = state.lastSwitchLevelSetTime - state.lastAutoLevelSetTime
  if (state.debug) log.debug "let's do some math: lastSwitchLevelSetTime=${state.lastSwitchLevelSetTime}, lastAutoLevelSetTime=${state.lastAutoLevelSetTime}, diff=$diff"
  if (evt.isPhysical()) {
    if (state.debug) log.debug "last event from switch was a physical override override; evt.isPhysical=${evt.isPhysical()}; setting hasRecentOverride=${state.hasRecentOverride}"
    return true
  } else if (diff > 6000) {
    state.hasRecentOverride = true
    if (state.debug) log.debug "last event from switch was an override; the difference was $diff; setting hasRecentOverride=${state.hasRecentOverride}"
    return true
  } else {
    if (state.debug) log.debug "last event from switch and the app were probably the same; the difference was $diff; leaving hasRecentOverride=${state.hasRecentOverride}"
    return false
  }
}

private def tempCheck(currentTemp, desiredTemp)
{
  if (state.debug) log.debug "TEMPCHECK#1(CT=$currentTemp,SP=$desiredTemp,FD=$fanDimmer.currentSwitch,FD_LVL=$fanDimmer.currentLevel,autoMode=$autoMode,FDTstring=$fanDiffTempString, FDTvalue=$fanDiffTempValue)"
  
  //convert Fan Diff Temp input enum string to number value and if user doesn't select a Fan Diff Temp default to 1.0 
  def fanDiffTempValue = (settings.fanDiffTempString != null && settings.fanDiffTempString != "") ? Double.parseDouble(settings.fanDiffTempString): 1.0
  
  def LowDiff = fanDiffTempValue*1 
  def MedDiff = fanDiffTempValue*2
  def HighDiff = fanDiffTempValue*3
  
  if (autoMode && !state.hasRecentOverride) {
    def currentLevel = fanDimmer.currentLevel
    switch (currentTemp - desiredTemp) {
      case { it >= HighDiff }:
        if (shouldAutoAdjust("high")) {
          // turn on fan high speed
          state.lastAutoLevelSetTime = now()
          fanDimmer.setLevel(90)
          if (state.debug) log.debug "HI speed(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, HighDiff=$HighDiff)"
        }
        break
      case { it >= MedDiff }:
        if (shouldAutoAdjust("medium")) {
          // turn on fan medium speed
          state.lastAutoLevelSetTime = now()
          fanDimmer.setLevel(60)
          if (state.debug) log.debug "MED speed(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, MedDiff=$MedDiff)"
        }
        break
      case { it >= LowDiff }:
        if (shouldAutoAdjust("low")) {
          // turn on fan low speed -- this is a place where we're likely to get false overrides
          if (fanDimmer.currentSwitch == "off") {   // if fan is OFF to make it easier on motor by   
            state.lastAutoLevelSetTime = now()
            fanDimmer.setLevel(90) // starting fan in High speed temporarily then
            state.lastAutoLevelSetTime = now()
            fanDimmer.setLevel(30, [delay: 2000]) // change to Low speed after 2 seconds
            if (state.debug) log.debug "LO speed after HI 2secs(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, LowDiff=$LowDiff)"
          } else {
            state.lastAutoLevelSetTime = now()
            fanDimmer.setLevel(30)  //fan is already running, not necessary to protect motor
            if (state.debug) log.debug "LO speed immediately(CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, LowDiff=$LowDiff)"
          }
        }
        break
      default:
        // check to see if fan should be turned off
        if (desiredTemp - currentTemp >= 0 && shouldAutoAdjust("off")) {  //below or equal to setpoint, turn off fan, zero level
          state.lastAutoLevelSetTime = now()
          fanDimmer.off()
          if (state.debug) log.debug "below SP+Diff=fan OFF (CT=$currentTemp, SP=$desiredTemp, FD-LVL=$fanDimmer.currentLevel, FD=$fanDimmer.currentSwitch,autoMode=$autoMode,)"
        } 
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