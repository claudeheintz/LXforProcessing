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

package lxprocessing;

import processing.core.*;

/**
 * LXPRadioButton is a member of a LXPRadioGroup
 * it highlights when it is moused over
 * when clicked, it requests the RadioGroup select its station
*/

public class LXPRadioButton {
  int _x;
  int _y;
  int _r;
  boolean _over;
  LXPRadioGroup _group;
  public int fillColor;
  public boolean state = false;
  public String title = null;
  
  /**
   * constructs a LXPRadioButton  (called by LXPRadioGroup addButton)
   * @param xp x location in Processing Applet window
   * @param yp y location in Processing Applet window
   * @param r  radius of button's circle
   * @param g  the LXPRadioGroup to which this button belongs
   */
  public LXPRadioButton(int xp, int yp, int r, LXPRadioGroup g) {
    _x = xp;
    _y = yp;
    _r = r;
    _group = g;
    fillColor = 0xffffff;
  }
  
  /**
   * draws the radio button, showing a change when the mouse is over the button
   * @param p the Processing Applet containing the button
   */
  public void draw(PApplet p) {
    _over = overCircle(_x, _y, _r, p);
    
    if ( state ) {
      p.stroke(0);
    } else {
      p.stroke(128);
    }
    p.fill(fillColor);
    
    p.strokeWeight(1);
    if ( _over ) {
      p.strokeWeight(5);
      p.ellipse (_x, _y, _r+5, _r+5);
    } else {
      p.strokeWeight(3);
      p.ellipse (_x, _y, _r, _r);
    }
    
    if ( title != null ) {
    	p.stroke(255);
    	p.textAlign(PApplet.RIGHT);
    	p.fill(64);
    	p.text(title, _x-(_r+1), _y+5);
    }
  }
  
  /**
   * called from the LXPRadioGroup's mousePressed method
   * sets the station of the group if the mouse was pressed inside this button
   */
  public void mousePressed() {
    if ( _over ) {
      _group.setStation(this);
    }
  }
  
  /**
   * utility function for determining if the mouse is within a circle
   * @param x coordinate of the center of the circle
   * @param y coordinate of the center of the circle
   * @param diameter of the circle
   * @param p the Processing Applet window tracking the mouse
   * @return true if mouse is over circle
   */
  public static boolean overCircle(int x, int y, int diameter, PApplet p) {
    float disX = x - p.mouseX;
    float disY = y - p.mouseY;
    if (PApplet.sqrt(PApplet.sq(disX) + PApplet.sq(disY)) < diameter/2 ) {
      return true;
    } else {
      return false;
    }
  }
}//class LXPRadioButton