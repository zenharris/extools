/*
 *    EXAMPLE MAIN LINE FIREING UP AND USING WDBM/extools TO OPEN AND EDIT
 *    an sql database under postgreSQL in the table "resie".
 */
package forms;

import com.googlecode.lanterna.input.Key;
import org.househarris.extools.indexscroll;
import org.househarris.extools.wdbm;
import static org.househarris.extools.wdbm.scrn;



public class Forms {

    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
      try {
          wdbm resieFile = new wdbm("resie.dict");
          resieFile.CreateIndexScroll("SELECT * FROM resie order by lot_number;",resieFile);
          resieFile.ScrollingIndexAndEditLoop();
          resieFile.scrn.stopScreen(); 
      } catch (Exception e) {
          System.err.println(e.getClass().getName() + ": " + e.getMessage() + "Zen");
          //    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            // System.exit(0);
      }
    }
    
    
}
