package com.seblogapps.stognacci.translator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by stognacci on 11/05/2016.
 */
public class SharedPreferencesUtils {

    public SharedPreferences mSharedPreferences;
    public Context mContext;

    public SharedPreferencesUtils(Context context) {
        mContext = context;
        mSharedPreferences = context.getSharedPreferences(Constants.SPEECH_TO_TEXT_PREFERENCES, Context.MODE_PRIVATE);
    }

    public int getSpeechModeIndex() {
        return mSharedPreferences.getInt(Constants.SPEECH_MODE_INDEX, 0);
    }

    public void updateSpeechModeIndex(int speechModeIndex) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Constants.SPEECH_MODE_INDEX, speechModeIndex);
        editor.apply();
    }

    public int getBaseLanguageIndex() {
        return mSharedPreferences.getInt(Constants.BASE_LANGUAGE_INDEX, 0);
    }

    public void updateBaseLanguageIndex(int languageIndex) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Constants.BASE_LANGUAGE_INDEX, languageIndex);
        editor.apply();
    }

    public int getConvertLanguageIndex() {
        return mSharedPreferences.getInt(Constants.CONVERT_LANGUAGE_INDEX, 0);
    }

    public void updateConvertLanguageIndex(int languageIndex) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Constants.CONVERT_LANGUAGE_INDEX, languageIndex);
        editor.apply();
    }
}
