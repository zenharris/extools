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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.househarris.extools.wdbm.TerminalWindow;


/**
 *
 * @author harris
 */
public class indexscroll implements extools {
    public int ListScrollsIndex = 0;
    public String IndexScrollName;
    public boolean ConnectedForm = false;
    
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

    
    public searchAtom CurrentSearchAtom;
    public Stack<searchAtom> SearchAtomStack = new Stack();
    
    public indexscroll(String ScrollName ,String SQLQuery,wdbm WDBMAttach,int... Dimensions) throws SQLException {
        System.err.println("Making New Index Scroll :" + ScrollName);
        if(Dimensions.length > 0){
            ListScreenTopLine = Dimensions[0];
            ListScreenLength = WDBMAttach.WindowStack.peek().rawTerminal.getTerminalSize().getRows()- ListScreenTopLine -3;
        }
        if(Dimensions.length > 1) ListScreenLength = Dimensions[1];
        SearchAtomStack.push(CurrentSearchAtom = new searchAtom(SQLQuery, WDBMAttach));
        Results = SearchAtomStack.peek().AtomicResultSet;
        if (!Results.first()) System.err.println("This returned nothing " + SQLQuery);     //      Empirical kudge ????

        ScreenCurrentRow = 0;
        ResultsCurrentRow = Results.getRow();
        AttachedWDBM = WDBMAttach;
        CurrentSQLQuery = SQLQuery;
        IndexScrollName = ScrollName;
        ListScrollsIndex = AttachedWDBM.ScrollStack.size();
        System.err.println(IndexScrollName + " " + ListScrollsIndex + " " +  CurrentSQLQuery );
    }
    
    //private List<String> Substitute = new ArrayList();
    private List<String> FieldNames2ValuesSubstitute(List<String> FieldNameList,ResultSet LocalResult) throws SQLException {
        List<String> Substitute = new ArrayList();
   //     Substitute.clear();
        for (String FieldName : FieldNameList) {
            boolean add = Substitute.add(LocalResult.getString(FieldName));
        }
      //  String[] returnArray = Substitute.toArray();
        return Substitute;
    }

            
    public void ReDrawScroll(searchAtom... AttachedSearchAtom) throws SQLException,InterruptedException {
        TerminalWindow UseWindow;
        if(AttachedSearchAtom.length>0) UseWindow = AttachedSearchAtom[0].AtomicOutTerminal;
        else UseWindow = CurrentSearchAtom.AtomicOutTerminal;

        TerminalSize Tsize = UseWindow.TerminalSize();
        int SaveResultRow = CurrentSearchAtom.AtomicResultSet.getRow();
        if (SaveResultRow < 1) AttachedWDBM.DisplayError("Something  Wrong  ReDrawScroll getRow" + SaveResultRow,UseWindow);   /////Diagnostic
        int startrow = SaveResultRow - ScreenCurrentRow;
        if (startrow < 1) {
            startrow = 1;
            ScreenCurrentRow = SaveResultRow -1;
        }
        int iter;
        for (iter = 0; (ListScreenLength==0 || iter < ListScreenLength)  && ListScreenTopLine+iter + 4 <= Tsize.getRows() && 
                CurrentSearchAtom.AtomicResultSet.absolute(startrow + iter); iter++) {
            UseWindow.DisplayString(0, ListScreenTopLine+iter, String.format(AttachedWDBM.DefaultScrollFormat,
                    FieldNames2ValuesSubstitute(AttachedWDBM.DefaultScrollFields,CurrentSearchAtom.AtomicResultSet).toArray()));
        }
        iter--;
        while (iter++ < ListScreenLength-1) UseWindow.DisplayString(0, ListScreenTopLine+iter, BLANK);
        CurrentSearchAtom.AtomicResultSet.absolute(SaveResultRow);
    }

    public void IlluminateCurrentRow() throws SQLException {
        CurrentSearchAtom.AtomicOutTerminal.screenHandle.putString(0, ListScreenTopLine + ScreenCurrentRow,
                String.format(AttachedWDBM.DefaultScrollFormat, FieldNames2ValuesSubstitute(AttachedWDBM.DefaultScrollFields,CurrentSearchAtom.AtomicResultSet).toArray()),
                Terminal.Color.BLACK, Terminal.Color.WHITE);
        CurrentSearchAtom.AtomicOutTerminal.Refresh();
    }
    
    /**
     *
     * @throws SQLException
     */
    public void DeEmphasiseCurrentRow() throws SQLException {
        CurrentSearchAtom.AtomicOutTerminal.screenHandle.putString(0, ListScreenTopLine + ScreenCurrentRow,
                String.format(AttachedWDBM.DefaultScrollFormat, FieldNames2ValuesSubstitute(AttachedWDBM.DefaultScrollFields,CurrentSearchAtom.AtomicResultSet).toArray()),
                Terminal.Color.WHITE, Terminal.Color.BLACK);
        CurrentSearchAtom.AtomicOutTerminal.Refresh();
    }
    
    public Thread SQLQueryThread;
    private final BlockingQueue<searchAtom> SQLQueryQueue = new LinkedBlockingQueue();
    
    public searchAtom returnNewSearchAtom(String SQLStatement,wdbm Wdbm) throws SQLException {
        return new searchAtom(SQLStatement, Wdbm);
        
    }

    @Override
    public indexscroll WithTheIndexScroll(String ScrollName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  
    
    public class searchAtom{
        Connection AtomicSQLMainPipe;
        String AtomicSQL;
        TerminalWindow AtomicOutTerminal;
        ResultSet AtomicResultSet;
        Statement AtomicStatement;
        PreparedStatement AtomicCompiledStatement;
        
        public searchAtom (String SQLStatement,wdbm Wdbm) throws SQLException {
            AtomicSQLMainPipe = Wdbm.SQLconnection; //SQLMainPipe;
            AtomicSQL = SQLStatement;
            AtomicOutTerminal = Wdbm.WindowStack.peek(); //Terminal;
            //  AtomicResultSet = Results;
            AtomicCompiledStatement = AtomicSQLMainPipe.prepareStatement(SQLStatement, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            AtomicResultSet = AtomicCompiledStatement.executeQuery();
            if (!AtomicResultSet.first()) {
                throw new SQLException("searchAtom Error Condition -> no find first");
            }
        }

        /**
         * Recycles Current Search Atom giving it a new SQL query and optionally
         * a new Terminal Window to send it's output.
         *
         * @param NewSQLQuery
         * @param ToTerm Optional TerminalWindow to send all output
         * @throws SQLException
         * @throws InterruptedException;
         * @return New SearchAtom if called with no currentSearchAtom.
         */
        public searchAtom Recycle(String NewSQLQuery, TerminalWindow... ToTerm) throws SQLException, InterruptedException {
            TerminalWindow UseWindow;

            if (ToTerm.length > 0)
                UseWindow = ToTerm[0];
            else UseWindow = AtomicOutTerminal;

            if (NewSQLQuery.equals(AtomicSQL)) return this;
          
            if (SQLQueryThread != null && SQLQueryThread.isAlive()) {
                
                if (SQLQueryQueue.size() > 1) {
                    SQLQueryThread.interrupt();
                }
                SQLQueryQueue.clear();  //overrun mittigated
                
                //      SQLQueryQueue.add(SearchAtom = new searchAtom(NewSQLQuery,UseWindow,AttachedWDBM.SQLconnection));
                AtomicSQL = NewSQLQuery;
                if (ToTerm.length > 0) {
                    AtomicOutTerminal = ToTerm[0];
                }
                SQLQueryQueue.add(this); // new searchAtom(NewSQLQuery,UseWindow,AttachedWDBM.SQLconnection));
                return this;
            }
            AtomicSQL = NewSQLQuery;
            boolean add = SQLQueryQueue.add(this); // new searchAtom(NewSQLQuery,UseWindow,AttachedWDBM.SQLconnection));
            SQLQueryThread = new Thread(new QuantumResearchDaemon());
            
            UseWindow.ThreadPool.add(SQLQueryThread.getId());

            SQLQueryThread.start();
            return this;
        }

        
    }
    
    public class QuantumResearchDaemon implements Runnable {
        @Override
        public void run() {
           // ResultSet LocalResults;
            TerminalWindow Terminal = null;
            try {
                do {
                        searchAtom QueuedParameter = SQLQueryQueue.take();
                        Terminal = QueuedParameter.AtomicOutTerminal;

                       // if (!QueuedParameter.AtomicSQL.equals(CurrentSQLQuery)) {
                            AttachedWDBM.DisplayStatusLine("SQL Data Transfer Taking Place");
                            Terminal.Refresh();
                            
                            CurrentCompiledSQLStatement = AttachedWDBM.SQLconnection.prepareStatement(QueuedParameter.AtomicSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                            QueuedParameter.AtomicCompiledStatement = CurrentCompiledSQLStatement;
                            QueuedParameter.AtomicResultSet = CurrentCompiledSQLStatement.executeQuery();
                            
                            if (QueuedParameter.AtomicResultSet.first()) {
                            //    Results = QueuedParameter.AtomicResultSet;
                                ScreenCurrentRow = 0;
                                ResultsCurrentRow = QueuedParameter.AtomicResultSet.getRow();
                                // AttachedWDBM.scrn.clear();
                                ReDrawScroll(QueuedParameter);
                                SQLQueryHistory.add(CurrentSQLQuery);
                                CurrentSQLQuery = QueuedParameter.AtomicSQL;
                            }
                       // }
                        AttachedWDBM.DisplayStatusLine("");
//                    AttachedWDBM.screenHandle.refresh();
                } while (true);
            } catch (InterruptedException IEx) {
                
            } catch (NoSuchElementException | SQLException | NullPointerException ex) {
                try {
                    // SQLQueryQueue.clear();
                    AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + "Zen",Terminal);
                } catch (SQLException ex1) {
                    Logger.getLogger(indexscroll.class.getName()).log(Level.SEVERE, null, ex1);
                }
                ex.printStackTrace();
            } catch (IllegalMonitorStateException ex) {
          //     AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + "Zen"); 
            }finally {
            }
        }
    }
 
    /**
     *
     * @param SQLQuery
     * @throws SQLException
     */
    public void ExecuteSQLQuery(String SQLQuery,TerminalWindow... ToTerm) throws SQLException,InterruptedException {
        // ResultSet LocalResults;
        TerminalWindow UseWindow;
        
        if (ToTerm.length>0) UseWindow = ToTerm[0];
        else UseWindow = CurrentSearchAtom.AtomicOutTerminal;
        
        try {
            AttachedWDBM.TextEditor.LineEditorBuffer = CurrentSQLQuery;
            AttachedWDBM.TextEditor.LineEditorPosition = CurrentSQLQuery.length();
            String LocalString = AttachedWDBM.PromptForString("->",UseWindow);
            if (AttachedWDBM.TextEditor.LineEditorReturnKey.getKind() != Key.Kind.Escape) {
                Results = CurrentSearchAtom.Recycle(LocalString).AtomicResultSet;
                // Results = CurrentSearchAtom.AtomicResultSet;
            }
        } catch (SQLException ex) {
            AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + " SQLState " + ex.getSQLState(),UseWindow);
            ex.printStackTrace();
        } finally {
            AttachedWDBM.DisplayError("",UseWindow);
            AttachedWDBM.DisplayStatusLine("");
            UseWindow.Refresh();
        }
    }
  
 
    public Key ActivateScroll(TerminalWindow... AttachedWindow) throws SQLException,InterruptedException{
        Key KeyReturn;
        TerminalSize Tsize;
        TerminalWindow UseWindow;

        if (AttachedWindow.length>0) UseWindow = AttachedWindow[0];
        else UseWindow = CurrentSearchAtom.AtomicOutTerminal;
        
        ReDrawScroll();
        
        while (true) {
            UseWindow.Refresh();
            Tsize = UseWindow.TerminalSize();
            ResultsCurrentRow = CurrentSearchAtom.AtomicResultSet.getRow();
            IlluminateCurrentRow();
           
            if(ConnectedForm) AttachedWDBM.FormDisplay(CurrentSearchAtom.AtomicResultSet,CurrentSearchAtom.AtomicOutTerminal);
            UseWindow.Refresh();
            if ((KeyReturn = AttachedWDBM.KeyInput(UseWindow,ScrollPrompt)).getKind() == Key.Kind.ReverseTab) {
                DeEmphasiseCurrentRow();
                return KeyReturn;
            } else if (KeyReturn.getKind() == Key.Kind.ArrowDown && !CurrentSearchAtom.AtomicResultSet.isLast()) {
                if ((ListScreenLength == 0 || ScreenCurrentRow + 1 < ListScreenLength) && ListScreenTopLine + ScreenCurrentRow + 4 < Tsize.getRows()) {
                    DeEmphasiseCurrentRow();
                    ScreenCurrentRow++;
                    CurrentSearchAtom.AtomicResultSet.next();
                } else {
                    CurrentSearchAtom.AtomicResultSet.next();
                    ReDrawScroll();
                }
            } else if (KeyReturn.getKind() == Key.Kind.ArrowUp && !CurrentSearchAtom.AtomicResultSet.isFirst()) {
                if (ScreenCurrentRow > 0) {
                    DeEmphasiseCurrentRow();
                    ScreenCurrentRow--;
                    CurrentSearchAtom.AtomicResultSet.previous();
                } else {
                    CurrentSearchAtom.AtomicResultSet.previous();
                    ReDrawScroll();
                }
            } else if (KeyReturn.getKind() == Key.Kind.PageDown && !CurrentSearchAtom.AtomicResultSet.isLast()) {
                CurrentSearchAtom.AtomicResultSet.relative(ListScreenLength-1);
                if(CurrentSearchAtom.AtomicResultSet.isAfterLast()) CurrentSearchAtom.AtomicResultSet.last();
                ReDrawScroll();
            } else if (KeyReturn.getKind() == Key.Kind.PageUp) {
                if(CurrentSearchAtom.AtomicResultSet.getRow()-(ListScreenLength) > ListScreenLength) CurrentSearchAtom.AtomicResultSet.relative(1-ListScreenLength);
                else CurrentSearchAtom.AtomicResultSet.absolute(ScreenCurrentRow+1);
                // else Results.first();
                ReDrawScroll();
            } else if (KeyReturn.getKind() == Key.Kind.End) {
                CurrentSearchAtom.AtomicResultSet.last();
                ReDrawScroll();
            } else if (KeyReturn.getKind() == Key.Kind.Home) {
                CurrentSearchAtom.AtomicResultSet.first();
                ReDrawScroll();
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
