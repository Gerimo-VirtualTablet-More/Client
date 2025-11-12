package com.antozstudios.drawnow.Manager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResult;
import androidx.appcompat.widget.ResourceManagerInternal;

public class PrefManager {



    Context context;
    private  static PrefManager instance;

    public static enum DataPref {

        SHOW_HOSTS("SHOW_HOSTS"),
        SETTINGS_CONFIG("SETTINGS_CONFIG"),
        PROFILE_CONFIG("PROFILE_CONFIG");

        DataPref(String dataPref) {
            this.dataPref = dataPref;
        }

        private String dataPref;

        public String getDataPref() {
            return dataPref;
        }
    }

    public static enum KeyPref {


        LAST_IP("LAST_IP"),
        LAST_PORT("LAST_PORT"),

        CURRENT_SHORTPROFILE("CURRENT_SHORTPPROFILE",0),

        BACKGROUND("BACKGROUND"),
        THEME("THEME"),
        CURRENT_SCREEN_INDEX("CURRENT_SCREEN_INDEX"),
        MONITOR_DATA("MONITOR_DATA"),
        CURRENT_PROFILE("CURRENT_PROFILE", 1),
        CURRENT_INDEX_PROFILE("CURRENT_INDEX_PROFILE", 2),

        API_KEY("API_KEY"),
        CURRENT_TOOLBAR_POSITION("CURRENT_TOOLBAR_POSITION");



        private final String key;
        private final int index;

        KeyPref(String key, int index) {
            this.key = key;
            this.index = index;
        }

        KeyPref(String key) {
            this.key = key;
            index = 0;
        }



        public String getKey() {
            return key;
        }


    }


    private PrefManager(Context context){this.context = context;}

    public static PrefManager getInstance(Context context){

        if (instance == null) {

            instance = new PrefManager(context);

        }
        return instance;



    }

    public SharedPreferences getDataPref(DataPref dataPref){
        return context.getSharedPreferences(dataPref.getDataPref(), Context.MODE_PRIVATE);

    }
    public SharedPreferences.Editor putDataPref(DataPref dataPref){
        return context.getSharedPreferences(dataPref.getDataPref(), Context.MODE_PRIVATE).edit();
    }










}
