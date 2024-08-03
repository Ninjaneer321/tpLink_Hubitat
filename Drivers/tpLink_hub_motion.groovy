/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Verified on T100 Motion Sensor.
=================================================================================================*/

metadata {
	definition (name: "TpLink Hub Motion", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub_motion.groovy")
	{
		capability "Refresh"
		capability "Sensor"
		capability "Motion Sensor"
		attribute "lowBattery", "string"
	}
	preferences {
		input ("getTriggerLogs", "bool", title: "Get Device's last 20 trigger logs", defaultValue: false)
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
	}
}

def installed() { runIn(1, updated) }

def updated() {
	if (getTriggerLogs == true) {
		getTriggerLog(20)
	} else {
		unschedule()
		def logData = [method: "updated"]
		logData << setLogsOff()
		logInfo(logData)
	}
	pauseExecution(5000)
}

def refresh() { parent.refresh() }

//	Parse Methods
def parseDevData(childData) {
	try {
		def motion = "inactive"
		if (childData.detected) { motion = "active" }
		updateAttr("motion", motion)
		updateAttr("lowBattery", childData.at_low_battery.toString())
	} catch (err) {
		logWarn([method: "parseDevData", status: "FAILED", error: err])
	}
}

def getTriggerLog(count = 1) {
	Map cmdBody = [
		method: "control_child",
		params: [
			device_id: getDataValue("deviceId"),
			requestData: [
				method: "get_trigger_logs",
				params: [page_size: count,"start_id": 0]
			]
		]
	]
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), "distTriggerLog")
}

def parseTriggerLog(triggerData, data=null) {
	def triggerLog = triggerData.result.responseData.result
	log.info "<b>TRIGGERLOGS</b>: ${triggerLog.logs}"
	device.updateSetting("getTriggerLogs", [type:"bool", value: false])
}

def updateAttr(attr, value) {
	if (device.currentValue(attr) != value) {
		sendEvent(name: attr, value: value)
	}
}



// ~~~~~ start include (30) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

def nameSpace() { return "davegut" } // library marker davegut.Logging, line 10

def version() { return "2.3.9a" } // library marker davegut.Logging, line 12

def label() { // library marker davegut.Logging, line 14
	if (device) {  // library marker davegut.Logging, line 15
		return device.displayName + "-${version()}" // library marker davegut.Logging, line 16
	} else {  // library marker davegut.Logging, line 17
		return app.getLabel() + "-${version()}" // library marker davegut.Logging, line 18
	} // library marker davegut.Logging, line 19
} // library marker davegut.Logging, line 20

def listAttributes() { // library marker davegut.Logging, line 22
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 23
	Map attrs = [:] // library marker davegut.Logging, line 24
	attrData.each { // library marker davegut.Logging, line 25
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 26
	} // library marker davegut.Logging, line 27
	return attrs // library marker davegut.Logging, line 28
} // library marker davegut.Logging, line 29

def setLogsOff() { // library marker davegut.Logging, line 31
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 32
	if (logEnable) { // library marker davegut.Logging, line 33
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 34
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 35
	} // library marker davegut.Logging, line 36
	return logData // library marker davegut.Logging, line 37
} // library marker davegut.Logging, line 38

def logTrace(msg){ log.trace "${label()}: ${msg}" } // library marker davegut.Logging, line 40

def logInfo(msg) {  // library marker davegut.Logging, line 42
	if (infoLog) { log.info "${label()}: ${msg}" } // library marker davegut.Logging, line 43
} // library marker davegut.Logging, line 44

def debugLogOff() { // library marker davegut.Logging, line 46
	if (device) { // library marker davegut.Logging, line 47
		device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 48
	} else { // library marker davegut.Logging, line 49
		app.updateSetting("logEnable", false) // library marker davegut.Logging, line 50
	} // library marker davegut.Logging, line 51
	logInfo("debugLogOff") // library marker davegut.Logging, line 52
} // library marker davegut.Logging, line 53

def logDebug(msg) { // library marker davegut.Logging, line 55
	if (logEnable) { log.debug "${label()}: ${msg}" } // library marker davegut.Logging, line 56
} // library marker davegut.Logging, line 57

def logWarn(msg) { log.warn "${label()}: ${msg}" } // library marker davegut.Logging, line 59

def logError(msg) { log.error "${label()}: ${msg}" } // library marker davegut.Logging, line 61

// ~~~~~ end include (30) davegut.Logging ~~~~~
