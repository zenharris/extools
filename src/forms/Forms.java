/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package forms;

import com.googlecode.lanterna.input.Key;
import org.househarris.extools.ScrollingIndex;
import org.househarris.extools.wdbm;
import static org.househarris.extools.wdbm.scrn;



public class Forms {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
      try {
          wdbm resieFile = new wdbm("resie.dict");
          // resieFile.listdriver("SELECT * FROM resie order by lot_number;");
          ScrollingIndex ResieList = new ScrollingIndex("SELECT * FROM resie order by lot_number;");
          // ResieList.DisplayList();
          if (ResieList.Results.first()) while (ResieList.DisplayList().getKind() != Key.Kind.Home && resieFile.DisplayAndEditRecord(ResieList.Results).getKind() != Key.Kind.Home) scrn.clear();
          resieFile.scrn.stopScreen(); 
          
      } catch (Exception e) {
          System.err.println(e.getClass().getName() + ": " + e.getMessage() + "Zen");
          //    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            // System.exit(0);
      }
       

        
        // TODO code application logic here
    }
    
    
}
