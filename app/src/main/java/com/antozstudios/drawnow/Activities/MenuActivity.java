package com.antozstudios.drawnow.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.R;
import com.antozstudios.drawnow.databinding.ActivityMenuBinding;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class MenuActivity extends AppCompatActivity {

    ActivityMenuBinding binding;
    ProfileManager profileManager;
    PrefManager prefManager;
    ShowHostsActivity showHostsActivity;


    CreateProfileActivity createProfileActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        profileManager = ProfileManager.getInstance(this);
        prefManager = PrefManager.getInstance(this);

        showHostsActivity = new ShowHostsActivity();
createProfileActivity = new CreateProfileActivity();

        prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                .getString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), "");


        binding.connectButton.setOnClickListener((View) -> {
            startActivity(new Intent(this, ShowHostsActivity.class));
        });

        binding.createProfilButton.setOnClickListener(view -> {

            startActivity(new Intent(this,CreateProfileActivity.class));

        });
    }






}
