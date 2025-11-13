package com.antozstudios.drawnow.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.R;
import com.antozstudios.drawnow.databinding.ProfileSettingsLayoutBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

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
    ArrayAdapter<String> profileAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ProfileSettingsLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        profileManager = ProfileManager.getInstance(this);
        prefManager = PrefManager.getInstance(this);

        List<String> profileNames = profileManager.getAllProfileNames();
        profileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, profileNames);

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
                    .commit();
        }

        profileSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= profileAdapter.getCount()) return;
            String profileName = profileAdapter.getItem(position).toLowerCase();
            prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                    .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), position)
                    .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), profileName)
                    .commit();
            Log.d("CreateProfileActivity", "Profile selected: " + profileName);
        });

        binding.manageProfilesButton.setOnClickListener(this::showManageProfilesMenu);
        binding.importButton.setOnClickListener(v -> handleImport());
        binding.exportButton.setOnClickListener(v -> handleExport());
    }

    private void showManageProfilesMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.manage_main_profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_create_profile) {
                handleCreateProfile();
                return true;
            } else if (itemId == R.id.action_rename_profile) {
                handleRenameProfile();
                return true;
            } else if (itemId == R.id.action_delete_profile) {
                handleDeleteProfile();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void handleCreateProfile() {
        final EditText input = new EditText(this);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create)
                .setView(input)
                .setPositiveButton(R.string.create, (dialog, which) -> {
                    String profileName = input.getText().toString().trim();
                    if (profileName.isEmpty()) {
                        Snackbar.make(binding.getRoot(), "Please enter a profile name.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (profileManager.profileExists(profileName)) {
                        Snackbar.make(binding.getRoot(), "Profile already exists.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    profileManager.createProfile(profileName);
                    profileAdapter.add(profileName);
                    profileAdapter.notifyDataSetChanged();
                    binding.profileSpinner.setText(profileName, false);
                    int idx = profileAdapter.getPosition(profileName);
                    prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                            .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), idx)
                            .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), profileName)
                            .commit();
                    Snackbar.make(binding.getRoot(), "Profile '" + profileName + "' created.", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void handleRenameProfile() {
        String selectedProfile = binding.profileSpinner.getText().toString();
        if (selectedProfile.isEmpty()) {
            Snackbar.make(binding.getRoot(), "No profile selected to rename.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(this);
        input.setText(selectedProfile);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename Profile")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newProfileName = input.getText().toString().trim();
                    if (newProfileName.isEmpty() || newProfileName.equals(selectedProfile)) {
                        return;
                    }
                    if (profileManager.profileExists(newProfileName)) {
                        Snackbar.make(binding.getRoot(), "Profile already exists.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    profileManager.renameProfile(selectedProfile, newProfileName);
                    profileAdapter.remove(selectedProfile);
                    profileAdapter.add(newProfileName);
                    profileAdapter.notifyDataSetChanged();
                    binding.profileSpinner.setText(newProfileName, false);
                    int idx = profileAdapter.getPosition(newProfileName);
                    prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                            .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), idx)
                            .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), newProfileName)
                            .commit();
                    Snackbar.make(binding.getRoot(), "Profile renamed to '" + newProfileName + "'.", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleDeleteProfile() {
        String profileToDelete = binding.profileSpinner.getText().toString();
        if (profileToDelete.isEmpty()) {
            Snackbar.make(binding.getRoot(), "No profile selected to delete.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (!profileManager.profileExists(profileToDelete)) {
            Snackbar.make(binding.getRoot(), "Profile does not exist.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
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
                            .commit();

                    Snackbar.make(binding.getRoot(), "Profile '" + profileToDelete + "' deleted.", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importLauncher.launch(intent);
    }

    private void handleExport() {
        if (profileAdapter.getCount() == 0) {
            Snackbar.make(binding.getRoot(), "No profiles to export.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "profiles.json");
        exportLauncher.launch(intent);
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
                                    Snackbar.make(binding.getRoot(), "Export successful!", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    throw new IOException("OutputStream is null");
                                }
                            } catch (IOException e) {
                                Snackbar.make(binding.getRoot(), "Export failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
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
                                    Snackbar.make(binding.getRoot(), "Import successful!", Snackbar.LENGTH_SHORT).show();

                                    profileAdapter.clear();
                                    profileAdapter.addAll(profileManager.getAllProfileNames());
                                    profileAdapter.notifyDataSetChanged();
                                    binding.profileSpinner.setText("", false);

                                    prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                                            .putString(PrefManager.KeyPref.CURRENT_PROFILE.getKey(), "")
                                            .putInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), -1)
                                            .commit();
                                } else {
                                    Snackbar.make(binding.getRoot(), "Import failed. Invalid file format.", Snackbar.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                Snackbar.make(binding.getRoot(), "Import failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
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
