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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.terminal.swing.SwingTerminal;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.lang.Object;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.househarris.extools.indexscroll.searchAtom;
// import java.util.concurrent.Executor;
// import java.text.SimpleDateFormat;


// import java.sql.Statement;

public class wdbm implements extools {

    public Terminal rawTerminal;
    public Screen screenHandle;
    public ScreenWriter screenWriter = null;
    public TerminalWindow DefaultWindow;
    public Stack<TerminalWindow> WindowStack = new Stack();
    public List<String> DefaultFormTemplate = new ArrayList();
    public List<String> DefaultFormFieldList = new ArrayList();
    public List<String> ServerDetails = new ArrayList();
    public List<String> CurrentRecord = new ArrayList();
    public List<String> DefaultScrollFields = new ArrayList();
    public indexscroll DefaultScroll;
    public String DefaultScrollFormat;
    public String DefaultScrollSearchSQL;
    public Connection SQLconnection;
    public ResultSet CurrentRecordResultSet;

    public Stack<indexscroll> IndexScrolls = new Stack();
    public Stack<Thread> ActivatedFormsThreadPool = new Stack();
  
    public List<String> ErrorLog = new ArrayList();
    public String CurrentErrorBuffer = "";
    public Thread ResearchDaemonThread;
    
    
    /**
     *
     */
    public texaco TextEditor;

    public wdbm(String DataDictionaryFilename) throws ClassNotFoundException,SQLException,IOException {
     /*   rawTerminal = TerminalFacade.createTerminal();
        TerminalSize Tsize = rawTerminal.getTerminalSize();
        Tsize.setColumns(83); // Max 100
        Tsize.setRows(30); //Max 30
        screenHandle = TerminalFacade.createScreen(rawTerminal);
        screenHandle.startScreen();
        screenHandle.clear();
        screenHandle.refresh();
        screenWriter = new ScreenWriter(screenHandle);
        screenWriter.setForegroundColor(Terminal.Color.WHITE);
        screenWriter.setBackgroundColor(Terminal.Color.BLACK);
     */ 

        WindowStack.push(DefaultWindow = new TerminalWindow(0,0,30,83));
        TextEditor = new texaco(this);
        ReadDataDictionary(Paths.get(DataDictionaryFilename));
        OpenSQLfile();
        
        // IndexScrolls.add(DefaultScroll = new indexscroll("default",ScrollingListDefaultSearchSQL,this,FormTemplate.size()));
        CreateDefaultScroll(DefaultFormTemplate.size());
        CreateAnyScrollsInDefaultForm();
       
    }
    
 //   public TerminalWindow TopWindow() {
 //       return NamedWindow(Thread.currentThread().getId());
       // return WindowStack.peek();
 //////   }
    
    /*
   public TerminalWindow NamedWindow(long searchfor) {
        for (TerminalWindow SearchPointer : WindowStack) {
            if (SearchPointer.ThreadId == searchfor) return SearchPointer;
        }
        return null; // WindowStack.peek();
    }
   */
         public TerminalWindow TopWindow() throws SQLException {
             return ApropriateWindow();
            // return NamedWindow(Thread.currentThread().getId());
            // return WindowStack.peek();
        }
        
        public TerminalWindow ApropriateWindow(long... OverrideThread) throws SQLException {
            return DefaultWindow;
            /*
            long searcher;
            if (OverrideThread.length > 0) searcher = OverrideThread[0];
            else searcher = Thread.currentThread().getId();
            for (TerminalWindow searchWindow : WindowStack ) {
                for (long iter : searchWindow.ThreadPool) {
                    if (iter == searcher) return searchWindow ;
                }
            }
            throw new SQLException("Could Not find Apropriate window");
            */
        }
        public TerminalWindow NamedWindow(long searchfor) throws SQLException {
            for (TerminalWindow searchWindow : WindowStack) {
                for (long iter : searchWindow.ThreadPool) {
                    if (iter == searchfor) return searchWindow;
                }
                
            } 
            throw new SQLException("Could Not find Named window ->"+searchfor);
        }
    
    
    
    private void OpenSQLfile() throws ClassNotFoundException,SQLException {
            Class.forName("org.postgresql.Driver");
            SQLconnection = DriverManager.getConnection(ServerDetails.get(ServerDetails.size()-1),"postgres",""); // "jdbc:postgresql://10.8.0.1:5432/sewer","postgres", "");
            // SQLconnection.setAutoCommit(false);
            System.out.println("Opened database successfully");
    }
    
 
    private void ReadDataDictionary(Path PathOfFile) throws IOException,SQLException {
        try (InputStream in = Files.newInputStream(PathOfFile); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            int FieldSwitch = 0;
            int LineNumber =0;
            int LineCounter = 0;
            while ((line = reader.readLine()) != null) {
                LineCounter++;
                if (line.matches("%FIELD_DEF%")) FieldSwitch = 1;
                else if (line.matches("%LIST_DEF%")) FieldSwitch = 2;
                else if (line.matches("%SERVER_DETAILS%")) FieldSwitch = 3;
                else if (line.matches("%END%")) FieldSwitch = 4;
                else {
                    
                    switch (FieldSwitch) {
                        case 0:
                            DefaultFormTemplate.add(line);
                            break;
                        case 1:
                            DefaultFormFieldList.add(line);
                            break;
                        case 2:
                            if(LineNumber == 0){
                                DefaultScrollSearchSQL = line;
                                LineNumber++;
                            } else if (LineNumber == 1) {
                                DefaultScrollFormat = line;
                                LineNumber++;
                            }else {
                                DefaultScrollFields.add(line);
                            }
                            break;
                        case 3:
                            ServerDetails.add(line);
                            break;
                    }
                }
            }
        }
    }
    
    private void CreateAnyScrollsInDefaultForm() throws SQLException {
        for (String Field : DefaultFormFieldList) {
            String[] FieldElements = Field.split(SplittingColon);
            if (FieldElements[1].equals(IndexScrollFieldLabel)) {
                IndexScrolls.push(new indexscroll(FieldElements[0], ResolveSQLStatementInFieldTemplate(Field), this, MeasureDimensionsOf(FieldElements[0])));
                // IndexScroll(FieldElements[0]).ConnectedForm = false;
            }
        }
    }
    
    public void CreateDefaultScroll(int... Dimensions) throws SQLException {
        IndexScrolls.push(DefaultScroll = new indexscroll("default", DefaultScrollSearchSQL, this, Dimensions));
        DefaultScroll.ConnectedForm = true;
    }

   
    public int GetFieldNumber(String FieldName) throws SQLException{
       int LineCounter = 0;
        for (String Searcher : DefaultFormFieldList) {
            if (Searcher.split(SplittingColon)[0].equals(FieldName)) {
                return LineCounter;
            }
            LineCounter++;
        }
        throw new SQLException("No Field Named "+FieldName+" .Zen");
    }

    
    public int[] MeasureDimensionsOf(String FieldName) throws SQLException {
        int row = 0;
        int column = 0;
        int width = 0;
        int length = 0;
//        String type;
        //      int number;
        int LineCounter = 0;
        boolean firstTime = true;
        String SearchRegex = String.format("@%s<+", GetFieldNumber(FieldName));
        Pattern p = Pattern.compile(SearchRegex);
        for (String LocalBuffer : DefaultFormTemplate) {
            Matcher m = p.matcher(LocalBuffer);
            while (m.find()) {
                length++;
                if (firstTime) {
                    row = LineCounter;
                    column = m.start();
                    width = m.end() - m.start();
                    firstTime = false;
                }
            }
            LineCounter++;
        }
        if (firstTime) throw new SQLException("Field Not Found "+FieldName+" .zen");
        int[] returnArray = {row, length, width};
        return returnArray;
    }


    /**
     * Constructs SQL statement from the template embeded in a form field definition
     * from the dictionary file. Gets values from current result set.
     * 
     * @param Cursor
     * @return
     * @throws SQLException 
     */
    private String ResolveSQLStatementInFieldTemplate (String FormFieldDefinition) throws SQLException{
        String[] FieldElements = FormFieldDefinition.split(SplittingColon);
        String Replacement = DefaultScroll.CurrentSearchAtom.AtomicResultSet.getString(FieldElements[3]);
        System.err.println(String.format(FieldElements[2],Replacement));
        return String.format(FieldElements[2],Replacement);
    }
    
    /**
     *Search Index Scroll Stack to find scroll named in param.
     * Returns it's index scroll object. null if not found.
     * 
     * @param ScrollName    text name of scroll
     * @return
     */
    public indexscroll WithTheIndexScroll(String ScrollName) throws SQLException{
        for (indexscroll SearchCursor : IndexScrolls) {
            if (SearchCursor.IndexScrollName.equals(ScrollName+Thread.currentThread().getId())){
                System.err.println("Found Scroll "+ScrollName+" "+Thread.currentThread().getId());
                return SearchCursor;
            }
        }
        for (indexscroll SearchCursor : IndexScrolls) {
            if (SearchCursor.IndexScrollName.equals(ScrollName)){
                System.err.println("Found Scroll "+ScrollName);
                return SearchCursor;
            }
        }
        throw new SQLException("Scroll Not Found in IndexScrolls "+ScrollName+" .zen");
    }

/**
 * Get the number value from the form template field token in the dictionary file and return it.
 * identifies the form field definition that corresponds to this form field item.
 * looks up FieldList to find it.
 * 
 * @param FieldTemplate
 * @return 
 */
    private int TheFieldNumberFrom (String FieldTemplate) throws SQLException{
        Pattern p = Pattern.compile(REGEXToMatchNumberEmbededInFieldTemplate);
        Matcher m = p.matcher(FieldTemplate);
        if (m.find()) return Integer.parseInt(FieldTemplate.substring(m.start(), m.end()));
        throw new SQLException("No Numeric in FieldTemplate"+FieldTemplate+" .zen");
    }
    
    private List<String> unpackRecordBuffer(ResultSet ResultsSetToUnpack) throws SQLException {
        String FieldValue;
        List<String> WorkList = new ArrayList();
        //CurrentRecord.clear();
        for (String FieldName : DefaultFormFieldList) {
            if (FieldName.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {
                ////  Ignore index scroll fields, they are not real.
            } else {
                FieldValue = ResultsSetToUnpack.getString(FieldName.split(SplittingColon)[0]);
                if (FieldValue == null) FieldValue = "";              // Strange Processing for when feilds have a null value
                WorkList.add(FieldValue);
            }
        }
        return WorkList;
    }
    
    
    public void FormDisplay(ResultSet LocalResultSet,TerminalWindow AttachedWindow) throws SQLException,InterruptedException {
        int iter = 0;
        boolean onceScroll = false;
        List<String> RecordBuffer = new ArrayList();
        RecordBuffer = unpackRecordBuffer(LocalResultSet);
        Pattern p = Pattern.compile(REGEXToMatchEmbededFieldTemplate);
      //  scrn.clear();
        for (String LineBuffer : DefaultFormTemplate) {
            Matcher m = p.matcher(LineBuffer);
            while (m.find()) {
                String FieldTemplate = LineBuffer.substring(m.start(), m.end());
                String Field = DefaultFormFieldList.get(TheFieldNumberFrom(FieldTemplate));
                String FieldName = Field.split(SplittingColon)[0];
                if (Field.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {
                    if (!onceScroll) {
                        onceScroll = true;
                        WithTheIndexScroll(FieldName).CurrentSearchAtom.Recycle(ResolveSQLStatementInFieldTemplate(Field));
                        WithTheIndexScroll(FieldName).ReDrawScroll();
                    }
                    LineBuffer = "";
                } else {
                 //   String FieldValue = LocalResultSet.getString(FieldName);// CurrentRecord.get(ExtractFieldNumberFrom(FieldTemplate));
                     String FieldValue = RecordBuffer.get(TheFieldNumberFrom(FieldTemplate));
                   if (FieldValue == null)  FieldValue = "";
                    FieldValue = TrimToEditingLength(FieldValue, FieldTemplate);
                    FieldValue = PadToPrintingLength(FieldValue, FieldTemplate);
                    LineBuffer = LineBuffer.replaceFirst(REGEXToMatchEmbededFieldTemplate, FieldValue);
                    
                }
            }
            // screenWriter.drawString(0, iter, LineBuffer);
           // if(AttachedWindow.length>0){
                AttachedWindow.DisplayString(0, iter, LineBuffer);
           // }
           // else ApropriateWindow().DisplayString(0, iter, LineBuffer);
            iter++;
        }
        //if(AttachedWindow.length>0)AttachedWindow[0].Refresh();
        //else ApropriateWindow().Refresh();
        AttachedWindow.Refresh();

    }

 
    private String PadToPrintingLength (String FieldValue, String FieldTemplate){
        FieldValue += BLANK.substring(0,FieldTemplate.length()-FieldValue.length());
        return(FieldValue);
    }
    private String TrimToEditingLength (String FieldValue, String FieldTemplate){
                if (FieldValue.length() > FieldTemplate.length()) FieldValue = FieldValue.substring(0, FieldTemplate.length());
                return(FieldValue);
    }
    
    public void FormEditor(TerminalWindow Terminal) throws SQLException,InterruptedException{
        int iter = 0;
        Pattern p = Pattern.compile(REGEXToMatchEmbededFieldTemplate);
        for (String LineBuffer : DefaultFormTemplate) {
            Matcher m = p.matcher(LineBuffer);
            while (m.find()) {
                String FieldTemplate = LineBuffer.substring(m.start(), m.end());
                String Field = DefaultFormFieldList.get(TheFieldNumberFrom(FieldTemplate));
                String FieldName = Field.split(SplittingColon)[0];
                if (Field.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {
                    
                  //  Key ExitKey = IndexScroll(FieldName).DisplayList();

                } else {
                    String FieldValue = CurrentRecord.get(TheFieldNumberFrom(FieldTemplate));
                    FieldValue = TrimToEditingLength(FieldValue, FieldTemplate);
                    TextEditor.LineEditorPosition = 0;
                    TextEditor.LineEditor(Terminal,m.start(), iter, FieldTemplate.length(), FieldValue);
                    if (TextEditor.LineEditorReturnKey.getKind() == Key.Kind.Escape) {
                        return;
                    }
                }
            }
            iter++;
        }
    }
    
    private void unpackCurrentRecord(ResultSet... SetThisAsCurrent) throws SQLException {
        String FieldValue;
        if (SetThisAsCurrent.length > 0) {
            CurrentRecordResultSet = SetThisAsCurrent[0];
        }
        CurrentRecord.clear();
        for (String FieldName : DefaultFormFieldList) {
            if (FieldName.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {
                ////  Ignore index scroll fields, they are not real.
            } else {
                FieldValue = CurrentRecordResultSet.getString(FieldName.split(SplittingColon)[0]);
                if (FieldValue == null) FieldValue = "";              // Strange Processing for when feilds have a null value
                CurrentRecord.add(FieldValue);
            }
        }
    }

    public void DisplayStatusLine(String StatusText) {
        for (TerminalWindow iter : WindowStack) {
            iter.DisplayString(0, 0, BLANK.substring(0, iter.rawTerminal.getTerminalSize().getColumns() - 30));
            iter.DisplayString(0, 0, StatusText, ScreenCharacterStyle.Underline, ScreenCharacterStyle.Bold);
            iter.Refresh();
        }
    }

    public void DisplayError(String ErrorText,TerminalWindow Terminal) throws SQLException {
        TerminalSize Tsize = Terminal.rawTerminal.getTerminalSize();
        Terminal.DisplayString(0, Tsize.getRows() - 1, BLANK);
        Terminal.DisplayString(0, Tsize.getRows() - 1, ErrorText);
        Terminal.Refresh();
        
        if (CurrentErrorBuffer.equals("")) ErrorLog.add(ErrorText);
        CurrentErrorBuffer = ErrorText;
        
    }
    
    public void DisplayPrompt(String Prompt,TerminalWindow Terminal) throws SQLException {
        TerminalSize Tsize = Terminal.rawTerminal.getTerminalSize();
        Terminal.DisplayString(0, Tsize.getRows() - 3, BLANK);
        Terminal.DisplayString(0, Tsize.getRows() - 3, Prompt);
        Terminal.DisplayString(0, Tsize.getRows() - 2, "?: ");
        Terminal.CursorTo(2, Tsize.getRows() - 2);
    }
    
    public String PromptForString(String Prompt,TerminalWindow Terminal) throws SQLException,InterruptedException{
        TerminalSize Tsize = Terminal.rawTerminal.getTerminalSize();
        Terminal.DisplayString(0, Tsize.getRows() - 2, BLANK);
        Terminal.DisplayString(0, Tsize.getRows() - 2, Prompt);
        String LocalString = TextEditor.LineEditor(Terminal,Prompt.length()+1, Tsize.getRows() - 2, 80);
        Terminal.DisplayString(0, Tsize.getRows() - 2, BLANK);
        return LocalString;
    }

    /**
     * takes keyboard character and returns a Key object representation
     * if optional prompt is provided as param then it will take input as
     * if it is a menu bar prompt.
     * 
     * @param Prompt
     * @return
     * @throws java.sql.SQLException
     */
    public Key KeyInput(TerminalWindow Terminal,String... Prompt) throws SQLException,InterruptedException {
        Key KeyReceived;
        int iter = 0;
        Date thisSec;
        int FromCharacter = 0;

        if (Prompt.length > 0) {
            DisplayPrompt(Prompt[0],Terminal);
            TopWindow().Refresh();
        }
        while ((KeyReceived = Terminal.GetKey()) == null) {
        //    try {
                Thread.sleep(1);
        //    } catch (InterruptedException ex) {
        //        DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + " sleep failed somehow KeyInput");
        //    }
            if (iter++ == 1000) {
                thisSec = new Date(); //LocalTime.now();
                // implementation of display code is left to the reader
                //display(thisSec.getHour(), thisSec.getMinute(), thisSec.getSecond());
                // TerminalSize Tsize = rawTerminal.getTerminalSize();
                for (TerminalWindow Witer : WindowStack) {
                    Witer.DisplayString(Witer.rawTerminal.getTerminalSize().getColumns() - 30, 0, thisSec.toString());
                    Witer.Refresh();
                }
                iter = 0;
            }else if (iter == 1 || iter == 200 || iter == 400 || iter == 600 || iter == 800 || iter == 1000 ){
      
                if (CurrentErrorBuffer.length() > 0) {
                    TerminalSize Tsize = Terminal.rawTerminal.getTerminalSize();
                    Terminal.DisplayString(0, Tsize.getRows() - 1, BLANK);
                    Terminal.DisplayString(0, Tsize.getRows() - 1, CurrentErrorBuffer.substring(FromCharacter++));
                    Terminal.Refresh();
                    if (FromCharacter + Tsize.getColumns() > CurrentErrorBuffer.length()+5) {
                        FromCharacter = 0;
                    }
                    Terminal.Refresh();
                }

            }
   
            if (Terminal.screenHandle.resizePending()) {
                if (Prompt.length > 0) {
                    Terminal.Clear();
                    TerminalSize Tsize = Terminal.rawTerminal.getTerminalSize();
                    if (DefaultScroll.ScreenCurrentRow < 0) DefaultScroll.ScreenCurrentRow = 0;
                    int LocalMaximum = DefaultScroll.ScreenCurrentRow + DefaultScroll.ListScreenTopLine;
                    if (LocalMaximum > Tsize.getRows() - 4) {
                        DefaultScroll.ScreenCurrentRow
                                = Tsize.getRows() - 4 - DefaultScroll.ListScreenTopLine;
                    }

                    DefaultScroll.ListScreenLength = Tsize.getRows() - DefaultScroll.ListScreenTopLine - 3;

                    DefaultScroll.ReDrawScroll();
                    DefaultScroll.IlluminateCurrentRow();
                    FormDisplay(DefaultScroll.Results,DefaultWindow);
                    DisplayPrompt(Prompt[0],Terminal);
                    DisplayStatusLine(String.format("Term Dimensions %3s col x %-3srow", Tsize.getColumns(), Tsize.getRows()));
                }
                Terminal.Refresh();
            }
        }
        return KeyReceived;
    }

    
    public String FirstScrollInDefaultForm() {
        for (String SearchCursor : DefaultFormFieldList) {
            if (SearchCursor.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {
                return SearchCursor.split(SplittingColon)[0];
            }
        }
        return null;
    }

    /**
     *
     * @param LocalResult
     * @return
     * @throws SQLException
     * @throws InterruptedException;
     */
    public void ActivateForm(ResultSet LocalResult,List<String>... RecordBuffer) throws SQLException,InterruptedException {
        QuantumForkForm NewForm ;
         //   ActivatedFormQueue.add(LocalResult);
        ActivateFormThread = new Thread(NewForm = new QuantumForkForm());
        
        
        NewForm.LocalResult = LocalResult;
        NewForm.AttachedWdbm = this;
       
        if (RecordBuffer.length > 0) {
            unpackCurrentRecord(LocalResult);
            NewForm.RecordBuffer = CurrentRecord;
        }

        ActivatedFormStack.push(NewForm);
        ActivatedFormsThreadPool.add(ActivateFormThread);
        ActivateFormThread.start();
        
    }
    public Thread ActivateFormThread;
     final Stack<QuantumForkForm> ActivatedFormStack = new Stack();
    
    public class QuantumForkForm implements Runnable {

        public Key ExitedWithKey = null;
        public ResultSet LocalResult = null;
        public wdbm AttachedWdbm;
        public TerminalWindow AttachedWindow;
        public List<String> RecordBuffer = new ArrayList();
        private boolean ScrollPresent = false;
        private String ScrollName;
        private String ScrollField;
        private indexscroll ThisScroll;
        @Override
        public void run() {
            try {
                WindowStack.push(AttachedWindow = new TerminalWindow(0, 0, DefaultFormTemplate.size() + 3, 83));
                AttachedWindow.Refresh();
                
                for (String Field : DefaultFormFieldList) {
                    String[] FieldElements = Field.split(SplittingColon);
                    if (FieldElements[1].equals(IndexScrollFieldLabel)) {
                        if (!ScrollPresent) {
                            ScrollPresent = true;
                            ScrollName = FieldElements[0];
                            ScrollField = Field;
                            IndexScrolls.push(ThisScroll = new indexscroll(ScrollName + Thread.currentThread().getId(),
                                    ResolveSQLStatementInFieldTemplate(Field), AttachedWdbm, MeasureDimensionsOf(FieldElements[0])));
                        }

                    }
                }
                
                do {
         
                    // new search scroll&atom for scrolls in form template.
                    if (ScrollPresent) WithTheIndexScroll(ScrollName).CurrentSearchAtom.Recycle(ResolveSQLStatementInFieldTemplate(ScrollField));  //ConnectedForm = true;

                    FormDisplay(LocalResult, AttachedWindow);
                    AttachedWindow.Refresh();
                    String ScrollFieldName = FirstScrollInDefaultForm();
                    if (ScrollFieldName != null) {
                       ExitedWithKey = WithTheIndexScroll(ScrollFieldName).ActivateScroll();
                        if (ExitedWithKey.getKind() == Key.Kind.Enter) {
                            ActivateForm(WithTheIndexScroll(ScrollFieldName).CurrentSearchAtom.AtomicResultSet);
                        }
                        DisplayPrompt(FormMenuPrompt,AttachedWindow);
                    } else {
                        ExitedWithKey = KeyInput(AttachedWindow,FormMenuPrompt); //"[ESC]Back  [E]dit                             [N]ext [P]rev      [Home]Exit");
                    }

                    if (ExitedWithKey.getKind() == Key.Kind.NormalKey) {
                        if (ExitedWithKey.getCharacter() == 'n' && !LocalResult.isLast()) {
                            LocalResult.next();
                        } else if (ExitedWithKey.getCharacter() == 'p' && !LocalResult.isFirst()) {
                            LocalResult.previous();
                        } else if (ExitedWithKey.getCharacter() == 'e') {
                            unpackCurrentRecord(LocalResult);
                            FormEditor(AttachedWindow);
                        }
                    }
                } while (ExitedWithKey.getKind() != Key.Kind.ReverseTab);
            } catch (InterruptedException IEx) {
                
            } catch (Exception ex) {
                try {
                    ThisScroll.AttachedWDBM.DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + "Zen",AttachedWindow);
                } catch (SQLException ex1) {
                    Logger.getLogger(wdbm.class.getName()).log(Level.SEVERE, null, ex1);
                }
                ex.printStackTrace();
            } finally {
              
                AttachedWindow.screenHandle.stopScreen();

               // if (ScrollPresent) {

                    ThisScroll.SearchAtomStack.remove(ThisScroll.CurrentSearchAtom);
                    ThisScroll.AttachedWDBM.WindowStack.remove(AttachedWindow);
                    ThisScroll.AttachedWDBM.ActivatedFormStack.remove(this);
                    ThisScroll.AttachedWDBM.ActivatedFormsThreadPool.remove(Thread.currentThread());
                    if(ThisScroll.SQLQueryThread != null) ThisScroll.SQLQueryThread.interrupt();

               // }


            }
        }
    }
    
    private Thread ActivatedWDBM;
    public void ActivateWDBM (boolean... Daemonise) throws SQLException,InterruptedException {
        
        ActivatedWDBM = new Thread(new QuantumForkActivateWDBM());

        WindowStack.peek().ThreadPool.add(ActivatedWDBM.getId());
        ActivatedWDBM.start();
        if (Daemonise.length > 0 && !Daemonise[0]) {
            ActivatedWDBM.join();
        }

    }
    
    public class QuantumForkActivateWDBM implements Runnable {

        @Override
        public void run() {
            try {
                while (DefaultScroll.ActivateScroll(DefaultScroll.CurrentSearchAtom.AtomicOutTerminal).getKind() != Key.Kind.ReverseTab) {
                    ActivateForm(WithTheIndexScroll("default").CurrentSearchAtom.AtomicResultSet); //DefaultScroll.Results);
                  //  if (ActivateForm(DefaultScroll.Results).getKind() == Key.Kind.ReverseTab) {
                  //      return; //throw new SQLException("Exiting Exception ");
                  //  }
                }
                
            } catch (InterruptedException IEx) {
                close();
            } catch (Exception ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                close();
            }
        }
       
        public void close() {
            Thread KillThread;
            DefaultWindow.screenHandle.stopScreen();
            while(!IndexScrolls.empty()){
                KillThread = IndexScrolls.pop().SQLQueryThread;
             if (KillThread!= null) KillThread.interrupt();
            }

            while (!ActivatedFormsThreadPool.empty()) ActivatedFormsThreadPool.pop().interrupt();
            
        }
    }
   
    public class TerminalWindow {
        public String Name;
        public List<Long> ThreadPool = new ArrayList();
        public Terminal rawTerminal;
        public Screen screenHandle;
        public ScreenWriter screenWriter = null;
        public int Column = 0;
        public int Row = 0;
        public int Length = 0;
        public int Width = 0;

        public TerminalWindow(int column,int row,int length,int width,wdbm... CloneFrom) throws SQLException {
            // Name = name;
            
            //if (Thread.currentThread() != null) 
                ThreadPool.add(Thread.currentThread().getId());
            
            if (CloneFrom.length > 0) {
                rawTerminal = CloneFrom[0].rawTerminal;
                screenHandle = CloneFrom[0].screenHandle;
                screenWriter = CloneFrom[0].screenWriter;
            } else {
                rawTerminal = TerminalFacade.createTerminal();
                TerminalSize Tsize = rawTerminal.getTerminalSize();
                Tsize.setColumns(Width=width); // Max 100
                Tsize.setRows(Length=length); //Max 30
                Column = column;
                Row = row;
                screenHandle = TerminalFacade.createScreen(rawTerminal);
                screenHandle.startScreen();
                screenHandle.clear();
                screenHandle.refresh();
                screenWriter = new ScreenWriter(screenHandle);
                screenWriter.setForegroundColor(Terminal.Color.WHITE);
                screenWriter.setBackgroundColor(Terminal.Color.BLACK);
            }
            
        }

        
        public void DisplayString(int col,int row,String LineBuffer, ScreenCharacterStyle... styles) {
            screenWriter.drawString(col, row, LineBuffer,styles);
        }

        public void CursorTo(int col,int row){
            screenHandle.setCursorPosition(col, row);
        }

        public void Refresh() {
            //  for (TerminalWindow iter : WindowStack) {
            screenHandle.refresh();
  //  }
        }
      
        public void Clear() {
            screenHandle.clear();
        }
        
        public Key GetKey () {
            return screenHandle.readInput();
        }
        
        public TerminalSize TerminalSize() {
            return rawTerminal.getTerminalSize();
        }
      
        public TerminalWindow TopWindow() throws SQLException {
            return NamedWindow(Thread.currentThread().getId());
            // return WindowStack.peek();
        }
        
        public TerminalWindow ApropriateWindow(long... OverrideThread) throws SQLException {
            long searcher;
        if ( OverrideThread.length > 0 ) searcher = OverrideThread[0];
        else searcher = Thread.currentThread().getId();
        for (TerminalWindow searchWindow : WindowStack) {
            for (long iter : searchWindow.ThreadPool) {
                if (iter == searcher) {
                    return searchWindow;
                }
            }
        }
        throw new SQLException("Could Not find Apropriate window");
    }
        
        
    /*    public TerminalWindow AproprateWindow(Thread searcher) throws SQLException {
            for (TerminalWindow searchWindow : WindowStack ) {
                for (long iter : searchWindow.ThreadPool) {
                    if (iter == searcher.getId()) return searchWindow ;
                }
            }
            throw new SQLException("Could Not find Apropriate window");
        }*/
        
        public TerminalWindow NamedWindow(long searchfor) throws SQLException {
            for (TerminalWindow searchWindow : WindowStack) {
                for (long iter : searchWindow.ThreadPool) {
                    if (iter == searchfor) return searchWindow;
                }
                
            } 
            throw new SQLException("Could Not find Named window");
        }
    }
    
}     
