package com.antozstudios.drawnow;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.util.JsonWriter;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class ProfileManager {



  private final Context context;



    private final String fileName = "Profiles.json";

    private static ProfileManager instance;



    public static ProfileManager getInstance(AppCompatActivity appCompatActivity){
        if(instance==null){
            instance = new ProfileManager(appCompatActivity);
        }
        return instance;
    }

    private  ProfileManager(Context context){
        this.context = context;
    }


    public void init(){
        try (FileOutputStream fileOutputStream =
                     context.openFileOutput(fileName, Context.MODE_PRIVATE);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
             JsonWriter jsonWriter = new JsonWriter(outputStreamWriter)) {


            jsonWriter.beginObject();
            jsonWriter.name("profiles").beginObject();
            jsonWriter.endObject();
            jsonWriter.endObject();
            jsonWriter.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }






    }

    public void addCommandToProfile(String profileName,String shortCutProfile, String commandName,String[] commands) {
        JSONObject jsonObject;
        StringBuilder stringBuilder = new StringBuilder();

        for(int i =0;i<commands.length;i++){
            if(i<commands.length-1) {
                stringBuilder.append(commands[i]).append(";");

            }else{
                stringBuilder.append(commands[i]);
            }

        }


        try {
             jsonObject = new JSONObject(readFile().toString());
            jsonObject.getJSONObject("profiles").getJSONObject(profileName).getJSONObject("shortCutProfiles").getJSONObject(shortCutProfile).put(commandName,stringBuilder);

            try (FileOutputStream fos = context.openFileOutput(fileName,MODE_PRIVATE)){
                try {
                    fos.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }


        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }



    }


    public JSONObject createProfile(String name) {
        JSONObject readData;
        try {
             readData = new JSONObject(readFile().toString());
            readData.getJSONObject("profiles").put(name,new JSONObject().put("shortCutProfiles",new JSONObject()));


        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try(FileOutputStream fos = context.openFileOutput(fileName,MODE_PRIVATE)){
            fos.write(readData.toString().getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return readData;

    }



    public void createShortCutProfile(String profileName, String shortCutProfileName){
        JSONObject jsonObject;
        try {
             jsonObject = new JSONObject(readFile().toString());
             jsonObject.getJSONObject("profiles").getJSONObject(profileName).getJSONObject("shortCutProfiles").put(shortCutProfileName,new JSONObject());

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        try (FileOutputStream fos = context.openFileOutput(fileName,MODE_PRIVATE)){
            fos.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public StringBuilder readFile(){
        StringBuilder stringBuilder = new StringBuilder();

        try (FileInputStream fileInputStream = context.openFileInput(fileName)){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            String line;
            while((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stringBuilder;

    }









}
