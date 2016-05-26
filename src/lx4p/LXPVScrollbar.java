/**
 * Copyright (c) 2015-2016 by Claude Heintz Design
 *
 * This file is part of a library called LXforProcessing - https://github.com/claudeheintz/LXforProcessing
 * 
 * LXforProcessing is free software: you can redistribute it and/or modify
 * it under the terms of a BSD style license that should have been included with this file.
 * If not, see https://www.claudeheintzdesign.com/lx/opensource.html.
 * 
*/

package lx4p;

import processing.core.*;
import java.net.*;

/**
 * LXPVScrollbar is a vertical slider type control
 * 
 * A LXPVScrollbar should receive both an update and draw message inside a PApplet's draw method.
 * 
 * A LXPVScrollbar can hold an OSC address pattern and send and be sent messages reflecting its value.
 * 
*/

public class LXPVScrollbar {
  int swidth, sheight;    			// width and height of bar
  float xpos, ypos;       			// x and y position of bar
  float spos, newspos;    			// x position of slider
  float sposMin, sposMax; 			// max and min values of slider
  float loose;              			// how loose/heavy
  boolean over;           			// is the mouse over the slider?
  boolean locked;		  			// locked on tracking the mouse
  float ratio;			  			// aspect
  public String oscAddress = null;	// string to match OSC address pattern
  
  public static int INDICATOR_RED = 0;
  public static int INDICATOR_GREEN = 1;
  public static int INDICATOR_BLUE = 2;
  
  /**
  * Create a vertical scrollbar
  * @param xp the x position of the scrollbar (upper left corner)
  * @param yp the y position of the scrollbar (upper left corner)
  * @param sw the width of the scrollbar
  * @param sh the height of the scrollbar
  * @param l how loose the scrollbar is (how quickly it tracks mouse)
  */
  public LXPVScrollbar (float xp, float yp, int sw, int sh, int l) {
    swidth = sw;
    sheight = sh;
    int heighttowidth = sh - sw;
    ratio = (float)sh / (float)heighttowidth;
    xpos = xp-swidth/2;
    ypos = yp;
    sposMin = ypos;							// top
    sposMax = ypos + sheight - swidth;  // åbottom
    spos = sposMax;
    newspos = spos;
    loose = l;
  }

  /**
   * Updates the location of the scroll indicator
   * This is called in the Processing Applet's draw() method so that the mouse can be continually tracked
   * @param p the Processing Applet
   * @return true if updated
   */
  public boolean update(PApplet p) {
	// determine if the mouse is over the control
    if ( p.mouseX > xpos && p.mouseX < xpos+swidth &&
       	 p.mouseY > ypos && p.mouseY < ypos+sheight) {
    	over = true;
    } else {
    	over = false;
    }
    // if the mouse is over the control and the button is pressed, lock on and track the mouse
    if ( p.mousePressed && over) {
      locked = true;
    }
    // stop tracking if the mouse is released
    if (! p.mousePressed) {
      locked = false;
    }
    // if tracking the mouse, compute the new position of the sliding indicator
    if ( locked ) {
      newspos = constrain(p.mouseY-swidth/2, sposMin, sposMax);
    }
    // move the sliding indicator towards the desired location
    boolean updated = false;
    if (Math.abs(newspos - spos) > 0) {
      spos = spos + (newspos-spos)/loose;
      updated = locked;
    }
    return updated;
  }
  
  /**
   * draw the control and its position indicator
   * @param p the Processing Applet in which to draw the control
   */
  public void draw(PApplet p) {
	    p.noStroke();
	    p.fill(204);
	    p.rect(xpos, ypos, swidth, sheight);
	    if (over || locked) {
	    	p.fill(0, 0, 0);
	    } else {
	    	p.fill(102, 102, 102);
	    }
	    p.rect(xpos, spos, swidth, swidth);
	  }
  
  /**
   * draw the control, its position indicator and an output indicator
   * @param p the Processing Applet in which to draw the control
   * @param level the output indicator level
   * @param indicator_color selects the color red or green or blue of the output indicator
   */
  public void draw(PApplet p, float level, int indicator_color) {
	  draw(p);
	  if ( indicator_color == INDICATOR_GREEN ) {
		  p.fill(0, level*255, 0);
	  } else if ( indicator_color == INDICATOR_BLUE ) {
		  p.fill(0, 0, level*255);
	  } else {
		  p.fill(level*255, 0, 0);
	  }
	  p.ellipse (Math.round(xpos+((swidth/2)-2.5)), ypos-3, 5, 5);
  }
  
  /**
   * getValue represents the location of the control's sliding indicator
   * @return the slider location as an integer in the range 0-255
   */
  public int getValue() {
    return Math.round(PApplet.map(spos, sposMin, sposMax, 255, 0));
  }
  
  /**
   * getFloatValue represents the location of the control's sliding indicator
   * @return the slider location as an integer in the range 0-1.0
   */
  public float getFloatValue() {
    return PApplet.map(spos, sposMin, sposMax, 1, 0);
  }
  
  /**
   * setValue adjusts the location of the control's sliding indicator
   * @param v the new location of the control's indicator
   * @param max the value corresponding to the full range of the slider
   * @param animate will either snap the slider to its new value of move it there through iterations of the update()/draw() loop
   */
  
  public void setValue(int v, int max, boolean animate) {
	  newspos = PApplet.map(v, max, 0 , sposMin, sposMax);
	  if ( ! animate ) {
		  spos = newspos;
	  }
  }
  
  /**
   * setValue adjusts the location of the control's sliding indicator
   * @param v the new location of the control's indicator in the range 0-255
   * @param animate will either snap the slider to its new value of move it there through iterations of the update()/draw() loop
   */
  
  public void setValue(int v, boolean animate) {
	  setValue(v, 255, animate);
  }
  
  /**
   * setValue adjusts the location of the control's sliding indicator
   * @param v the new location of the control's indicator in the range 0-255
   */
  
  public void setValue(int v) {
	  setValue(v, 255, true);
  }
  
  /**
   * setValue adjusts the location of the control's sliding indicator
   * @param v the new location of the control's indicator
   * @param max the value corresponding to the full range of the slider
   * @param animate will either snap the slider to its new value of move it there through iterations of the update()/draw() loop
   */
  
  public void setValue(float v, float max, boolean animate) {
	  float cv = (v/max) * (sposMax-sposMin);
	  newspos = Math.round(sposMax - cv);
	  if ( ! animate ) {
		  spos = newspos;
	  }
  }
  
  /**
   * setValue adjusts the location of the control's sliding indicator
   * @param v the new location of the control's indicator in the range 0-255
   */
  
  public void setValue(float v) {
	  setValue(v, 1.0f, true);
  }

  /**
   * utility function for keeping a value within a range
   * @param val	the input
   * @param minv the minimum
   * @param maxv the maximum
   * @return the constrained value where minv =< out <= maxv
   */
  public float constrain(float val, float minv, float maxv) {
    return Math.min(Math.max(val, minv), maxv);
  }

  /**
   * utility function for converting spos to be values between 0 and the total width of the scrollbar
   * @return float representing position
   */
  public float getPos() {
    return spos * ratio;
  }
  
  /**
   * Sends the value of the scrollbar using its oscAddress
   * @param osc the osc interface used to send the message
   * @param osc_target_address	the target ip address for the message
   * @param osc_target_port  the target port for the message
   */
  public void sendValueWithOSC(LXOSC osc, InetAddress osc_target_address, int osc_target_port) {
	  if ( oscAddress != null ) {
		  LXOSCMessage msg = new LXOSCMessage(oscAddress);
		  msg.addArgument(getFloatValue());
		  osc.sendOSC(msg,osc_target_address, osc_target_port);
	  }
  }
  
  /**
   * checks to see if msg address pattern matches the scrollbar's
   * and if so, sets scrollbar's value based on msg's float argument (0-1.0)
   * @param msg the incoming OSC message object
   * @return true if value was set (allows synchronization to prevent feedback loop)
   */
  
  public boolean setValueWithOSCMessage(LXOSCMessage msg) {
	  if ( oscAddress != null ) {
		  if ( msg.matchesAddressPattern(oscAddress) ) {
			  setValue(msg.floatAt(0), 1.0f, false);		// do not animate to prevent feedback!
			  return true;
		  }
	  }
	  return false;
  }
  
} //class LXPVScrollbar