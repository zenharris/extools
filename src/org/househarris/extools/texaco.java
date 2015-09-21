/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.househarris.extools;

import com.googlecode.lanterna.input.Key;

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
    
    public static String LineEditor(int x, int y, int LengthLimit, String... InitialValue) throws InterruptedException {
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
