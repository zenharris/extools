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
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.lang.Object;
import java.util.Date;
// import java.text.SimpleDateFormat;


// import java.sql.Statement;

public class wdbm implements extools {

    public Terminal rawTerminal;
    public static Screen screenHandle;
    public ScreenWriter screenWriter = null;
    public List<String> FormTemplate = new ArrayList();
    public List<String> FieldList = new ArrayList();
    public List<String> ServerDetails = new ArrayList();
    public List<String> CurrentRecord = new ArrayList();
    public List<String> ScrollingListFields = new ArrayList();
    public String ScrollingListFormat;
    public String ScrollingListDefaultSearchSQL;
    public Connection SQLconnection;
    public ResultSet CurrentRecordResultSet;
    public indexscroll CurrentIndexScroll;
    
    public List<indexscroll> IndexScrolls = new ArrayList();
    public List<String> ErrorLog = new ArrayList();
    public String CurrentErrorBuffer = "";
    
    /**
     *
     */
    public texaco TextEditor;

    public wdbm(String DataDictionaryFilename) throws ClassNotFoundException,SQLException,IOException {
        rawTerminal = TerminalFacade.createTerminal();
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
        TextEditor = new texaco(this);
  
        ReadDataDictionary(Paths.get(DataDictionaryFilename));
        OpenSQLfile();
        IndexScrolls.add(CurrentIndexScroll = new indexscroll("default",ScrollingListDefaultSearchSQL,this,FormTemplate.size()));
        CreateAnyIndexScrollFields();
       
    }
 
    /**
     * Con Job Right On
     */
    public void close() {
     
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
                            FormTemplate.add(line);
                            break;
                        case 1:
                            FieldList.add(line);
                            break;
                        case 2:
                            if(LineNumber == 0){
                                ScrollingListDefaultSearchSQL = line;
                                LineNumber++;
                            } else if (LineNumber == 1) {
                                ScrollingListFormat = line;
                                LineNumber++;
                            }else {
                                ScrollingListFields.add(line);
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
    
    private void CreateAnyIndexScrollFields() throws SQLException {
        for (String Cursor : FieldList) {
            String[] FieldElements = Cursor.split(SplittingColon);
            if (FieldElements[1].equals(IndexScrollFieldLabel)) {
           //     ResolveSQLStatements(Cursor);
           //     String Replacement = CurrentIndexScroll.Results.getString("run");//  roll("runscroll").toString(); //Results.getString("run");
                
                IndexScrolls.add(new indexscroll(FieldElements[0], ResolveSQLStatementIn(Cursor), this, ReturnFieldSpec(FieldElements[0])));
                IndexScroll(FieldElements[0]).ConnectedForm = false;
            }
        }
    }
    
    public void CreateDefaultIndexScroll(int... Dimensions) throws SQLException {
        IndexScrolls.add(CurrentIndexScroll = new indexscroll("default", ScrollingListDefaultSearchSQL, this, FormTemplate.size()));
    }

   
    public int GetFieldNumber(String FieldName) throws SQLException{
       int LineCounter = 0;
        for (String Searcher : FieldList) {
            if (Searcher.split(SplittingColon)[0].equals(FieldName)) {
                return LineCounter;
            }
            LineCounter++;
        }
        throw new SQLException("No Field Named "+FieldName);
    }

    
    public int[] ReturnFieldSpec(String FieldName) throws SQLException {
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
        for (String LocalBuffer : FormTemplate) {
            Matcher m = p.matcher(LocalBuffer);
            while (m.find()) {
                // if (m.find()) {
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
    
    private String ResolveSQLStatementIn (String FormFieldDefinition) throws SQLException{
        String[] FieldElements = FormFieldDefinition.split(SplittingColon);
        String Replacement = CurrentIndexScroll.Results.getString(FieldElements[3]);
        return String.format(FieldElements[2],Replacement);
    }
    
    /**
     *Search Index Scroll Stack to find scroll named in param.
     * Returns it's index scroll object. null if not found.
     * 
     * @param ScrollName    text name of scroll
     * @return
     */
    public indexscroll IndexScroll(String ScrollName) {
        for (indexscroll SearchCursor : IndexScrolls) {
            if (SearchCursor.IndexScrollName.equals(ScrollName)) return SearchCursor;
        }
        return null;
    }

/**
 * Get the number value from the form template field token in the dictionary file and return it.
 * identifies the form field definition that corresponds to this form field item.
 * looks up FieldList to find it.
 * 
 * @param FieldTemplate
 * @return 
 */
    private int ExtractFieldNumberFrom (String FieldTemplate){
        Pattern p = Pattern.compile(REGEXToMatchNumberEmbededInFieldTemplate);
        Matcher m = p.matcher(FieldTemplate);
        if (m.find()) return Integer.parseInt(FieldTemplate.substring(m.start(), m.end())); 
        return 0;
    }
    
    public void FormDisplay(ResultSet LocalResultSet) throws SQLException {
        int iter = 0;
        Pattern p = Pattern.compile(REGEXToMatchEmbededFieldTemplate);
      //  scrn.clear();
        for (String LineBuffer : FormTemplate) {
            Matcher m = p.matcher(LineBuffer);
            while (m.find()) {
                String FieldTemplate = LineBuffer.substring(m.start(), m.end());
                String Field = FieldList.get(ExtractFieldNumberFrom(FieldTemplate));
                String FieldName = Field.split(SplittingColon)[0];
                if (Field.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {

                    IndexScroll(FieldName).ReSearch(ResolveSQLStatementIn(Field));
                    // IndexScroll(FieldName).ReDrawScroll();
                    LineBuffer = "";
                } else {
                    String FieldValue = LocalResultSet.getString(FieldName);// CurrentRecord.get(ExtractFieldNumberFrom(FieldTemplate));
                    if (FieldValue == null)  FieldValue = "";
                    FieldValue = TrimToEditingLength(FieldValue, FieldTemplate);
                    FieldValue = PadToPrintingLength(FieldValue, FieldTemplate);
                    LineBuffer = LineBuffer.replaceFirst(REGEXToMatchEmbededFieldTemplate, FieldValue);
                    
                }
            }
            screenWriter.drawString(0, iter, LineBuffer);
            iter++;
        }
        screenHandle.refresh();
    }

    private String PadToPrintingLength (String FieldValue, String FieldTemplate){
        FieldValue += BLANK.substring(0,FieldTemplate.length()-FieldValue.length());
        return(FieldValue);
    }
    private String TrimToEditingLength (String FieldValue, String FieldTemplate){
                if (FieldValue.length() > FieldTemplate.length()) FieldValue = FieldValue.substring(0, FieldTemplate.length());
                return(FieldValue);
    }
    
    public void FormEditor() throws SQLException{
        int iter = 0;
        Pattern p = Pattern.compile(REGEXToMatchEmbededFieldTemplate);
        for (String LineBuffer : FormTemplate) {
            Matcher m = p.matcher(LineBuffer);
            while (m.find()) {
                String FieldTemplate = LineBuffer.substring(m.start(), m.end());
                String Field = FieldList.get(ExtractFieldNumberFrom(FieldTemplate));
                String FieldName = Field.split(SplittingColon)[0];
                if (Field.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {
                    
                  //  Key ExitKey = IndexScroll(FieldName).DisplayList();

                } else {
                    String FieldValue = CurrentRecord.get(ExtractFieldNumberFrom(FieldTemplate));
                    FieldValue = TrimToEditingLength(FieldValue, FieldTemplate);
                    TextEditor.LineEditorPosition = 0;
                    TextEditor.LineEditor(m.start(), iter, FieldTemplate.length(), FieldValue);
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
        for (String FieldName : FieldList) {
            if (FieldName.split(SplittingColon)[1].equals(IndexScrollFieldLabel)) {
                ////  Ignore index scroll fields, they are not real.
            } else {
                FieldValue = CurrentRecordResultSet.getString(FieldName.split(SplittingColon)[0]);
                if (FieldValue == null) FieldValue = "";              // Strange Processing for when feilds have a null value
                CurrentRecord.add(FieldValue);
            }
        }
    }

    public void DisplayError(String ErrorText) {
        TerminalSize Tsize = rawTerminal.getTerminalSize();
        screenWriter.drawString(0, Tsize.getRows() - 1, BLANK);
        screenWriter.drawString(0, Tsize.getRows() - 1, ErrorText);
        screenHandle.refresh();
        
        if (CurrentErrorBuffer.equals("")) ErrorLog.add(ErrorText);
        CurrentErrorBuffer = ErrorText;
        
    }
    
    public void DisplayPrompt(String Prompt) {
        TerminalSize Tsize = rawTerminal.getTerminalSize();
        screenWriter.drawString(0, Tsize.getRows() - 3, BLANK);
        screenWriter.drawString(0, Tsize.getRows() - 3, Prompt);
        screenWriter.drawString(0, Tsize.getRows() - 2, "?: ");
        screenHandle.setCursorPosition(2, Tsize.getRows() - 2);
    }
    
    public String PromptForString(String Prompt) throws SQLException {
        TerminalSize Tsize = rawTerminal.getTerminalSize();
        screenWriter.drawString(0, Tsize.getRows() - 2, BLANK);
        screenWriter.drawString(0, Tsize.getRows() - 2, Prompt);
        String LocalString = TextEditor.LineEditor(Prompt.length()+1, Tsize.getRows() - 2, 80);
        screenWriter.drawString(0, Tsize.getRows() - 2, BLANK);
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
    public Key KeyInput(String... Prompt) throws SQLException {
        Key KeyReceived;
        int iter = 0;
        Date thisSec;
        int FromCharacter = 0;

        if (Prompt.length > 0) {
            DisplayPrompt(Prompt[0]);
            screenHandle.refresh();
        }
        while ((KeyReceived = screenHandle.readInput()) == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + " sleep failed somehow KeyInput");
            }
            if (iter++ == 1000) {
                thisSec = new Date(); //LocalTime.now();
                // implementation of display code is left to the reader
                //display(thisSec.getHour(), thisSec.getMinute(), thisSec.getSecond());
                TerminalSize Tsize = rawTerminal.getTerminalSize();
                screenWriter.drawString(Tsize.getColumns()-30, 0, thisSec.toString());
                iter = 0;
                screenHandle.refresh();
            }else if (iter == 1 || iter == 200 || iter == 400 || iter == 600 || iter == 800 || iter == 1000 ){
      
                if (CurrentErrorBuffer.length() > 0) {
                    TerminalSize Tsize = rawTerminal.getTerminalSize();
                    screenWriter.drawString(0, Tsize.getRows() - 1, BLANK);
                    screenWriter.drawString(0, Tsize.getRows() - 1, CurrentErrorBuffer.substring(FromCharacter++));
                    screenHandle.refresh();
                    if (FromCharacter + Tsize.getColumns() > CurrentErrorBuffer.length()+5) {
                        FromCharacter = 0;
                    }
                    screenHandle.refresh();
                }

            }
   
            if (screenHandle.resizePending()) {
                if (Prompt.length > 0) {
                    screenHandle.clear();
                    TerminalSize Tsize = rawTerminal.getTerminalSize();
                    if (CurrentIndexScroll.ScreenCurrentRow < 0) CurrentIndexScroll.ScreenCurrentRow = 0;
                    int LocalMaximum = CurrentIndexScroll.ScreenCurrentRow + CurrentIndexScroll.ListScreenTopLine;
                    if (LocalMaximum > Tsize.getRows() - 4)
                        CurrentIndexScroll.ScreenCurrentRow
                                = Tsize.getRows() - 4 - CurrentIndexScroll.ListScreenTopLine;
                    CurrentIndexScroll.ReDrawScroll();
                    CurrentIndexScroll.IlluminateCurrentRow();
                    FormDisplay(CurrentIndexScroll.Results);
                    DisplayPrompt(Prompt[0]);
                    DisplayError(String.format("Term Dimensions %3s col x %-3srow", Tsize.getColumns(), Tsize.getRows()));
                }
                screenHandle.refresh();
            }
        }
        return KeyReceived;
    }

    
    public String FindAnyIndexScrollFields() {
        for (String SearchCursor : FieldList) {
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
     */
    public Key ActivateForm(ResultSet LocalResult) throws SQLException {
        Key ExitedWithKey;
        do {
            FormDisplay(LocalResult);
            String ScrollFieldName = FindAnyIndexScrollFields();
            if (ScrollFieldName != null) {
                ExitedWithKey = IndexScroll(ScrollFieldName).ActivateScroll();
                DisplayPrompt(FormMenuPrompt);
            } else {
                ExitedWithKey = KeyInput(FormMenuPrompt); //"[ESC]Back  [E]dit                             [N]ext [P]rev      [Home]Exit");
            }
            if (ExitedWithKey.getKind() == Key.Kind.NormalKey) {
                if (ExitedWithKey.getCharacter() == 'n' && !LocalResult.isLast()) {
                    LocalResult.next();
                } else if (ExitedWithKey.getCharacter() == 'p' && !LocalResult.isFirst()) {
                    LocalResult.previous();
                } else if (ExitedWithKey.getCharacter() == 'e') {
                    unpackCurrentRecord(LocalResult); //unpackCurrentRecord(StatementResultSet);
                    FormEditor();
                }
            }
        } while (ExitedWithKey.getKind() != Key.Kind.Escape && ExitedWithKey.getKind() != Key.Kind.Home);
        return ExitedWithKey;
    }
     
    public void ActivateWDBM () throws SQLException {
        try {
            while (CurrentIndexScroll.ActivateScroll().getKind() != Key.Kind.Home) {
                if (ActivateForm(CurrentIndexScroll.Results).getKind() == Key.Kind.Home) {
                    return;
                }
            }
        } catch (SQLException ex) {
            DisplayError(ex.getClass().getName() + ": " + ex.getMessage() + " SQLstate " + ex.getSQLState());
            ex.printStackTrace();
        }
        
     //   int LastIndexScroll = IndexScrolls.size() - 1;
     //   if (IndexScrolls.get(LastIndexScroll).Results.first()) 
     //       while (IndexScrolls.get(LastIndexScroll).DisplayList("default").getKind() != Key.Kind.Home
     //               && IndexScrolls.get(LastIndexScroll).AttachedWDBM.DisplayAndEditRecord(IndjavaexScrolls.get(LastIndexScroll).Results).getKind() != Key.Kind.Home) scrn.clear();
    }
    
   
    
}     
