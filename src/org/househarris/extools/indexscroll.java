/*
 *
 *                WDBM Database Development Project
 *       Rewritten In Java By Zen Harris 2015 househarris@b5.net
 *                  From the original code in Perl
 *     Copyright (C) 1993   Michael Brown mbrown@scorch.hna.com.au
 *
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 1, or (at your option)
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */



package org.househarris.extools;

import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalSize;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import static org.househarris.extools.wdbm.IndexScrolls;
//import static org.househarris.extools.wdbm.CurrentRecordResultSet;
// import static org.househarris.extools.wdbm.ScrollingListFormat;
//import static org.househarris.extools.wdbm.ScrollingListFields;
//import static org.househarris.extools.wdbm.ValuesSubstitute;
//import static org.househarris.extools.wdbm.terminal;
//import static org.househarris.extools.wdbm.writer;
//import static org.househarris.extools.wdbm.scrn;
///import static org.househarris.extools.wdbm.KeyInput;
import static org.househarris.extools.wdbm.SQLconnection;
//import static org.househarris.extools.wdbm.FormDisplay;
//import static org.househarris.extools.wdbm.FormEditor;
//import static org.househarris.extools.wdbm.unpackCurrentRecord;
/**
 *
 * @author harris
 */
public class indexscroll{
    public static ResultSet Results;
    static int ScreenCurrentRow;
    static int ResultsCurrentRow;
    static String ScrollPrompt = "[Enter]Select                               [ARROWS]ScrollUP/DN             [Home]Exit";
    static wdbm AttachedWDBM;


    public indexscroll(String SQLQuery) throws SQLException {
        Statement stmt = SQLconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        Results = stmt.executeQuery(SQLQuery);
        ScreenCurrentRow = 0;
        ResultsCurrentRow = Results.getRow();
    }
    
    static List<String> ValuesSubstitute(List<String> FieldNameList) throws SQLException {
        List<String> Substitute = new ArrayList();
        for(String FieldName : FieldNameList ) Substitute.add(Results.getString(FieldName));
        return Substitute;
    }
    
    static void scrollListUp (int CurrentScreenLine,wdbm Wdbm) throws SQLException {
       //int iter; 
       //int StartRow = Results.getRow() - CurrentScreenLine+1;
       //TerminalSize Tsize = Wdbm.terminal.getTerminalSize();
       //for (iter = 0; iter+3 < Tsize.getRows() && Results.absolute(StartRow++); iter++) {
       //     Wdbm.writer.drawString(0, iter, String.format(Wdbm.ScrollingListFormat, ValuesSubstitute(Wdbm.ScrollingListFields).toArray()));
       // }   
    }
    
    static void scrollListDown(wdbm Wdbm) throws SQLException {
        //int iter;
        //int StartRow = Results.getRow();
        //TerminalSize Tsize = Wdbm.terminal.getTerminalSize();
        //for (iter = 0; iter + 3 < Tsize.getRows() && Results.absolute(StartRow+iter-1); iter++) {
        //    Wdbm.writer.drawString(0, iter, String.format(Wdbm.ScrollingListFormat, ValuesSubstitute(Wdbm.ScrollingListFields).toArray()));
        //}
        //Results.absolute(StartRow-1);
    }
    
    public static void ReDrawList() throws SQLException {
        TerminalSize Tsize = AttachedWDBM.terminal.getTerminalSize();
        int SaveResultRow = Results.getRow();
        int startrow = SaveResultRow - ScreenCurrentRow;
        if (startrow < 1) {
            startrow = 1;
            ScreenCurrentRow = SaveResultRow-1;
        }
        int iter;
        for (iter = 0; iter + 4 <= Tsize.getRows() && Results.absolute(startrow + iter); iter++) {
            AttachedWDBM.writer.drawString(0, iter, String.format(AttachedWDBM.ScrollingListFormat, ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()));
        }
        Results.absolute(SaveResultRow);
    }

    public static Key DisplayList(wdbm Wdbm) throws SQLException, InterruptedException {
        Key KeyReturn;
        String LocalString;
        TerminalSize Tsize = Wdbm.terminal.getTerminalSize();
       
        AttachedWDBM = Wdbm;
        ReDrawList();
        while (true) {
            Tsize = Wdbm.terminal.getTerminalSize();
            Wdbm.scrn.refresh();
            LocalString = String.format(Wdbm.ScrollingListFormat, ValuesSubstitute(Wdbm.ScrollingListFields).toArray());
            ResultsCurrentRow = Results.getRow();
            Wdbm.scrn.putString(0, ScreenCurrentRow, LocalString, Terminal.Color.BLACK, Terminal.Color.WHITE);
            Wdbm.scrn.refresh();
            if ((KeyReturn = Wdbm.KeyInput(ScrollPrompt)).getKind() == Key.Kind.Home) {
                return KeyReturn;
            } else if (KeyReturn.getKind() == Key.Kind.ArrowDown && !Results.isLast()) {
                if (ScreenCurrentRow + 4 < Tsize.getRows()) {
                    Wdbm.scrn.putString(0, ScreenCurrentRow++, LocalString, Terminal.Color.WHITE, Terminal.Color.BLACK);
                    Results.next();
                } else {
                    Results.next();
                    ReDrawList();
                    // scrollListUp(ScreenCurrentRow, Wdbm);
                }
            } else if (KeyReturn.getKind() == Key.Kind.ArrowUp && !Results.isFirst()) {
                if (ScreenCurrentRow > 0) {
                    Wdbm.scrn.putString(0, ScreenCurrentRow--, LocalString, Terminal.Color.WHITE, Terminal.Color.BLACK);
                    Results.previous();
                } else {
                    Results.previous();
                    ReDrawList();
                    // scrollListDown(Wdbm);
                }
            } else if (KeyReturn.getKind() == Key.Kind.Enter) return KeyReturn;
        }
    }
}
