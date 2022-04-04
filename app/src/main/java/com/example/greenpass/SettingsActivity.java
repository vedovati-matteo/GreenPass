package com.example.greenpass;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

public class SettingsActivity extends AppCompatPreferenceActivity {

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add toolbar
        getLayoutInflater().inflate(R.layout.toolbar, (ViewGroup)findViewById(android.R.id.content));
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // add back button on the toolbar
        toolbar.setNavigationIcon(getResources().getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // return to main activity
                Intent i=new Intent(getApplicationContext(), MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

        // add preferences
        addPreferencesFromResource(R.xml.preferences);

        // adding space for toolbar
        int horizontalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2, getResources().getDisplayMetrics());
        int verticalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2, getResources().getDisplayMetrics());
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                60, getResources().getDisplayMetrics());
        getListView().setPadding(horizontalMargin, topMargin, horizontalMargin, verticalMargin);


        // loads Shared preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // if not validity, disable other settings
        if (!prefs.getBoolean(Global.S_KEY_validity, true)) {
            getPreferenceScreen().findPreference(Global.S_KEY_test).setEnabled(false);
            getPreferenceScreen().findPreference(Global.S_KEY_2dose).setEnabled(false);
            getPreferenceScreen().findPreference(Global.S_KEY_resetBtn).setEnabled(false);
        }

        // OnChange Listener for validity switch for enable/disable other settings
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(Global.S_KEY_validity)) {
                    if (prefs.getBoolean(key, true)) {
                        getPreferenceScreen().findPreference(Global.S_KEY_test).setEnabled(true);
                        getPreferenceScreen().findPreference(Global.S_KEY_2dose).setEnabled(true);
                        getPreferenceScreen().findPreference(Global.S_KEY_resetBtn).setEnabled(true);
                    } else {
                        getPreferenceScreen().findPreference(Global.S_KEY_test).setEnabled(false);
                        getPreferenceScreen().findPreference(Global.S_KEY_2dose).setEnabled(false);
                        getPreferenceScreen().findPreference(Global.S_KEY_resetBtn).setEnabled(false);
                    }
                }
            }

            ;
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);

    }

    // onClick Reset Values button
    public void resetVals(View view) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Global.S_KEY_test, Integer.toString(Global.DURATION_TEST));
        editor.putString(Global.S_KEY_2dose, Integer.toString(Global.DURATION_DOSE));
        editor.apply();
        finish();
        startActivity(getIntent());
    }

}