package com.antozstudios.gerimo.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.antozstudios.gerimo.Helper.HelperClass;
import com.antozstudios.gerimo.Helper.Records.ScreenData;
import com.antozstudios.gerimo.Manager.PrefManager;
import com.antozstudios.gerimo.R;
import com.antozstudios.gerimo.databinding.SettingsLayoutBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    PrefManager prefManager;

    SettingsLayoutBinding settingsLayoutBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefManager = PrefManager.getInstance(this);

        settingsLayoutBinding = SettingsLayoutBinding.inflate(getLayoutInflater());
        setContentView(settingsLayoutBinding.getRoot());

        loadInitialUiState();

        setupScreenSelection();
        setupApiKeyInput();
        setupToolbarPositionToggle();
        setupThemeToggle();

        settingsLayoutBinding.goToOpenRouter.setOnClickListener((v) -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/")));
        });
    }

    private void loadInitialUiState() {
        int theme = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getInt(PrefManager.KeyPref.THEME.getKey(), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        updateThemeToggle(theme);
    }

    private void setupScreenSelection() {
        String tempRecieve = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getString(PrefManager.KeyPref.MONITOR_DATA.getKey(), "");
        if (!tempRecieve.isBlank()) {
            ArrayList<ScreenData> screenDataArrayList = HelperClass.getAllScreenData(tempRecieve);
            for (int i = 0; i < screenDataArrayList.size(); i++) {
                final int currentIndex = i;
                final ScreenData s = screenDataArrayList.get(i);

                MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                button.setText((s.isPrimary() ? "Primary" : "") + " " + s.workareaWidth() + "x" + s.workareaHeight());
                button.setOnClickListener((v) -> {
                    prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putInt(PrefManager.KeyPref.CURRENT_SCREEN_INDEX.getKey(), currentIndex).commit();
                    Snackbar.make(settingsLayoutBinding.getRoot(), "Selected screen: " + s.deviceName(), Snackbar.LENGTH_SHORT).show();
                });
                settingsLayoutBinding.screenSelectionToggleGroup.addView(button);
            }
        }
    }

    private void setupApiKeyInput() {
        String savedKey = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getString(PrefManager.KeyPref.API_KEY.getKey(), "");
        settingsLayoutBinding.apiKeyInput.setText(savedKey.isBlank() ? "" : savedKey);

        settingsLayoutBinding.apiKeyInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String apiKey = settingsLayoutBinding.apiKeyInput.getText().toString();
                prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putString(PrefManager.KeyPref.API_KEY.getKey(), apiKey).commit();
                return false;
            }
        });
    }

    private void setupToolbarPositionToggle() {
        String toolbarPosition = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getString(PrefManager.KeyPref.CURRENT_TOOLBAR_POSITION.getKey(), "LEFT");
        if (toolbarPosition.equals("LEFT")) {
            settingsLayoutBinding.toolbarPositionToggleGroup.check(R.id.toolbar_position_left);
        } else {
            settingsLayoutBinding.toolbarPositionToggleGroup.check(R.id.toolbar_position_right);
        }

        settingsLayoutBinding.toolbarPositionToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.toolbar_position_left) {
                    prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putString(PrefManager.KeyPref.CURRENT_TOOLBAR_POSITION.getKey(), "LEFT").commit();
                    Snackbar.make(this,settingsLayoutBinding.getRoot(),"Position updated",Snackbar.LENGTH_SHORT).show();

                } else if (checkedId == R.id.toolbar_position_right) {
                    prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putString(PrefManager.KeyPref.CURRENT_TOOLBAR_POSITION.getKey(), "RIGHT").commit();
                    Snackbar.make(this,settingsLayoutBinding.getRoot(),"Position updated",Snackbar.LENGTH_SHORT).show();

                }

            }
        });
    }

    private void setupThemeToggle() {
        settingsLayoutBinding.themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            String chosenTheme = "";
            if (isChecked) {
                int theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                if (checkedId == R.id.theme_light) {
                    theme = AppCompatDelegate.MODE_NIGHT_NO;
                    chosenTheme = "LIGHT";
                } else if (checkedId == R.id.theme_dark) {
                    theme = AppCompatDelegate.MODE_NIGHT_YES;
                    chosenTheme = "DARK";

                }else{
                    chosenTheme = "SYSTEM";
                }
                AppCompatDelegate.setDefaultNightMode(theme);
                Snackbar.make(this,settingsLayoutBinding.getRoot(),"Set theme to " + chosenTheme,Snackbar.LENGTH_SHORT).show();
                prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putInt(PrefManager.KeyPref.THEME.getKey(), theme).commit();
                recreate();
            }
        });
    }

    private void updateThemeToggle(int theme) {
        if (theme == AppCompatDelegate.MODE_NIGHT_NO) {
            settingsLayoutBinding.themeToggleGroup.check(R.id.theme_light);
        } else if (theme == AppCompatDelegate.MODE_NIGHT_YES) {
            settingsLayoutBinding.themeToggleGroup.check(R.id.theme_dark);
        } else {
            settingsLayoutBinding.themeToggleGroup.check(R.id.theme_system);
        }
    }
}
