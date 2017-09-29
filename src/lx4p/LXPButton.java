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

/**
 * LXPButton
 * it highlights when it is moused over
 * when clicked, it calls its mouseClicked() method
 * to use LXPButton, create a subclass and override mouseClicked()
*/

public class LXPButton {
  int _x;
  int _y;
  int _w;
  int _h;
  public boolean over;
  public boolean outline = true;
  public String title=null;
 

  /**
   * constructs a LXPButton
   * @param xp x location in Processing Applet window
   * @param yp y location in Processing Applet window
   * @param w  width
   * @param h  height
   */
  public LXPButton(int xp, int yp, int w, int h) {
    _x = xp;
    _y = yp;
    _w = w;
    _h = h;
  }
  
  /**
   * constructs a LXPButton
   * @param xp x location in Processing Applet window
   * @param yp y location in Processing Applet window
   * @param w  width
   * @param h  height
   */
  public LXPButton(int xp, int yp, int w, int h, String t) {
    _x = xp;
    _y = yp;
    _w = w;
    _h = h;
    title = t;
  }
  
  /**
   * draws the button, showing a change when the mouse is over the button
   * @param p the Processing Applet containing the button
   */
  public void draw(PApplet p) {
    over = overRect(_x, _y, _w, _h, p);

    p.strokeWeight(1);
    if ( outline ) {
    	p.stroke(0);
    } else {
    	p.noStroke();
    }
    if ( over ) {
      if ( p.mousePressed ) {
    	  p.fill(64);
      } else {
    	  p.fill(192);
      }
    } else {
      p.fill(255);
    }
    p.rect (_x, _y, _w, _h);
    
    p.stroke(0);
    p.fill(0);
    if ( title != null ) {
      p.textAlign(PApplet.CENTER);
      p.text(title, _x+_w/2, _y+_h/2+5);
    }
  }
  
  /**
   * called from the applet's mouseReleased method
   */
  public void mouseReleased() {
    if ( over ) {
      mouseClicked();
    }
  }
  
  /**
   * create a subclass and override this to have the button do something:
   * 
 public class LXPMyButton extends LXPButton {
    public LXPMyButton(int xp, int yp, int w, int h, String t) {
      super(xp,yp,w,h,t);
    }
    
    public void mouseClicked() {
      //do something here
    }
 }
   */  
  public void mouseClicked() {
  
  }
  
    /**
   * utility function for determining if the mouse is within a rectangle
   * @param x coordinate
   * @param y coordinate
   * @param w width
   * @param h height
   * @param p the Processing Applet window tracking the mouse
   * @return true if the mouse is inside the rectangle
   */
  public static boolean overRect(int x, int y, int w, int h, PApplet p) {
    if ( ( x <= p.mouseX ) && ( p.mouseX <= (x + w) ) ) {
      if ( ( y <= p.mouseY ) && ( p.mouseY <= (y + h) ) ) {
        return true;
      }
    }
    return false;
  }
  
}//class LXPButton