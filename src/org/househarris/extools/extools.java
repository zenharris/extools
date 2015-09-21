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
import java.sql.Statement;

public class extools {

    public static String LineEditorBuffer = "";
    public static int LineEditorPosition = 0;
    public static Key LineEditorReturnKey = null;
    public static Terminal terminal;
    public static Screen scrn;
    public static ScreenWriter writer;
    public static List<String> FormTemplate = new ArrayList();
    public static List<String> FieldList = new ArrayList();
   // public static List<String> ListList = new ArrayList();
    public static List<String> ServerDetails = new ArrayList();
    public static List<String> CurrentRecord = new ArrayList();
    public static List<String> ScrollingListFields = new ArrayList();
    public static String ScrollingListFormat;
    public static Connection SQLconnection;
    public static ResultSet CurrentRecordResultSet;
    
    static final String BLANK = "                                                                                                                             ";


    // The following regular expressions find the field templates embeded in the FormTemplate list
    // Field Templates Look Like @1<<<<<<<<<<     @20<<<<<
    // The Numbers being the link to the name of the field from the SQL database in the FieldList list
    // These having been loaded in from the WDBM common data dictionary file 
    // set up for the SQL database being edited.
    
    public static final String REGEXToMatchEmbededFieldTemplate = "@\\d+<+"; 
    public static final String REGEXToMatchNumberEmbededInFieldTemplate = "\\d+";

    public extools(String DataDictionaryFilename) {
        terminal = TerminalFacade.createTerminal();
        scrn = TerminalFacade.createScreen(terminal);
        scrn.startScreen();
        scrn.clear();
        scrn.refresh();
        writer = new ScreenWriter(scrn);
        writer.setForegroundColor(Terminal.Color.WHITE);
        writer.setBackgroundColor(Terminal.Color.BLACK);
 
        try {
            ReadDataDictionary(Paths.get(DataDictionaryFilename));
            OpenSQLfile();
        } catch (IOException ErrorMessage) {
            System.err.println(ErrorMessage);
            System.exit(0);
        }
    }

    public static void OpenSQLfile() {
        try {
            Class.forName("org.postgresql.Driver");
            SQLconnection = DriverManager.getConnection(ServerDetails.get(ServerDetails.size()-1),"postgres",""); // "jdbc:postgresql://10.8.0.1:5432/sewer","postgres", "");
            SQLconnection.setAutoCommit(false);
            System.out.println("Opened database successfully");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
    
 
    public static void ReadDataDictionary(Path PathOfFile) throws IOException {
        try (InputStream in = Files.newInputStream(PathOfFile); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            int FieldSwitch = 0;
            boolean ScrollingListFirstFlag = true;
            while ((line = reader.readLine()) != null) {
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
                            if(ScrollingListFirstFlag){
                                ScrollingListFormat = line;
                                ScrollingListFirstFlag = false;
                            } else ScrollingListFields.add(line);
                            break;
                        case 3:
                            ServerDetails.add(line);
                            break;
                    }
                }
            }
        }
    }
    
    static int ExtractFieldNumberFrom (String FieldTemplate){
        Pattern p = Pattern.compile(REGEXToMatchNumberEmbededInFieldTemplate);
        Matcher m = p.matcher(FieldTemplate);
        if (m.find()) return Integer.parseInt(FieldTemplate.substring(m.start(), m.end())); 
        return 0;
    }
    
    public static void FormDisplay() throws SQLException {
        int iter=0;
        Pattern p = Pattern.compile(REGEXToMatchEmbededFieldTemplate);
        scrn.clear();
        for (String LineBuffer : FormTemplate) {
            Matcher m = p.matcher(LineBuffer);
            while (m.find()) {
                String FieldTemplate = LineBuffer.substring(m.start(), m.end());
                String FieldValue = CurrentRecordResultSet.getString(FieldList.get(ExtractFieldNumberFrom(FieldTemplate)).split(":")[0]);// CurrentRecord.get(ExtractFieldNumberFrom(FieldTemplate));
                if (FieldValue == null) FieldValue = "";
                FieldValue = TrimToEditingLength(FieldValue, FieldTemplate);
                FieldValue = PadToPrintingLength(FieldValue, FieldTemplate);
                LineBuffer = LineBuffer.replaceFirst(REGEXToMatchEmbededFieldTemplate, FieldValue);
            }
            writer.drawString(0, iter, LineBuffer);
            iter++;
        }
        scrn.refresh();
    }

    static String PadToPrintingLength (String FieldValue, String FieldTemplate){
        FieldValue += BLANK.substring(0,FieldTemplate.length()-FieldValue.length());
        return(FieldValue);
    }
    static String TrimToEditingLength (String FieldValue, String FieldTemplate){
                if (FieldValue.length() > FieldTemplate.length()) FieldValue = FieldValue.substring(0, FieldTemplate.length());
                return(FieldValue);
    }
    
    public static void FormEditor() throws InterruptedException {
        int iter = 0;
        Pattern p = Pattern.compile(REGEXToMatchEmbededFieldTemplate);
        for (String LineBuffer : FormTemplate) {
            Matcher m = p.matcher(LineBuffer);
            while (m.find()) {
                String FieldTemplate = LineBuffer.substring(m.start(), m.end());
                String FieldValue = CurrentRecord.get(ExtractFieldNumberFrom(FieldTemplate));
                FieldValue = TrimToEditingLength(FieldValue, FieldTemplate);
                LineEditorPosition = 0;
                LineEditor(m.start(), iter, FieldTemplate.length(), FieldValue);
                if (LineEditorReturnKey.getKind() == Key.Kind.Escape) return;
            }
            iter++;
        }
    }

    static void InsertCharacterIntoLineEditorBuffer(char CharacterToInsert) {
        LineEditorBuffer = LineEditorBuffer.substring(0, LineEditorPosition) + CharacterToInsert + LineEditorBuffer.substring(LineEditorPosition); 
    }
    
    static void DeleteCharacterFromLineEditorBuffer(){
        LineEditorBuffer = LineEditorBuffer.substring(0, LineEditorPosition) + LineEditorBuffer.substring(LineEditorPosition + 1);
    }
    
    static void BlankLastCharacterOfFieldBeingEdited(int x,int y) {
        writer.drawString(x + LineEditorBuffer.length(), y, " ");
    }
    
    public static String LineEditor(int x, int y, int LengthLimit, String... InitialValue) throws InterruptedException {
        Key KeyReceived;
        if (InitialValue.length > 0) LineEditorBuffer = InitialValue[0];
        else LineEditorBuffer = "";
        if (LineEditorPosition > LineEditorBuffer.length()) LineEditorPosition = LineEditorBuffer.length();
        while (true) {
            writer.drawString(x, y, LineEditorBuffer);
            scrn.setCursorPosition(x + LineEditorPosition, y);
            scrn.refresh();
            LineEditorReturnKey = KeyReceived = KeyInput();
            if (KeyReceived.getKind() == Key.Kind.NormalKey && LineEditorBuffer.length() < LengthLimit) {
                InsertCharacterIntoLineEditorBuffer(KeyReceived.getCharacter());
                LineEditorPosition++;
            } else if (KeyReceived.getKind() == Key.Kind.Backspace && LineEditorPosition > 0) {
                LineEditorPosition--;
                DeleteCharacterFromLineEditorBuffer();
                BlankLastCharacterOfFieldBeingEdited(x, y);
            } else if (KeyReceived.getKind() == Key.Kind.ArrowDown && LineEditorPosition < LineEditorBuffer.length()) {   // using down arrow for delete key
                DeleteCharacterFromLineEditorBuffer();
                BlankLastCharacterOfFieldBeingEdited(x, y);
            } else if (KeyReceived.getKind() == Key.Kind.ArrowLeft && LineEditorPosition > 0) {
                LineEditorPosition--;
            } else if (KeyReceived.getKind() == Key.Kind.ArrowRight && LineEditorPosition < LineEditorBuffer.length()) {
                LineEditorPosition++;
            } else if (KeyReceived.getKind() == Key.Kind.Enter || KeyReceived.getKind() == Key.Kind.Escape) {
                return LineEditorBuffer;
            }
        }
    }
 
    
    public static void unpackCurrentRecord(ResultSet... SetThisAsCurrent) throws SQLException {
        String FieldValue;
        if (SetThisAsCurrent.length > 0) CurrentRecordResultSet = SetThisAsCurrent[0];
        CurrentRecord.clear();
        for (String FieldName : FieldList) {
            FieldValue = CurrentRecordResultSet.getString(FieldName.split(":")[0]);
            if (FieldValue == null) FieldValue = "";              // Strange Processing for when feilds have a null value
            CurrentRecord.add(FieldValue);
        }
    }
   
    /**
     *
     * @param Prompt
     * @return
     * @throws InterruptedException
     */
    public static Key KeyInput(String... Prompt) throws InterruptedException {
        Key KeyReceived;
        if (Prompt.length > 0) {
            TerminalSize Tsize = terminal.getTerminalSize();
            writer.drawString(0, Tsize.getRows()-3, BLANK);
            writer.drawString(0, Tsize.getRows()-3, Prompt[0]);
            writer.drawString(0, Tsize.getRows()-2, "?: ");
            scrn.setCursorPosition(2, Tsize.getRows()-2);
            scrn.refresh();
        }
        while ((KeyReceived = scrn.readInput()) == null) {
                Thread.sleep(3);
        }
        return KeyReceived;
    }
    
    static Key DisplayAndEditRecord() throws SQLException, InterruptedException {
        Key ExitedWithKey;
        do {
            FormDisplay();
            ExitedWithKey = KeyInput("[ESC]Back  [E]dit                             [N]ext [P]rev      [Home]Exit");
            if (ExitedWithKey.getKind() == Key.Kind.NormalKey) {
                if (ExitedWithKey.getCharacter() == 'n' && !CurrentRecordResultSet.isLast()) {
                    CurrentRecordResultSet.next();
                } else if (ExitedWithKey.getCharacter() == 'p' && !CurrentRecordResultSet.isFirst()) {
                    CurrentRecordResultSet.previous();
                } else if (ExitedWithKey.getCharacter() == 'e') {
                    unpackCurrentRecord(); //unpackCurrentRecord(StatementResultSet);
                    FormEditor();
                }
            }
        } while (ExitedWithKey.getKind() != Key.Kind.Escape && ExitedWithKey.getKind() != Key.Kind.Home);
        return ExitedWithKey;
    }
  
    static List<String> ValuesSubstitute(List<String> FieldNameList) throws SQLException {
        int iter = 0;
        List<String> Substitute = new ArrayList();
        for(String FieldName : FieldNameList ){
            Substitute.add(CurrentRecordResultSet.getString(FieldName));
        }
        return Substitute;
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
    
    public static void listdriver(String SQLQuery) {
        try (Statement stmt = SQLconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet RSet = stmt.executeQuery(SQLQuery)) {
            CurrentRecordResultSet = RSet; //CurrentRecordResultSet = StatementResultSet;
            if (CurrentRecordResultSet.first()) while (DisplayList().getKind() != Key.Kind.Home && DisplayAndEditRecord().getKind() != Key.Kind.Home) scrn.clear();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage() + "Zen");
            //    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            // System.exit(0);
        }
    }
}     
