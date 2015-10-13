/*
 *
 *              WDBM/extools Database Development Project
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


/**
 *
 * @author harris
 */
public class indexscroll implements extools {
    public int ListScrollsIndex = 0;
    public String IndexScrollName;
    public boolean ConnectedForm = true;
    
    public ResultSet Results;
    public int ScreenCurrentRow;
    public int ResultsCurrentRow;
    public int ListScreenTopLine = 0;
    public int ListScreenLength = 0;
    private final String ScrollPrompt = "[Enter]Select          [S]QL Query       [ARROWS]ScrollUP/DN            [Home]Exit";
    public wdbm AttachedWDBM;
    public Statement stmt;
    public String CurrentSQLQuery;
    public PreparedStatement CurrentCompiledSQLStatement;
    public List<String> SQLQueryHistory = new ArrayList();


    public indexscroll(String ScrollName ,String SQLQuery,wdbm WDBMAttach,int... Dimensions) throws SQLException {
        System.err.println("Making New Index Scroll :" + ScrollName);
        if(Dimensions.length > 0) ListScreenTopLine = Dimensions[0];
        if(Dimensions.length > 1) ListScreenLength = Dimensions[1];
        
        stmt = WDBMAttach.SQLconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        Results = stmt.executeQuery(SQLQuery);
        if (!Results.first()) System.err.println("This returned nothing " + SQLQuery);     //      Empirical kudge ????
        ScreenCurrentRow = 0;
        ResultsCurrentRow = Results.getRow();
        AttachedWDBM = WDBMAttach;
        CurrentSQLQuery = SQLQuery;
        IndexScrollName = ScrollName;
        ListScrollsIndex = AttachedWDBM.IndexScrolls.size();
        System.err.println(IndexScrollName + " " + ListScrollsIndex + " " +  CurrentSQLQuery );
    }
    
    private List<String> FieldNames2ValuesSubstitute(List<String> FieldNameList) throws SQLException {
        List<String> Substitute = new ArrayList();
        Substitute.clear();
        for (String FieldName : FieldNameList) {
            Substitute.add(Results.getString(FieldName));
        }
        return Substitute;
    }

    private String ResolveSQLStatements (String Cursor) throws SQLException{
        String[] FieldElements = Cursor.split(SplittingColon);
        String Replacement = Results.getString(FieldElements[3]);
        return String.format(FieldElements[2],Replacement);
    }
           
            
    public void ReDrawScroll() throws SQLException {
        TerminalSize Tsize = AttachedWDBM.rawTerminal.getTerminalSize();
        int SaveResultRow = Results.getRow();
        if (SaveResultRow < 1) AttachedWDBM.DisplayError("Something  Wrong  ReDrawScroll getRow" + SaveResultRow);   /////Diagnostic
        int startrow = SaveResultRow - ScreenCurrentRow;
        if (startrow < 1) {
            startrow = 1;
            ScreenCurrentRow = SaveResultRow -1;
        }
        int iter;
        for (iter = 0; (ListScreenLength==0 || iter < ListScreenLength)  && ListScreenTopLine+iter + 4 <= Tsize.getRows() && Results.absolute(startrow + iter); iter++) {
            AttachedWDBM.screenWriter.drawString(0, ListScreenTopLine+iter, String.format(AttachedWDBM.ScrollingListFormat,
                    FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()));
        }
        iter--;
        while (iter++ < ListScreenLength-1) AttachedWDBM.screenWriter.drawString(0, ListScreenTopLine+iter, BLANK);
        // if (SaveResultRow < 1) SaveResultRow = 1;//    MYSTERY EMPIRICAL FIX
        Results.absolute(SaveResultRow);
    }

    public void IlluminateCurrentRow() throws SQLException {
        AttachedWDBM.screenHandle.putString(0, ListScreenTopLine + ScreenCurrentRow,
                String.format(AttachedWDBM.ScrollingListFormat, FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()),
                Terminal.Color.BLACK, Terminal.Color.WHITE);
        AttachedWDBM.screenHandle.refresh();
    }
    
    /**
     *
     * @throws SQLException
     */
    public void DeEmphasiseCurrentRow() throws SQLException {
        AttachedWDBM.screenHandle.putString(0, ListScreenTopLine + ScreenCurrentRow,
                String.format(AttachedWDBM.ScrollingListFormat, FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()),
                Terminal.Color.WHITE, Terminal.Color.BLACK);
    }
    
    /**
     *
     * @param NewSQLQuery
     * @throws SQLException
     */
    public void ReSearch(String NewSQLQuery) throws SQLException {
        ResultSet LocalResults;
        CurrentCompiledSQLStatement = AttachedWDBM.SQLconnection.prepareStatement(NewSQLQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        LocalResults = CurrentCompiledSQLStatement.executeQuery();
        if (LocalResults.first()) {
            Results = LocalResults;
            ScreenCurrentRow = 0;
            ResultsCurrentRow = Results.getRow();
            // AttachedWDBM.scrn.clear();
            ReDrawScroll();
            // SQLQueryHistory.add(CurrentSQLQuery);
            CurrentSQLQuery = NewSQLQuery;
        }
    }
    /**
     *
     * @param SQLQuery
     * @throws SQLException
     */
    public void ExecuteSQLQuery(String SQLQuery) throws SQLException {
        ResultSet LocalResults;
        try {
            AttachedWDBM.TextEditor.LineEditorBuffer = CurrentSQLQuery;
            AttachedWDBM.TextEditor.LineEditorPosition = CurrentSQLQuery.length();
            String LocalString = AttachedWDBM.PromptForString("->");
            if (AttachedWDBM.TextEditor.LineEditorReturnKey.getKind() != Key.Kind.Escape) {
                CurrentCompiledSQLStatement = AttachedWDBM.SQLconnection.prepareStatement(LocalString, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                LocalResults = CurrentCompiledSQLStatement.executeQuery();
                if (LocalResults.first()) {
                    Results = LocalResults;
                    ScreenCurrentRow = 0;
                    ResultsCurrentRow = Results.getRow();
                    // AttachedWDBM.scrn.clear();
                    ReDrawScroll();
                    SQLQueryHistory.add(CurrentSQLQuery);
                    CurrentSQLQuery = LocalString;
                    AttachedWDBM.DisplayError("");
                }
            }
        } catch (SQLException ex) {
            AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() +" SQLState "+ex.getSQLState());
            ex.printStackTrace();
        }
    }
  
 
    public Key ActivateScroll() throws SQLException {
        Key KeyReturn;
        TerminalSize Tsize;
        ReDrawScroll();
        AttachedWDBM.screenHandle.refresh();
        
        
        while (true) {
            Tsize = AttachedWDBM.rawTerminal.getTerminalSize();
            ResultsCurrentRow = Results.getRow();
            IlluminateCurrentRow();
            if(ConnectedForm) AttachedWDBM.FormDisplay(Results);
            AttachedWDBM.screenHandle.refresh();
            if ((KeyReturn = AttachedWDBM.KeyInput(ScrollPrompt)).getKind() == Key.Kind.Home) {
                return KeyReturn;
            } else if (KeyReturn.getKind() == Key.Kind.ArrowDown && !Results.isLast()) {
                if ((ListScreenLength == 0 || ScreenCurrentRow + 1 < ListScreenLength) && ListScreenTopLine + ScreenCurrentRow + 4 < Tsize.getRows()) {
                    DeEmphasiseCurrentRow();
                    ScreenCurrentRow++;
                    Results.next();
                } else {
                    Results.next();
                    ReDrawScroll();
                }
            } else if (KeyReturn.getKind() == Key.Kind.ArrowUp && !Results.isFirst()) {
                if (ScreenCurrentRow > 0) {
                    DeEmphasiseCurrentRow();
                    ScreenCurrentRow--;
                    Results.previous();
                } else {
                    Results.previous();
                    ReDrawScroll();
                }
            } else if (KeyReturn.getKind() == Key.Kind.NormalKey) {
                if (KeyReturn.getCharacter() == 's') {
                    ExecuteSQLQuery(CurrentSQLQuery);
                } else return KeyReturn;
            } else if (KeyReturn.getKind() == Key.Kind.Enter || KeyReturn.getKind() == Key.Kind.Escape) {
                DeEmphasiseCurrentRow();
                return KeyReturn;
            }
        }
    }
}
