package com.antozstudios.drawnow.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;



import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.databinding.ProfileSettingsLayoutBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class CreateProfileActivity extends AppCompatActivity {

    ProfileManager profileManager;
    PrefManager prefManager;

    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    ProfileSettingsLayoutBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ProfileSettingsLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        profileManager = ProfileManager.getInstance(this);
        prefManager = PrefManager.getInstance(this);


        List<String> profileNames = profileManager.getAllProfileNames();
        ArrayAdapter<String> profileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, profileNames);

        AutoCompleteTextView profileSpinner = binding.profileSpinner;

        profileSpinner.setAdapter(profileAdapter);

        registerActivityResults();

        SharedPreferences prefs = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG);
        String savedProfile = prefs.getString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), "");
        if (!savedProfile.isEmpty() && profileAdapter.getPosition(savedProfile) >= 0) {
            profileSpinner.setText(savedProfile, false);
        } else {
            prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                    .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), "")
                    .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), -1)
                    .apply();
        }

        profileSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= profileAdapter.getCount()) return;
            String profileName = profileAdapter.getItem(position);
            prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                    .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), position)
                    .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), profileName)
                    .apply();
            Log.d("CreateProfileActivity", "Profile selected: " + profileName);
        });

        binding.createButton.setOnClickListener(v -> {
            String profileName = Objects.toString(binding.textInputEditText.getText(), "").trim();
            if (profileName.isEmpty()) {
                Toast.makeText(this, "Please enter a profile name.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (profileManager.profileExists(profileName)) {
                Toast.makeText(this, "Profile already exists.", Toast.LENGTH_SHORT).show();
                return;
            }
            profileManager.createProfile(profileName);
            profileAdapter.add(profileName);
            profileAdapter.notifyDataSetChanged();
            binding.textInputEditText.setText("");

            profileSpinner.setText(profileName, false);
            int idx = profileAdapter.getPosition(profileName);
            prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                    .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), idx)
                    .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), profileName)
                    .apply();

            Toast.makeText(this, "Profile '" + profileName + "' created.", Toast.LENGTH_SHORT).show();
        });

        binding.deleteButton.setOnClickListener(v -> {
            String profileToDelete = binding.profileSpinner.getText().toString();
            if (profileToDelete.isEmpty()) {
                Toast.makeText(this, "No profile selected to delete.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!profileManager.profileExists(profileToDelete)) {
                Toast.makeText(this, "Profile does not exist.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete Profile")
                    .setMessage("Are you sure you want to delete the profile '" + profileToDelete + "'?")
                    .setPositiveButton("Delete", (dialogInterface, i) -> {
                        profileManager.deleteProfile(profileToDelete);
                        profileAdapter.remove(profileToDelete);
                        profileAdapter.notifyDataSetChanged();
                        binding.profileSpinner.setText("", false);

                        prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                                .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), "")
                                .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), -1)
                                .apply();

                        Toast.makeText(this, "Profile '" + profileToDelete + "' deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        binding.exportButton.setOnClickListener(v -> {
            if (profileAdapter.getCount() == 0) {
                Toast.makeText(this, "No profiles to export.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "profiles.json");
            exportLauncher.launch(intent);
        });

        binding.importButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            importLauncher.launch(intent);
        });




    }
    private void registerActivityResults() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String jsonToExport = profileManager.exportProfiles();
                            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                                if (outputStream != null) {
                                    outputStream.write(jsonToExport.getBytes());
                                    Toast.makeText(this, "Export successful!", Toast.LENGTH_SHORT).show();
                                } else {
                                    throw new IOException("OutputStream is null");
                                }
                            } catch (IOException e) {
                                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("ExportProfile", "File write failed", e);
                            }
                        }
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                String jsonContent = readTextFromUri(uri);
                                if (profileManager.importProfiles(jsonContent)) {
                                    Toast.makeText(this, "Import successful!", Toast.LENGTH_SHORT).show();

                                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.profileSpinner.getAdapter();
                                    adapter.clear();
                                    adapter.addAll(profileManager.getAllProfileNames());
                                    adapter.notifyDataSetChanged();
                                    binding.profileSpinner.setText("", false);

                                    prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                                            .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), "")
                                            .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), -1)
                                            .apply();
                                } else {
                                    Toast.makeText(this, "Import failed. Invalid file format.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("ImportProfile", "File read failed", e);
                            }
                        }
                    }
                });
    }
    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

}
