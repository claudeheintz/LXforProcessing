/**
 * Copyright (c) 2015-2017 by Claude Heintz Design
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
*/

import java.net.*;
import java.util.*;
import lx4p.*;
import processing.serial.*;

//*********************************  Control Variables  *********************************

// If you specify a network interface name, binding address can be set automatically
// Note on your computer, your desired NIC may be different than this
String interfaceToFind = "en0";    

// ***** DMX output settings
// EDIT these settings if you don't want to search for a specific node to send DMX output
String desiredArtNetNodeName = "my Art-Net node";
boolean searchForDesiredNode = true;
boolean broadcastArtDMXEnabled = false;

// DMX interface object
LXDMXInterface dmx;


// ***** Auto-fade
boolean fading = false;
// fade direction (masters up/down, fade to bank x/y)
boolean fading_to_x = true;
// Auto-fade timing
double time_fade_started;
double duration = 3;

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
        title = "Stop";
      }
    }
 }
LXPSetupModeButton mode_button;

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
  mastery = new LXPVScrollbar(645, 100, 24, 200, 1);
  bars = new LXPVScrollbar[numberOfBars];
  barsy = new LXPVScrollbar[numberOfBars];
  barlevels = new int[numberOfBars];
  barylevels = new int[numberOfBars];
  outlevels = new int[numberOfBars];
  outAddresses = new int[numberOfBars];
  for( int i=0; i<numberOfBars; i++) {
    bars[i] = new LXPVScrollbar(40+i*45, 30, 24, 150, 1);
    barsy[i] = new LXPVScrollbar(40+i*45, 210, 24, 150, 1);
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
  
  setupNetworkSocket();
  
  mode_button = new LXPSetupModeButton(680, 360, 50, 20, "Run");
  go_button = new LXPGoButton(590, 310, 65, 20, "Go");
}

//*********************************  drawing & events  *********************************

void draw() {
  background(255);
  stroke(0);
  mode_button.draw(this);
  
  // The draw loop operates in two modes, setup and run
  // In setup mode, ArtPolls are sent to find the desired target node.
  if ( setup_mode ) {
    if ( dmx != null ) {
      if ( dmx instanceof LXArtNet ) {
        if ( searchForDesiredNode ) {
          checkPollReply();
        }
      }
    }
  } else {
    // In Run Mode, the sliders are drawn and their values are used to set slots in the LXArtNet dmx object.
    // The dmx objects then sends an ArtDMX packet containing its current slots.
    
    noStroke();
    int barlevel;
    int barylevel;
    float barout;
    float baryout;
    int outlevel;
    int oldmasterx = masterx.getValue();
    int oldmastery = mastery.getValue();
    
    go_button.draw(this);
    //update to get current values
    if ( fading ) {
      fading = ! updateFade();
    }
    masterx.update(this);
    mastery.update(this);
    
    int newmasterx = masterx.getValue();
    int newmastery = mastery.getValue();
    
    //convert to 0.0->1.0
    float mLevelx = newmasterx/255.0;
    float mLevely = newmastery/255.0;
    
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
      }
      if ( barylevel != barylevels[i] ) {
        barylevels[i] = barylevel;
      }
      if ( outlevel != outlevels[i] ) {
        outlevels[i] = outlevel;
        if ( dmx != null ) {
          dmx.setSlot(outAddresses[i], outlevel);
        }
      }
    }      // for loop i in bars.length
    
    
    // Here's all it takes to send the accumulated DMXSlot values in an ArtDMX packet.
    if ( dmx != null ) {
      dmx.sendDMX();
    }
  }
}

void mousePressed() {
  
}

void mouseReleased() {
  mode_button.mouseReleased();
  go_button.mouseReleased();
}

void dispose(){
  if ( dmx != null) {
    dmx.close();
    System.out.print("closed dmx");
  }

  System.out.println("bye bye!");
}

//*********************************  Network Methods  *********************************

void setupNetworkSocket() {
  if ( dmx != null ) {
    dmx.close();
    dmx = null;
  }

  // The factory method createDMXEthernet does all the necessary work to make and bind
  // a DatagramSocket and assigns it to a new LXArtNet object that can then be used to
  // send and receive Art-Net.
  // If interfaceToFind matches a NIC, the broadcast address is set using that interface's IP address and subnet
  // The DatagramSocket is bound to "0.0.0.0" or ANY_ADDRESS.  To bind specifically to the NIC, change this and pass null
  // The target address will be found using polling so null is passed as the last parameter
    dmx = LXDMXEthernet.createDMXEthernet(LXDMXEthernet.CREATE_ARTNET, interfaceToFind, "0.0.0.0", null);
    
    ((LXArtNet) dmx).setBroadcastDMXEnabled(broadcastArtDMXEnabled);// broadcast is disabled by default
    
    // create an LXArtNetPollReplyListener to process ArtPoll replies and set the target address
    // when the desired node is found
    ((LXArtNet) dmx).setPollReplyListener(new LXArtNetPollReplyListener() {
      public boolean pollReplyReceived(LXArtNetPollReplyInfo info) {
          System.out.println("Found node:" + info.longNodeName() + " @ " + info.nodeAddress());
          if ( info.longNodeName().equals(desiredArtNetNodeName) ) {
            searchForDesiredNode = false;
            return true; // set to true to automatically use found address
          }
          return false;
        }
    });
    
    checkPollReply();
 
}

/*
 * checkPollReply()
 * If searchingForDesired Node, this uses a socket bound to "any address" to
 * broadcast an Art-Net Poll and receive replies to it.
 * The LXArtNet object has a listerner object that deals with the replies
 * (see setupNetworkSocket).
*/
void checkPollReply() {
  if ( searchForDesiredNode ) {
    if ( dmx instanceof LXArtNet ) {
      try {
        // use a socket bound to Any address:  a socket bound to a specific address will not get broadcast replies
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
  }
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