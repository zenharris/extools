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

// import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
// import com.googlecode.lanterna.terminal.TerminalSize;
import java.io.IOException;
import java.sql.SQLException;
import static org.househarris.extools.extools.BLANK;

/**
 *
 * @author Zen Harris
 */
public class texaco implements extools {
    public String LineEditorBuffer = "";
    public int LineEditorPosition = 0;
    public KeyStroke LineEditorReturnKey;
    public wdbm AttachedWDBM;
    
   public texaco(wdbm Attach) throws IOException,ClassNotFoundException,SQLException {
       AttachedWDBM = Attach;
//        wdbm X = null;
//        super(DataDictionaryFilename);
//        return X;
    }
   
    private void InsertCharacterIntoLineEditorBuffer(char CharacterToInsert) {
        LineEditorBuffer = LineEditorBuffer.substring(0, LineEditorPosition) + CharacterToInsert + LineEditorBuffer.substring(LineEditorPosition); 
    }
    
    private void DeleteCharacterFromLineEditorBuffer(){
        LineEditorBuffer = LineEditorBuffer.substring(0, LineEditorPosition) + LineEditorBuffer.substring(LineEditorPosition + 1);
    }
    
    private void BlankLastCharacterOfFieldBeingEdited(wdbm.TerminalWindow Terminal,int x,int y) throws SQLException,IOException {
        Terminal.DisplayString(x + LineEditorBuffer.length(), y, " ");
    }
    
    /**
     *
     * Inputs text with line editing features
     * 
     * @param x Screen Col
     * @param y Screen Rows
     * @param LengthLimit
     * @param InitialValue
     * @return
     * @throws SQLException
     */
    public String LineEditor(wdbm.TerminalWindow Terminal,int x, int y, int LengthLimit, String... InitialValue) 
            throws SQLException,InterruptedException,IOException {
        KeyStroke KeyReceived;
        if (InitialValue.length > 0) LineEditorBuffer = InitialValue[0];
//        else LineEditorBuffer = "";
        if (LineEditorPosition > LineEditorBuffer.length()) LineEditorPosition = LineEditorBuffer.length();
        while (true) {
            Terminal.DisplayString(x, y, LineEditorBuffer);
        ///    Terminal.screenHandle.setCursorPosition(x + LineEditorPosition, y);
            Terminal.rawTerminal .setCursorPosition(x + LineEditorPosition, y);
            Terminal.screenHandle.refresh();
           LineEditorReturnKey = KeyReceived = AttachedWDBM.KeyInput(Terminal);
        ///    if (KeyReceived.getKind() == Key.Kind.NormalKey && LineEditorBuffer.length() < LengthLimit) {
            if (KeyReceived.getKeyType() == KeyType.Character && LineEditorBuffer.length() < LengthLimit) {
                InsertCharacterIntoLineEditorBuffer(KeyReceived.getCharacter());
                LineEditorPosition++;
            } else if (KeyReceived.getKeyType() == KeyType.Backspace && LineEditorPosition > 0) {
                LineEditorPosition--;
                DeleteCharacterFromLineEditorBuffer();
                BlankLastCharacterOfFieldBeingEdited(Terminal,x, y);
            } else if (KeyReceived.getKeyType() == KeyType.ArrowDown && LineEditorPosition < LineEditorBuffer.length()) {   // using down arrow for delete key
                DeleteCharacterFromLineEditorBuffer();
                BlankLastCharacterOfFieldBeingEdited(Terminal,x, y);
            } else if (KeyReceived.getKeyType() == KeyType.ArrowLeft && LineEditorPosition > 0) {
                LineEditorPosition--;
            } else if (KeyReceived.getKeyType() == KeyType.ArrowRight && LineEditorPosition < LineEditorBuffer.length()) {
                LineEditorPosition++;
            } else if (KeyReceived.getKeyType() == KeyType.Enter || KeyReceived.getKeyType() == KeyType.Escape) {
                return LineEditorBuffer;
            }
        }
    }

    
    @Override
    public indexscroll WithTheIndexScroll(String ScrollName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
/**
    public void ReDrawTextWindow(wdbm.TerminalWindow Terminal,int x, int y, int LengthLimit, String... InitialValue) throws SQLException, InterruptedException {
        wdbm.TerminalWindow UseWindow = Terminal;
;        int TextScreenTopLine = 0;
        int TextScreenLength = 0;
        int ScreenCurrentRow = 0;

        TerminalSize Tsize = UseWindow.TerminalSize();
       
        
        
        
        int startrow = SaveResultRow - ScreenCurrentRow;
        if (startrow < 1) {
            startrow = 1;
            ScreenCurrentRow = SaveResultRow - 1;
        }
        
        
        int iter;
        for (iter = 0; (TextScreenLength == 0 || iter < TextScreenLength) && TextScreenTopLine + iter + 4 <= Tsize.getRows()
                && CurrentSearchAtom.AtomicResultSet.absolute(startrow + iter); iter++) {
            
            // display current line of text
            
            UseWindow.DisplayString(0, TextScreenTopLine + iter, TextString);

        }
        iter--;
        while (iter++ < TextScreenLength - 1) {
            UseWindow.DisplayString(0, TextScreenTopLine + iter, BLANK);
        }
        
    }

*/
    
    
}
    
