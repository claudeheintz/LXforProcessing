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
* RGBColorPicker
* color picker design inspired by http://www.julapy.com/processing/ColorPicker.pde
*
* Chosen color sets group of 3 addresses/channels of DMX (Art-Net or sACN E1.31)
*
* use_sacn = true,  selects sACN E1.31 multicast to universe 1
* use_sacn = false, selects Art-Net broadcasting to myBroadcastAddress
*
*/
import java.net.*;
import lxprocessing.*;

// if you specify a network interface, myNetworkAddress can be set automatically
// on Mac ethernet is ofter en0 and WiFi is en1
// if the requested interface is not found, a list of available interfaces is printed
// if you want to bind to a specific connection, specify the address and set myNetworkInterface to null
String myNetworkInterface = "en1";
String myNetworkAddress = "192.168.1.102";

// pick the protocol Art-Net or sACN
// myMulticastAddress is only used for sACN.  239.255.0.1 = universe 1
boolean use_sacn = false;
String myMulticastAddress = "239.255.0.1";

LXDMXEthernet dmx;

LXPColorPicker cp;
LXPRadioGroup rg;
static int numberOfButtons = 12;

void setup() 
{
  size( 500, 500 );
  frameRate( 40 );
  
  cp = new LXPColorPicker( 10, 10, 400, 400, 255 );
  rg = new LXPRadioGroup(numberOfButtons);
  for ( int i=0; i<numberOfButtons; i++) {
    rg.addButton(30+i*30, 435, 20).fillColor = color(0);
  }
  rg.setSelectedIndex(0);
  
  // Network interfaces are named differently on Windows
  if ( myNetworkInterface != null ) {
    if ( myNetworkInterface.equals("en0") ) {
      if ( System.getProperty("os.name").startsWith("Win") ) {
        myNetworkInterface = "eth1";
      }
    }
  }
  
  if ( use_sacn ) {
    dmx = LXDMXEthernet.createDMXEthernet(use_sacn, myNetworkInterface, myNetworkAddress, myMulticastAddress);
  } else {
    //null targetAddress means broadcast address automatically assigned
    dmx = LXDMXEthernet.createDMXEthernet(use_sacn, myNetworkInterface, myNetworkAddress, null);
  }
  if ( dmx != null ) {
    dmx.setNumberOfSlots(36);
  }
}

void draw ()
{
  background( 80 );
  rg.draw(this);
  cp.draw(this);
  if ( cp.pick(this) ) {
    rg.selectedButton().fillColor = cp.current;
  }
  
  if ( dmx != null ) {
    for(int j=0; j<rg.buttonCount; j++) {
      int k = j*3;
      color kc = rg.buttons[j].fillColor;
      dmx.setSlot(k+1, ((kc>>16) & 0xff));
      dmx.setSlot(k+2, ((kc>>8) & 0xff));
      dmx.setSlot(k+3, (kc & 0xff));
    }
    if ( dmx != null ) {
      dmx.sendDMX();
    }
  }
}

void mousePressed() {
  rg.mousePressed();
}

void dispose(){
  if ( dmx != null) {
    dmx.close();
    System.out.print("closed socket, ");
  }
  System.out.println("bye bye!");
}