package com.antozstudios.gerimo.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonPayload {

    public static String buildJsonPayload(String prompt,String modelName) throws JSONException {
        // System-Nachricht mit Anweisungen
        String systemContent = "You are Gerimo AI, a Windows 11 shortcut generator. " +
                "Output ONLY a single JSON object (NOT an array). " +
                "The root must be an object with program names as keys, not an array of objects. " +
                "Correct format: {\"Program_Name\":{\"Name_of_Shortcut_1\":\"LControl;C\",\"Name_of_Shortcut_2\":\"LControl;A;LControl;X\"}}. " +
                "WRONG format: [{\"Program_Name\":{...}}]. " +
                "No comments, explanations, markdown formatting, or extra fields. Maximum 15 shortcuts/sequences. " +
                "You MUST use the exact key names from the list below. Do NOT use hex codes. Key names are case-sensitive. " +
                "Separate keys in a shortcut combination with a semicolon (;). You can also combine multiple shortcuts for example 'LControl;A;LControl;X' for 'Select All' followed by 'Cut'. " +
                "List of allowed key names: " +
                "None, LButton, RButton, Cancel, MButton, XButton1, XButton2, Backspace, Tab, LineFeed, Clear, Return, Enter, Shift, Control, Alt, Menu, Pause, Capital, CapsLock, KanaMode, HanguelMode, HangulMode, JunjaMode, FinalMode, HanjaMode, KanjiMode, Escape, IMEConvert, IMENonconvert, IMEAccept, IMEAceept, IMEModeChange, Space, PageUp, Prior, PageDown, Next, End, Home, Left, Up, Right, Down, Select, Print, Execute, PrintScreen, Snapshot, Insert, Delete, Help, D0, D1, D2, D3, D4, D5, D6, D7, D8, D9, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, LWin, RWin, Apps, Sleep, NumPad0, NumPad1, NumPad2, NumPad3, NumPad4, NumPad5, NumPad6, NumPad7, NumPad8, NumPad9, Multiply, Add, Separator, Subtract, Decimal, Divide, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, NumLock, Scroll, LShift, LShiftKey, RShift, RShiftKey, LControl, LControlKey, RControl, RControlKey, LAlt, LMenu, RMenu, RAlt, BrowserBack, BrowserForward, BrowserRefresh, BrowserStop, BrowserSearch, BrowserFavorites, BrowserHome, VolumeMute, VolumeDown, VolumeUp, MediaNextTrack, MediaPreviousTrack, MediaStop, MediaPlayPause, LaunchMail, SelectMedia, LaunchApplication1, LaunchApplication2, OemSemicolon, Oem1, Oemplus, Oemcomma, OemMinus, OemPeriod, OemQuestion, Oem2, Oemtilde, Oem3, OemOpenBrackets, Oem4, OemPipe, Oem5, OemCloseBrackets, Oem6, OemQuotes, Oem7, Oem8, OemBackslash, Oem102, ProcessKey, Packet, Attn, Crsel, Exsel, EraseEof, Play, Zoom, NoName, Pa1, OemClear";


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
