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
 * LXPRadioGroup is a controller for a group of RadioButtons
 * only a single station (button) can be selected at a time
*/

public class LXPRadioGroup {
  
  /**
   * array containing the buttons in this group
   */
  public LXPRadioButton[] buttons;
  /**
   * the number of buttons in this group
   */
  public int buttonCount = 0;
  /**
   * the index of the selected button or -1 if no selection
   */
  public int selected = -1;
  
  /**
   * construct an array to hold buttons
   * buttons must be added with calls to addButton
   * @param size the maximum number of buttons in this group
   */
  public LXPRadioGroup(int size) {
    buttons = new LXPRadioButton[size];
  }
  
  /**
   * draw the buttons in this group
   * @param p the Processing Applet containing the button group
   */
  public void draw(PApplet p) {
    for (int i=0; i< buttonCount; i++) {
      buttons[i].draw(p);
    }
  }
  
  /**
  * check to see if a button was clicked and if so, set the station
  * @return true if station was changed
  */
  public boolean mousePressed() {
    int oldSelected = selected;
    for (int i=0; i< buttonCount; i++) {
      buttons[i].mousePressed();
    }
    return oldSelected != selected;
  }
  
  /**
   * add a button to the group
   * @param xp x coordinate of the center of the radio button
   * @param yp y coordinate of the center of the radio button
   * @param r radius of the circle representing the radio button
   * @return the new LXPRadioButton
   */
  public LXPRadioButton addButton(int xp, int yp, int r) {
    if ( buttonCount < buttons.length ) {
      buttonCount++;
      buttons[buttonCount-1] = new LXPRadioButton(xp,yp,r,this);
      return buttons[buttonCount-1];
    }
    return null;
  }
  
  /**
   * @return the selected button or null
   */
  public LXPRadioButton selectedButton() {
    if ( selected >= 0 ) {
      return buttons[selected];
    }
    return null;
  }
  
  /**
   * get a particular button 
   * @param index of the button in the array, should be < buttonCount
   * @return LXPRadioButton
   */
  public LXPRadioButton buttonAtIndex(int index) {
	    return buttons[index];
  }
  
  /**
   * sets the station selected by this button group
   * @param index zero based, should be < buttonCount
   */
  public void setSelectedIndex(int index) {
    if ( index < buttonCount ) {
      setStation(buttons[index]);
    }
  }
  
  /**
   * sets the station selected by this button group
   * @param srb the button representing the selected station
   */
  public void setStation(LXPRadioButton srb) {
    for (int i=0; i<buttonCount; i++) {
      if ( buttons[i] == srb ) {
        buttons[i].state = true;
        selected = i;
      } else {
        buttons[i].state = false;
      }
    }
  }
  
}  // class LXPRadioGroup