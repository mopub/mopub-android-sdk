package com.mopub.mobileads.resource;

public class MraidJavascript {
    public static final String JAVASCRIPT_SOURCE =
            "(function() {\n" +
            "  var isIOS = (/iphone|ipad|ipod/i).test(window.navigator.userAgent.toLowerCase());\n" +
            "  if (isIOS) {\n" +
            "    console = {};\n" +
            "    console.log = function(log) {\n" +
            "      var iframe = document.createElement('iframe');\n" +
            "      iframe.setAttribute('src', 'ios-log: ' + log);\n" +
            "      document.documentElement.appendChild(iframe);\n" +
            "      iframe.parentNode.removeChild(iframe);\n" +
            "      iframe = null;\n" +
            "    };\n" +
            "    console.debug = console.info = console.warn = console.error = console.log;\n" +
            "  }\n" +
            "}());\n" +
            "\n" +
            "\n" +
            "(function() {\n" +
            "  var mraid = window.mraid = {};\n" +
            "\n" +
            "  //////////////////////////////////////////////////////////////////////////////////////////////////\n" +
            "\n" +
            "  // Bridge interface to SDK\n" +
            "\n" +
            "  var bridge = window.mraidbridge = {\n" +
            "    nativeSDKFiredReady: false,\n" +
            "    nativeCallQueue: [],\n" +
            "    nativeCallInFlight: false,\n" +
            "    lastSizeChangeProperties: null\n" +
            "  };\n" +
            "\n" +
            "\n" +
            "  bridge.fireChangeEvent = function(properties) {\n" +
            "    for (var p in properties) {\n" +
            "      if (properties.hasOwnProperty(p)) {\n" +
            "        // Change handlers defined by MRAID below\n" +
            "        var handler = changeHandlers[p];\n" +
            "        handler(properties[p]);\n" +
            "      }\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  bridge.nativeCallComplete = function(command) {\n" +
            "    if (this.nativeCallQueue.length === 0) {\n" +
            "      this.nativeCallInFlight = false;\n" +
            "      return;\n" +
            "    }\n" +
            "\n" +
            "    var nextCall = this.nativeCallQueue.pop();\n" +
            "    window.location = nextCall;\n" +
            "  };\n" +
            "\n" +
            "  bridge.executeNativeCall = function(args) {\n" +
            "    var command = args.shift();\n" +
            "\n" +
            "    if (!this.nativeSDKFiredReady) {\n" +
            "        console.log('rejecting ' + command + ' because mraid is not ready');\n" +
            "        bridge.notifyErrorEvent('mraid is not ready', command);\n" +
            "        return;\n" +
            "    }\n" +
            "\n" +
            "    var call = 'mraid://' + command;\n" +
            "\n" +
            "    var key, value;\n" +
            "    var isFirstArgument = true;\n" +
            "\n" +
            "    for (var i = 0; i < args.length; i += 2) {\n" +
            "      key = args[i];\n" +
            "      value = args[i + 1];\n" +
            "\n" +
            "      if (value === null) continue;\n" +
            "\n" +
            "      if (isFirstArgument) {\n" +
            "        call += '?';\n" +
            "        isFirstArgument = false;\n" +
            "      } else {\n" +
            "        call += '&';\n" +
            "      }\n" +
            "\n" +
            "      call += encodeURIComponent(key) + '=' + encodeURIComponent(value);\n" +
            "    }\n" +
            "\n" +
            "    if (this.nativeCallInFlight) {\n" +
            "      this.nativeCallQueue.push(call);\n" +
            "    } else {\n" +
            "      this.nativeCallInFlight = true;\n" +
            "      window.location = call;\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "\n" +
            "  bridge.setCurrentPosition = function(x, y, width, height) {\n" +
            "    currentPosition = {\n" +
            "      x: x,\n" +
            "      y: y,\n" +
            "      width: width,\n" +
            "      height: height\n" +
            "    };\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set current position to ' + stringify(currentPosition));\n" +
            "  };\n" +
            "\n" +
            "  bridge.setDefaultPosition = function(x, y, width, height) {\n" +
            "    defaultPosition = {\n" +
            "      x: x,\n" +
            "      y: y,\n" +
            "      width: width,\n" +
            "      height: height\n" +
            "    };\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set default position to ' + stringify(defaultPosition));\n" +
            "  };\n" +
            "\n" +
            "  bridge.setMaxSize = function(width, height) {\n" +
            "    maxSize = {\n" +
            "      width: width,\n" +
            "      height: height\n" +
            "    };\n" +
            "\n" +
            "    expandProperties.width = width;\n" +
            "    expandProperties.height = height;\n" +
            "\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set max size to ' + stringify(maxSize));\n" +
            "  };\n" +
            "\n" +
            "  bridge.setPlacementType = function(_placementType) {\n" +
            "    placementType = _placementType;\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set placement type to ' + stringify(placementType));\n" +
            "  };\n" +
            "\n" +
            "  bridge.setScreenSize = function(width, height) {\n" +
            "    screenSize = {\n" +
            "      width: width,\n" +
            "      height: height\n" +
            "    };\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set screen size to ' + stringify(screenSize));\n" +
            "  };\n" +
            "\n" +
            "  bridge.setState = function(_state) {\n" +
            "    state = _state;\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set state to ' + stringify(state));\n" +
            "    broadcastEvent(EVENTS.STATECHANGE, state);\n" +
            "  };\n" +
            "\n" +
            "  bridge.setIsViewable = function(_isViewable) {\n" +
            "    isViewable = _isViewable;\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set isViewable to ' + stringify(isViewable));\n" +
            "    broadcastEvent(EVENTS.VIEWABLECHANGE, isViewable);\n" +
            "  };\n" +
            "\n" +
            "  bridge.setSupports = function(sms, tel, calendar, storePicture, inlineVideo) {\n" +
            "    supportProperties = {\n" +
            "      sms: sms,\n" +
            "      tel: tel,\n" +
            "      calendar: calendar,\n" +
            "      storePicture: storePicture,\n" +
            "      inlineVideo: inlineVideo\n" +
            "    };\n" +
            "  };\n" +
            "\n" +
            "  bridge.notifyReadyEvent = function() {\n" +
            "    this.nativeSDKFiredReady = true;\n" +
            "    broadcastEvent(EVENTS.READY);\n" +
            "  };\n" +
            "\n" +
            "  bridge.notifyErrorEvent = function(message, action) {\n" +
            "    broadcastEvent(EVENTS.ERROR, message, action);\n" +
            "  };\n" +
            "\n" +
            "  // Temporary aliases while we migrate to the new API\n" +
            "  bridge.fireReadyEvent = bridge.notifyReadyEvent;\n" +
            "  bridge.fireErrorEvent = bridge.notifyErrorEvent;\n" +
            "\n" +
            "  bridge.notifySizeChangeEvent = function(width, height) {\n" +
            "    if (this.lastSizeChangeProperties &&\n" +
            "          width == this.lastSizeChangeProperties.width && height == this.lastSizeChangeProperties.height) {\n" +
            "      return;\n" +
            "    }\n" +
            "\n" +
            "    this.lastSizeChangeProperties = {\n" +
            "        width: width,\n" +
            "        height: height\n" +
            "    };\n" +
            "    broadcastEvent(EVENTS.SIZECHANGE, width, height);\n" +
            "  };\n" +
            "\n" +
            "  bridge.notifyStateChangeEvent = function() {\n" +
            "    if (state === STATES.LOADING) {\n" +
            "      broadcastEvent(EVENTS.INFO, 'Native SDK initialized.');\n" +
            "    }\n" +
            "\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set state to ' + stringify(state));\n" +
            "    broadcastEvent(EVENTS.STATECHANGE, state);\n" +
            "  };\n" +
            "\n" +
            "  bridge.notifyViewableChangeEvent = function() {\n" +
            "    broadcastEvent(EVENTS.INFO, 'Set isViewable to ' + stringify(isViewable));\n" +
            "    broadcastEvent(EVENTS.VIEWABLECHANGE, isViewable);\n" +
            "  };\n" +
            "\n" +
            "\n" +
            "  // Constants. ////////////////////////////////////////////////////////////////////////////////////\n" +
            "\n" +
            "  var VERSION = mraid.VERSION = '2.0';\n" +
            "\n" +
            "  var STATES = mraid.STATES = {\n" +
            "    LOADING: 'loading',\n" +
            "    DEFAULT: 'default',\n" +
            "    EXPANDED: 'expanded',\n" +
            "    HIDDEN: 'hidden',\n" +
            "    RESIZED: 'resized'\n" +
            "  };\n" +
            "\n" +
            "  var EVENTS = mraid.EVENTS = {\n" +
            "    ERROR: 'error',\n" +
            "    INFO: 'info',\n" +
            "    READY: 'ready',\n" +
            "    STATECHANGE: 'stateChange',\n" +
            "    VIEWABLECHANGE: 'viewableChange',\n" +
            "    SIZECHANGE: 'sizeChange'\n" +
            "  };\n" +
            "\n" +
            "  var PLACEMENT_TYPES = mraid.PLACEMENT_TYPES = {\n" +
            "    UNKNOWN: 'unknown',\n" +
            "    INLINE: 'inline',\n" +
            "    INTERSTITIAL: 'interstitial'\n" +
            "  };\n" +
            "\n" +
            "  // External MRAID state: may be directly or indirectly modified by the ad JS. ////////////////////\n" +
            "\n" +
            "  // Properties which define the behavior of an expandable ad.\n" +
            "  var expandProperties = {\n" +
            "    width: false,\n" +
            "    height: false,\n" +
            "    useCustomClose: false,\n" +
            "    isModal: true\n" +
            "  };\n" +
            "\n" +
            "  var resizeProperties = {\n" +
            "    width: false,\n" +
            "    height: false,\n" +
            "    offsetX: false,\n" +
            "    offsetY: false,\n" +
            "    customClosePosition: 'top-right',\n" +
            "    allowOffscreen: true\n" +
            "  };\n" +
            "\n" +
            "  var orientationProperties = {\n" +
            "    allowOrientationChange: true,\n" +
            "    forceOrientation: \"none\"\n" +
            "  };\n" +
            "\n" +
            "  var supportProperties = {\n" +
            "    sms: false,\n" +
            "    tel: false,\n" +
            "    calendar: false,\n" +
            "    storePicture: false,\n" +
            "    inlineVideo: false\n" +
            "  };\n" +
            "\n" +
            "  // default is undefined so that notifySizeChangeEvent can track changes\n" +
            "  var lastSizeChangeProperties;\n" +
            "\n" +
            "  var maxSize = {};\n" +
            "\n" +
            "  var currentPosition = {};\n" +
            "\n" +
            "  var defaultPosition = {};\n" +
            "\n" +
            "  var screenSize = {};\n" +
            "\n" +
            "  var hasSetCustomClose = false;\n" +
            "\n" +
            "  var listeners = {};\n" +
            "\n" +
            "  // Internal MRAID state. Modified by the native SDK. /////////////////////////////////////////////\n" +
            "\n" +
            "  var state = STATES.LOADING;\n" +
            "\n" +
            "  var isViewable = false;\n" +
            "\n" +
            "  var placementType = PLACEMENT_TYPES.UNKNOWN;\n" +
            "\n" +
            "  //////////////////////////////////////////////////////////////////////////////////////////////////\n" +
            "\n" +
            "  var EventListeners = function(event) {\n" +
            "    this.event = event;\n" +
            "    this.count = 0;\n" +
            "    var listeners = {};\n" +
            "\n" +
            "    this.add = function(func) {\n" +
            "      var id = String(func);\n" +
            "      if (!listeners[id]) {\n" +
            "        listeners[id] = func;\n" +
            "        this.count++;\n" +
            "      }\n" +
            "    };\n" +
            "\n" +
            "    this.remove = function(func) {\n" +
            "      var id = String(func);\n" +
            "      if (listeners[id]) {\n" +
            "        listeners[id] = null;\n" +
            "        delete listeners[id];\n" +
            "        this.count--;\n" +
            "        return true;\n" +
            "      } else {\n" +
            "        return false;\n" +
            "      }\n" +
            "    };\n" +
            "\n" +
            "    this.removeAll = function() {\n" +
            "      for (var id in listeners) {\n" +
            "        if (listeners.hasOwnProperty(id)) this.remove(listeners[id]);\n" +
            "      }\n" +
            "    };\n" +
            "\n" +
            "    this.broadcast = function(args) {\n" +
            "      for (var id in listeners) {\n" +
            "        if (listeners.hasOwnProperty(id)) listeners[id].apply(mraid, args);\n" +
            "      }\n" +
            "    };\n" +
            "\n" +
            "    this.toString = function() {\n" +
            "      var out = [event, ':'];\n" +
            "      for (var id in listeners) {\n" +
            "        if (listeners.hasOwnProperty(id)) out.push('|', id, '|');\n" +
            "      }\n" +
            "      return out.join('');\n" +
            "    };\n" +
            "  };\n" +
            "\n" +
            "  var broadcastEvent = function() {\n" +
            "    var args = new Array(arguments.length);\n" +
            "    var l = arguments.length;\n" +
            "    for (var i = 0; i < l; i++) args[i] = arguments[i];\n" +
            "    var event = args.shift();\n" +
            "    if (listeners[event]) listeners[event].broadcast(args);\n" +
            "  };\n" +
            "\n" +
            "  var contains = function(value, array) {\n" +
            "    for (var i in array) {\n" +
            "      if (array[i] === value) return true;\n" +
            "    }\n" +
            "    return false;\n" +
            "  };\n" +
            "\n" +
            "  var clone = function(obj) {\n" +
            "    if (obj === null) return null;\n" +
            "    var f = function() {};\n" +
            "    f.prototype = obj;\n" +
            "    return new f();\n" +
            "  };\n" +
            "\n" +
            "  var stringify = function(obj) {\n" +
            "    if (typeof obj === 'object') {\n" +
            "      var out = [];\n" +
            "      if (obj.push) {\n" +
            "        // Array.\n" +
            "        for (var p in obj) out.push(obj[p]);\n" +
            "        return '[' + out.join(',') + ']';\n" +
            "      } else {\n" +
            "        // Other object.\n" +
            "        for (var p in obj) out.push(\"'\" + p + \"': \" + obj[p]);\n" +
            "        return '{' + out.join(',') + '}';\n" +
            "      }\n" +
            "    } else return String(obj);\n" +
            "  };\n" +
            "\n" +
            "  var trim = function(str) {\n" +
            "    return str.replace(/^\\s+|\\s+$/g, '');\n" +
            "  };\n" +
            "\n" +
            "  // Functions that will be invoked by the native SDK whenever a \"change\" event occurs.\n" +
            "  var changeHandlers = {\n" +
            "    state: function(val) {\n" +
            "      if (state === STATES.LOADING) {\n" +
            "        broadcastEvent(EVENTS.INFO, 'Native SDK initialized.');\n" +
            "      }\n" +
            "      state = val;\n" +
            "      broadcastEvent(EVENTS.INFO, 'Set state to ' + stringify(val));\n" +
            "      broadcastEvent(EVENTS.STATECHANGE, state);\n" +
            "    },\n" +
            "\n" +
            "    viewable: function(val) {\n" +
            "      isViewable = val;\n" +
            "      broadcastEvent(EVENTS.INFO, 'Set isViewable to ' + stringify(val));\n" +
            "      broadcastEvent(EVENTS.VIEWABLECHANGE, isViewable);\n" +
            "    },\n" +
            "\n" +
            "    placementType: function(val) {\n" +
            "      broadcastEvent(EVENTS.INFO, 'Set placementType to ' + stringify(val));\n" +
            "      placementType = val;\n" +
            "    },\n" +
            "\n" +
            "    sizeChange: function(val) {\n" +
            "      broadcastEvent(EVENTS.INFO, 'Set screenSize to ' + stringify(val));\n" +
            "      for (var key in val) {\n" +
            "        if (val.hasOwnProperty(key)) screenSize[key] = val[key];\n" +
            "      }\n" +
            "    },\n" +
            "\n" +
            "    supports: function(val) {\n" +
            "      broadcastEvent(EVENTS.INFO, 'Set supports to ' + stringify(val));\n" +
            "        supportProperties = val;\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  var validate = function(obj, validators, action, merge) {\n" +
            "    if (!merge) {\n" +
            "      // Check to see if any required properties are missing.\n" +
            "      if (obj === null) {\n" +
            "        broadcastEvent(EVENTS.ERROR, 'Required object not provided.', action);\n" +
            "        return false;\n" +
            "      } else {\n" +
            "        for (var i in validators) {\n" +
            "          if (validators.hasOwnProperty(i) && obj[i] === undefined) {\n" +
            "            broadcastEvent(EVENTS.ERROR, 'Object is missing required property: ' + i, action);\n" +
            "            return false;\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "\n" +
            "    for (var prop in obj) {\n" +
            "      var validator = validators[prop];\n" +
            "      var value = obj[prop];\n" +
            "      if (validator && !validator(value)) {\n" +
            "        // Failed validation.\n" +
            "        broadcastEvent(EVENTS.ERROR, 'Value of property ' + prop + ' is invalid: ' + value, action);\n" +
            "        return false;\n" +
            "      }\n" +
            "    }\n" +
            "    return true;\n" +
            "  };\n" +
            "\n" +
            "  var expandPropertyValidators = {\n" +
            "    useCustomClose: function(v) { return (typeof v === 'boolean'); },\n" +
            "  };\n" +
            "\n" +
            "  //////////////////////////////////////////////////////////////////////////////////////////////////\n" +
            "\n" +
            "  mraid.addEventListener = function(event, listener) {\n" +
            "    if (!event || !listener) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'Both event and listener are required.', 'addEventListener');\n" +
            "    } else if (!contains(event, EVENTS)) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'Unknown MRAID event: ' + event, 'addEventListener');\n" +
            "    } else {\n" +
            "      if (!listeners[event]) {\n" +
            "        listeners[event] = new EventListeners(event);\n" +
            "      }\n" +
            "      listeners[event].add(listener);\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.close = function() {\n" +
            "    if (state === STATES.HIDDEN) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'Ad cannot be closed when it is already hidden.',\n" +
            "        'close');\n" +
            "    } else bridge.executeNativeCall(['close']);\n" +
            "  };\n" +
            "\n" +
            "  mraid.expand = function(URL) {\n" +
            "    if (!(this.getState() === STATES.DEFAULT || this.getState() === STATES.RESIZED)) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'Ad can only be expanded from the default or resized state.', 'expand');\n" +
            "    } else {\n" +
            "      var args = ['expand',\n" +
            "        'shouldUseCustomClose', expandProperties.useCustomClose\n" +
            "      ];\n" +
            "\n" +
            "      if (URL) {\n" +
            "        args = args.concat(['url', URL]);\n" +
            "      }\n" +
            "\n" +
            "      bridge.executeNativeCall(args);\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.getExpandProperties = function() {\n" +
            "    var properties = {\n" +
            "      width: expandProperties.width,\n" +
            "      height: expandProperties.height,\n" +
            "      useCustomClose: expandProperties.useCustomClose,\n" +
            "      isModal: expandProperties.isModal\n" +
            "    };\n" +
            "    return properties;\n" +
            "  };\n" +
            "\n" +
            "\n" +
            "  mraid.getCurrentPosition = function() {\n" +
            "    return {\n" +
            "      x: currentPosition.x,\n" +
            "      y: currentPosition.y,\n" +
            "      width: currentPosition.width,\n" +
            "      height: currentPosition.height\n" +
            "    };\n" +
            "  };\n" +
            "\n" +
            "  mraid.getDefaultPosition = function() {\n" +
            "    return {\n" +
            "      x: defaultPosition.x,\n" +
            "      y: defaultPosition.y,\n" +
            "      width: defaultPosition.width,\n" +
            "      height: defaultPosition.height\n" +
            "    };\n" +
            "  };\n" +
            "\n" +
            "  mraid.getMaxSize = function() {\n" +
            "    return {\n" +
            "      width: maxSize.width,\n" +
            "      height: maxSize.height\n" +
            "    };\n" +
            "  };\n" +
            "\n" +
            "  mraid.getPlacementType = function() {\n" +
            "    return placementType;\n" +
            "  };\n" +
            "\n" +
            "  mraid.getScreenSize = function() {\n" +
            "    return {\n" +
            "      width: screenSize.width,\n" +
            "      height: screenSize.height\n" +
            "    };\n" +
            "  };\n" +
            "\n" +
            "  mraid.getState = function() {\n" +
            "    return state;\n" +
            "  };\n" +
            "\n" +
            "  mraid.isViewable = function() {\n" +
            "    return isViewable;\n" +
            "  };\n" +
            "\n" +
            "  mraid.getVersion = function() {\n" +
            "    return mraid.VERSION;\n" +
            "  };\n" +
            "\n" +
            "  mraid.open = function(URL) {\n" +
            "    if (!URL) broadcastEvent(EVENTS.ERROR, 'URL is required.', 'open');\n" +
            "    else bridge.executeNativeCall(['open', 'url', URL]);\n" +
            "  };\n" +
            "\n" +
            "  mraid.removeEventListener = function(event, listener) {\n" +
            "    if (!event) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'Event is required.', 'removeEventListener');\n" +
            "      return;\n" +
            "    }\n" +
            "\n" +
            "    if (listener) {\n" +
            "      // If we have a valid event, we'll try to remove the listener from it.\n" +
            "      var success = false;\n" +
            "      if (listeners[event]) {\n" +
            "        success = listeners[event].remove(listener);\n" +
            "      }\n" +
            "\n" +
            "      // If we didn't have a valid event or couldn't remove the listener from the event, broadcast an error and return early.\n" +
            "      if (!success) {\n" +
            "        broadcastEvent(EVENTS.ERROR, 'Listener not currently registered for event.', 'removeEventListener');\n" +
            "        return;\n" +
            "      }\n" +
            "\n" +
            "    } else if (!listener && listeners[event]) {\n" +
            "      listeners[event].removeAll();\n" +
            "    }\n" +
            "\n" +
            "    if (listeners[event] && listeners[event].count === 0) {\n" +
            "      listeners[event] = null;\n" +
            "      delete listeners[event];\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.setExpandProperties = function(properties) {\n" +
            "    if (validate(properties, expandPropertyValidators, 'setExpandProperties', true)) {\n" +
            "      if (properties.hasOwnProperty('useCustomClose')) {\n" +
            "        expandProperties.useCustomClose = properties.useCustomClose;\n" +
            "      }\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.useCustomClose = function(shouldUseCustomClose) {\n" +
            "    expandProperties.useCustomClose = shouldUseCustomClose;\n" +
            "    hasSetCustomClose = true;\n" +
            "    bridge.executeNativeCall(['usecustomclose', 'shouldUseCustomClose', shouldUseCustomClose]);\n" +
            "  };\n" +
            "\n" +
            "  // MRAID 2.0 APIs ////////////////////////////////////////////////////////////////////////////////\n" +
            "\n" +
            "  mraid.createCalendarEvent = function(parameters) {\n" +
            "    CalendarEventParser.initialize(parameters);\n" +
            "    if (CalendarEventParser.parse()) {\n" +
            "      bridge.executeNativeCall(CalendarEventParser.arguments);\n" +
            "    } else {\n" +
            "      broadcastEvent(EVENTS.ERROR, CalendarEventParser.errors[0], 'createCalendarEvent');\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.supports = function(feature) {\n" +
            "    return supportProperties[feature];\n" +
            "  };\n" +
            "\n" +
            "  mraid.playVideo = function(uri) {\n" +
            "    if (!mraid.isViewable()) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'playVideo cannot be called until the ad is viewable', 'playVideo');\n" +
            "      return;\n" +
            "    }\n" +
            "\n" +
            "    if (!uri) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'playVideo must be called with a valid URI', 'playVideo');\n" +
            "    } else {\n" +
            "      bridge.executeNativeCall(['playVideo', 'uri', uri]);\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.storePicture = function(uri) {\n" +
            "    if (!mraid.isViewable()) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'storePicture cannot be called until the ad is viewable', 'storePicture');\n" +
            "      return;\n" +
            "    }\n" +
            "\n" +
            "    if (!uri) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'storePicture must be called with a valid URI', 'storePicture');\n" +
            "    } else {\n" +
            "      bridge.executeNativeCall(['storePicture', 'uri', uri]);\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "\n" +
            "  var resizePropertyValidators = {\n" +
            "    width: function(v) {\n" +
            "      return !isNaN(v) && v > 0;\n" +
            "    },\n" +
            "    height: function(v) {\n" +
            "      return !isNaN(v) && v > 0;\n" +
            "    },\n" +
            "    offsetX: function(v) {\n" +
            "      return !isNaN(v);\n" +
            "    },\n" +
            "    offsetY: function(v) {\n" +
            "      return !isNaN(v);\n" +
            "    },\n" +
            "    customClosePosition: function(v) {\n" +
            "      return (typeof v === 'string' &&\n" +
            "        ['top-right', 'bottom-right', 'top-left', 'bottom-left', 'center', 'top-center', 'bottom-center'].indexOf(v) > -1);\n" +
            "    },\n" +
            "    allowOffscreen: function(v) {\n" +
            "      return (typeof v === 'boolean');\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.setOrientationProperties = function(properties) {\n" +
            "\n" +
            "    if (properties.hasOwnProperty('allowOrientationChange')) {\n" +
            "      orientationProperties.allowOrientationChange = properties.allowOrientationChange;\n" +
            "    }\n" +
            "\n" +
            "    if (properties.hasOwnProperty('forceOrientation')) {\n" +
            "      orientationProperties.forceOrientation = properties.forceOrientation;\n" +
            "    }\n" +
            "\n" +
            "    var args = ['setOrientationProperties',\n" +
            "      'allowOrientationChange', orientationProperties.allowOrientationChange,\n" +
            "      'forceOrientation', orientationProperties.forceOrientation\n" +
            "    ];\n" +
            "    bridge.executeNativeCall(args);\n" +
            "  };\n" +
            "\n" +
            "  mraid.getOrientationProperties = function() {\n" +
            "    return {\n" +
            "      allowOrientationChange: orientationProperties.allowOrientationChange,\n" +
            "      forceOrientation: orientationProperties.forceOrientation\n" +
            "    };\n" +
            "  };\n" +
            "\n" +
            "  mraid.resize = function() {\n" +
            "    if (!(this.getState() === STATES.DEFAULT || this.getState() === STATES.RESIZED)) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'Ad can only be resized from the default or resized state.', 'resize');\n" +
            "    } else if (!resizeProperties.width || !resizeProperties.height) {\n" +
            "      broadcastEvent(EVENTS.ERROR, 'Must set resize properties before calling resize()', 'resize');\n" +
            "    } else {\n" +
            "      var args = ['resize',\n" +
            "        'width', resizeProperties.width,\n" +
            "        'height', resizeProperties.height,\n" +
            "        'offsetX', resizeProperties.offsetX || 0,\n" +
            "        'offsetY', resizeProperties.offsetY || 0,\n" +
            "        'customClosePosition', resizeProperties.customClosePosition,\n" +
            "        'allowOffscreen', !!resizeProperties.allowOffscreen\n" +
            "        ];\n" +
            "\n" +
            "      bridge.executeNativeCall(args);\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  mraid.getResizeProperties = function() {\n" +
            "    var properties = {\n" +
            "      width: resizeProperties.width,\n" +
            "      height: resizeProperties.height,\n" +
            "      offsetX: resizeProperties.offsetX,\n" +
            "      offsetY: resizeProperties.offsetY,\n" +
            "      customClosePosition: resizeProperties.customClosePosition,\n" +
            "      allowOffscreen: resizeProperties.allowOffscreen\n" +
            "    };\n" +
            "    return properties;\n" +
            "  };\n" +
            "\n" +
            "  mraid.setResizeProperties = function(properties) {\n" +
            "    if (validate(properties, resizePropertyValidators, 'setResizeProperties', true)) {\n" +
            "\n" +
            "      var desiredProperties = ['width', 'height', 'offsetX', 'offsetY', 'customClosePosition', 'allowOffscreen'];\n" +
            "\n" +
            "      var length = desiredProperties.length;\n" +
            "\n" +
            "      for (var i = 0; i < length; i++) {\n" +
            "        var propname = desiredProperties[i];\n" +
            "        if (properties.hasOwnProperty(propname)) {\n" +
            "          resizeProperties[propname] = properties[propname];\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  var CalendarEventParser = {\n" +
            "    initialize: function(parameters) {\n" +
            "      this.parameters = parameters;\n" +
            "      this.errors = [];\n" +
            "      this.arguments = ['createCalendarEvent'];\n" +
            "    },\n" +
            "\n" +
            "    parse: function() {\n" +
            "      if (!this.parameters) {\n" +
            "        this.errors.push('The object passed to createCalendarEvent cannot be null.');\n" +
            "      } else {\n" +
            "        this.parseDescription();\n" +
            "        this.parseLocation();\n" +
            "        this.parseSummary();\n" +
            "        this.parseStartAndEndDates();\n" +
            "        this.parseReminder();\n" +
            "        this.parseRecurrence();\n" +
            "        this.parseTransparency();\n" +
            "      }\n" +
            "\n" +
            "      var errorCount = this.errors.length;\n" +
            "      if (errorCount) {\n" +
            "        this.arguments.length = 0;\n" +
            "      }\n" +
            "\n" +
            "      return (errorCount === 0);\n" +
            "    },\n" +
            "\n" +
            "    parseDescription: function() {\n" +
            "      this._processStringValue('description');\n" +
            "    },\n" +
            "\n" +
            "    parseLocation: function() {\n" +
            "      this._processStringValue('location');\n" +
            "    },\n" +
            "\n" +
            "    parseSummary: function() {\n" +
            "      this._processStringValue('summary');\n" +
            "    },\n" +
            "\n" +
            "    parseStartAndEndDates: function() {\n" +
            "      this._processDateValue('start');\n" +
            "      this._processDateValue('end');\n" +
            "    },\n" +
            "\n" +
            "    parseReminder: function() {\n" +
            "      var reminder = this._getParameter('reminder');\n" +
            "      if (!reminder) {\n" +
            "        return;\n" +
            "      }\n" +
            "\n" +
            "      if (reminder < 0) {\n" +
            "        this.arguments.push('relativeReminder');\n" +
            "        this.arguments.push(parseInt(reminder) / 1000);\n" +
            "      } else {\n" +
            "        this.arguments.push('absoluteReminder');\n" +
            "        this.arguments.push(reminder);\n" +
            "      }\n" +
            "    },\n" +
            "\n" +
            "    parseRecurrence: function() {\n" +
            "      var recurrenceDict = this._getParameter('recurrence');\n" +
            "      if (!recurrenceDict) {\n" +
            "        return;\n" +
            "      }\n" +
            "\n" +
            "      this.parseRecurrenceInterval(recurrenceDict);\n" +
            "      this.parseRecurrenceFrequency(recurrenceDict);\n" +
            "      this.parseRecurrenceEndDate(recurrenceDict);\n" +
            "      this.parseRecurrenceArrayValue(recurrenceDict, 'daysInWeek');\n" +
            "      this.parseRecurrenceArrayValue(recurrenceDict, 'daysInMonth');\n" +
            "      this.parseRecurrenceArrayValue(recurrenceDict, 'daysInYear');\n" +
            "      this.parseRecurrenceArrayValue(recurrenceDict, 'monthsInYear');\n" +
            "    },\n" +
            "\n" +
            "    parseTransparency: function() {\n" +
            "      var validValues = ['opaque', 'transparent'];\n" +
            "\n" +
            "      if (this.parameters.hasOwnProperty('transparency')) {\n" +
            "        var transparency = this.parameters.transparency;\n" +
            "        if (contains(transparency, validValues)) {\n" +
            "          this.arguments.push('transparency');\n" +
            "          this.arguments.push(transparency);\n" +
            "        } else {\n" +
            "          this.errors.push('transparency must be opaque or transparent');\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "\n" +
            "    parseRecurrenceArrayValue: function(recurrenceDict, kind) {\n" +
            "      if (recurrenceDict.hasOwnProperty(kind)) {\n" +
            "        var array = recurrenceDict[kind];\n" +
            "        if (!array || !(array instanceof Array)) {\n" +
            "          this.errors.push(kind + ' must be an array.');\n" +
            "        } else {\n" +
            "          var arrayStr = array.join(',');\n" +
            "          this.arguments.push(kind);\n" +
            "          this.arguments.push(arrayStr);\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "\n" +
            "    parseRecurrenceInterval: function(recurrenceDict) {\n" +
            "      if (recurrenceDict.hasOwnProperty('interval')) {\n" +
            "        var interval = recurrenceDict.interval;\n" +
            "        if (!interval) {\n" +
            "          this.errors.push('Recurrence interval cannot be null.');\n" +
            "        } else {\n" +
            "          this.arguments.push('interval');\n" +
            "          this.arguments.push(interval);\n" +
            "        }\n" +
            "      } else {\n" +
            "        // If a recurrence rule was specified without an interval, use a default value of 1.\n" +
            "        this.arguments.push('interval');\n" +
            "        this.arguments.push(1);\n" +
            "      }\n" +
            "    },\n" +
            "\n" +
            "    parseRecurrenceFrequency: function(recurrenceDict) {\n" +
            "      if (recurrenceDict.hasOwnProperty('frequency')) {\n" +
            "        var frequency = recurrenceDict.frequency;\n" +
            "        var validFrequencies = ['daily', 'weekly', 'monthly', 'yearly'];\n" +
            "        if (contains(frequency, validFrequencies)) {\n" +
            "          this.arguments.push('frequency');\n" +
            "          this.arguments.push(frequency);\n" +
            "        } else {\n" +
            "          this.errors.push('Recurrence frequency must be one of: \"daily\", \"weekly\", \"monthly\", \"yearly\".');\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "\n" +
            "    parseRecurrenceEndDate: function(recurrenceDict) {\n" +
            "      var expires = recurrenceDict.expires;\n" +
            "\n" +
            "      if (!expires) {\n" +
            "        return;\n" +
            "      }\n" +
            "\n" +
            "      this.arguments.push('expires');\n" +
            "      this.arguments.push(expires);\n" +
            "    },\n" +
            "\n" +
            "    _getParameter: function(key) {\n" +
            "      if (this.parameters.hasOwnProperty(key)) {\n" +
            "        return this.parameters[key];\n" +
            "      }\n" +
            "\n" +
            "      return null;\n" +
            "    },\n" +
            "\n" +
            "    _processStringValue: function(kind) {\n" +
            "      if (this.parameters.hasOwnProperty(kind)) {\n" +
            "        var value = this.parameters[kind];\n" +
            "        this.arguments.push(kind);\n" +
            "        this.arguments.push(value);\n" +
            "      }\n" +
            "    },\n" +
            "\n" +
            "    _processDateValue: function(kind) {\n" +
            "      if (this.parameters.hasOwnProperty(kind)) {\n" +
            "        var dateString = this._getParameter(kind);\n" +
            "        this.arguments.push(kind);\n" +
            "        this.arguments.push(dateString);\n" +
            "      }\n" +
            "    }\n" +
            "  };\n" +
            "}());\n";
}
