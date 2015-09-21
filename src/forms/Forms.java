/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package forms;

import org.househarris.extools.wdbm;



public class Forms {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
      try {
          wdbm resieFile = new wdbm("resie.dict");
          resieFile.listdriver("SELECT * FROM resie order by lot_number;");
          resieFile.scrn.stopScreen(); 
          
      } catch (Exception e) {
          System.err.println(e.getClass().getName() + ": " + e.getMessage() + "Zen");
          //    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            // System.exit(0);
      }
       

        
        // TODO code application logic here
    }
    
    
}
