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
 * LXPTextGroup is a controller for a group of LXPTextFields
 * only a single field can have focus and receive keyPressed messages
*/

public class LXPTextGroup {
  
  /**
   * array containing the fields in this group
   */
  public LXPTextField[] fields;
  /**
   * the number of fields in this group
   */
  public int fieldCount = 0;
  /**
   * the index of the selected field or -1 if no selection
   */
  public int selected = -1;
  
  /**
   * construct an array to hold fields
   * fields must be added with calls to addField
   * @param size the maximum number of fields in this group
   */
  public LXPTextGroup(int size) {
    fields = new LXPTextField[size];
  }
  
  /**
   * draw the fields in this group
   * @param p the Processing Applet containing the button group
   */
  public void draw(PApplet p) {
    for (int i=0; i< fieldCount; i++) {
      fields[i].draw(p);
    }
  }
  
  /**
   * pass a keyPressed message to the text field that is selected to have focus
   * @param p the Processing Applet containing the button group
   */
  public void keyPressed(PApplet p) {
    if ( selected >= 0 ) {
      fields[selected].keyPressed(p);
    }
  }
  
  /**
  * check to see if a field was clicked and if so, select it to have focus
  * @return true if station was changed
  */
  public boolean mousePressed() {
    int oldSelected = selected;
    for (int i=0; i< fieldCount; i++) {
      fields[i].mousePressed();
    }
    return oldSelected != selected;
  }
  
  /**
   * add a field to the group
   * @param xp x coordinate of the field
   * @param yp y coordinate of the field
   * @param s size of field in characters
   * @param t title can be null
   * @return the new LXPTextField
   */
  public LXPTextField addField(int xp, int yp, int s, String t) {
    if ( fieldCount < fields.length ) {
      fieldCount++;
      fields[fieldCount-1] = new LXPTextField(xp,yp,s,this);
      if ( t != null ) {
    	  fields[fieldCount-1].title = t;
      }
      return fields[fieldCount-1];
    }
    return null;
  }
  
  /**
   * @return the focus field or null
   */
  public LXPTextField focusField() {
    if ( selected >= 0 ) {
      return fields[selected];
    }
    return null;
  }
  
  /**
   * get a particular text field 
   * @param index of the field in the array, should be < fieldCount
   * @return text field at index
   */
  public LXPTextField fieldAtIndex(int index) {
	    return fields[index];
  }
  
  /**
   * sets the station with key focus
   * -1 sets no field in focus
   * fieldCount 
   * @param index zero based
   */
  public void setFocusIndex(int index) {
    if ( index < 0 ) {
      setFocusField(null);
    } else if ( index < fieldCount ) {
      setFocusField(fields[index]);
    } else {
      setFocusField(fields[0]);
    }
  }
  
  /**
   * sets the field that has key focus
   * @param ftf the field with key focus
   */
  public void setFocusField(LXPTextField ftf) {
    selected = -1;
    for (int i=0; i<fieldCount; i++) {
      if ( fields[i] == ftf ) {
        fields[i].focus = true;
        selected = i;
      } else {
        fields[i].focus = false;
      }
    }
  }
  /**
   * called when a field receives a tab key press
   */
  public void tabInField() {
		setFocusIndex(this.selected+1);
  }
  /**
   * called when a field receives a return/enter key press
   */  
  public void returnInField() {
  		setFocusIndex(-1);
  }
  
}  // class LXPTextGroup