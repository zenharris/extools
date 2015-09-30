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
import java.sql.PreparedStatement;
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
import static org.househarris.extools.texaco.*;

//import static org.househarris.extools.wdbm.FormDisplay;
//import static org.househarris.extools.wdbm.FormEditor;
//import static org.househarris.extools.wdbm.unpackCurrentRecord;


/**
 *
 * @author harris
 */
public class indexscroll {
    public static ResultSet Results;
    static int ScreenCurrentRow;
    static int ResultsCurrentRow;
    static String ScrollPrompt = "[Enter]Select          [S]QL Query                     [ARROWS]ScrollUP/DN             [Home]Exit";
    static wdbm AttachedWDBM;
    static Statement stmt;
    static String CurrentSQLQuery;
    static PreparedStatement CurrentCompiledSQLStatement;


    public indexscroll(String SQLQuery,wdbm WDBMAttach) throws SQLException {
        stmt = SQLconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        Results = stmt.executeQuery(SQLQuery);
        ScreenCurrentRow = 0;
        ResultsCurrentRow = Results.getRow();
        AttachedWDBM = WDBMAttach;
        CurrentSQLQuery = SQLQuery;
        
        
    }
    
    static List<String> FieldNames2ValuesSubstitute(List<String> FieldNameList) throws SQLException {
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
            AttachedWDBM.writer.drawString(0, iter, String.format(AttachedWDBM.ScrollingListFormat,
                    FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()));
        }
        Results.absolute(SaveResultRow);
    }

    public static void IlluminateCurrentRow() throws SQLException {
        AttachedWDBM.scrn.putString(0, ScreenCurrentRow,
                String.format(AttachedWDBM.ScrollingListFormat, FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()),
                Terminal.Color.BLACK, Terminal.Color.WHITE);
    }
    
    
    public static void ExecuteSQLQuery(String SQLQuery) throws SQLException,InterruptedException {
        ResultSet LocalResults;
            LocalResults = stmt.executeQuery(SQLQuery);
            if (LocalResults.first()) {
                Results = LocalResults;
                ScreenCurrentRow = 0;
                ResultsCurrentRow = Results.getRow();
                ReDrawList();
            }
    }
    
    public static Key DisplayList() throws SQLException,InterruptedException {
        Key KeyReturn;
        String LocalString;
        ResultSet LocalResults ;
        TerminalSize Tsize = AttachedWDBM.terminal.getTerminalSize();
        ReDrawList();
        while (true) {
            Tsize = AttachedWDBM.terminal.getTerminalSize();
            LocalString = String.format(AttachedWDBM.ScrollingListFormat, FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray());
            ResultsCurrentRow = Results.getRow();
            IlluminateCurrentRow();
            AttachedWDBM.scrn.refresh();
            if ((KeyReturn = AttachedWDBM.KeyInput(ScrollPrompt)).getKind() == Key.Kind.Home) {
                return KeyReturn;
            } else if (KeyReturn.getKind() == Key.Kind.ArrowDown && !Results.isLast()) {
                if (ScreenCurrentRow + 4 < Tsize.getRows()) {
                    AttachedWDBM.scrn.putString(0, ScreenCurrentRow++, LocalString, Terminal.Color.WHITE, Terminal.Color.BLACK);
                    Results.next();
                } else {
                    Results.next();
                    ReDrawList();
                    // scrollListUp(ScreenCurrentRow, Wdbm);
                }
            } else if (KeyReturn.getKind() == Key.Kind.ArrowUp && !Results.isFirst()) {
                if (ScreenCurrentRow > 0) {
                    AttachedWDBM.scrn.putString(0, ScreenCurrentRow--, LocalString, Terminal.Color.WHITE, Terminal.Color.BLACK);
                    Results.previous();
                } else {
                    Results.previous();
                    ReDrawList();
                    // scrollListDown(Wdbm);
                }
            } else if (KeyReturn.getKind() == Key.Kind.NormalKey) {
                if (KeyReturn.getCharacter() == 's') {
                    try {
                        LocalString = wdbm.PromptForString("SQL Statement");
                        CurrentCompiledSQLStatement = SQLconnection.prepareStatement(LocalString, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                        LocalResults = CurrentCompiledSQLStatement.executeQuery();
                        if (LocalResults.first()) {
                            Results = LocalResults;
                            ScreenCurrentRow = 0;
                            ResultsCurrentRow = Results.getRow();
                            ReDrawList();
                            CurrentSQLQuery = LocalString;
                        }
                    } catch (SQLException ex) {
                        wdbm.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + " Zen");
                    }
                }
            } else if (KeyReturn.getKind() == Key.Kind.Enter) {
                return KeyReturn;
            }
        }
    }
}
