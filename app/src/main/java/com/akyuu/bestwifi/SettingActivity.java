package com.akyuu.bestwifi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;

import java.util.Locale;

public class SettingActivity extends AppCompatActivity {

    public static final String PREFERENCE_NAME = "config";
    public static final String KEY_STRENGTH = "strength";
    public static final String KEY_DIFFERENCE = "difference";
    public static final String KEY_INTERVAL = "interval";

    SharedPreferences mPreferences;

    EditText mStrengthEditText;
    EditText mDifferenceEditText;
    EditText mIntervalEditText;

    public static Intent newIntent(Context context) {
        return new Intent(context, SettingActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mStrengthEditText = (EditText) findViewById(R.id.et_min_signal);
        mDifferenceEditText = (EditText) findViewById(R.id.et_min_difference);
        mIntervalEditText = (EditText) findViewById(R.id.et_interval);

        Toolbar toolbar = new Toolbar(this);
        setSupportActionBar(toolbar);

        mPreferences = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
        mStrengthEditText.setText(castIntegerToString(mPreferences.getInt(KEY_STRENGTH, 60)));
        mDifferenceEditText.setText(castIntegerToString(mPreferences.getInt(KEY_DIFFERENCE, 5)));
        mIntervalEditText.setText(castIntegerToString(mPreferences.getInt(KEY_INTERVAL, 15)));
    }

    private String castIntegerToString(int i) {
        return String.format(Locale.getDefault(), "%d", i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mStrengthEditText.getText().toString().equals("")) {
            mPreferences.edit()
                    .putInt(KEY_STRENGTH,
                            Integer.valueOf(mStrengthEditText.getText().toString()))
                    .apply();
        }
        if (!mDifferenceEditText.getText().toString().equals("")) {
            mPreferences.edit()
                    .putInt(KEY_DIFFERENCE,
                            Integer.valueOf(mDifferenceEditText.getText().toString()))
                    .apply();
        }
        if (!mIntervalEditText.getText().toString().equals("")) {
            mPreferences.edit()
                    .putInt(KEY_INTERVAL,
                            Integer.valueOf(mIntervalEditText.getText().toString()))
                    .apply();
        }
    }
}
