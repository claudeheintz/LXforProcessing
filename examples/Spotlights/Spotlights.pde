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
 * Spotlights (based on Processing Spot example)
 * 
 * Adjusts color of four spotlights based on DMX input via ethernet
 * Addresses 1-3 =   R,G,B Left light
 * Addresses 4-6 =   R,G,B Right light
 * Addresses 7-9 =   R,G,B Top light
 * Addresses 10-12 = R,G,B Bottom light
*/
import java.net.*;
import lx4p.*;

// if you specify a network interface, myNetworkAddress can be set automatically (but won't receive broadcast packets)
// if you want a specific connection, specify the address and set myNetworkInterface to null
// if you want to receive packets from any source, specify any address, "0.0.0.0"
String myNetworkInterface = "en0";
String myNetworkAddress = "0.0.0.0";

// pick the protocol
// myMulticastAddress is only used for sACN.  239.255.0.1 = universe 1
boolean use_sacn = false;
String myMulticastAddress = "239.255.0.1";

LXDMXEthernet dmx;

float mx, my;

void setup() {
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
  System.out.println(dmx);
  
  size(400,400, P3D);
  noStroke();
  background(0);
}

void draw() {
  float y;
  if ( dmx != null ) {
    if ( dmx.readPacket() ) {
       background(0);
       //mx = map(mouseX, 0, width, -1, 1);
       //System.out.println(mx);
       spotLight(dmx.getSlot(1), dmx.getSlot(2), dmx.getSlot(3), 0, 200, 200, 1, 0, -0.7, PI/6, 20);
       if ( testSlots(4) )
         spotLight(dmx.getSlot(4), dmx.getSlot(5), dmx.getSlot(6), 400, 200, 200, -1, 0, -0.7, PI/6, 20);
       if ( testSlots(7) )
         spotLight(dmx.getSlot(7), dmx.getSlot(8), dmx.getSlot(9), 200, 0, 200, 0, 1, -0.7, PI/6, 20);
       if ( testSlots(10) )
         spotLight(dmx.getSlot(10), dmx.getSlot(11), dmx.getSlot(12), 200, 400, 200, 0, -1, -0.7, PI/6, 20);
       translate(200, 200, 0);
       sphere(100);
    }
  }
}

boolean testSlots(int i) {
  if ( dmx.getSlot(i) > 0 ) return true;
  if ( dmx.getSlot(i+1) > 0 ) return true;
  if ( dmx.getSlot(i+2) > 0 ) return true;
  return false;
}

void dispose(){
  if ( dmx != null) {
    dmx.close();
    System.out.print("closed socket, ");
  }
  System.out.println("bye bye!");
}