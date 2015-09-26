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
import java.sql.SQLException;

/**
 *
 * @author harris
 */
public class texaco {
    static String LineEditorBuffer = "";
    static int LineEditorPosition = 0;
    static Key LineEditorReturnKey;
    static wdbm wdbmAttached;
    
    
    public texaco (wdbm AttachedTo) {
        wdbmAttached = AttachedTo;
    }
    
    static void InsertCharacterIntoLineEditorBuffer(char CharacterToInsert) {
        LineEditorBuffer = LineEditorBuffer.substring(0, LineEditorPosition) + CharacterToInsert + LineEditorBuffer.substring(LineEditorPosition); 
    }
    
    static void DeleteCharacterFromLineEditorBuffer(){
        LineEditorBuffer = LineEditorBuffer.substring(0, LineEditorPosition) + LineEditorBuffer.substring(LineEditorPosition + 1);
    }
    
    static void BlankLastCharacterOfFieldBeingEdited(int x,int y) {
        wdbmAttached.writer.drawString(x + LineEditorBuffer.length(), y, " ");
    }
    
    public static String LineEditor(int x, int y, int LengthLimit, String... InitialValue) throws SQLException, InterruptedException {
        Key KeyReceived;
        if (InitialValue.length > 0) LineEditorBuffer = InitialValue[0];
        else LineEditorBuffer = "";
        if (LineEditorPosition > LineEditorBuffer.length()) LineEditorPosition = LineEditorBuffer.length();
        while (true) {
            wdbmAttached.writer.drawString(x, y, LineEditorBuffer);
            wdbmAttached.scrn.setCursorPosition(x + LineEditorPosition, y);
            wdbmAttached.scrn.refresh();
            LineEditorReturnKey = KeyReceived = wdbmAttached.KeyInput();
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
    
}
