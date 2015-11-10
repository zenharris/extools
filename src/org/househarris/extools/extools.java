/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.househarris.extools;

import java.sql.SQLException;
import java.util.Stack;
import org.househarris.extools.wdbm.TerminalWindow;

/**
 *
 * @author Zen Harris
 */


public interface extools {

    static final String BLANK = "                                                                                                                                                                                                                                                                                      ";

    // The following regular expressions find the field templates embeded in the FormTemplate list
    // Field Templates Look Like @1<<<<<<<<<<     @20<<<<<
    // The Numbers being the link to the name of the field from the SQL database in the FieldList list
    // These having been loaded in from the WDBM common data dictionary file 
    // set up for the SQL database being edited.
    public static final String REGEXToMatchEmbededFieldTemplate = "@\\d+<+";
    public static final String REGEXToMatchNumberEmbededInFieldTemplate = "\\d+";
    
    
    public static final String SplittingColon = ":";
    public static final String IndexScrollFieldLabel = "listscroll";

    /**
     *
     */
    public static final String FormMenuPrompt = "[ESC]Back  [E]dit                             [N]ext [P]rev      [Home]Exit";
 //   public extools(){
 //   }
 //  public Key KeyInput(String... test);
 // public wdbm wdbm(String test);
    
    /**
     *Stack containing Terminal Window definitions
     */
  //  public Stack<TerminalWindow> WindowStack = new Stack();
    
    //public TerminalWindow TopWindow();
    
    /**
     * Stack containing Terminal Window definitions
     * @param ScrollName
     * @return 
     * @throws java.sql.SQLException 
     */
    public indexscroll WithTheIndexScroll(String ScrollName)throws SQLException;
    
}
