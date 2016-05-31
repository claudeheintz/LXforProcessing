/**
 * Copyright (c) 2015-2016 by Claude Heintz Design
 *
 * This file is part of a library called LXforProcessing - https://github.com/claudeheintz/LXforProcessing
 * 
 * LXforProcessing is free software: you can redistribute it and/or modify
 * it under the terms of a BSD style license that should have been included with this file.
 * If not, see https://www.claudeheintzdesign.com/lx/opensource.html.
 * 
 ********************************************************************
 *
 * HueDemo
 *
 * Demonstrates interfacing with Phillips Hue hub.
 * -> Uses uPnP to discover Phillips Hue Bridge
 * -> Reads status from bridge
 * -> Allows control brightness/hue/saturation + on/off of 3 lights
 * -> Accepts remote control from OSC.  An example file for TouchOSC included.
 * 
*/

import lx4p.*;
import java.net.*;
import java.io.*;
import java.util.*;

// These connection options are used for finding a Hue Bridge on the local network
String nicName = "en1";          // on OS X, "en0" is usually Ethernet and "en1" is usually WiFi
                                 // if interface with name is not found, a list is printed and localAddress is used
String localAddress = "0.0.0.0";  // default is "0.0.0.0" or any address

// This user name is used to authenticate with the Hue Bridge.
// The first time it is used, it is necessary to manually
// press the button on the bridge when prompted, then press the 'c' key
String hueUser = "lx4hue1234";

// pick the OSC output target and port for receiving
String myNetworkInterface = "en1";
String osc_target_address = "127.0.0.1";
int osc_target_port = 60006;
int osc_receive_port = 60007;

LXOSC myosc;

/// The network interface to a Hue Bridge handles establishing connection and getting/setting lights' state
LXHueInterface myHue = null;
/// A color picker for choosing hue/saturation
LXPColorPicker picker;
/// Group of radio buttons for selecting the light being controlled
LXPRadioGroup rg;
/// On/Off Radio buttons
LXPRadioGroup rg2;
/// A fader for controlling intensity
LXPVScrollbar levelFader;

/// Arrays holding the state of the lights
boolean[] onswitch = {true, true, true};
int[] hhue = {0,0,0};
int[] hbri = {255, 255, 255};
int[] hsat = {0, 0, 0};
/// index of the current light being controlled
int hlight = 0;
/// flag indicating if the initial state of the lights has been retrieved from the Hue Bridge
boolean initializedLightsState = false;

boolean lightStateNeedsUpdate = false;
int fcount = 0;

//***************************************************************************
// PApplet functions

void setup() {
  size( 500, 500 );
  frameRate( 10 );
  
  picker = new LXPColorPicker( 10, 10, 400, 400, 255 );

  myHue = new LXHueInterface(hueUser);
  myHue.connectToBridge(localAddress, nicName);
  
  rg = new LXPRadioGroup(3);
  rg.addButton(40, 425, 20).fillColor = color(200);
  rg.addButton(40, 455, 20).fillColor = color(200);
  rg.addButton(40, 485, 20).fillColor = color(200);
  rg.setSelectedIndex(0);
  
  rg2 = new LXPRadioGroup(2);
  rg2.addButton(440, 435, 20).fillColor = color(200);
  rg2.addButton(440, 465, 20).fillColor = color(200);
  rg2.setSelectedIndex(0);
  
  levelFader = new LXPVScrollbar(440, 10, 24, 400, 6);
  
  // Network interfaces are named differently on Windows
  if ( myNetworkInterface != null ) {
    if ( myNetworkInterface.equals("en0") ) {
      if ( System.getProperty("os.name").startsWith("Win") ) {
        myNetworkInterface = "eth1";
      }
    }
  }
  
  myosc = LXOSC.createLXOSC(null, "0.0.0.0", osc_receive_port);
}

void draw() {
  if ( ! initializedLightsState ) {
    initializeLightsState();
  }
  checkOSC();
  background( 80 );
  
  // ***** draw and update the color picker
  picker.draw(this);
  if ( picker.pick(this) ) {
    hhue[hlight] = (int)(256*hue(picker.current));
    hsat[hlight] = (int)saturation(picker.current);
    rg.selectedButton().fillColor = picker.current;
    lightStateNeedsUpdate = true;
  }
  
  // ***** draw the radio buttons and labels
  rg.draw(this);
  fill( 200, 200, 200);
  text("1", 18, 430);
  text("2", 18, 460);
  text("3", 18, 490);
  rg2.draw(this);
  text("On", 458, 440);
  text("Off", 458, 470);
  
  // ***** update and draw the fader
  if ( levelFader.update(this) ) {
    hbri[hlight] = levelFader.getValue();
    lightStateNeedsUpdate = true;
  }
  levelFader.draw(this);
  
  // draw a string showing the status of the connection
  drawStatusString();
  
  // if one of the update methods caused a change, send it to the Hue Bridge
  // but only once every 5th loop (1/2 sec at 10 frames/sec)
  fcount++;
  if ( fcount > 5 ) {
    fcount = 0;
    if ( lightStateNeedsUpdate ) {
      setLightParams();
      lightStateNeedsUpdate = false;
    }
  }
}

void mousePressed() {
  if ( rg.mousePressed() ) {
    if ( rg.selected >= 0 ) {
      hlight = rg.selected;
      setupControlsForLight(hlight);
    }
  } else if ( rg2.mousePressed() ) {
    if ( rg2.selected == 0 ) {
      if ( ! onswitch[hlight] ) {
        onswitch[hlight] = true;
        setLightParams();
      }
    } else if ( onswitch[hlight] ) {
        onswitch[hlight] = false;
        setLightParams();
    }
  }
}

/**
 * Called when a key is pressed.
 * "q" quits the application
 * "c" is used to try clear an error in the Hue interface
 *
 * When first connecting to the Hue Bridge it is necessary to
 * authenticate a user name by physically pushing the button.
 *
 * When the bridge returns an unauthorized user error,
 * the interface is in a mode where a call to clearStatus will
 * tell the bridge to create a user with the supplied name.
 *
 * After the button on the bridge is pressed, pressing the "c"
 * key will tell the bridge to create the userName.
 */
void keyPressed() {
  if ( key == 'c' ) {
    myHue.clearStatus();
  }
  
  if ( key == 'q' ) {
    myHue.close();
    System.exit(0);
  }
}

//***************************************************************************
// custom functions

/**
 * Sends the state of the current light (given by the index variable hlight)
 * to the Hue Bridge.
 */
void setLightParams() {
  if ( myHue.status == 1 ) {
    String jbody = myHue.params2json(onswitch[hlight], hsat[hlight], hbri[hlight], hhue[hlight]);
    if ( myHue.setLight(hlight+1, jbody) ) {
       System.out.println("Set Light " + (hlight+1) + "->" + jbody);
    }
  }
}

void drawStatusString() {
  if ( myHue.status == 0 ) {
    fill( 255, 255, 0);
    text("Connecting...",175, 430);
  } else if ( myHue.status == -1 ) {
    fill( 255, 100, 100);
    text("ERROR: Press 'c' to clear",145, 430);
  } else if ( myHue.status == -2 ) {
    fill( 255, 100, 100);
    text("Press button on Hue Bridge, then press 'c'",100, 430);
  } else {
    fill( 0, 255, 0);
    text("Connection OK",170, 430);
  }
}

/**
 * Adjusts the controls to reflect state of the light at lightIndex
 * (current is identified by the index variable hlight)
 */
void setupControlsForLight(int lightIndex) {
  levelFader.setValue(hbri[lightIndex], false);
  if ( onswitch[lightIndex] ) {
    rg2.setSelectedIndex(0);  //on button
  } else {
    rg2.setSelectedIndex(1);  //off button
  }
  setButtonColorForLight(lightIndex);
}

/**
 * Sets the fillColor of the light selection button at index i
 */
public void setButtonColorForLight(int i) {
  rg.buttonAtIndex(i).fillColor = LXPColorPicker.HueSat2RGB(hhue[i]>>8, hsat[i]);
 }

/**
 *  Uses the Hue interface to get a JSON string representing the state of the lights.
 *  Then it syncs the status variables for each of the lights and updates the controls.
 *  This function is called by the draw loop until it completes the setup.
 */
void initializeLightsState() {
  if ( myHue.status == 1 ) {
    String jstr = myHue.getHueLightsState();
    if ( jstr != null ) {
      LXJSONParser jparser = new LXJSONParser();
      LXJSONElement jroot = jparser.parseString(jstr);
      if ( jroot != null ) {
        //jroot.print();
        syncLightStatus(jroot, 0);
        syncLightStatus(jroot, 1);
        syncLightStatus(jroot, 2);
      }
    }
    setupControlsForLight(hlight);
    initializedLightsState= true;  // don't call this function anymore
  }
}

/**
 * Utility function to get an int from a string
 * trapping a format exception.
 */
public int safeParseInt(String s) {
  try {
    return( Integer.parseInt(s) );
  } catch ( NumberFormatException e ) {}
  return 0;
}

/**
 * Gets the state of a light at lightIndex from JSON element tree
 * and uses the values to set the state of the status variables.
 * Also, sets the button color for the light.
 */
void syncLightStatus(LXJSONElement jroot, int lightIndex ) {
  LXJSONElement jlight = jroot.findSubElement(Integer.toString(lightIndex+1));
  if ( jlight != null ) {
    LXJSONElement jstate = jlight.findSubElement("state");
    if ( jstate != null ) {
      LXJSONElement jparam = jstate.findSubElement("on");
      if ( jparam != null ) {
        if ( jparam.value.equals("true") ) {
          onswitch[lightIndex] = true;
        } else {
          onswitch[lightIndex] = false;
        }
      }
      jparam = jstate.findSubElement("bri");
       if ( jparam != null ) {
          hbri[lightIndex] = safeParseInt(jparam.value);
       }
       jparam = jstate.findSubElement("sat");
       if ( jparam != null ) {
          hsat[lightIndex] = safeParseInt(jparam.value);
       }
       jparam = jstate.findSubElement("hue");
       if ( jparam != null ) {
          hhue[lightIndex] = safeParseInt(jparam.value);
       }
       setButtonColorForLight(lightIndex);
    }  //state
  }    //light
}

/**
 * Called when window is closed.
 * Properly stops search for devices and closes network sockets if necessary
 */
void dispose(){
  myHue.close();
  System.out.println("bye bye!");
}

void checkOSC() {
  boolean ok2read = true;
  int msgct = 0;
  while ( ok2read ) {
    Vector<LXOSCMessage> msgs = myosc.readPacket();
    if ( msgs.size() > 0 ) {

      LXOSCMessage msg = msgs.elementAt(0);
      System.out.println("OSC");
      if ( msg.partOfPatternMatchesAddressString(0, "1") ) {      //first address pattern element
        if ( msg.partOfPatternMatchesAddressString(1, "xy1") ) {
          System.out.println();
          System.out.println(msg.argumentAt(0).getFloat() + ", " + msg.argumentAt(1).getFloat());
          
          hhue[hlight] = (int) (msg.argumentAt(1).getFloat() * 65535);  // y position sets hue
          hsat[hlight] = (int) (msg.argumentAt(0).getFloat() * 255);    // x position sets saturation
          lightStateNeedsUpdate = true;
        } else if ( msg.partOfPatternMatchesAddressString(1, "push1") ) {
          rg.setSelectedIndex(0);
          hlight = 0;
        } else if ( msg.partOfPatternMatchesAddressString(1, "push2") ) {
          rg.setSelectedIndex(1);
          hlight = 1;
        } else if ( msg.partOfPatternMatchesAddressString(1, "push3") ) {
          rg.setSelectedIndex(2);
          hlight = 2;
        } else if ( msg.partOfPatternMatchesAddressString(1, "push4") ) {
          rg2.setSelectedIndex(0);
          onswitch[hlight] = true;
          lightStateNeedsUpdate = true;
        } else if ( msg.partOfPatternMatchesAddressString(1, "push5") ) {
          rg2.setSelectedIndex(1);
          onswitch[hlight] = false;
          lightStateNeedsUpdate = true;
        } else if ( msg.partOfPatternMatchesAddressString(1, "fader1") ) {
          hbri[hlight] = (int) (msg.argumentAt(0).getFloat() * 255);
          levelFader.setValue(hbri[hlight], false);
          lightStateNeedsUpdate = true;
        }
      }
      msgct++;
      if ( msgct > 10 ) {
        ok2read = false;
      }
    } else {             // msgs.size = 0;
      ok2read = false;
    }
  }                      // ok2read
}                        // checkOSC()