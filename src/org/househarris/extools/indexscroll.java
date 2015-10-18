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


    public indexscroll(String ScrollName ,String SQLQuery,wdbm WDBMAttach,int... Dimensions) throws SQLException {
        System.err.println("Making New Index Scroll :" + ScrollName);
        if(Dimensions.length > 0){
            ListScreenTopLine = Dimensions[0];
            ListScreenLength = WDBMAttach.TopWindow().rawTerminal.getTerminalSize().getRows()- ListScreenTopLine -3;
        }
        
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
    
    public TerminalWindow TopWindow() {
        return AttachedWDBM.WindowStack.peek();
    }
 
    
    
    //private List<String> Substitute = new ArrayList();
    private List<String> FieldNames2ValuesSubstitute(List<String> FieldNameList,ResultSet LocalResult) throws SQLException {
        List<String> Substitute = new ArrayList();
   //     int iter = 0;
   //     Substitute.clear();
        for (String FieldName : FieldNameList) {
            boolean add = Substitute.add(LocalResult.getString(FieldName));
//            Substitute.add(Results.getString(FieldName));
        }
      //  String[] returnArray = Substitute.toArray();
        return Substitute;
    }

            
    public void ReDrawScroll() throws SQLException,InterruptedException {
        TerminalSize Tsize = TopWindow().rawTerminal.getTerminalSize();
        int SaveResultRow = Results.getRow();
        if (SaveResultRow < 1) AttachedWDBM.DisplayError("Something  Wrong  ReDrawScroll getRow" + SaveResultRow);   /////Diagnostic
        int startrow = SaveResultRow - ScreenCurrentRow;
        if (startrow < 1) {
            startrow = 1;
            ScreenCurrentRow = SaveResultRow -1;
        }
        int iter;
        for (iter = 0; (ListScreenLength==0 || iter < ListScreenLength)  && ListScreenTopLine+iter + 4 <= Tsize.getRows() && Results.absolute(startrow + iter); iter++) {
            AttachedWDBM.TopWindow().DisplayString(0, ListScreenTopLine+iter, String.format(AttachedWDBM.DefaultScrollFormat,
                    FieldNames2ValuesSubstitute(AttachedWDBM.DefaultScrollFields,Results).toArray()));
        }
        iter--;
        while (iter++ < ListScreenLength-1) AttachedWDBM.TopWindow().DisplayString(0, ListScreenTopLine+iter, BLANK);
        // if (SaveResultRow < 1) SaveResultRow = 1;//    MYSTERY EMPIRICAL FIX
        Results.absolute(SaveResultRow);
    }

    public void IlluminateCurrentRow() throws SQLException {
        AttachedWDBM.TopWindow().screenHandle.putString(0, ListScreenTopLine + ScreenCurrentRow,
                String.format(AttachedWDBM.DefaultScrollFormat, FieldNames2ValuesSubstitute(AttachedWDBM.DefaultScrollFields,Results).toArray()),
                Terminal.Color.BLACK, Terminal.Color.WHITE);
        AttachedWDBM.TopWindow().screenHandle.refresh();
    }
    
    /**
     *
     * @throws SQLException
     */
    public void DeEmphasiseCurrentRow() throws SQLException {
        AttachedWDBM.TopWindow().screenHandle.putString(0, ListScreenTopLine + ScreenCurrentRow,
                String.format(AttachedWDBM.DefaultScrollFormat, FieldNames2ValuesSubstitute(AttachedWDBM.DefaultScrollFields,Results).toArray()),
                Terminal.Color.WHITE, Terminal.Color.BLACK);
    }
    
    public Thread SQLQueryThread;
    //private Queue SQLQueryQueue = new LinkedList();
    private final BlockingQueue<String> SQLQueryQueue = new LinkedBlockingQueue();
//    private PriorityQueue SQLQueryPriorityQueue = new PriorityQueue();
    private final Semaphore SQLQueryQueueLock = new Semaphore(1, true);

    /**
     *
     * @param NewSQLQuery
     * @throws SQLException
     * @throws InterruptedException;
     */
    public void ReSearch(String NewSQLQuery) throws SQLException, InterruptedException {
        if (SQLQueryThread != null && SQLQueryThread.isAlive()) {
            if (SQLQueryQueue.size() > 1) {
            //    SQLQueryQueueLock.acquire();
                SQLQueryThread.interrupt();
            }
//            SQLQueryThread.join();
            SQLQueryQueue.clear();  //overrun mittigated
            SQLQueryQueue.add(NewSQLQuery);
            SQLQueryQueueLock.release();
            return;
        }
        boolean add = SQLQueryQueue.add(NewSQLQuery);
        SQLQueryQueueLock.release();
        SQLQueryThread = new Thread(new QuantumForkReSearch());
        SQLQueryThread.start();
    }

    public class QuantumForkReSearch implements Runnable {
        @Override
        public void run() {
            ResultSet LocalResults;
            try {
                while (true) {
                        SQLQueryQueueLock.acquire();
                        String NewSQLQuery = SQLQueryQueue.take();
                        SQLQueryQueueLock.release();
                        if (!NewSQLQuery.equals(CurrentSQLQuery)) {
                            AttachedWDBM.DisplayStatusLine("SQL Data Transfer Taking Place");
                            AttachedWDBM.TopWindow().screenHandle.refresh();
                            CurrentCompiledSQLStatement = AttachedWDBM.SQLconnection.prepareStatement(NewSQLQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                            LocalResults = CurrentCompiledSQLStatement.executeQuery();
                            if (LocalResults.first()) {
                                Results = LocalResults;
                                ScreenCurrentRow = 0;
                                ResultsCurrentRow = Results.getRow();
                                // AttachedWDBM.scrn.clear();
                                ReDrawScroll();
                                SQLQueryHistory.add(CurrentSQLQuery);
                                CurrentSQLQuery = NewSQLQuery;
                            }
                        }
                        AttachedWDBM.DisplayStatusLine("");
//                    AttachedWDBM.screenHandle.refresh();
                }
            } catch (InterruptedException | NoSuchElementException | SQLException | NullPointerException ex) {
                // SQLQueryQueue.clear();
                AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + "Zen");
                ex.printStackTrace();
            } catch (IllegalMonitorStateException ex) {
               AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + "Zen"); 
            }finally {
                SQLQueryQueueLock.release();
            }
        }
    }
 
    /**
     *
     * @param SQLQuery
     * @throws SQLException
     */
    public void ExecuteSQLQuery(String SQLQuery) throws SQLException,InterruptedException {
        // ResultSet LocalResults;
        try {
            AttachedWDBM.TextEditor.LineEditorBuffer = CurrentSQLQuery;
            AttachedWDBM.TextEditor.LineEditorPosition = CurrentSQLQuery.length();
            String LocalString = AttachedWDBM.PromptForString("->");
            if (AttachedWDBM.TextEditor.LineEditorReturnKey.getKind() != Key.Kind.Escape) {
                ReSearch(LocalString);
            }
        } catch (SQLException ex) {
            AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + " SQLState " + ex.getSQLState());
            ex.printStackTrace();
        } finally {
            AttachedWDBM.DisplayError("");
            AttachedWDBM.DisplayStatusLine("");
            AttachedWDBM.TopWindow().screenHandle.refresh();
        }
    }
  
 
    public Key ActivateScroll() throws SQLException,InterruptedException{
        Key KeyReturn;
        TerminalSize Tsize;

        ReDrawScroll();
        AttachedWDBM.TopWindow().screenHandle.refresh();
        while (true) {
            Tsize = AttachedWDBM.TopWindow().rawTerminal.getTerminalSize();
            ResultsCurrentRow = Results.getRow();
            IlluminateCurrentRow();
            if(ConnectedForm) AttachedWDBM.FormDisplay(Results);
            AttachedWDBM.TopWindow().screenHandle.refresh();
            if ((KeyReturn = AttachedWDBM.KeyInput(ScrollPrompt)).getKind() == Key.Kind.ReverseTab) {
                DeEmphasiseCurrentRow();
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
            } else if (KeyReturn.getKind() == Key.Kind.PageDown && !Results.isLast()) {
                Results.relative(ListScreenLength-1);
                if(Results.isAfterLast()) Results.last();
                ReDrawScroll();
            } else if (KeyReturn.getKind() == Key.Kind.PageUp) {
                if(Results.getRow()-(ListScreenLength) > ListScreenLength) Results.relative(1-ListScreenLength);
                else Results.absolute(ScreenCurrentRow+1);
                // else Results.first();
                ReDrawScroll();
            } else if (KeyReturn.getKind() == Key.Kind.End) {
                Results.last();
                ReDrawScroll();
            } else if (KeyReturn.getKind() == Key.Kind.Home) {
                Results.first();
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
