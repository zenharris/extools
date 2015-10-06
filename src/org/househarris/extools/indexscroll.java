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


/**
 *
 * @author harris
 */
public class indexscroll {
    public int ListScrollsIndex = 0;
    public String IndexScrollName;
    public boolean ConnectedForm = true;
    
    public ResultSet Results;
    public int ScreenCurrentRow;
    public int ResultsCurrentRow;
    public int ListScreenTopLine = 0;
    public int ListScreenLength = 0;
    private final String ScrollPrompt = "[Enter]Select          [S]QL Query       [ARROWS]ScrollUP/DN             [Home]Exit";
    public wdbm AttachedWDBM;
    public Statement stmt;
    public String CurrentSQLQuery;
    public PreparedStatement CurrentCompiledSQLStatement;


    public indexscroll(String ScrollName ,String SQLQuery,wdbm WDBMAttach,int... Dimensions) throws SQLException {
        this.Substitute = new ArrayList();
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
    //    if (ScrollName.equals("default")) AttachedWDBM.CurrentIndexScroll = this;
        System.err.println(IndexScrollName + " " + ListScrollsIndex + " " +  CurrentSQLQuery );
    }
    
    private final List<String> Substitute;
    private List<String> FieldNames2ValuesSubstitute(List<String> FieldNameList) throws SQLException {
       // List<String> Substitute = new ArrayList();
       Substitute.clear();
        
        for(String FieldName : FieldNameList ){
           // System.err.println(FieldName);
           // System.err.println(AttachedWDBM.CurrentIndexScroll.Results.getString(FieldName));
                Substitute.add(Results.getString(FieldName));
        }
        return Substitute;
    }
    
    private void scrollListUp (int CurrentScreenLine,wdbm Wdbm) throws SQLException {
       //int iter; 
       //int StartRow = Results.getRow() - CurrentScreenLine+1;
       //TerminalSize Tsize = Wdbm.terminal.getTerminalSize();
       //for (iter = 0; iter+3 < Tsize.getRows() && Results.absolute(StartRow++); iter++) {
       //     Wdbm.writer.drawString(0, iter, String.format(Wdbm.ScrollingListFormat, ValuesSubstitute(Wdbm.ScrollingListFields).toArray()));
       // }
    }
    
    private void scrollListDown(wdbm Wdbm) throws SQLException {
        //int iter;
        //int StartRow = Results.getRow();
        //TerminalSize Tsize = Wdbm.terminal.getTerminalSize();
        //for (iter = 0; iter + 3 < Tsize.getRows() && Results.absolute(StartRow+iter-1); iter++) {
        //    Wdbm.writer.drawString(0, iter, String.format(Wdbm.ScrollingListFormat, ValuesSubstitute(Wdbm.ScrollingListFields).toArray()));
        //}
        //Results.absolute(StartRow-1);
    }
    
   
    
    public void ReDrawList() throws SQLException {
        TerminalSize Tsize = AttachedWDBM.terminal.getTerminalSize();
        int SaveResultRow = Results.getRow();
        if (SaveResultRow < 1) System.err.println("Something Wrong" + SaveResultRow);   /////Diagnostic
        int startrow = SaveResultRow - ScreenCurrentRow;
        if (startrow < 1) {
            startrow = 1;
            ScreenCurrentRow = SaveResultRow -1;
        }
        int iter;
        for (iter = 0; (ListScreenLength==0 || iter < ListScreenLength)  && ListScreenTopLine+iter + 4 <= Tsize.getRows() && Results.absolute(startrow + iter); iter++) {
            AttachedWDBM.writer.drawString(0, ListScreenTopLine+iter, String.format(AttachedWDBM.ScrollingListFormat,
                    FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()));
        }
        // if (SaveResultRow < 1) SaveResultRow = 1;//    MYSTERY EMPIRICAL FIX
        Results.absolute(SaveResultRow);
    }

    public void IlluminateCurrentRow() throws SQLException {
        AttachedWDBM.scrn.putString(0, ListScreenTopLine+ScreenCurrentRow,
                String.format(AttachedWDBM.ScrollingListFormat, FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()),
                Terminal.Color.BLACK, Terminal.Color.WHITE);
        AttachedWDBM.scrn.refresh();
    }
    
    /**
     *
     * @throws SQLException
     */
    public void DeEmphasiseCurrentRow() throws SQLException {
        
        AttachedWDBM.scrn.putString(0, ListScreenTopLine + ScreenCurrentRow,
                String.format(AttachedWDBM.ScrollingListFormat, FieldNames2ValuesSubstitute(AttachedWDBM.ScrollingListFields).toArray()),
                Terminal.Color.WHITE, Terminal.Color.BLACK);
    }
    
    
    public void ExecuteSQLQuery(String SQLQuery) throws SQLException,InterruptedException {
        ResultSet LocalResults;
            LocalResults = stmt.executeQuery(SQLQuery);
            if (LocalResults.first()) {
                Results = LocalResults;
                ScreenCurrentRow = 0;
                ResultsCurrentRow = Results.getRow();
                ReDrawList();
            }
    }
    
 
    public Key DisplayList() throws SQLException,InterruptedException {
        Key KeyReturn;
        ResultSet LocalResults;
        TerminalSize Tsize;
        ReDrawList();
        AttachedWDBM.scrn.refresh();
        while (true) {
            Tsize = AttachedWDBM.terminal.getTerminalSize();
            ResultsCurrentRow = Results.getRow();
            
            IlluminateCurrentRow();
            
            if(ConnectedForm) AttachedWDBM.FormDisplay(Results);
            AttachedWDBM.scrn.refresh();
            if ((KeyReturn = AttachedWDBM.KeyInput(ScrollPrompt)).getKind() == Key.Kind.Home) {
                return KeyReturn;
            } else if (KeyReturn.getKind() == Key.Kind.ArrowDown && !Results.isLast()) {
                if ((ListScreenLength == 0 || ScreenCurrentRow+1 < ListScreenLength)  && ListScreenTopLine+ScreenCurrentRow + 4 < Tsize.getRows()) {
                    DeEmphasiseCurrentRow();
                    ScreenCurrentRow++;
                    Results.next();
                } else {
                    Results.next();
                    ReDrawList();
                    // scrollListUp(ScreenCurrentRow, Wdbm);
                }
            } else if (KeyReturn.getKind() == Key.Kind.ArrowUp && !Results.isFirst()) {
                if (ScreenCurrentRow > 0) {
                    DeEmphasiseCurrentRow();
                    ScreenCurrentRow--;
                    Results.previous();
                } else {
                    Results.previous();
                    ReDrawList();
                    // scrollListDown(Wdbm);
                }
            } else if (KeyReturn.getKind() == Key.Kind.NormalKey) {
                if (KeyReturn.getCharacter() == 's') {
                    try {
                        AttachedWDBM.Texaco.LineEditorBuffer = CurrentSQLQuery;
                        AttachedWDBM.Texaco.LineEditorPosition = CurrentSQLQuery.length();
                        String LocalString = AttachedWDBM.PromptForString("SQL Query");
                        if (AttachedWDBM.Texaco.LineEditorReturnKey.getKind() != Key.Kind.Escape) {
                            CurrentCompiledSQLStatement = AttachedWDBM.SQLconnection.prepareStatement(LocalString, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                            LocalResults = CurrentCompiledSQLStatement.executeQuery();
                            if (LocalResults.first()) {
                                Results = LocalResults;
                                ScreenCurrentRow = 0;
                                ResultsCurrentRow = Results.getRow();
                                AttachedWDBM.scrn.clear();
                                ReDrawList();
                                CurrentSQLQuery = LocalString;
                                AttachedWDBM.DisplayError("");
                            }
                        }
                    } catch (SQLException ex) {
                        AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + " ZenXoan");
                    }
                }
            } else if (KeyReturn.getKind() == Key.Kind.Enter) {
                DeEmphasiseCurrentRow();
                return KeyReturn;
            }
        }
    }
}
