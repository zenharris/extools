/*
 *    EXAMPLE MAIN LINE FIREING UP AND USING WDBM/extools TO OPEN AND EDIT
 *    an sql database under postgreSQL using the table "resie".
 */
package forms;

//import com.googlecode.lanterna.LanternaException;
import org.househarris.extools.wdbm;
// import org.househarris.extools.wdbm.scrn;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.TerminalFacade;



public class Forms {

    /**
     * @param args the command line arguments
     */
    
    
   
    
    
    public static void main(String[] args) {
      try {
          wdbm resieFile = new wdbm("prim.dict");
          resieFile.ScrollingIndexAndEditLoop();
          resieFile.scrn.stopScreen();
      } catch (Exception e) {
          System.err.println(e.getClass().getName() + ": " + e.getMessage() + "Zen");
          //    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            // System.exit(0);
               // wdbm.close();
      } finally {
      //   resieFile.scrn.stopScreen();  
      }
    }
}
