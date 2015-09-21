/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.househarris.extools;

import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalSize;
import java.sql.SQLException;
import static org.househarris.extools.wdbm.CurrentRecordResultSet;
import static org.househarris.extools.wdbm.ScrollingListFormat;
import static org.househarris.extools.wdbm.ScrollingListFields;
import static org.househarris.extools.wdbm.ValuesSubstitute;
import static org.househarris.extools.wdbm.terminal;
import static org.househarris.extools.wdbm.writer;
import static org.househarris.extools.wdbm.scrn;
import static org.househarris.extools.wdbm.KeyInput;
/**
 *
 * @author harris
 */
public class ScrollingIndex {
    
    public void ScrollingIndex () {
        
        
    }
    
       public static Key DisplayList() throws SQLException, InterruptedException {
        int iter;
        Key KeyReturn;
        String LocalString;
        TerminalSize Tsize = terminal.getTerminalSize();
        for (iter = 0; iter+3 < Tsize.getRows() && CurrentRecordResultSet.absolute(iter+1); iter++) {
            writer.drawString(0, iter, String.format(ScrollingListFormat, ValuesSubstitute(ScrollingListFields).toArray()));
        }
        CurrentRecordResultSet.first();
        iter = 0;
        while (true) {
            Tsize = terminal.getTerminalSize();
            scrn.refresh();
            LocalString = String.format(ScrollingListFormat, ValuesSubstitute(ScrollingListFields).toArray());
            scrn.putString(0, iter, LocalString, Terminal.Color.BLACK, Terminal.Color.WHITE);
            scrn.refresh();
            if ((KeyReturn = KeyInput("[Enter]Select                      [ARROWS]ScrollUP/DN             [Home]Exit")).getKind() == Key.Kind.Home) {
                return KeyReturn;
            } else if (KeyReturn.getKind() == Key.Kind.ArrowDown && !CurrentRecordResultSet.isLast()) {
                if (iter+3 < Tsize.getRows()-1) {
                scrn.putString(0, iter, LocalString, Terminal.Color.WHITE, Terminal.Color.BLACK);
                iter++;
                CurrentRecordResultSet.next();
                }
            } else if (KeyReturn.getKind() == Key.Kind.ArrowUp && !CurrentRecordResultSet.isFirst()) {
                scrn.putString(0, iter, LocalString, Terminal.Color.WHITE, Terminal.Color.BLACK);
                iter--;
                CurrentRecordResultSet.previous();
            } else if (KeyReturn.getKind() == Key.Kind.Enter) return KeyReturn;
        }
    }
}
