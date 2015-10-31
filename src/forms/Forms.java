/*
 *    EXAMPLE MAIN LINE FIREING UP AND USING WDBM/extools TO OPEN AND EDIT
 *    an sql database under postgreSQL using the table "resie".
 */
package forms;

import org.househarris.extools.wdbm;



public class Forms {

    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
      try {
          
          wdbm primaryFile = new wdbm("prim.dict");
        // wdbm resieFile = new wdbm("resie.dict");
          primaryFile.ActivateWDBM();
       //   resieFile.ActivateWDBM();
          
      } catch (Exception e) {
          e.printStackTrace();
          System.err.println(e.getClass().getName() + ": " + e.getMessage() + "Zen");
          //    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
}
