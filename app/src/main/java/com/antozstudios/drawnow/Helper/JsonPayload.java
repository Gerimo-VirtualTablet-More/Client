package com.antozstudios.drawnow.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonPayload {

    public static String buildJsonPayload(String prompt,String modelName) throws JSONException {
        // System-Nachricht mit Anweisungen
        String systemContent = "You are Gerimo AI, a Windows 11 shortcut generator. " +
                "Output ONLY a single JSON object (NOT an array). " +
                "The root must be an object with program names as keys, not an array of objects. " +
                "Correct format: {\"Program_Name\":{\"Shortcut_1\":\"LControl;C\",\"Shortcut_2\":\"LControl;V\"}}. " +
                "WRONG format: [{\"Program_Name\":{...}}]. " +
                "No comments, explanations, markdown formatting, or extra fields. Maximum 15 shortcuts/sequences. " +
                "Use official C# KeyCodes from System.Windows.Forms.Keys (hex format). " +
                "Modifiers: LControl=0xA2, RControl=0xA3, LShift=0xA0, RShift=0xA1, LAlt=0xA4, RAlt=0xA5, LWin=0x5B, RWin=0x5C. " +
                "Letters: A-Z=0x41-0x5A. Digits: D0-D9=0x30-0x39, NumPad0-9=0x60-0x69. " +
                "Function: F1-F24=0x70-0x87. Navigation: Left=0x25, Up=0x26, Right=0x27, Down=0x28, Home=0x24, End=0x23, PageUp=0x21, PageDown=0x22. " +
                "Special: Enter=0xD, Backspace=8, Tab=9, Escape=0x1B, Space=0x20, Delete=0x2E, Insert=0x2D. " +
                "OEM: OemSemicolon=0xBA, Oemplus=0xBB, Oemcomma=0xBC, OemMinus=0xBD, OemPeriod=0xBE, OemQuestion=0xBF, Oemtilde=0xC0, OemOpenBrackets=0xDB, OemPipe=0xDC, OemCloseBrackets=0xDD, OemQuotes=0xDE, OemBackslash=0xE2. " +
                "Media: VolumeMute=0xAD, VolumeDown=0xAE, VolumeUp=0xAF, MediaPlayPause=0xB3, MediaNextTrack=0xB0, MediaPreviousTrack=0xB1, MediaStop=0xB2. " +
                "Browser: BrowserBack=0xA6, BrowserForward=0xA7, BrowserRefresh=0xA8, BrowserHome=0xAC. " +
                "Other: PrintScreen=0x2C, NumLock=0x90, CapsLock=0x14, Scroll=0x91.";


        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemContent);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);


        JSONArray messages = new JSONArray();
        messages.put(systemMessage);
        messages.put(userMessage);


        JSONObject payload = new JSONObject();
        payload.put("model", modelName);  // Korrekte Modellbezeichnung ohne Leerzeichen
        payload.put("response_format", new JSONObject().put("type", "json_object"));
        payload.put("messages", messages);

        // JSON-String zurückgeben
        return payload.toString(2); // Formatiert mit Einrückungen zum besseren Lesen
    }
}
