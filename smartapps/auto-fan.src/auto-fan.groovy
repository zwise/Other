definition(
  name: "Automatic Fan Manager",
  namespace: "zwise",
  author: "Zach Wise",
  description: "Automatic control for 3 Speed Ceiling Fan using Low, Medium, High speeds with any temperature sensor.",
  category: "My Apps",
  iconUrl: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png", 
  iconX2Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
  iconX3Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png"
)

preferences {
  // The parent app preferences are pretty simple: just use the app input for the child app.
  page(name: "mainPage", title: "Fan Automations", install: true, uninstall: true, submitOnChange: true) {
    section {
      app(name: "automaticFan", appName: "Automatic Fan", namespace: "zwise", title: "New Fan Automation", multiple: true)
    }
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
  def children = getChildApps()
  log.debug "there are ${children.size()} fan automations"
}