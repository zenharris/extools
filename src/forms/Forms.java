/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package forms;






import org.househarris.extools.extools;
/**
 
 * @author harris
 */
public class Forms {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
      extools resieFile = new extools("resie.dict");
       
        resieFile.listdriver("SELECT * FROM resie order by lot_number;");

        // 
        // resieFile.FormEdit();
       
        //
        // resieFile.scrn.refresh();
        
        resieFile.scrn.stopScreen();   
        
        
        
        // TODO code application logic here
    }
    
}
