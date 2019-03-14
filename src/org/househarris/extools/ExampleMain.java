/**
 *
 * @author Zen Harris

                        Example Main for simple database with 2 files.
*/


import javax.swing.JFrame;
import org.househarris.extools.wdbm;


public class ExampleMain {

    
    public static void main(String[] args) {
        
        
        
        try {
            wdbm primaryFile = new wdbm("prim.dict");
            wdbm resieFile = new wdbm("resie.dict");
            primaryFile.ActivateWDBM();
            resieFile.ActivateWDBM();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
   
