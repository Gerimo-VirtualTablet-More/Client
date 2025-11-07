package com.antozstudios.drawnow.Activities;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.databinding.SettingsLayoutBinding;

public class SettingsActivity extends AppCompatActivity {

    PrefManager prefManager;

SettingsLayoutBinding settingsLayoutBinding;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        settingsLayoutBinding= SettingsLayoutBinding.inflate(getLayoutInflater());
        setContentView(settingsLayoutBinding.getRoot());
        prefManager = PrefManager.getInstance(this);


        settingsLayoutBinding.toolbarPositionLeft.setOnClickListener((v -> {

            prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putInt(PrefManager.KeyPref.CURRENT_TOOLBAR_POSITION.getKey(),0).apply();
            v.post(()->{
                Toast.makeText(this, "Toolbar position changed to left", Toast.LENGTH_SHORT).show();
            });

        }));
        settingsLayoutBinding.toolbarPositionRight.setOnClickListener((v)->{
            prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putInt(PrefManager.KeyPref.CURRENT_TOOLBAR_POSITION.getKey(),1).apply();
            v.post(()->{
                Toast.makeText(this, "Toolbar position changed to right", Toast.LENGTH_SHORT).show();
            });
        });



        String savedKey = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getString(PrefManager.KeyPref.API_KEY.getKey(),"");
        settingsLayoutBinding.apiKeyInput.setText(savedKey.isBlank()?"Leer":savedKey);

        settingsLayoutBinding.saveButton.setOnClickListener((view)->{
            String apiKey = settingsLayoutBinding.apiKeyInput.getText().toString();
            PrefManager.getInstance(this).putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putString(PrefManager.KeyPref.API_KEY.getKey(),apiKey).apply();
            view.post(()->{
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            });
   finish();

        });

        settingsLayoutBinding.goToOpenRouter.setOnClickListener((v)->{
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/")));
        });

    }

}
