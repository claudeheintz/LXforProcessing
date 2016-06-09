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
 * Two Scene Preset Simulator
 *
 * This example functions like a classic two scene preset light board where two
 * banks of faders are controlled by opposing x/y master faders.
 *
 * When a DMX interface is selected, changing the position of a scrollbar sets the level of the corresponding
 * address/channel.  (to patch the sliders to other dmx addresses uncomment and edit outAddresses array)
 *
 * When OSC is active, changing the position of a scrollbar sends an OSC Message
 * with one of the following the address patterns:
      /1/faderN   (where N is the number of the slider in the x bank)
      /2/faderN   (where N is the number of the slider in the y bank)
      /3/fader1   the x master fader
      /3/fader2   the y master fader
 * The message has a float argument 0-1.0 that represents the position of the slider.
 *
 * Sliders are controllable also controllable by the same OSC messages.
 * TouchOSC and Lemur files for bi-directional control are included in the example folder.
 *
 * The master sliders can be controlled manually.  Click on the thumb of one master.
 * Drag the mouse over to the other and you can move both at the same time.
 * Optionally the master faders can be auto faded between the two scenes for 5 seconds.
 *
*/

import java.net.*;
import java.util.*;
import lx4p.*;
import processing.serial.*;

//*********************************  Control Variables  *********************************

// if you specify a network interface name, binding address can be set automatically
// otherwise, set myNetworkInterface = "" and enter the local ip address of the desired interface
// networkInterfaceIndex is used to search through available interfaces
int networkInterfaceIndex = 0;
String myNetworkInterface = getNextNetworkInterfaceName();
String myNetworkAddress = "127.0.0.1";

// ***** DMX output modes.
//       Selected with radio group rgOutput
static final int OUTPUT_OFF = 0;
static final int OUTPUT_ENTTEC = 1;
static final int OUTPUT_ARTNET = 2;
static final int OUTPUT_SACN = 3;

// ***** DMX output settings
int protocol = OUTPUT_OFF;

// sACN uses multicast
String myMulticastAddress = "239.255.0.1";
// a seral port is used for the ENTTEC DMX USB Pro protocol
// serialPortIndex is used to search through available interfaces
int serialPortIndex = 0;
String myPortName = getNextSerialPortName();
// DMX interface object
LXDMXInterface dmx;

// ***** OSC settings
//       select mode with radio group rgOSC
static final int OSC_OFF = 0;
static final int OSC_ENABLED = 1;
// OSC output target and port for receiving
String osc_target_address_string = "127.0.0.1";
InetAddress osc_target_address = null;
int osc_target_port = 53010;
// set the output address when OSC is received
boolean aquire_target = true;
// OSC input port
int osc_receive_port = 53011;
// OSC interface object
LXOSC myosc = null;

// ***** Auto-fade
//       select mode with radio group rgAutoFade
static final int AUTO_FADE_OFF = 0;
static final int AUTO_FADE_ENABLED = 1;
boolean fading = false;
// fade direction (masters up/down, fade to bank x/y)
boolean fading_to_x = true;
// Auto-fade timing
double time_fade_started;
double duration = 3;
double last_artnet_poll;

// ***** arrays
//       hold the current values of the x/y faders, masters and resulting output
int[] barlevels;
int[] barylevels;
int[] outlevels;
// array that can be used to patch output to DMX addresses
int[] outAddresses;



//*********************************  UI Variables  *********************************

// The app has are two modes, setup and operate.
//     Text fields and radio buttons are shown in setup mode.
//     Sliders and masters are shown in operate mode.
boolean setup_mode = true;
static int numberOfBars = 12;

// ***** sliders for the x and y bank of faders as well as the master faders
LXPVScrollbar[] bars;
LXPVScrollbar[] barsy;
LXPVScrollbar masterx;
LXPVScrollbar mastery;

// ***** radio groups for setup mode
LXPRadioGroup rgOutput;
LXPRadioGroup rgOSC;
LXPRadioGroup rgAutoFade;

// ***** text fields
// text fields need to be in a group which sends key presses to the active field
LXPTextGroup fieldGroup;
LXPTextField network_interface_field;
LXPTextField local_ip_address_field;
LXPTextField dmx_target_address_field;
LXPTextField widget_port_name_field;
LXPTextField osc_target_ip_field;
LXPTextField osc_target_port_field;
LXPTextField osc_receive_port_field;

// ***** push buttons
// buttons subclass LXPButton and override mouseClicked() method
public class LXPSetupModeButton extends LXPButton {
    public LXPSetupModeButton(int xp, int yp, int w, int h, String t) {
      super(xp,yp,w,h,t);
    }
    
    public void mouseClicked() {
      setup_mode = ! setup_mode;
      if ( setup_mode ) {
        title = "Run";
      } else {
        title = "Setup";
      }
    }
 }
LXPSetupModeButton mode_button;

public class LXPNextNetworkInterfaceButton extends LXPButton {
    public LXPNextNetworkInterfaceButton(int xp, int yp, int w, int h, String t) {
      super(xp,yp,w,h,t);
    }
    
    public void mouseClicked() {
		myNetworkInterface = getNextNetworkInterfaceName();
		network_interface_field.value = myNetworkInterface;
    }
 }
LXPNextNetworkInterfaceButton nicSelect_button;

public class LXPNextSerialPortButton extends LXPButton {
    public LXPNextSerialPortButton(int xp, int yp, int w, int h, String t) {
      super(xp,yp,w,h,t);
    }
    
    public void mouseClicked() {
		myPortName = getNextSerialPortName();
		widget_port_name_field.value = myPortName;
    }
 }
LXPNextSerialPortButton serialSelect_button;

public class LXPGoButton extends LXPButton {
    public LXPGoButton(int xp, int yp, int w, int h, String t) {
      super(xp,yp,w,h,t);
    }
    
    public void mouseClicked() {
      startFade();
    }
 }
LXPGoButton go_button;

//*********************************  setup UI & control  *********************************

void setup() {
  size(740, 400);
  frameRate( 10 );
  
  masterx = new LXPVScrollbar(600, 100, 24, 200, 1);
  masterx.oscAddress = "/3/fader1";
  mastery = new LXPVScrollbar(645, 100, 24, 200, 1);
  mastery.oscAddress = "/3/fader2";
  bars = new LXPVScrollbar[numberOfBars];
  barsy = new LXPVScrollbar[numberOfBars];
  barlevels = new int[numberOfBars];
  barylevels = new int[numberOfBars];
  outlevels = new int[numberOfBars];
  outAddresses = new int[numberOfBars];
  for( int i=0; i<numberOfBars; i++) {
    bars[i] = new LXPVScrollbar(40+i*45, 30, 24, 150, 1);
    bars[i].oscAddress = "/1/fader" + (i+1);
    barsy[i] = new LXPVScrollbar(40+i*45, 210, 24, 150, 1);
    barsy[i].oscAddress = "/2/fader" + (i+1);
    barlevels[i] = 0;
    outAddresses[i] = i+1;	//patch 1 to 1.  
  }
  // Uncomment the following and use outAddresses array to set patch of slider to dmx address
  // note that array index is zero based
  /*
  outAddresses[0] = 1;
  outAddresses[1] = 2;
  outAddresses[2] = 3;
  outAddresses[3] = 4;
  outAddresses[4] = 5;
  outAddresses[5] = 6;
  outAddresses[6] = 7;
  outAddresses[7] = 8;
  outAddresses[8] = 9;
  outAddresses[9] = 10;
  outAddresses[10] = 11;
  outAddresses[11] = 12;
  */

  rgOutput = new LXPRadioGroup(4);
  LXPRadioButton nrb = rgOutput.addButton(100, 160, 20);
  nrb.title = "Off:";
  nrb = rgOutput.addButton(100, 120, 20);
  nrb.title = "ENTTEC:";
  nrb = rgOutput.addButton(100, 80, 20);
  nrb.title = "Art-Net:";
  nrb = rgOutput.addButton(100, 40, 20);
  nrb.title = "sACN:";
  
  if ( dmx != null ) {
    rgOutput.setSelectedIndex(protocol);
  } else {
    rgOutput.setSelectedIndex(OUTPUT_OFF);
  }
  
  rgOSC = new LXPRadioGroup(2);
  nrb = rgOSC.addButton(100, 265, 20);
  nrb.title = "Off";
  nrb = rgOSC.addButton(100, 225, 20);
  nrb.title = "OSC";
  nrb.state = true;
  rgOSC.setSelectedIndex(OSC_OFF);      //OSC starts off
  
  rgAutoFade = new LXPRadioGroup(2);
  nrb = rgAutoFade.addButton(600, 50, 20);
  nrb.title = "Manual";
  nrb.state = true;
  nrb = rgAutoFade.addButton(600, 90, 20);
  nrb.title = "Auto";
  rgAutoFade.setSelectedIndex(AUTO_FADE_OFF);      //defaults manual
  
  /* A text field group is necessary to direct key presses to the
  *  text field that has keyboard "focus".  In the keyPressed() method below,
  * the fieldGroup object manages the distribution of the key press to the "focused" field
  */
  fieldGroup = new LXPTextGroup(8);
  network_interface_field = fieldGroup.addField(275,28,8, "Network Interface:");
  network_interface_field.value = myNetworkInterface;
  local_ip_address_field = fieldGroup.addField(275,53,15, "Local Bind Address:");
  local_ip_address_field.value = myNetworkAddress;
  dmx_target_address_field = fieldGroup.addField(275,78,15, "DMX Target Address:");
  if ( protocol == 0 ) {
    dmx_target_address_field.value = myMulticastAddress;
  } else {
    dmx_target_address_field.value = "";
  }
  nicSelect_button = new LXPNextNetworkInterfaceButton(350, 28, 20, 16, ">");
  
  widget_port_name_field = fieldGroup.addField(200,113,32, "Serial Port:");
  widget_port_name_field.value = myPortName;
  serialSelect_button = new LXPNextSerialPortButton(490, 113, 20, 16, ">");
  
  osc_target_ip_field = fieldGroup.addField(255,210,15, "OSC Target IP:");
  osc_target_ip_field.value = "127.0.0.1";
  osc_target_port_field = fieldGroup.addField(255,235,6, "OSC Target Port:");
  osc_target_port_field.value = "53010";
  osc_receive_port_field = fieldGroup.addField(255,260,6, "OSC Receive Port:");
  osc_receive_port_field.value = "53011";
  
  // Network interfaces are named differently on Windows
  // adjust for ethernet
  if ( network_interface_field.value.equals("en0") ) {
    if ( System.getProperty("os.name").startsWith("Win") ) {
      network_interface_field.value = "eth1";
    }
  }
  setupNetworkSocket();
  
  mode_button = new LXPSetupModeButton(680, 360, 50, 20, "Run");
  go_button = new LXPGoButton(590, 310, 65, 20, "Go");
}

//*********************************  drawing & events  *********************************

void draw() {
  checkOSC();
  background(255);
  stroke(0);
  mode_button.draw(this);
  if ( setup_mode ) {
    fieldGroup.draw(this);
    rgOutput.draw(this);
    rgOSC.draw(this);
    rgAutoFade.draw(this);
    nicSelect_button.draw(this);
    serialSelect_button.draw(this);
  } else {
    noStroke();
    int barlevel;
    int barylevel;
    float barout;
    float baryout;
    int outlevel;
    int oldmasterx = masterx.getValue();
    int oldmastery = mastery.getValue();
    
    if ( rgAutoFade.selected == AUTO_FADE_ENABLED ) {
      go_button.draw(this);
      //update to get current values
      if ( fading ) {
        fading = ! updateFade();
      }
    }
    masterx.update(this);
    mastery.update(this);
    
    int newmasterx = masterx.getValue();
    int newmastery = mastery.getValue();
    
    //convert to 0.0->1.0
    float mLevelx = newmasterx/255.0;
    float mLevely = newmastery/255.0;
    
    //if changed, send OSC
    if ( osc_target_address != null ) {
      if ( oldmasterx != newmasterx ) {
        masterx.sendValueWithOSC(myosc, osc_target_address, osc_target_port);
      }
      if ( oldmastery != newmastery ) {
        mastery.sendValueWithOSC(myosc, osc_target_address, osc_target_port);
      }
    }
    mLevely = 1 - mLevely;  //invert y master level
    
    masterx.draw(this, mLevelx, LXPVScrollbar.INDICATOR_RED);
    mastery.draw(this, mLevely, LXPVScrollbar.INDICATOR_GREEN);
    
    for (int i=0; i<bars.length; i++) {
      bars[i].update(this);
      barsy[i].update(this);
      
      
      barlevel = bars[i].getValue();
      barout = mLevelx*barlevel;
      barylevel = barsy[i].getValue();
      baryout = barylevel*mLevely;
      
      outlevel = Math.round(barout+baryout);  //dipless
      if ( outlevel > 255 ) {
        outlevel = 255;
      }
      
      bars[i].draw(this, barout/255.0, LXPVScrollbar.INDICATOR_RED);
      barsy[i].draw(this, baryout/255.0, LXPVScrollbar.INDICATOR_GREEN);
      
      if ( barlevel != barlevels[i] ) {
        barlevels[i] = barlevel;
        if ( osc_target_address != null ) {
          bars[i].sendValueWithOSC(myosc, osc_target_address, osc_target_port);
        }
      }
      if ( barylevel != barylevels[i] ) {
        barylevels[i] = barylevel;
        if ( osc_target_address != null ) {
          barsy[i].sendValueWithOSC(myosc, osc_target_address, osc_target_port);
        }
      }
      if ( outlevel != outlevels[i] ) {
        outlevels[i] = outlevel;
        if ( dmx != null ) {
          dmx.setSlot(outAddresses[i], outlevel);
        }
      }
    }      // for loop i in bars.length
    
    if ( dmx != null ) {
      dmx.sendDMX();
    }
  }
}

void mousePressed() {
  if ( setup_mode ) {
    if ( rgOutput.mousePressed() ) {
      protocol = rgOutput.selected;
      if ( protocol == OUTPUT_SACN ) {
        dmx_target_address_field.value = myMulticastAddress;
      } else if ( protocol == OUTPUT_ARTNET ) {
        if ( dmx_target_address_field.value.equals(myMulticastAddress) ) {
          dmx_target_address_field.value = "";
        }
      }
      setupNetworkSocket();
    } else if ( rgOSC.mousePressed() ) {
      if ( rgOSC.selected == OSC_OFF ) {
        if ( myosc != null ) {
          myosc.close();
        }
        myosc = null;
        osc_target_address = null;
      } else {
        setupOSC();
      }
    } else if ( rgAutoFade.mousePressed() ) {
      
    } else {
      fieldGroup.mousePressed();
    }
  }
}

void mouseReleased() {
  mode_button.mouseReleased();
  if ( setup_mode ) {
  	serialSelect_button.mouseReleased();
  	nicSelect_button.mouseReleased();
  } else {
	  if ( rgAutoFade.selected == AUTO_FADE_ENABLED ) {
		 go_button.mouseReleased();
	  }
  }
}

void keyPressed() {
  if ( setup_mode ) {
    fieldGroup.keyPressed(this);
  }
}

void dispose(){
  if ( dmx != null) {
    dmx.close();
    System.out.print("closed dmx");
  }
  if ( myosc != null ) {
    myosc.close();
  }
  System.out.println("bye bye!");
}

//*********************************  Network Methods  *********************************

void setupNetworkSocket() {
  if ( dmx != null ) {
    dmx.close();
    dmx = null;
  }
  
  String interfaceToFind = network_interface_field.value;
  if ( interfaceToFind.equals("search") ) {
    interfaceToFind = "";
  }
  
  if ( protocol == OUTPUT_SACN ) {
    dmx = LXDMXEthernet.createDMXEthernet(LXDMXEthernet.CREATE_SACN, interfaceToFind, local_ip_address_field.value, dmx_target_address_field.value);
  } else if ( protocol == OUTPUT_ARTNET ) {
    //null targetAddress means broadcast address automatically assigned
    String taddr = dmx_target_address_field.value;
    if ( taddr.length() == 0 ) {
      taddr = null;
    }
    dmx = LXDMXEthernet.createDMXEthernet(LXDMXEthernet.CREATE_ARTNET, interfaceToFind, local_ip_address_field.value, taddr);
    ((LXArtNet) dmx).setPollReplyListener(new LXArtNetPollReplyListener() {
      public boolean pollReplyReceived(LXArtNetPollReplyInfo info) {
          System.out.println("Found node:" + info.longNodeName() + " @ " + info.nodeAddress());
          dmx_target_address_field.value = info.nodeAddress().getHostAddress();
          return false; // set to true to automatically use found address
        }
    });
    checkPollReply();
  } else if ( protocol == OUTPUT_ENTTEC ) {
    dmx = LXENTTEC.createDMXSerial(this, widget_port_name_field.value, 9600);
  }
  if ( dmx != null ) {
    int m = bars.length;        // find highest output address
    for(int j=0; j<outAddresses.length; j++) {
      if ( outAddresses[j] > m ) {
        m = outAddresses[j];
      }
    }
    dmx.setNumberOfSlots(m);
  }
}

void setupOSC() {
  if ( myosc == null ) {
    myosc = LXOSC.createLXOSC(null, "0.0.0.0", safeParseInt(osc_receive_port_field.value));
    try {
      osc_target_address = InetAddress.getByName(osc_target_ip_field.value);
      osc_target_port = safeParseInt(osc_target_port_field.value);
      synchOSC();
    } catch (Exception e) {}  //ignore
  }
}

String getNextNetworkInterfaceName() {
	networkInterfaceIndex += 1;
	int ni = 0;
	try {
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		while ( nets.hasMoreElements() ) {
		  NetworkInterface nic = nets.nextElement();
		  if ( ni == networkInterfaceIndex ) {
				return nic.getName();
		  }
		  ni ++;
		}
		// did not find, reset and return first (if possible)
		networkInterfaceIndex = 0;
		/*nets = NetworkInterface.getNetworkInterfaces();
		if ( nets.hasMoreElements() ) {
			return nets.nextElement().getName();
		}*/
    return "search";
	} catch (Exception e) {}
	return "";
}

void checkPollReply() {
  try {
    DatagramSocket pollsocket = new DatagramSocket( null );
    pollsocket.setReuseAddress(true);
    pollsocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), ((LXArtNet)dmx).getPort()));
    pollsocket.setSoTimeout(500);
    pollsocket.setBroadcast(true);
    ((LXArtNet)dmx).sendArtPoll();
    ((LXArtNet)dmx).readArtNetPollPackets(pollsocket);
    pollsocket.close();
  } catch (Exception e) {
  }
}

//*********************************  Serial  *********************************

String getNextSerialPortName() {
	serialPortIndex += 1;
	String[] ports = Serial.list();
	if ( ports.length > 0 ) {
		if ( serialPortIndex >= ports.length ) {
			serialPortIndex = 0;
		}
		return ports[serialPortIndex];
	}
	return "";
}

//*********************************  OSC  *********************************

/**
 * read up to 10 mesages from the socket
*/
void checkOSC() {
  if ( myosc != null ) {
    boolean ok2read = true;
    int msgct = 0;
    while ( ok2read ) {
      Vector<LXOSCMessage> msgs = myosc.readPacket();  //may return a bundle
      if ( msgs.size() > 0 ) {
		  if ( aquire_target ) {
          osc_target_ip_field.value = myosc.receivedFrom.getHostAddress();
        }

        for ( int k=0; k<msgs.size(); k++ ) {
          LXOSCMessage msg = msgs.elementAt(k);
          if ( msg.partOfPatternMatchesAddressString(0,"1") ) {
            for (int i=0; i<bars.length; i++) {
              if ( bars[i].setValueWithOSCMessage(msg) ) {
                System.out.println("osc= " + bars[i].getValue());
                barlevels[i] = bars[i].getValue();
              }
            }
          } else if ( msg.partOfPatternMatchesAddressString(0,"2") ) {
            for (int i=0; i<barsy.length; i++) {
              if ( barsy[i].setValueWithOSCMessage(msg) ) {
                barylevels[i] = barsy[i].getValue();
              }
            }
          } else if ( msg.partOfPatternMatchesAddressString(0,"3") ) {
            if (  msg.partOfPatternMatchesAddressString(1, "push1") ) {
              if (( rgAutoFade.selected == AUTO_FADE_ENABLED ) && ( msg.floatAt(0) > 0 )) {
                startFade();
              }
            } else {
              masterx.setValueWithOSCMessage(msg);
              mastery.setValueWithOSCMessage(msg);
            }
          }
        }
          
        msgct++;
        if ( msgct > 10 ) {
          ok2read = false;
        }
      } else {
        ok2read = false;
      }
    }
  }
}

void synchOSC() {
  for (int k=0; k<bars.length; k++) {
    bars[k].sendValueWithOSC(myosc, osc_target_address, osc_target_port);
    barsy[k].sendValueWithOSC(myosc, osc_target_address, osc_target_port);
  }
  masterx.sendValueWithOSC(myosc, osc_target_address, osc_target_port);
  mastery.sendValueWithOSC(myosc, osc_target_address, osc_target_port);
}

//*********************************  Auto-Fade Methods  *********************************

public void startFade() {
  fading = true;
  time_fade_started = System.currentTimeMillis();
  fading_to_x = ( masterx.getValue() < 127 );
}

public boolean updateFade() {
     double elapsedSeconds = (System.currentTimeMillis()- time_fade_started) / 1000;
     boolean finished = false;
     
     // ratio is % progress of fade,  0.0 to 1.0,  elapsedSeconds/duration
     double ratio;
     
     // guard against division by zero if duration == 0
     if ( duration == 0 ) {
       ratio = 1;
       finished = true;  //if duration is zero, we're done
     } else {
       ratio = elapsedSeconds/duration;
       if ( ratio > 1 ) {
         ratio = 1;
       }
       finished = ( elapsedSeconds > duration );
     }
     
     if ( fading_to_x ) {
       masterx.setValue((int)Math.round(255.0*ratio));
       mastery.setValue((int)Math.round(255.0*ratio));
     } else {
       masterx.setValue((int)Math.round(255.0*(1-ratio)));
       mastery.setValue((int)Math.round(255.0*(1-ratio)));
     }
     
     return finished;
   }

/**
    utility function to get int from String
*/
int safeParseInt(String s) {
  try {
    return( Integer.parseInt(s) );
  } catch ( NumberFormatException e ) {}
  return (0);
}