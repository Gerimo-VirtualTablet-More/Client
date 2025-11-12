package com.antozstudios.drawnow.Helper;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class HelperMethods {

    public enum SET_ACTION {
        HOVER,
        CLICK,
        PINCH,
        HOTKEY
    }
    public enum GET_ACTION {
        RESOLUTION
    }

    public static void setUI(AppCompatActivity appCompatActivity) {
        appCompatActivity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    public static String sendData(SET_ACTION action, int x, int y, float pressure) {
        return getActionString(action) + ";" + x + ";" + y + ";" + pressure;
    }

    public static String sendData(SET_ACTION action, int x, int y, float pressure,int tiltX,int tiltY) {
        return getActionString(action) + ";" + x + ";" + y + ";" + pressure + ";" + tiltX + ";" + tiltY;
    }

    public static String sendData(SET_ACTION action, int pointerX1, int pointerX2, int pointerY1, int pointerY2) {
        return getActionString(action) + ";" + pointerX1 + ";" + pointerX2 + ";" + pointerY1 + ";" + pointerY2;
    }
    public static String sendEnquiry(GET_ACTION action){
        return getActionString(action);
    }

    public static String sendData(SET_ACTION action, KeyHelper.KeyCode[] msg) {
        StringBuilder mBuilder = new StringBuilder();
        for (int i = 0; i < msg.length; i++) {
            mBuilder.append(";");
            mBuilder.append(msg[i].getCode());
        }
        return getActionString(action) + mBuilder.toString();
    }

    private static String getActionString(SET_ACTION action) {
        return action.toString();
    }
    private static String getActionString(GET_ACTION action) {
        return action.toString();
    }


    public static StringBuilder GetSplitedString(char splitChar, List<String> names) {
        names = new ArrayList<>();
        
        StringBuilder a = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i < names.size() - 1) {
                a.append(names.get(i)).append(splitChar);
            } else {
                a.append(names.get(i));
            }

        }
        return a;

    }
}



