/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.househarris.extools;

/**
 *
 * @author harris
 */



public interface extools {

    static final String BLANK = "                                                                                                                                       ";

    // The following regular expressions find the field templates embeded in the FormTemplate list
    // Field Templates Look Like @1<<<<<<<<<<     @20<<<<<
    // The Numbers being the link to the name of the field from the SQL database in the FieldList list
    // These having been loaded in from the WDBM common data dictionary file 
    // set up for the SQL database being edited.
    public static final String REGEXToMatchEmbededFieldTemplate = "@\\d+<+";
    public static final String REGEXToMatchNumberEmbededInFieldTemplate = "\\d+";

 //   public extools(){
 //   }
 //   public void KeyInput(String... test);
  //  public wdbm wdbm(String test);
}
