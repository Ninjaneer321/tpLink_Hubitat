/*	TP-Link SMART API / PROTOCOL DRIVER SERIES for plugs, switches, bulbs, hubs and Hub-connected devices.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

This driver is part of a set of drivers for TP-LINK SMART API plugs, switches, hubs,
sensors and thermostats.  The set encompasses the following:
a.	ALL TAPO devices except cameras and Robot Vacuum Cleaners.
b.	NEWER KASA devices using the SMART API except cameras.
	1.	MATTER devices
	2.	Kasa Hub
	3.	Kasa TRV
=================================================================================================*/
def type() { return "tpLink_bulb_color" }
def gitPath() { return "DaveGut/tpLink_Hubitat/main/Drivers/" }
//def type() {return "kasaSmart_bulb_color" }
//def gitPath() { return "DaveGut/HubitatActive/master/KasaDevices/DeviceDrivers/" }

metadata {
	definition (name: type(), namespace: "davegut", author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/${gitPath()}${type()}.groovy")
	{
		capability "Light"
		attribute "commsError", "string"
	}
	preferences {
		commonPreferences()
		dimmerPreferences()
		securityPreferences()
	}
}

def installed() { 
	runIn(5, updated)
}

def updated() { commonUpdated() }

def delayedUpdates() {
	Map logData = [setGradualOnOff: setGradualOnOff()]
	logData << [common: commonDelayedUpdates()]
	logInfo("delayedUpdates: ${logData}")
}

def deviceParse(resp, data=null) {
	def cmdResp = parseData(resp)
	if (cmdResp.status == "OK") {
		def devData = cmdResp.cmdResp
		if (devData.result.responses) {
			devData = devData.result.responses.find{it.method == "get_device_info"}
		} 
		if (devData != null && devData.error_code == 0) {
			devData = devData.result
			logDebug("deviceParse: ${devData}")
			def onOff = "off"
			if (devData.device_on == true) { onOff = "on" }
			updateAttr("switch", onOff)
			updateAttr("level", devData.brightness)
			if (devData.color_temp == 0) {
				updateAttr("colorMode", "COLOR")
				def hubHue = (devData.hue / 3.6).toInteger()
				updateAttr("hue", hubHue)
				updateAttr("saturation", devData.saturation)
				updateAttr("color", "[hue: ${hubHue}, saturation: ${devData.saturation}]")
				updateAttr("colorName", colorName = convertHueToGenericColorName(hubHue))
				def rgb = hubitat.helper.ColorUtils.hsvToRGB([hubHue,
															  devData.saturation,
															  devData.brightness])
				updateAttr("RGB", rgb)
				updateAttr("colorTemperature", 0)
			} else {
				updateAttr("colorMode", "CT")
				updateAttr("colorTemperature", devData.color_temp)
				updateAttr("colorName", convertTemperatureToGenericColorName(devData.color_temp.toInteger()))
				updateAttr("color", "[:]")
				updateAttr("RGB", "[]")
			}
		} else {
			logWarn("deviceParse: [status: FAILED, error: error in devData, cmdResp: ${cmdResp}]")
		}
	}
}

//	Library Inclusion








// ~~~~~ start include (1356) davegut.lib_tpLink_CapColor ~~~~~
library ( // library marker davegut.lib_tpLink_CapColor, line 1
	name: "lib_tpLink_CapColor", // library marker davegut.lib_tpLink_CapColor, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_CapColor, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_CapColor, line 4
	description: "Hubitat Capability Color and ColorTemperature Methods for TPLink SMART devices.", // library marker davegut.lib_tpLink_CapColor, line 5
	category: "utilities", // library marker davegut.lib_tpLink_CapColor, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_CapColor, line 7
) // library marker davegut.lib_tpLink_CapColor, line 8

capability "Color Temperature" // library marker davegut.lib_tpLink_CapColor, line 10
capability "Color Mode" // library marker davegut.lib_tpLink_CapColor, line 11
capability "Color Control" // library marker davegut.lib_tpLink_CapColor, line 12

def setColorTemperature(colorTemp, level = device.currentValue("level"), transTime=null) { // library marker davegut.lib_tpLink_CapColor, line 14
	logDebug("setColorTemperature: [level: ${level}, colorTemp: ${colorTemp}]") // library marker davegut.lib_tpLink_CapColor, line 15
	def lowCt = getDataValue("ctLow").toInteger() // library marker davegut.lib_tpLink_CapColor, line 16
	def highCt = getDataValue("ctHigh").toInteger() // library marker davegut.lib_tpLink_CapColor, line 17
	if (colorTemp < lowCt) { colorTemp = lowCt } // library marker davegut.lib_tpLink_CapColor, line 18
	else if (colorTemp > highCt) { colorTemp = highCt } // library marker davegut.lib_tpLink_CapColor, line 19
	List requests = [[ // library marker davegut.lib_tpLink_CapColor, line 20
		method: "set_device_info", // library marker davegut.lib_tpLink_CapColor, line 21
		params: [ // library marker davegut.lib_tpLink_CapColor, line 22
			device_on: true, // library marker davegut.lib_tpLink_CapColor, line 23
			brightness: level, // library marker davegut.lib_tpLink_CapColor, line 24
			color_temp: colorTemp // library marker davegut.lib_tpLink_CapColor, line 25
		]]] // library marker davegut.lib_tpLink_CapColor, line 26
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapColor, line 27
	asyncPassthrough(createMultiCmd(requests), "setColorTemperature", "deviceParse") // library marker davegut.lib_tpLink_CapColor, line 28
} // library marker davegut.lib_tpLink_CapColor, line 29

def setHue(hue){ // library marker davegut.lib_tpLink_CapColor, line 31
	logDebug("setHue: ${hue}") // library marker davegut.lib_tpLink_CapColor, line 32
	hue = (3.6 * hue).toInteger() // library marker davegut.lib_tpLink_CapColor, line 33
	logInfo("setHue: ${hue}") // library marker davegut.lib_tpLink_CapColor, line 34
	List requests = [[ // library marker davegut.lib_tpLink_CapColor, line 35
		method: "set_device_info", // library marker davegut.lib_tpLink_CapColor, line 36
		params: [ // library marker davegut.lib_tpLink_CapColor, line 37
			device_on: true, // library marker davegut.lib_tpLink_CapColor, line 38
			hue: hue, // library marker davegut.lib_tpLink_CapColor, line 39
			color_temp: 0 // library marker davegut.lib_tpLink_CapColor, line 40
		]]] // library marker davegut.lib_tpLink_CapColor, line 41
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapColor, line 42
	asyncPassthrough(createMultiCmd(requests), "setHue", "deviceParse") // library marker davegut.lib_tpLink_CapColor, line 43
} // library marker davegut.lib_tpLink_CapColor, line 44

def setSaturation(saturation) { // library marker davegut.lib_tpLink_CapColor, line 46
	logDebug("setSatiratopm: ${saturation}") // library marker davegut.lib_tpLink_CapColor, line 47
	List requests = [[ // library marker davegut.lib_tpLink_CapColor, line 48
		method: "set_device_info", // library marker davegut.lib_tpLink_CapColor, line 49
		params: [ // library marker davegut.lib_tpLink_CapColor, line 50
			device_on: true, // library marker davegut.lib_tpLink_CapColor, line 51
			saturation: saturation, // library marker davegut.lib_tpLink_CapColor, line 52
			color_temp: 0 // library marker davegut.lib_tpLink_CapColor, line 53
		]]] // library marker davegut.lib_tpLink_CapColor, line 54
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapColor, line 55
	asyncPassthrough(createMultiCmd(requests), "setSaturation", "deviceParse") // library marker davegut.lib_tpLink_CapColor, line 56
} // library marker davegut.lib_tpLink_CapColor, line 57

def setColor(color) { // library marker davegut.lib_tpLink_CapColor, line 59
	def hue = (3.6 * color.hue).toInteger() // library marker davegut.lib_tpLink_CapColor, line 60
	logDebug("setColor: ${color}") // library marker davegut.lib_tpLink_CapColor, line 61
	List requests = [[ // library marker davegut.lib_tpLink_CapColor, line 62
		method: "set_device_info", // library marker davegut.lib_tpLink_CapColor, line 63
		params: [ // library marker davegut.lib_tpLink_CapColor, line 64
			device_on: true, // library marker davegut.lib_tpLink_CapColor, line 65
			hue: hue, // library marker davegut.lib_tpLink_CapColor, line 66
			saturation: color.saturation, // library marker davegut.lib_tpLink_CapColor, line 67
			brightness: color.level, // library marker davegut.lib_tpLink_CapColor, line 68
			color_temp: 0 // library marker davegut.lib_tpLink_CapColor, line 69
		]]] // library marker davegut.lib_tpLink_CapColor, line 70
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapColor, line 71
	asyncPassthrough(createMultiCmd(requests), "setColor", "deviceParse") // library marker davegut.lib_tpLink_CapColor, line 72
} // library marker davegut.lib_tpLink_CapColor, line 73

// ~~~~~ end include (1356) davegut.lib_tpLink_CapColor ~~~~~

// ~~~~~ start include (1355) davegut.lib_tpLink_CapDimmer ~~~~~
library ( // library marker davegut.lib_tpLink_CapDimmer, line 1
	name: "lib_tpLink_CapDimmer", // library marker davegut.lib_tpLink_CapDimmer, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_CapDimmer, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_CapDimmer, line 4
	description: "Hubitat Capability Switch Level and Change Level Methods for TPLink SMART devices.", // library marker davegut.lib_tpLink_CapDimmer, line 5
	category: "utilities", // library marker davegut.lib_tpLink_CapDimmer, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_CapDimmer, line 7
) // library marker davegut.lib_tpLink_CapDimmer, line 8

capability "Switch" // library marker davegut.lib_tpLink_CapDimmer, line 10
capability "Switch Level" // library marker davegut.lib_tpLink_CapDimmer, line 11
capability "Change Level" // library marker davegut.lib_tpLink_CapDimmer, line 12

def dimmerPreferences() { // library marker davegut.lib_tpLink_CapDimmer, line 14
	input ("gradualOnOff", "bool", title: "Set Gradual ON/OFF", defaultValue: false) // library marker davegut.lib_tpLink_CapDimmer, line 15
} // library marker davegut.lib_tpLink_CapDimmer, line 16

def setGradualOnOff() { // library marker davegut.lib_tpLink_CapDimmer, line 18
	List requests = [[method: "set_on_off_gradually_info", params: [enable: gradualOnOff]]] // library marker davegut.lib_tpLink_CapDimmer, line 19
	requests << [method: "get_on_off_gradually_info"] // library marker davegut.lib_tpLink_CapDimmer, line 20
	def cmdResp = syncPassthrough(createMultiCmd(requests)) // library marker davegut.lib_tpLink_CapDimmer, line 21
	def gradOnOffData = cmdResp.result.responses.find { it.method == "get_on_off_gradually_info" } // library marker davegut.lib_tpLink_CapDimmer, line 22
	def newGradualOnOff = gradOnOffData.result.enable // library marker davegut.lib_tpLink_CapDimmer, line 23
	device.updateSetting("gradualOnOff",[type:"bool", value: newGradualOnOff]) // library marker davegut.lib_tpLink_CapDimmer, line 24
	return newGradualOnOff // library marker davegut.lib_tpLink_CapDimmer, line 25
} // library marker davegut.lib_tpLink_CapDimmer, line 26

def setLevel(level, transTime=null) { // library marker davegut.lib_tpLink_CapDimmer, line 28
	//	Note: Tapo Devices do not support transition time.  Set preference "Set Bulb to Gradual ON/OFF" // library marker davegut.lib_tpLink_CapDimmer, line 29
	logDebug("setLevel: [brightness: ${level}]") // library marker davegut.lib_tpLink_CapDimmer, line 30
	List requests = [[ // library marker davegut.lib_tpLink_CapDimmer, line 31
		method: "set_device_info", // library marker davegut.lib_tpLink_CapDimmer, line 32
		params: [ // library marker davegut.lib_tpLink_CapDimmer, line 33
			device_on: true, // library marker davegut.lib_tpLink_CapDimmer, line 34
			brightness: level // library marker davegut.lib_tpLink_CapDimmer, line 35
		]]] // library marker davegut.lib_tpLink_CapDimmer, line 36
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapDimmer, line 37
	asyncPassthrough(createMultiCmd(requests), "setLevel", "deviceParse") // library marker davegut.lib_tpLink_CapDimmer, line 38
} // library marker davegut.lib_tpLink_CapDimmer, line 39

def startLevelChange(direction) { // library marker davegut.lib_tpLink_CapDimmer, line 41
	logDebug("startLevelChange: [level: ${device.currentValue("level")}, direction: ${direction}]") // library marker davegut.lib_tpLink_CapDimmer, line 42
	if (direction == "up") { levelUp() } // library marker davegut.lib_tpLink_CapDimmer, line 43
	else { levelDown() } // library marker davegut.lib_tpLink_CapDimmer, line 44
} // library marker davegut.lib_tpLink_CapDimmer, line 45

def stopLevelChange() { // library marker davegut.lib_tpLink_CapDimmer, line 47
	logDebug("stopLevelChange: [level: ${device.currentValue("level")}]") // library marker davegut.lib_tpLink_CapDimmer, line 48
	unschedule(levelUp) // library marker davegut.lib_tpLink_CapDimmer, line 49
	unschedule(levelDown) // library marker davegut.lib_tpLink_CapDimmer, line 50
} // library marker davegut.lib_tpLink_CapDimmer, line 51

def levelUp() { // library marker davegut.lib_tpLink_CapDimmer, line 53
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.lib_tpLink_CapDimmer, line 54
	if (curLevel != 100) { // library marker davegut.lib_tpLink_CapDimmer, line 55
		def newLevel = curLevel + 4 // library marker davegut.lib_tpLink_CapDimmer, line 56
		if (newLevel > 100) { newLevel = 100 } // library marker davegut.lib_tpLink_CapDimmer, line 57
		setLevel(newLevel) // library marker davegut.lib_tpLink_CapDimmer, line 58
		runIn(1, levelUp) // library marker davegut.lib_tpLink_CapDimmer, line 59
	} // library marker davegut.lib_tpLink_CapDimmer, line 60
} // library marker davegut.lib_tpLink_CapDimmer, line 61

def levelDown() { // library marker davegut.lib_tpLink_CapDimmer, line 63
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.lib_tpLink_CapDimmer, line 64
	if (device.currentValue("switch") == "on") { // library marker davegut.lib_tpLink_CapDimmer, line 65
		def newLevel = curLevel - 4 // library marker davegut.lib_tpLink_CapDimmer, line 66
		if (newLevel <= 0) { off() } // library marker davegut.lib_tpLink_CapDimmer, line 67
		else { // library marker davegut.lib_tpLink_CapDimmer, line 68
			setLevel(newLevel) // library marker davegut.lib_tpLink_CapDimmer, line 69
			runIn(1, levelDown) // library marker davegut.lib_tpLink_CapDimmer, line 70
		} // library marker davegut.lib_tpLink_CapDimmer, line 71
	} // library marker davegut.lib_tpLink_CapDimmer, line 72
} // library marker davegut.lib_tpLink_CapDimmer, line 73

// ~~~~~ end include (1355) davegut.lib_tpLink_CapDimmer ~~~~~

// ~~~~~ start include (1354) davegut.lib_tpLink_CapSwitch ~~~~~
library ( // library marker davegut.lib_tpLink_CapSwitch, line 1
	name: "lib_tpLink_CapSwitch", // library marker davegut.lib_tpLink_CapSwitch, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_CapSwitch, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_CapSwitch, line 4
	description: "Hubitat Capability Switch Methods for TPLink SMART devices.", // library marker davegut.lib_tpLink_CapSwitch, line 5
	category: "utilities", // library marker davegut.lib_tpLink_CapSwitch, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_CapSwitch, line 7
) // library marker davegut.lib_tpLink_CapSwitch, line 8

capability "Switch" // library marker davegut.lib_tpLink_CapSwitch, line 10

def plugSwitchPreferences() { // library marker davegut.lib_tpLink_CapSwitch, line 12
	input ("autoOffEnable", "bool", title: "Enable Auto Off", defaultValue: false) // library marker davegut.lib_tpLink_CapSwitch, line 13
	input ("autoOffTime", "NUMBER", title: "Auto Off Time (minutes)", defaultValue: 120) // library marker davegut.lib_tpLink_CapSwitch, line 14
	input ("defState", "enum", title: "Power Loss Default State", // library marker davegut.lib_tpLink_CapSwitch, line 15
		   options: ["lastState", "on", "off"], defaultValue: "lastState") // library marker davegut.lib_tpLink_CapSwitch, line 16
} // library marker davegut.lib_tpLink_CapSwitch, line 17

def on() { // library marker davegut.lib_tpLink_CapSwitch, line 19
	setPower(true) // library marker davegut.lib_tpLink_CapSwitch, line 20
	if (autoOffEnable) { // library marker davegut.lib_tpLink_CapSwitch, line 21
		runIn(5 + 60 * autoOffTime.toInteger(), refresh) // library marker davegut.lib_tpLink_CapSwitch, line 22
	} // library marker davegut.lib_tpLink_CapSwitch, line 23
} // library marker davegut.lib_tpLink_CapSwitch, line 24

def off() { // library marker davegut.lib_tpLink_CapSwitch, line 26
	setPower(false) // library marker davegut.lib_tpLink_CapSwitch, line 27
	unschedule(off) // library marker davegut.lib_tpLink_CapSwitch, line 28
} // library marker davegut.lib_tpLink_CapSwitch, line 29

def setPower(onOff) { // library marker davegut.lib_tpLink_CapSwitch, line 31
	logDebug("setPower: [device_on: ${onOff}]") // library marker davegut.lib_tpLink_CapSwitch, line 32
	List requests = [[ // library marker davegut.lib_tpLink_CapSwitch, line 33
		method: "set_device_info", // library marker davegut.lib_tpLink_CapSwitch, line 34
		params: [device_on: onOff]]] // library marker davegut.lib_tpLink_CapSwitch, line 35
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapSwitch, line 36
	asyncPassthrough(createMultiCmd(requests), "setPower", "deviceParse") // library marker davegut.lib_tpLink_CapSwitch, line 37
} // library marker davegut.lib_tpLink_CapSwitch, line 38

def setAutoOff() { // library marker davegut.lib_tpLink_CapSwitch, line 40
	List requests =  [[method: "set_auto_off_config", // library marker davegut.lib_tpLink_CapSwitch, line 41
					   params: [enable:autoOffEnable, // library marker davegut.lib_tpLink_CapSwitch, line 42
								delay_min: autoOffTime.toInteger()]]] // library marker davegut.lib_tpLink_CapSwitch, line 43
	requests << [method: "get_auto_off_config"] // library marker davegut.lib_tpLink_CapSwitch, line 44
	def devData = syncPassthrough(createMultiCmd(requests)) // library marker davegut.lib_tpLink_CapSwitch, line 45
	Map retData = [cmdResp: "ERROR"] // library marker davegut.lib_tpLink_CapSwitch, line 46
	if (cmdResp != "ERROR") { // library marker davegut.lib_tpLink_CapSwitch, line 47
		def data = devData.result.responses.find { it.method == "get_auto_off_config" } // library marker davegut.lib_tpLink_CapSwitch, line 48
		device.updateSetting("autoOffTime", [type: "number", value: data.result.delay_min]) // library marker davegut.lib_tpLink_CapSwitch, line 49
		device.updateSetting("autoOffEnable", [type: "bool", value: data.result.enable]) // library marker davegut.lib_tpLink_CapSwitch, line 50
		retData = [enable: data.result.enable, time: data.result.delay_min] // library marker davegut.lib_tpLink_CapSwitch, line 51
	} // library marker davegut.lib_tpLink_CapSwitch, line 52
	return retData // library marker davegut.lib_tpLink_CapSwitch, line 53
} // library marker davegut.lib_tpLink_CapSwitch, line 54

def setDefaultState() { // library marker davegut.lib_tpLink_CapSwitch, line 56
	def type = "last_states" // library marker davegut.lib_tpLink_CapSwitch, line 57
	def state = [] // library marker davegut.lib_tpLink_CapSwitch, line 58
	if (defState == "on") { // library marker davegut.lib_tpLink_CapSwitch, line 59
		type = "custom" // library marker davegut.lib_tpLink_CapSwitch, line 60
		state = [on: true] // library marker davegut.lib_tpLink_CapSwitch, line 61
	} else if (defState == "off") { // library marker davegut.lib_tpLink_CapSwitch, line 62
		type = "custom" // library marker davegut.lib_tpLink_CapSwitch, line 63
		state = [on: false] // library marker davegut.lib_tpLink_CapSwitch, line 64
	} // library marker davegut.lib_tpLink_CapSwitch, line 65
	List requests = [[method: "set_device_info", // library marker davegut.lib_tpLink_CapSwitch, line 66
					  params: [default_states: [type: type, state: state]]]] // library marker davegut.lib_tpLink_CapSwitch, line 67
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapSwitch, line 68
	def devData = syncPassthrough(createMultiCmd(requests)) // library marker davegut.lib_tpLink_CapSwitch, line 69
	Map retData = [cmdResp: "ERROR"] // library marker davegut.lib_tpLink_CapSwitch, line 70
	if (cmdResp != "ERROR") { // library marker davegut.lib_tpLink_CapSwitch, line 71
		def data = devData.result.responses.find { it.method == "get_device_info" } // library marker davegut.lib_tpLink_CapSwitch, line 72
		def defaultStates = data.result.default_states // library marker davegut.lib_tpLink_CapSwitch, line 73
		def newState = "lastState" // library marker davegut.lib_tpLink_CapSwitch, line 74
		if (defaultStates.type == "custom"){ // library marker davegut.lib_tpLink_CapSwitch, line 75
			newState = "off" // library marker davegut.lib_tpLink_CapSwitch, line 76
			if (defaultStates.state.on == true) { // library marker davegut.lib_tpLink_CapSwitch, line 77
				newState = "on" // library marker davegut.lib_tpLink_CapSwitch, line 78
			} // library marker davegut.lib_tpLink_CapSwitch, line 79
		} // library marker davegut.lib_tpLink_CapSwitch, line 80
		device.updateSetting("defState", [type: "enum", value: newState]) // library marker davegut.lib_tpLink_CapSwitch, line 81
		retData = [defState: newState] // library marker davegut.lib_tpLink_CapSwitch, line 82
	} // library marker davegut.lib_tpLink_CapSwitch, line 83
	return retData // library marker davegut.lib_tpLink_CapSwitch, line 84
} // library marker davegut.lib_tpLink_CapSwitch, line 85

// ~~~~~ end include (1354) davegut.lib_tpLink_CapSwitch ~~~~~

// ~~~~~ start include (1335) davegut.lib_tpLink_common ~~~~~
library ( // library marker davegut.lib_tpLink_common, line 1
	name: "lib_tpLink_common", // library marker davegut.lib_tpLink_common, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_common, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_common, line 4
	description: "Method common to tpLink device DRIVERS", // library marker davegut.lib_tpLink_common, line 5
	category: "utilities", // library marker davegut.lib_tpLink_common, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_common, line 7
) // library marker davegut.lib_tpLink_common, line 8
def driverVer() { return "1.0" } // library marker davegut.lib_tpLink_common, line 9

capability "Refresh" // library marker davegut.lib_tpLink_common, line 11

def commonPreferences() { // library marker davegut.lib_tpLink_common, line 13
	input ("nameSync", "enum", title: "Synchronize Names", // library marker davegut.lib_tpLink_common, line 14
		   options: ["none": "Don't synchronize", // library marker davegut.lib_tpLink_common, line 15
					 "device" : "TP-Link device name master", // library marker davegut.lib_tpLink_common, line 16
					 "Hubitat" : "Hubitat label master"], // library marker davegut.lib_tpLink_common, line 17
		   defaultValue: "none") // library marker davegut.lib_tpLink_common, line 18
	input ("pollInterval", "enum", title: "Poll Interval (< 1 min can cause issues)", // library marker davegut.lib_tpLink_common, line 19
		   options: ["5 sec", "10 sec", "30 sec", "1 min", "10 min"], // library marker davegut.lib_tpLink_common, line 20
		   defaultValue: "10 min") // library marker davegut.lib_tpLink_common, line 21
	input ("developerData", "bool", title: "Get Data for Developer", defaultValue: false) // library marker davegut.lib_tpLink_common, line 22
	input ("rebootDev", "bool", title: "Reboot Device then run Save Preferences", defaultValue: false) // library marker davegut.lib_tpLink_common, line 23
} // library marker davegut.lib_tpLink_common, line 24

def commonUpdated() { // library marker davegut.lib_tpLink_common, line 26
	unschedule() // library marker davegut.lib_tpLink_common, line 27
	Map logData = [:] // library marker davegut.lib_tpLink_common, line 28
	if (rebootDev == true) { // library marker davegut.lib_tpLink_common, line 29
		runInMillis(50, rebootDevice) // library marker davegut.lib_tpLink_common, line 30
		device.updateSetting("rebootDev",[type:"bool", value: false]) // library marker davegut.lib_tpLink_common, line 31
		pauseExecution(5000) // library marker davegut.lib_tpLink_common, line 32
	} // library marker davegut.lib_tpLink_common, line 33
	updateAttr("commsError", false) // library marker davegut.lib_tpLink_common, line 34
	state.errorCount = 0 // library marker davegut.lib_tpLink_common, line 35
	state.lastCmd = "" // library marker davegut.lib_tpLink_common, line 36
	logData << [login: setLoginInterval()] // library marker davegut.lib_tpLink_common, line 37
	logData << setLogsOff() // library marker davegut.lib_tpLink_common, line 38
	logData << deviceLogin() // library marker davegut.lib_tpLink_common, line 39
	pauseExecution(5000) // library marker davegut.lib_tpLink_common, line 40
	if (logData.status == "ERROR") { // library marker davegut.lib_tpLink_common, line 41
		logError("updated: ${logData}") // library marker davegut.lib_tpLink_common, line 42
	} else { // library marker davegut.lib_tpLink_common, line 43
		logInfo("updated: ${logData}") // library marker davegut.lib_tpLink_common, line 44
	} // library marker davegut.lib_tpLink_common, line 45
	runIn(3, delayedUpdates) // library marker davegut.lib_tpLink_common, line 46
	pauseExecution(10000) // library marker davegut.lib_tpLink_common, line 47
} // library marker davegut.lib_tpLink_common, line 48

def commonDelayedUpdates() { // library marker davegut.lib_tpLink_common, line 50
	Map logData = [syncName: syncName()] // library marker davegut.lib_tpLink_common, line 51
	logData << [pollInterval: setPollInterval()] // library marker davegut.lib_tpLink_common, line 52
	if (developerData) { getDeveloperData() } // library marker davegut.lib_tpLink_common, line 53
	runEvery10Minutes(refresh) // library marker davegut.lib_tpLink_common, line 54
	logData << [refresh: "15 mins"] // library marker davegut.lib_tpLink_common, line 55
	refresh() // library marker davegut.lib_tpLink_common, line 56
	return logData // library marker davegut.lib_tpLink_common, line 57
} // library marker davegut.lib_tpLink_common, line 58

def rebootDevice() { // library marker davegut.lib_tpLink_common, line 60
	logWarn("rebootDevice: Rebooting device per preference request") // library marker davegut.lib_tpLink_common, line 61
	def result = syncPassthrough([method: "device_reboot"]) // library marker davegut.lib_tpLink_common, line 62
	logWarn("rebootDevice: ${result}") // library marker davegut.lib_tpLink_common, line 63
} // library marker davegut.lib_tpLink_common, line 64

def setPollInterval() { // library marker davegut.lib_tpLink_common, line 66
	def method = "poll" // library marker davegut.lib_tpLink_common, line 67
	if (getDataValue("capability") == "plug_em") { // library marker davegut.lib_tpLink_common, line 68
		method = "emPoll" // library marker davegut.lib_tpLink_common, line 69
	} // library marker davegut.lib_tpLink_common, line 70
	if (pollInterval.contains("sec")) { // library marker davegut.lib_tpLink_common, line 71
		def interval= pollInterval.replace(" sec", "").toInteger() // library marker davegut.lib_tpLink_common, line 72
		def start = Math.round((interval-1) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 73
		schedule("${start}/${interval} * * * * ?", method) // library marker davegut.lib_tpLink_common, line 74
		logWarn("setPollInterval: Polling intervals of less than one minute " + // library marker davegut.lib_tpLink_common, line 75
				"can take high resources and may impact hub performance.") // library marker davegut.lib_tpLink_common, line 76
	} else { // library marker davegut.lib_tpLink_common, line 77
		def interval= pollInterval.replace(" min", "").toInteger() // library marker davegut.lib_tpLink_common, line 78
		def start = Math.round(59 * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 79
		schedule("${start} */${interval} * * * ?", method) // library marker davegut.lib_tpLink_common, line 80
	} // library marker davegut.lib_tpLink_common, line 81
	return pollInterval // library marker davegut.lib_tpLink_common, line 82
} // library marker davegut.lib_tpLink_common, line 83

def setLoginInterval() { // library marker davegut.lib_tpLink_common, line 85
	def startS = Math.round((59) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 86
	def startM = Math.round((59) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 87
	def startH = Math.round((11) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 88
	schedule("${startS} ${startM} ${startH}/12 * * ?", "deviceLogin") // library marker davegut.lib_tpLink_common, line 89
	return "12 hrs" // library marker davegut.lib_tpLink_common, line 90
} // library marker davegut.lib_tpLink_common, line 91

def syncName() { // library marker davegut.lib_tpLink_common, line 93
	def logData = [syncName: nameSync] // library marker davegut.lib_tpLink_common, line 94
	if (nameSync == "none") { // library marker davegut.lib_tpLink_common, line 95
		logData << [status: "Label Not Updated"] // library marker davegut.lib_tpLink_common, line 96
	} else { // library marker davegut.lib_tpLink_common, line 97
		def cmdResp // library marker davegut.lib_tpLink_common, line 98
		String nickname // library marker davegut.lib_tpLink_common, line 99
		if (nameSync == "device") { // library marker davegut.lib_tpLink_common, line 100
			cmdResp = syncPassthrough([method: "get_device_info"]) // library marker davegut.lib_tpLink_common, line 101
			nickname = cmdResp.result.nickname // library marker davegut.lib_tpLink_common, line 102
		} else if (nameSync == "Hubitat") { // library marker davegut.lib_tpLink_common, line 103
			nickname = device.getLabel().bytes.encodeBase64().toString() // library marker davegut.lib_tpLink_common, line 104
			List requests = [[method: "set_device_info",params: [nickname: nickname]]] // library marker davegut.lib_tpLink_common, line 105
			requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_common, line 106
			cmdResp = syncPassthrough(createMultiCmd(requests)) // library marker davegut.lib_tpLink_common, line 107
			cmdResp = cmdResp.result.responses.find { it.method == "get_device_info" } // library marker davegut.lib_tpLink_common, line 108
			nickname = cmdResp.result.nickname // library marker davegut.lib_tpLink_common, line 109
		} // library marker davegut.lib_tpLink_common, line 110
		byte[] plainBytes = nickname.decodeBase64() // library marker davegut.lib_tpLink_common, line 111
		String label = new String(plainBytes) // library marker davegut.lib_tpLink_common, line 112
		device.setLabel(label) // library marker davegut.lib_tpLink_common, line 113
		logData << [nickname: nickname, label: label, status: "Label Updated"] // library marker davegut.lib_tpLink_common, line 114
	} // library marker davegut.lib_tpLink_common, line 115
	device.updateSetting("nameSync",[type: "enum", value: "none"]) // library marker davegut.lib_tpLink_common, line 116
	return logData // library marker davegut.lib_tpLink_common, line 117
} // library marker davegut.lib_tpLink_common, line 118

def getDeveloperData() { // library marker davegut.lib_tpLink_common, line 120
	device.updateSetting("developerData",[type:"bool", value: false]) // library marker davegut.lib_tpLink_common, line 121
	def attrData = device.getCurrentStates() // library marker davegut.lib_tpLink_common, line 122
	Map attrs = [:] // library marker davegut.lib_tpLink_common, line 123
	attrData.each { // library marker davegut.lib_tpLink_common, line 124
		attrs << ["${it.name}": it.value] // library marker davegut.lib_tpLink_common, line 125
	} // library marker davegut.lib_tpLink_common, line 126
	Date date = new Date() // library marker davegut.lib_tpLink_common, line 127
	Map devData = [ // library marker davegut.lib_tpLink_common, line 128
		currentTime: date.toString(), // library marker davegut.lib_tpLink_common, line 129
		lastLogin: state.lastSuccessfulLogin, // library marker davegut.lib_tpLink_common, line 130
		name: device.getName(), // library marker davegut.lib_tpLink_common, line 131
		status: device.getStatus(), // library marker davegut.lib_tpLink_common, line 132
		aesKey: aesKey, // library marker davegut.lib_tpLink_common, line 133
		cookie: getDataValue("deviceCookie"), // library marker davegut.lib_tpLink_common, line 134
		tokenLen: getDataValue("deviceToken"), // library marker davegut.lib_tpLink_common, line 135
		dataValues: device.getData(), // library marker davegut.lib_tpLink_common, line 136
		attributes: attrs, // library marker davegut.lib_tpLink_common, line 137
		cmdResp: syncPassthrough([method: "get_device_info"]), // library marker davegut.lib_tpLink_common, line 138
		childData: getChildDevData() // library marker davegut.lib_tpLink_common, line 139
	] // library marker davegut.lib_tpLink_common, line 140
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.lib_tpLink_common, line 141
} // library marker davegut.lib_tpLink_common, line 142

def getChildDevData(){ // library marker davegut.lib_tpLink_common, line 144
	Map cmdBody = [ // library marker davegut.lib_tpLink_common, line 145
		method: "get_child_device_list" // library marker davegut.lib_tpLink_common, line 146
	] // library marker davegut.lib_tpLink_common, line 147
	def childData = syncPassthrough(cmdBody) // library marker davegut.lib_tpLink_common, line 148
	if (childData.error_code == 0) { // library marker davegut.lib_tpLink_common, line 149
		return childData.result.child_device_list // library marker davegut.lib_tpLink_common, line 150
	} else { // library marker davegut.lib_tpLink_common, line 151
		return "noChildren" // library marker davegut.lib_tpLink_common, line 152
	} // library marker davegut.lib_tpLink_common, line 153
} // library marker davegut.lib_tpLink_common, line 154

def deviceLogin() { // library marker davegut.lib_tpLink_common, line 156
	Map logData = [:] // library marker davegut.lib_tpLink_common, line 157
	def handshakeData = handshake(getDataValue("deviceIP")) // library marker davegut.lib_tpLink_common, line 158
	if (handshakeData.respStatus == "OK") { // library marker davegut.lib_tpLink_common, line 159
		Map credentials = [encUsername: getDataValue("encUsername"),  // library marker davegut.lib_tpLink_common, line 160
						   encPassword: getDataValue("encPassword")] // library marker davegut.lib_tpLink_common, line 161
		def tokenData = loginDevice(handshakeData.cookie, handshakeData.aesKey,  // library marker davegut.lib_tpLink_common, line 162
									credentials, getDataValue("deviceIP")) // library marker davegut.lib_tpLink_common, line 163
		if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_common, line 164
			logData << [rsaKeys: handshakeData.rsaKeys, // library marker davegut.lib_tpLink_common, line 165
						cookie: handshakeData.cookie, // library marker davegut.lib_tpLink_common, line 166
						aesKey: handshakeData.aesKey, // library marker davegut.lib_tpLink_common, line 167
						token: tokenData.token] // library marker davegut.lib_tpLink_common, line 168

			device.updateSetting("aesKey", [type:"password", value: handshakeData.aesKey]) // library marker davegut.lib_tpLink_common, line 170
			updateDataValue("deviceCookie", handshakeData.cookie) // library marker davegut.lib_tpLink_common, line 171
			updateDataValue("deviceToken", tokenData.token) // library marker davegut.lib_tpLink_common, line 172
			logData << [status: "OK"] // library marker davegut.lib_tpLink_common, line 173
		} else { // library marker davegut.lib_tpLink_common, line 174
			logData << [status: "ERROR.",tokenData: tokenData] // library marker davegut.lib_tpLink_common, line 175
		} // library marker davegut.lib_tpLink_common, line 176
	} else { // library marker davegut.lib_tpLink_common, line 177
		logData << [status: "ERROR",handshakeData: handshakeData] // library marker davegut.lib_tpLink_common, line 178
	} // library marker davegut.lib_tpLink_common, line 179
	Map logStatus = [:] // library marker davegut.lib_tpLink_common, line 180
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_common, line 181
		logInfo("deviceLogin: ${logData}") // library marker davegut.lib_tpLink_common, line 182
		logStatus << [logStatus: "SUCCESS"] // library marker davegut.lib_tpLink_common, line 183
	} else { // library marker davegut.lib_tpLink_common, line 184
		logWarn("deviceLogin: ${logData}") // library marker davegut.lib_tpLink_common, line 185
		logStatus << [logStatus: "FAILURE"] // library marker davegut.lib_tpLink_common, line 186
	} // library marker davegut.lib_tpLink_common, line 187
	return logStatus // library marker davegut.lib_tpLink_common, line 188
} // library marker davegut.lib_tpLink_common, line 189

def refresh() { // library marker davegut.lib_tpLink_common, line 191
	logDebug("refresh") // library marker davegut.lib_tpLink_common, line 192
	asyncPassthrough([method: "get_device_info"], "refresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 193
} // library marker davegut.lib_tpLink_common, line 194

def poll() { // library marker davegut.lib_tpLink_common, line 196
	logDebug("poll") // library marker davegut.lib_tpLink_common, line 197
	asyncPassthrough([method: "get_device_running_info"], "poll", "pollParse") // library marker davegut.lib_tpLink_common, line 198
} // library marker davegut.lib_tpLink_common, line 199

def pollParse(resp, data=null) { // library marker davegut.lib_tpLink_common, line 201
	def cmdResp = parseData(resp) // library marker davegut.lib_tpLink_common, line 202
	if (cmdResp.status == "OK") { // library marker davegut.lib_tpLink_common, line 203
		def devData = cmdResp.cmdResp.result // library marker davegut.lib_tpLink_common, line 204
		def onOff = "off" // library marker davegut.lib_tpLink_common, line 205
		if (devData.device_on == true) { onOff ="on" } // library marker davegut.lib_tpLink_common, line 206
		updateAttr("switch", onOff) // library marker davegut.lib_tpLink_common, line 207
	} // library marker davegut.lib_tpLink_common, line 208
} // library marker davegut.lib_tpLink_common, line 209

def emPoll() { // library marker davegut.lib_tpLink_common, line 211
	logDebug("poll") // library marker davegut.lib_tpLink_common, line 212
	List requests = [[method: "get_device_running_info"]] // library marker davegut.lib_tpLink_common, line 213
	requests << [method: "get_energy_usage"] // library marker davegut.lib_tpLink_common, line 214
	asyncPassthrough(createMultiCmd(requests), "emPoll", "emPollParse") // library marker davegut.lib_tpLink_common, line 215
} // library marker davegut.lib_tpLink_common, line 216

def emPollParse(resp, data=null) { // library marker davegut.lib_tpLink_common, line 218
	def cmdResp = parseData(resp) // library marker davegut.lib_tpLink_common, line 219
	if (cmdResp.status == "OK") { // library marker davegut.lib_tpLink_common, line 220
		def devData = cmdResp.cmdResp.result.responses.find{it.method == "get_device_running_info"}.result // library marker davegut.lib_tpLink_common, line 221
		def onOff = "off" // library marker davegut.lib_tpLink_common, line 222
		if (devData.device_on == true) { onOff ="on" } // library marker davegut.lib_tpLink_common, line 223
		updateAttr("switch", onOff) // library marker davegut.lib_tpLink_common, line 224
		def emData = cmdResp.cmdResp.result.responses.find{it.method == "get_energy_usage"} // library marker davegut.lib_tpLink_common, line 225
		if (emData.error_code == 0) { // library marker davegut.lib_tpLink_common, line 226
			emData = emData.result // library marker davegut.lib_tpLink_common, line 227
			updateAttr("power", emData.current_power) // library marker davegut.lib_tpLink_common, line 228
		} // library marker davegut.lib_tpLink_common, line 229
	} // library marker davegut.lib_tpLink_common, line 230
} // library marker davegut.lib_tpLink_common, line 231

def updateAttr(attr, value) { // library marker davegut.lib_tpLink_common, line 233
	if (device.currentValue(attr) != value) { // library marker davegut.lib_tpLink_common, line 234
		sendEvent(name: attr, value: value) // library marker davegut.lib_tpLink_common, line 235
	} // library marker davegut.lib_tpLink_common, line 236
} // library marker davegut.lib_tpLink_common, line 237

// ~~~~~ end include (1335) davegut.lib_tpLink_common ~~~~~

// ~~~~~ start include (1327) davegut.lib_tpLink_comms ~~~~~
library ( // library marker davegut.lib_tpLink_comms, line 1
	name: "lib_tpLink_comms", // library marker davegut.lib_tpLink_comms, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_comms, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_comms, line 4
	description: "Tapo Communications", // library marker davegut.lib_tpLink_comms, line 5
	category: "utilities", // library marker davegut.lib_tpLink_comms, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_comms, line 7
) // library marker davegut.lib_tpLink_comms, line 8
import org.json.JSONObject // library marker davegut.lib_tpLink_comms, line 9
import groovy.json.JsonOutput // library marker davegut.lib_tpLink_comms, line 10
import groovy.json.JsonBuilder // library marker davegut.lib_tpLink_comms, line 11
import groovy.json.JsonSlurper // library marker davegut.lib_tpLink_comms, line 12

def createMultiCmd(requests) { // library marker davegut.lib_tpLink_comms, line 14
	Map cmdBody = [ // library marker davegut.lib_tpLink_comms, line 15
		method: "multipleRequest", // library marker davegut.lib_tpLink_comms, line 16
		params: [requests: requests]] // library marker davegut.lib_tpLink_comms, line 17
	return cmdBody // library marker davegut.lib_tpLink_comms, line 18
} // library marker davegut.lib_tpLink_comms, line 19

def asyncPassthrough(cmdBody, method, action) { // library marker davegut.lib_tpLink_comms, line 21
	if (devIp == null) { devIp = getDataValue("deviceIP") }	//	used for Kasa Compatibility // library marker davegut.lib_tpLink_comms, line 22
	Map cmdData = [cmdBody: cmdBody, method: method, action: action] // library marker davegut.lib_tpLink_comms, line 23
	state.lastCmd = cmdData // library marker davegut.lib_tpLink_comms, line 24
	logDebug("asyncPassthrough: ${cmdData}") // library marker davegut.lib_tpLink_comms, line 25
	def uri = "http://${getDataValue("deviceIP")}/app?token=${getDataValue("deviceToken")}" // library marker davegut.lib_tpLink_comms, line 26
	Map reqBody = createReqBody(cmdBody) // library marker davegut.lib_tpLink_comms, line 27
	asyncPost(uri, reqBody, action, getDataValue("deviceCookie"), method) // library marker davegut.lib_tpLink_comms, line 28
} // library marker davegut.lib_tpLink_comms, line 29

def syncPassthrough(cmdBody) { // library marker davegut.lib_tpLink_comms, line 31
	if (devIp == null) { devIp = getDataValue("deviceIP") }	//	used for Kasa Compatibility // library marker davegut.lib_tpLink_comms, line 32
	Map logData = [cmdBody: cmdBody] // library marker davegut.lib_tpLink_comms, line 33
	def uri = "http://${getDataValue("deviceIP")}/app?token=${getDataValue("deviceToken")}" // library marker davegut.lib_tpLink_comms, line 34
	Map reqBody = createReqBody(cmdBody) // library marker davegut.lib_tpLink_comms, line 35
	def resp = syncPost(uri, reqBody, getDataValue("deviceCookie")) // library marker davegut.lib_tpLink_comms, line 36
	def cmdResp = "ERROR" // library marker davegut.lib_tpLink_comms, line 37
	if (resp.status == "OK") { // library marker davegut.lib_tpLink_comms, line 38
		try { // library marker davegut.lib_tpLink_comms, line 39
			cmdResp = new JsonSlurper().parseText(decrypt(resp.resp.data.result.response)) // library marker davegut.lib_tpLink_comms, line 40
			logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 41
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 42
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_comms, line 43
		} // library marker davegut.lib_tpLink_comms, line 44
	} else { // library marker davegut.lib_tpLink_comms, line 45
		logData << [status: "postJsonError", postJsonData: resp] // library marker davegut.lib_tpLink_comms, line 46
	} // library marker davegut.lib_tpLink_comms, line 47
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 48
		logDebug("syncPassthrough: ${logData}") // library marker davegut.lib_tpLink_comms, line 49
	} else { // library marker davegut.lib_tpLink_comms, line 50
		logWarn("syncPassthrough: ${logData}") // library marker davegut.lib_tpLink_comms, line 51
	} // library marker davegut.lib_tpLink_comms, line 52
	return cmdResp // library marker davegut.lib_tpLink_comms, line 53
} // library marker davegut.lib_tpLink_comms, line 54

def createReqBody(cmdBody) { // library marker davegut.lib_tpLink_comms, line 56
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_comms, line 57
	Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_comms, line 58
				   params: [request: encrypt(cmdStr)]] // library marker davegut.lib_tpLink_comms, line 59
	return reqBody // library marker davegut.lib_tpLink_comms, line 60
} // library marker davegut.lib_tpLink_comms, line 61

//	===== Sync comms for device update ===== // library marker davegut.lib_tpLink_comms, line 63
def syncPost(uri, reqBody, cookie=null) { // library marker davegut.lib_tpLink_comms, line 64
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 65
		uri: uri, // library marker davegut.lib_tpLink_comms, line 66
		headers: [ // library marker davegut.lib_tpLink_comms, line 67
			Cookie: cookie // library marker davegut.lib_tpLink_comms, line 68
		], // library marker davegut.lib_tpLink_comms, line 69
		body : new JsonBuilder(reqBody).toString() // library marker davegut.lib_tpLink_comms, line 70
	] // library marker davegut.lib_tpLink_comms, line 71
	logDebug("syncPost: [cmdParams: ${reqParams}]") // library marker davegut.lib_tpLink_comms, line 72
	Map respData = [:] // library marker davegut.lib_tpLink_comms, line 73
	try { // library marker davegut.lib_tpLink_comms, line 74
		httpPostJson(reqParams) {resp -> // library marker davegut.lib_tpLink_comms, line 75
			if (resp.status == 200 && resp.data.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 76
				respData << [status: "OK", resp: resp] // library marker davegut.lib_tpLink_comms, line 77
			} else { // library marker davegut.lib_tpLink_comms, line 78
				respData << [status: "lanDataError", respStatus: resp.status,  // library marker davegut.lib_tpLink_comms, line 79
					errorCode: resp.data.error_code] // library marker davegut.lib_tpLink_comms, line 80
			} // library marker davegut.lib_tpLink_comms, line 81
		} // library marker davegut.lib_tpLink_comms, line 82
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 83
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_comms, line 84
	} // library marker davegut.lib_tpLink_comms, line 85
	return respData // library marker davegut.lib_tpLink_comms, line 86
} // library marker davegut.lib_tpLink_comms, line 87

def asyncPost(uri, reqBody, parseMethod, cookie=null, reqData=null) { // library marker davegut.lib_tpLink_comms, line 89
	Map logData = [:] // library marker davegut.lib_tpLink_comms, line 90
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 91
		uri: uri, // library marker davegut.lib_tpLink_comms, line 92
		requestContentType: 'application/json', // library marker davegut.lib_tpLink_comms, line 93
		contentType: 'application/json', // library marker davegut.lib_tpLink_comms, line 94
		headers: [ // library marker davegut.lib_tpLink_comms, line 95
			Cookie: cookie // library marker davegut.lib_tpLink_comms, line 96
		], // library marker davegut.lib_tpLink_comms, line 97
		timeout: 4, // library marker davegut.lib_tpLink_comms, line 98
		body : new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.lib_tpLink_comms, line 99
	] // library marker davegut.lib_tpLink_comms, line 100
	try { // library marker davegut.lib_tpLink_comms, line 101
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.lib_tpLink_comms, line 102
		logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 103
	} catch (e) { // library marker davegut.lib_tpLink_comms, line 104
		logData << [status: e, reqParams: reqParams] // library marker davegut.lib_tpLink_comms, line 105
	} // library marker davegut.lib_tpLink_comms, line 106
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 107
		logDebug("asyncPost: ${logData}") // library marker davegut.lib_tpLink_comms, line 108
	} else { // library marker davegut.lib_tpLink_comms, line 109
		logWarn("asyncPost: ${logData}") // library marker davegut.lib_tpLink_comms, line 110
		handleCommsError() // library marker davegut.lib_tpLink_comms, line 111
	} // library marker davegut.lib_tpLink_comms, line 112
} // library marker davegut.lib_tpLink_comms, line 113

def parseData(resp) { // library marker davegut.lib_tpLink_comms, line 115
	def logData = [:] // library marker davegut.lib_tpLink_comms, line 116
	if (resp.status == 200 && resp.json.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 117
		def cmdResp // library marker davegut.lib_tpLink_comms, line 118
		try { // library marker davegut.lib_tpLink_comms, line 119
			cmdResp = new JsonSlurper().parseText(decrypt(resp.json.result.response)) // library marker davegut.lib_tpLink_comms, line 120
			setCommsError(false) // library marker davegut.lib_tpLink_comms, line 121
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 122
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_comms, line 123
		} // library marker davegut.lib_tpLink_comms, line 124
		if (cmdResp != null && cmdResp.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 125
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.lib_tpLink_comms, line 126
		} else { // library marker davegut.lib_tpLink_comms, line 127
			logData << [status: "deviceDataError", errCode:  // library marker davegut.lib_tpLink_comms, line 128
						cmdResp.error_code, cmdResp: cmdResp] // library marker davegut.lib_tpLink_comms, line 129
		} // library marker davegut.lib_tpLink_comms, line 130
	} else { // library marker davegut.lib_tpLink_comms, line 131
		logData << [status: "lanDataError", respStatus: resp.status,  // library marker davegut.lib_tpLink_comms, line 132
					errorCode: resp.json.error_code] // library marker davegut.lib_tpLink_comms, line 133
	} // library marker davegut.lib_tpLink_comms, line 134
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 135
		logDebug("parseData: ${logData}") // library marker davegut.lib_tpLink_comms, line 136
	} else { // library marker davegut.lib_tpLink_comms, line 137
		logWarn("parseData: ${logData}") // library marker davegut.lib_tpLink_comms, line 138
		handleCommsError() // library marker davegut.lib_tpLink_comms, line 139
	} // library marker davegut.lib_tpLink_comms, line 140
	return logData // library marker davegut.lib_tpLink_comms, line 141
} // library marker davegut.lib_tpLink_comms, line 142

def handleCommsError() { // library marker davegut.lib_tpLink_comms, line 144
	Map logData = [:] // library marker davegut.lib_tpLink_comms, line 145
	if (state.lastCommand != "") { // library marker davegut.lib_tpLink_comms, line 146
		def count = state.errorCount + 1 // library marker davegut.lib_tpLink_comms, line 147
		state.errorCount = count // library marker davegut.lib_tpLink_comms, line 148
		def retry = true // library marker davegut.lib_tpLink_comms, line 149
		def cmdData = new JSONObject(state.lastCmd) // library marker davegut.lib_tpLink_comms, line 150
		def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.lib_tpLink_comms, line 151
		logData << [count: count, command: cmdData] // library marker davegut.lib_tpLink_comms, line 152
		switch (count) { // library marker davegut.lib_tpLink_comms, line 153
			case 1: // library marker davegut.lib_tpLink_comms, line 154
				asyncPassthrough(cmdBody, cmdData.method, cmdData.action) // library marker davegut.lib_tpLink_comms, line 155
				logData << [status: "commandRetry"] // library marker davegut.lib_tpLink_comms, line 156
				logDebug("handleCommsError: ${logData}") // library marker davegut.lib_tpLink_comms, line 157
				break // library marker davegut.lib_tpLink_comms, line 158
			case 2: // library marker davegut.lib_tpLink_comms, line 159
				logData << [deviceLogin: deviceLogin()] // library marker davegut.lib_tpLink_comms, line 160
				Map data = [cmdBody: cmdBody, method: cmdData.method, action:cmdData.action] // library marker davegut.lib_tpLink_comms, line 161
				runIn(2, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_comms, line 162
				logData << [status: "newLogin and commandRetry"] // library marker davegut.lib_tpLink_comms, line 163
				logWarn("handleCommsError: ${logData}") // library marker davegut.lib_tpLink_comms, line 164
				break // library marker davegut.lib_tpLink_comms, line 165
			case 3: // library marker davegut.lib_tpLink_comms, line 166
				logData << [setCommsError: setCommsError(true), status: "retriesDisabled"] // library marker davegut.lib_tpLink_comms, line 167
				logError("handleCommsError: ${logData}") // library marker davegut.lib_tpLink_comms, line 168
				break // library marker davegut.lib_tpLink_comms, line 169
			default: // library marker davegut.lib_tpLink_comms, line 170
				break // library marker davegut.lib_tpLink_comms, line 171
		} // library marker davegut.lib_tpLink_comms, line 172
	} // library marker davegut.lib_tpLink_comms, line 173
} // library marker davegut.lib_tpLink_comms, line 174

def delayedPassThrough(data) { // library marker davegut.lib_tpLink_comms, line 176
	asyncPassthrough(data.cmdBody, data.method, data.action) // library marker davegut.lib_tpLink_comms, line 177
} // library marker davegut.lib_tpLink_comms, line 178

def setCommsError(status) { // library marker davegut.lib_tpLink_comms, line 180
	if (!status) { // library marker davegut.lib_tpLink_comms, line 181
		updateAttr("commsError", false) // library marker davegut.lib_tpLink_comms, line 182
		state.errorCount = 0 // library marker davegut.lib_tpLink_comms, line 183
	} else { // library marker davegut.lib_tpLink_comms, line 184
		updateAttr("commsError", true) // library marker davegut.lib_tpLink_comms, line 185
		return "commsErrorSet" // library marker davegut.lib_tpLink_comms, line 186
	} // library marker davegut.lib_tpLink_comms, line 187
} // library marker davegut.lib_tpLink_comms, line 188

// ~~~~~ end include (1327) davegut.lib_tpLink_comms ~~~~~

// ~~~~~ start include (1337) davegut.lib_tpLink_security ~~~~~
library ( // library marker davegut.lib_tpLink_security, line 1
	name: "lib_tpLink_security", // library marker davegut.lib_tpLink_security, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_security, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_security, line 4
	description: "tpLink RSA and AES security measures", // library marker davegut.lib_tpLink_security, line 5
	category: "utilities", // library marker davegut.lib_tpLink_security, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_security, line 7
) // library marker davegut.lib_tpLink_security, line 8
import groovy.json.JsonSlurper // library marker davegut.lib_tpLink_security, line 9
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.lib_tpLink_security, line 10
import javax.crypto.spec.SecretKeySpec // library marker davegut.lib_tpLink_security, line 11
import javax.crypto.spec.IvParameterSpec // library marker davegut.lib_tpLink_security, line 12
import javax.crypto.Cipher // library marker davegut.lib_tpLink_security, line 13
import java.security.KeyFactory // library marker davegut.lib_tpLink_security, line 14

def securityPreferences() { // library marker davegut.lib_tpLink_security, line 16
	input ("aesKey", "password", title: "Storage for the AES Key") // library marker davegut.lib_tpLink_security, line 17
//	input ("deviceCookie", "password", title: "Storage for the cookie") // library marker davegut.lib_tpLink_security, line 18
//	input ("deviceToken", "password", title: "Storage for the token") // library marker davegut.lib_tpLink_security, line 19
} // library marker davegut.lib_tpLink_security, line 20

//	===== Device Login Core ===== // library marker davegut.lib_tpLink_security, line 22
def handshake(devIp) { // library marker davegut.lib_tpLink_security, line 23
	def rsaKeys = getRsaKeys() // library marker davegut.lib_tpLink_security, line 24
	Map handshakeData = [method: "handshakeData", rsaKeys: rsaKeys.keyNo] // library marker davegut.lib_tpLink_security, line 25
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKeys.public}-----END PUBLIC KEY-----\n" // library marker davegut.lib_tpLink_security, line 26
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.lib_tpLink_security, line 27
	def uri = "http://${devIp}/app" // library marker davegut.lib_tpLink_security, line 28
	def respData = syncPost(uri, cmdBody) // library marker davegut.lib_tpLink_security, line 29
	if (respData.status == "OK") { // library marker davegut.lib_tpLink_security, line 30
		String deviceKey = respData.resp.data.result.key // library marker davegut.lib_tpLink_security, line 31
		try { // library marker davegut.lib_tpLink_security, line 32
			def cookieHeader = respData.resp.headers["set-cookie"].toString() // library marker davegut.lib_tpLink_security, line 33
			def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_security, line 34
			handshakeData << [cookie: cookie] // library marker davegut.lib_tpLink_security, line 35
		} catch (err) { // library marker davegut.lib_tpLink_security, line 36
			handshakeData << [respStatus: "FAILED", check: "respData.headers", error: err] // library marker davegut.lib_tpLink_security, line 37
		} // library marker davegut.lib_tpLink_security, line 38
		def aesArray = readDeviceKey(deviceKey, rsaKeys.private) // library marker davegut.lib_tpLink_security, line 39
		handshakeData << [aesKey: aesArray] // library marker davegut.lib_tpLink_security, line 40
		if (aesArray == "ERROR") { // library marker davegut.lib_tpLink_security, line 41
			handshakeData << [respStatus: "FAILED", check: "privateKey"] // library marker davegut.lib_tpLink_security, line 42
		} else { // library marker davegut.lib_tpLink_security, line 43
			handshakeData << [respStatus: "OK"] // library marker davegut.lib_tpLink_security, line 44
		} // library marker davegut.lib_tpLink_security, line 45
	} else { // library marker davegut.lib_tpLink_security, line 46
		handshakeData << [respStatus: "FAILED", check: "pubPem. devIp", respData: respData] // library marker davegut.lib_tpLink_security, line 47
	} // library marker davegut.lib_tpLink_security, line 48
	if (handshakeData.respStatus == "OK") { // library marker davegut.lib_tpLink_security, line 49
		logDebug("handshake: ${handshakeData}") // library marker davegut.lib_tpLink_security, line 50
	} else { // library marker davegut.lib_tpLink_security, line 51
		logWarn("handshake: ${handshakeData}") // library marker davegut.lib_tpLink_security, line 52
	} // library marker davegut.lib_tpLink_security, line 53
	return handshakeData // library marker davegut.lib_tpLink_security, line 54
} // library marker davegut.lib_tpLink_security, line 55

def readDeviceKey(deviceKey, privateKey) { // library marker davegut.lib_tpLink_security, line 57
	def response = "ERROR" // library marker davegut.lib_tpLink_security, line 58
	def logData = [:] // library marker davegut.lib_tpLink_security, line 59
	try { // library marker davegut.lib_tpLink_security, line 60
		byte[] privateKeyBytes = privateKey.decodeBase64() // library marker davegut.lib_tpLink_security, line 61
		byte[] deviceKeyBytes = deviceKey.getBytes("UTF-8").decodeBase64() // library marker davegut.lib_tpLink_security, line 62
    	Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.lib_tpLink_security, line 63
		instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.lib_tpLink_security, line 64
		byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.lib_tpLink_security, line 65
		response = cryptoArray // library marker davegut.lib_tpLink_security, line 66
		logData << [cryptoArray: "REDACTED for logs", status: "OK"] // library marker davegut.lib_tpLink_security, line 67
		logDebug("readDeviceKey: ${logData}") // library marker davegut.lib_tpLink_security, line 68
	} catch (err) { // library marker davegut.lib_tpLink_security, line 69
		logData << [status: "READ ERROR", data: err] // library marker davegut.lib_tpLink_security, line 70
		logWarn("readDeviceKey: ${logData}") // library marker davegut.lib_tpLink_security, line 71
	} // library marker davegut.lib_tpLink_security, line 72
	return response // library marker davegut.lib_tpLink_security, line 73
} // library marker davegut.lib_tpLink_security, line 74

def loginDevice(cookie, cryptoArray, credentials, devIp) { // library marker davegut.lib_tpLink_security, line 76
	Map tokenData = [method: "loginDevice"] // library marker davegut.lib_tpLink_security, line 77
	def uri = "http://${devIp}/app" // library marker davegut.lib_tpLink_security, line 78
	Map cmdBody = [method: "login_device", // library marker davegut.lib_tpLink_security, line 79
				   params: [password: credentials.encPassword, // library marker davegut.lib_tpLink_security, line 80
							username: credentials.encUsername], // library marker davegut.lib_tpLink_security, line 81
				   requestTimeMils: 0] // library marker davegut.lib_tpLink_security, line 82
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_security, line 83
	Map reqBody = [method: "securePassthrough", params: [request: encrypt(cmdStr, cryptoArray)]] // library marker davegut.lib_tpLink_security, line 84
	def respData = syncPost(uri, reqBody, cookie) // library marker davegut.lib_tpLink_security, line 85
	if (respData.status == "OK") { // library marker davegut.lib_tpLink_security, line 86
		if (respData.resp.data.error_code == 0) { // library marker davegut.lib_tpLink_security, line 87
			try { // library marker davegut.lib_tpLink_security, line 88
				def cmdResp = decrypt(respData.resp.data.result.response, cryptoArray) // library marker davegut.lib_tpLink_security, line 89
				cmdResp = new JsonSlurper().parseText(cmdResp) // library marker davegut.lib_tpLink_security, line 90
				if (cmdResp.error_code == 0) { // library marker davegut.lib_tpLink_security, line 91
					tokenData << [respStatus: "OK", token: cmdResp.result.token] // library marker davegut.lib_tpLink_security, line 92
				} else { // library marker davegut.lib_tpLink_security, line 93
					tokenData << [respStatus: "Error from device",  // library marker davegut.lib_tpLink_security, line 94
								  check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.lib_tpLink_security, line 95
				} // library marker davegut.lib_tpLink_security, line 96
			} catch (err) { // library marker davegut.lib_tpLink_security, line 97
				tokenData << [respStatus: "Error parsing", error: err] // library marker davegut.lib_tpLink_security, line 98
			} // library marker davegut.lib_tpLink_security, line 99
		} else { // library marker davegut.lib_tpLink_security, line 100
			tokenData << [respStatus: "Error in respData.data", data: respData.data] // library marker davegut.lib_tpLink_security, line 101
		} // library marker davegut.lib_tpLink_security, line 102
	} else { // library marker davegut.lib_tpLink_security, line 103
		tokenData << [respStatus: "Error in respData", data: respData] // library marker davegut.lib_tpLink_security, line 104
	} // library marker davegut.lib_tpLink_security, line 105
	if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_security, line 106
		logDebug("handshake: ${tokenData}") // library marker davegut.lib_tpLink_security, line 107
	} else { // library marker davegut.lib_tpLink_security, line 108
		logWarn("handshake: ${tokenData}") // library marker davegut.lib_tpLink_security, line 109
	} // library marker davegut.lib_tpLink_security, line 110
	return tokenData // library marker davegut.lib_tpLink_security, line 111
} // library marker davegut.lib_tpLink_security, line 112

//	===== AES Methods ===== // library marker davegut.lib_tpLink_security, line 114
//def encrypt(plainText, keyData) { // library marker davegut.lib_tpLink_security, line 115
def encrypt(plainText, keyData = null) { // library marker davegut.lib_tpLink_security, line 116
	if (keyData == null) { // library marker davegut.lib_tpLink_security, line 117
		keyData = new JsonSlurper().parseText(aesKey) // library marker davegut.lib_tpLink_security, line 118
	} // library marker davegut.lib_tpLink_security, line 119
	byte[] keyenc = keyData[0..15] // library marker davegut.lib_tpLink_security, line 120
	byte[] ivenc = keyData[16..31] // library marker davegut.lib_tpLink_security, line 121

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 123
	SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.lib_tpLink_security, line 124
	IvParameterSpec iv = new IvParameterSpec(ivenc) // library marker davegut.lib_tpLink_security, line 125
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_security, line 126
	String result = cipher.doFinal(plainText.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.lib_tpLink_security, line 127
	return result.replace("\r\n","") // library marker davegut.lib_tpLink_security, line 128
} // library marker davegut.lib_tpLink_security, line 129

def decrypt(cypherText, keyData = null) { // library marker davegut.lib_tpLink_security, line 131
	if (keyData == null) { // library marker davegut.lib_tpLink_security, line 132
		keyData = new JsonSlurper().parseText(aesKey) // library marker davegut.lib_tpLink_security, line 133
	} // library marker davegut.lib_tpLink_security, line 134
	byte[] keyenc = keyData[0..15] // library marker davegut.lib_tpLink_security, line 135
	byte[] ivenc = keyData[16..31] // library marker davegut.lib_tpLink_security, line 136

    byte[] decodedBytes = cypherText.decodeBase64() // library marker davegut.lib_tpLink_security, line 138
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 139
    SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.lib_tpLink_security, line 140
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivenc)) // library marker davegut.lib_tpLink_security, line 141
	String result = new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.lib_tpLink_security, line 142
	return result // library marker davegut.lib_tpLink_security, line 143
} // library marker davegut.lib_tpLink_security, line 144

//	===== RSA Key Methods ===== // library marker davegut.lib_tpLink_security, line 146
def getRsaKeys() { // library marker davegut.lib_tpLink_security, line 147
	def keyNo = Math.round(5 * Math.random()).toInteger() // library marker davegut.lib_tpLink_security, line 148
	def keyData = keyData() // library marker davegut.lib_tpLink_security, line 149
	def RSAKeys = keyData.find { it.keyNo == keyNo } // library marker davegut.lib_tpLink_security, line 150
	return RSAKeys // library marker davegut.lib_tpLink_security, line 151
} // library marker davegut.lib_tpLink_security, line 152

def keyData() { // library marker davegut.lib_tpLink_security, line 154
/*	User Note.  You can update these keys at you will using the site: // library marker davegut.lib_tpLink_security, line 155
		https://www.devglan.com/online-tools/rsa-encryption-decryption // library marker davegut.lib_tpLink_security, line 156
	with an RSA Key Size: 1024 bit // library marker davegut.lib_tpLink_security, line 157
	This is at your risk.*/ // library marker davegut.lib_tpLink_security, line 158
	return [ // library marker davegut.lib_tpLink_security, line 159
		[ // library marker davegut.lib_tpLink_security, line 160
			keyNo: 0, // library marker davegut.lib_tpLink_security, line 161
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.lib_tpLink_security, line 162
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw" // library marker davegut.lib_tpLink_security, line 163
		],[ // library marker davegut.lib_tpLink_security, line 164
			keyNo: 1, // library marker davegut.lib_tpLink_security, line 165
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCshy+qBKbJNefcyJUZ/3i+3KyLji6XaWEWvebUCC2r9/0jE6hc89AufO41a13E3gJ2es732vaxwZ1BZKLy468NnL+tg6vlQXaPkDcdunQwjxbTLNL/yzDZs9HRju2lJnupcksdJWBZmjtztMWQkzBrQVeSKzSTrKYK0s24EEXmtQIDAQAB", // library marker davegut.lib_tpLink_security, line 166
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKyHL6oEpsk159zIlRn/eL7crIuOLpdpYRa95tQILav3/SMTqFzz0C587jVrXcTeAnZ6zvfa9rHBnUFkovLjrw2cv62Dq+VBdo+QNx26dDCPFtMs0v/LMNmz0dGO7aUme6lySx0lYFmaO3O0xZCTMGtBV5IrNJOspgrSzbgQRea1AgMBAAECgYBSeiX9H1AkbJK1Z2ZwEUNF6vTJmmUHmScC2jHZNzeuOFVZSXJ5TU0+jBbMjtE65e9DeJ4suw6oF6j3tAZ6GwJ5tHoIy+qHRV6AjA8GEXjhSwwVCyP8jXYZ7UZyHzjLQAK+L0PvwJY1lAtns/Xmk5GH+zpNnhEmKSZAw23f7wpj2QJBANVPQGYT7TsMTDEEl2jq/ZgOX5Djf2VnKpPZYZGsUmg1hMwcpN/4XQ7XOaclR5TO/CJBJl3UCUEVjdrR1zdD8g8CQQDPDoa5Y5UfhLz4Ja2/gs2UKwO4fkTqqR6Ad8fQlaUZ55HINHWFd8FeERBFgNJzszrzd9BBJ7NnZM5nf2OPqU77AkBLuQuScSZ5HL97czbQvwLxVMDmLWyPMdVykOvLC9JhPgZ7cvuwqnlWiF7mEBzeHbBx9JDLJDd4zE8ETBPLgapPAkAHhCR52FaSdVQSwfNjr1DdHw6chODlj8wOp8p2FOiQXyqYlObrOGSpkH8BtuJs1sW+DsxdgR5vE2a2tRYdIe0/AkEAoQ5MzLcETQrmabdVCyB9pQAiHe4yY9e1w7cimsLJOrH7LMM0hqvBqFOIbSPrZyTp7Ie8awn4nTKoZQtvBfwzHw==" // library marker davegut.lib_tpLink_security, line 167
		],[ // library marker davegut.lib_tpLink_security, line 168
			keyNo: 2, // library marker davegut.lib_tpLink_security, line 169
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBeqRy4zAOs63Sc5yc0DtlFXG1stmdD6sEfUiGjlsy0S8aS8X+Qcjcu5AK3uBBrkVNIa8djXht1bd+pUof5/txzWIMJw9SNtNYqzSdeO7cCtRLzuQnQWP7Am64OBvYkXn2sUqoaqDE50LbSQWbuvZw0Vi9QihfBYGQdlrqjCPUsQIDAQAB", // library marker davegut.lib_tpLink_security, line 170
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIF6pHLjMA6zrdJznJzQO2UVcbWy2Z0PqwR9SIaOWzLRLxpLxf5ByNy7kAre4EGuRU0hrx2NeG3Vt36lSh/n+3HNYgwnD1I201irNJ147twK1EvO5CdBY/sCbrg4G9iRefaxSqhqoMTnQttJBZu69nDRWL1CKF8FgZB2WuqMI9SxAgMBAAECgYBBi2wkHI3/Y0Xi+1OUrnTivvBJIri2oW/ZXfKQ6w+PsgU+Mo2QII0l8G0Ck8DCfw3l9d9H/o2wTDgPjGzxqeXHAbxET1dS0QBTjR1zLZlFyfAs7WO8tDKmHVroUgqRkJgoQNQlBSe1E3e7pTgSKElzLuALkRS6p1jhzT2wu9U04QJBAOFr/G36PbQ6NmDYtVyEEr3vWn46JHeZISdJOsordR7Wzbt6xk6/zUDHq0OGM9rYrpBy7PNrbc0JuQrhfbIyaHMCQQCTCvETjXCMkwyUrQT6TpxVzKEVRf1rCitnNQCh1TLnDKcCEAnqZT2RRS3yNXTWFoJrtuEHMGmwUrtog9+ZJBlLAkEA2qxdkPY621XJIIO404mPgM7rMx4F+DsE7U5diHdFw2fO5brBGu13GAtZuUQ7k2W1WY0TDUO+nTN8XPDHdZDuvwJABu7TIwreLaKZS0FFJNAkCt+VEL22Dx/xn/Idz4OP3Nj53t0Guqh/WKQcYHkowxdYmt+KiJ49vXSJJYpiNoQ/NQJAM1HCl8hBznLZLQlxrCTdMvUimG3kJmA0bUNVncgUBq7ptqjk7lp5iNrle5aml99foYnzZeEUW6jrCC7Lj9tg+w==" // library marker davegut.lib_tpLink_security, line 171
		],[ // library marker davegut.lib_tpLink_security, line 172
			keyNo: 3, // library marker davegut.lib_tpLink_security, line 173
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCFYaoMvv5kBxUUbp4PQyd7RoZlPompsupXP2La0qGGxacF98/88W4KNUqLbF4X5BPqxoEA+VeZy75qqyfuYbGQ4fxT6usE/LnzW8zDY/PjhVBht8FBRyAUsoYAt3Ip6sDyjd9YzRzUL1Q/OxCgxz5CNETYxcNr7zfMshBHDmZXMQIDAQAB", // library marker davegut.lib_tpLink_security, line 174
			private: "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIVhqgy+/mQHFRRung9DJ3tGhmU+iamy6lc/YtrSoYbFpwX3z/zxbgo1SotsXhfkE+rGgQD5V5nLvmqrJ+5hsZDh/FPq6wT8ufNbzMNj8+OFUGG3wUFHIBSyhgC3cinqwPKN31jNHNQvVD87EKDHPkI0RNjFw2vvN8yyEEcOZlcxAgMBAAECgYA3NxjoMeCpk+z8ClbQRqJ/e9CC9QKUB4bPG2RW5b8MRaJA7DdjpKZC/5CeavwAs+Ay3n3k41OKTTfEfJoJKtQQZnCrqnZfq9IVZI26xfYo0cgSYbi8wCie6nqIBdu9k54nqhePPshi22VcFuOh97xxPvY7kiUaRbbKqxn9PFwrYQJBAMsO3uOnYSJxN/FuxksKLqhtNei2GUC/0l7uIE8rbRdtN3QOpcC5suj7id03/IMn2Ks+Vsrmi0lV4VV/c8xyo9UCQQCoKDlObjbYeYYdW7/NvI6cEntgHygENi7b6WFk+dbRhJQgrFH8Z/Idj9a2E3BkfLCTUM1Z/Z3e7D0iqPDKBn/tAkBAHI3bKvnMOhsDq4oIH0rj+rdOplAK1YXCW0TwOjHTd7ROfGFxHDCUxvacVhTwBCCw0JnuriPEH81phTg2kOuRAkAEPR9UrsqLImUTEGEBWqNto7mgbqifko4T1QozdWjI10K0oCNg7W3Y+Os8o7jNj6cTz5GdlxsHp4TS/tczAH7xAkBY6KPIlF1FfiyJAnBC8+jJr2h4TSPQD7sbJJmYw7mvR+f1T4tsWY0aGux69hVm8BoaLStBVPdkaENBMdP+a07u" // library marker davegut.lib_tpLink_security, line 175
		],[ // library marker davegut.lib_tpLink_security, line 176
			keyNo: 4, // library marker davegut.lib_tpLink_security, line 177
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClF0yuCpo3r1ZpYlGcyI5wy5nnvZdOZmxqz5U2rklt2b8+9uWhmsGdpbTv5+qJXlZmvUKbpoaPxpJluBFDJH2GSpq3I0whh0gNq9Arzpp/TDYaZLb6iIqDMF6wm8yjGOtcSkB7qLQWkXpEN9T2NsEzlfTc+GTKc07QXHnzxoLmwQIDAQAB", // library marker davegut.lib_tpLink_security, line 178
			private: "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKUXTK4KmjevVmliUZzIjnDLmee9l05mbGrPlTauSW3Zvz725aGawZ2ltO/n6oleVma9Qpumho/GkmW4EUMkfYZKmrcjTCGHSA2r0CvOmn9MNhpktvqIioMwXrCbzKMY61xKQHuotBaRekQ31PY2wTOV9Nz4ZMpzTtBcefPGgubBAgMBAAECgYB4wCz+05RvDFk45YfqFCtTRyg//0UvO+0qxsBN6Xad2XlvlWjqJeZd53kLTGcYqJ6rsNyKOmgLu2MS8Wn24TbJmPUAwZU+9cvSPxxQ5k6bwjg1RifieIcbTPC5wHDqVy0/Ur7dt+JVMOHFseR/pElDw471LCdwWSuFHAKuiHsaUQJBANHiPdSU3s1bbJYTLaS1tW0UXo7aqgeXuJgqZ2sKsoIEheEAROJ5rW/f2KrFVtvg0ITSM8mgXNlhNBS5OE4nSD0CQQDJXYJxKvdodeRoj+RGTCZGZanAE1naUzSdfcNWx2IMnYUD/3/2eB7ZIyQPBG5fWjc3bGOJKI+gy/14bCwXU7zVAkAdnsE9HBlpf+qOL3y0jxRgpYxGuuNeGPJrPyjDOYpBwSOnwmL2V1e7vyqTxy/f7hVfeU7nuKMB5q7z8cPZe7+9AkEAl7A6aDe+wlE069OhWZdZqeRBmLC7Gi1d0FoBwahW4zvyDM32vltEmbvQGQP0hR33xGeBH7yPXcjtOz75g+UPtQJBAL4gknJ/p+yQm9RJB0oq/g+HriErpIMHwrhNoRY1aOBMJVl4ari1Ch2RQNL9KQW7yrFDv7XiP3z5NwNDKsp/QeU=" // library marker davegut.lib_tpLink_security, line 179
		],[ // library marker davegut.lib_tpLink_security, line 180
			keyNo: 5, // library marker davegut.lib_tpLink_security, line 181
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChN8Xc+gsSuhcLVM1W1E+e1o+celvKlOmuV6sJEkJecknKFujx9+T4xvyapzyePpTBn0lA9EYbaF7UDYBsDgqSwgt0El3gV+49O56nt1ELbLUJtkYEQPK+6Pu8665UG17leCiaMiFQyoZhD80PXhpjehqDu2900uU/4DzKZ/eywwIDAQAB", // library marker davegut.lib_tpLink_security, line 182
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKE3xdz6CxK6FwtUzVbUT57Wj5x6W8qU6a5XqwkSQl5yScoW6PH35PjG/JqnPJ4+lMGfSUD0RhtoXtQNgGwOCpLCC3QSXeBX7j07nqe3UQtstQm2RgRA8r7o+7zrrlQbXuV4KJoyIVDKhmEPzQ9eGmN6GoO7b3TS5T/gPMpn97LDAgMBAAECgYAy+uQCwL8HqPjoiGR2dKTI4aiAHuEv6m8KxoY7VB7QputWkHARNAaf9KykawXsNHXt1GThuV0CBbsW6z4U7UvCJEZEpv7qJiGX8UWgEs1ISatqXmiIMVosIJJvoFw/rAoScadCYyicskjwDFBVNU53EAUD3WzwEq+dRYDn52lqQQJBAMu30FEReAHTAKE/hvjAeBUyWjg7E4/lnYvb/i9Wuc+MTH0q3JxFGGMb3n6APT9+kbGE0rinM/GEXtpny+5y3asCQQDKl7eNq0NdIEBGAdKerX4O+nVDZ7PXz1kQ2ca0r1tXtY/9sBDDoKHP2fQAH/xlOLIhLaH1rabSEJYNUM0ohHdJAkBYZqhwNWtlJ0ITtvSEB0lUsWfzFLe1bseCBHH16uVwygn7GtlmupkNkO9o548seWkRpnimhnAE8xMSJY6aJ6BHAkEAuSFLKrqGJGOEWHTx8u63cxiMb7wkK+HekfdwDUzxO4U+v6RUrW/sbfPNdQ/FpPnaTVdV2RuGhg+CD0j3MT9bgQJARH86hfxp1bkyc7f1iJQT8sofdqqVz5grCV5XeGY77BNmCvTOGLfL5pOJdgALuOoP4t3e94nRYdlW6LqIVugRBQ==" // library marker davegut.lib_tpLink_security, line 183
		] // library marker davegut.lib_tpLink_security, line 184
	] // library marker davegut.lib_tpLink_security, line 185
} // library marker davegut.lib_tpLink_security, line 186

// ~~~~~ end include (1337) davegut.lib_tpLink_security ~~~~~

// ~~~~~ start include (1339) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

preferences { // library marker davegut.Logging, line 10
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.Logging, line 11
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.Logging, line 12
	input ("traceLog", "bool", title: "Enable trace logging as directed by developer", defaultValue: false) // library marker davegut.Logging, line 13
} // library marker davegut.Logging, line 14

def setLogsOff() { // library marker davegut.Logging, line 16
	def logData = [logEnagle: logEnable, infoLog: infoLog, traceLog:traceLog] // library marker davegut.Logging, line 17
	if (logEnable) { // library marker davegut.Logging, line 18
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 19
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 20
	} // library marker davegut.Logging, line 21
	if (traceLog) { // library marker davegut.Logging, line 22
		runIn(1800, traceLogOff) // library marker davegut.Logging, line 23
		logData << [traceLogOff: "scheduled"] // library marker davegut.Logging, line 24
	} // library marker davegut.Logging, line 25
	return logData // library marker davegut.Logging, line 26
} // library marker davegut.Logging, line 27

def logTrace(msg){ // library marker davegut.Logging, line 29
	if (traceLog == true) { // library marker davegut.Logging, line 30
		log.trace "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 31
	} // library marker davegut.Logging, line 32
} // library marker davegut.Logging, line 33

def traceLogOff() { // library marker davegut.Logging, line 35
	device.updateSetting("traceLog", [type:"bool", value: false]) // library marker davegut.Logging, line 36
	logInfo("traceLogOff") // library marker davegut.Logging, line 37
} // library marker davegut.Logging, line 38

def logInfo(msg) {  // library marker davegut.Logging, line 40
	if (textEnable || infoLog) { // library marker davegut.Logging, line 41
		log.info "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 42
	} // library marker davegut.Logging, line 43
} // library marker davegut.Logging, line 44

def debugLogOff() { // library marker davegut.Logging, line 46
	device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 47
	logInfo("debugLogOff") // library marker davegut.Logging, line 48
} // library marker davegut.Logging, line 49

def logDebug(msg) { // library marker davegut.Logging, line 51
	if (logEnable || debugLog) { // library marker davegut.Logging, line 52
		log.debug "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 53
	} // library marker davegut.Logging, line 54
} // library marker davegut.Logging, line 55

def logWarn(msg) { log.warn "${device.displayName}-${driverVer()}: ${msg}" } // library marker davegut.Logging, line 57

def logError(msg) { log.error "${device.displayName}-${driverVer()}: ${msg}" } // library marker davegut.Logging, line 59

// ~~~~~ end include (1339) davegut.Logging ~~~~~