package com.seblogapps.stognacci.translator;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.microsoft.projectoxford.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.projectoxford.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.projectoxford.speechrecognition.RecognitionResult;
import com.microsoft.projectoxford.speechrecognition.RecognitionStatus;
import com.microsoft.projectoxford.speechrecognition.SpeechRecognitionMode;
import com.microsoft.projectoxford.speechrecognition.SpeechRecognitionServiceFactory;
import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;

public class MainActivity extends AppCompatActivity
        implements ISpeechRecognitionServerEvents {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private MicrophoneRecognitionClient mMicrophoneRecognitionClient = null;
    private SpeechRecognitionMode mSpeechRecognitionMode = SpeechRecognitionMode.ShortPhrase;
    private String mLanguageCode = Constants.LANGUAGE_CODES[0];
    private Language mLanguageTranslation = Constants.LANGUAGES[0];
    private String mKey = Constants.PRIMARY_SUBSCRIPTION_KEY;
    private String mSecKey = Constants.SECONDARY_SUBSCRIPTION_KEY;

    private TextView mResultText;
    private FloatingActionButton mFloatingActionButton;

    private ItemAdapter mItemAdapter = new ItemAdapter(this);
    private View mSuggestionLayoutView;

    private Drawable onlineIcon;
    private Drawable busyIcon;

    ImageButton speakButton;

    private boolean mHasStartedRecording = false;
    private boolean mHasOptionsChanged = true;

    private SharedPreferencesUtils sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mResultText = (TextView) findViewById(R.id.resultText);
        mSuggestionLayoutView = findViewById(R.id.suggestionLayout);

        onlineIcon = getResources().getDrawable(android.R.drawable.presence_audio_online);
        busyIcon = getResources().getDrawable(android.R.drawable.presence_audio_busy);

        speakButton = (ImageButton) findViewById(R.id.speak_button);

        sharedPreferences = new SharedPreferencesUtils(this);

        initLanguageSpinner();
        initSpeechModeSpinner();

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (WebUtils.hasInternetConnection(MainActivity.this)) {
                    mResultText.setText("");
                    mSuggestionLayoutView.setVisibility(View.GONE);
                    mResultText.setVisibility(View.VISIBLE);
                    // Check android runtime permission for RECORD_AUDIO
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(LOG_TAG, "RECORD_AUDIO permission denied");
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                            Snackbar.make(findViewById(R.id.activity_main), "I must have record audio permission", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("Try again", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
                                                    Constants.MY_PERMISSION_REQUEST_RECORD_AUDIO);
                                        }
                                    }).show();
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
                                    Constants.MY_PERMISSION_REQUEST_RECORD_AUDIO);
                        }
                    } else {
                        initRecording();
                        startRecording();
                    }
                } else {
                    Snackbar.make(view, getString(R.string.check_connection), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });
    }

    private void initSpeechModeSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.speech_mode_spinner);
        spinner.setSaveEnabled(true);
        int speechRecognitionModePref = sharedPreferences.getSpeechModeIndex();
        spinner.setSelection(speechRecognitionModePref, true);
        mSpeechRecognitionMode = speechRecognitionModePref == 0 ? SpeechRecognitionMode.ShortPhrase : SpeechRecognitionMode.LongDictation;
        spinner.post(new Runnable() {
            @Override
            public void run() {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(LOG_TAG, "in Speech spinner onItemSelected: " + position);
                        sharedPreferences.updateSpeechModeIndex(position);
                        mSpeechRecognitionMode = position == 0 ? SpeechRecognitionMode.ShortPhrase : SpeechRecognitionMode.LongDictation;
                        mHasOptionsChanged = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // no action required, but it must be implemented
                    }
                });
            }
        });
    }

    private void initLanguageSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.language_spinner);
        spinner.setSaveEnabled(true);
        int languageCodePref = sharedPreferences.getBaseLanguageIndex();
        spinner.setSelection(languageCodePref, true);
        mLanguageCode = Constants.LANGUAGE_CODES[languageCodePref];

        spinner.post(new Runnable() {
            @Override
            public void run() {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(LOG_TAG, "in Language spinner onItemSelected: " + position);
                        sharedPreferences.updateBaseLanguageIndex(position);
                        mLanguageCode = Constants.LANGUAGE_CODES[position];
                        mHasOptionsChanged = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // no action required, but it must be implemented
                    }
                });
            }
        });
    }

    private void initRecording() {
        if ((mHasOptionsChanged) || (mMicrophoneRecognitionClient == null)) {
            Log.d(LOG_TAG, "Language is " + mLanguageCode + " speech mode is " + mSpeechRecognitionMode.toString());
            if (mKey.equals(Constants.PRIMARY_SUBSCRIPTION_KEY)) {
                mResultText.append(getString(R.string.primary_connect));
            } else {
                mResultText.append(getString(R.string.secondary_connect));
            }
            mMicrophoneRecognitionClient = SpeechRecognitionServiceFactory.createMicrophoneClient(this, mSpeechRecognitionMode, mLanguageCode, this, mKey, mSecKey);
            mHasOptionsChanged = false;
        }
        // Discard previous translation
        mItemAdapter.clear();
        speakButton.setVisibility(View.GONE);
    }

    private void startRecording() {
        if (mMicrophoneRecognitionClient != null) {
            if (mSpeechRecognitionMode.equals(SpeechRecognitionMode.ShortPhrase)) {
                if (!mHasStartedRecording) {
                    mMicrophoneRecognitionClient.startMicAndRecognition();
                }
            } else if (mSpeechRecognitionMode.equals(SpeechRecognitionMode.LongDictation)) {
                if (!mHasStartedRecording) {
                    mMicrophoneRecognitionClient.startMicAndRecognition();
                } else {
                    mMicrophoneRecognitionClient.endMicAndRecognition();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSION_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "RECORD_AUDIO permission granted!");
                    initRecording();
                    startRecording();
                } else {
                    Snackbar.make(findViewById(R.id.activity_main), "Sorry app will not work without that permission", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                return;
            }
        }
    }

    @Override
    public void onPartialResponseReceived(String response) {
        mResultText.append("PARTIAL RESULT:\n");
        mResultText.append(response + "\n");
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        boolean isFinalDictationMessage = (
                mSpeechRecognitionMode == SpeechRecognitionMode.LongDictation
                        && (recognitionResult.RecognitionStatus == RecognitionStatus.EndOfDictation
                        || recognitionResult.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout
                        || recognitionResult.RecognitionStatus == RecognitionStatus.RecognitionSuccess));
        if (mSpeechRecognitionMode == SpeechRecognitionMode.ShortPhrase || isFinalDictationMessage) {
            if (mMicrophoneRecognitionClient != null) {
                mMicrophoneRecognitionClient.endMicAndRecognition();
            }
            mFloatingActionButton.setEnabled(true);
            mFloatingActionButton.setImageDrawable(onlineIcon);
        }

        if (recognitionResult.Results.length > 0) {
            ListView listView = (ListView) findViewById(R.id.resultList);
            listView.setAdapter(mItemAdapter);
            mSuggestionLayoutView.setVisibility(View.VISIBLE);
            mResultText.setVisibility(View.GONE);
            for (int i = 0; i < recognitionResult.Results.length; i++) {
                mItemAdapter.addItem(recognitionResult.Results[i].DisplayText);
            }

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.dialog_content);
                    dialog.setCancelable(true);
                    dialog.setTitle(getString(R.string.dialog_title));

                    ListView translationList = (ListView) dialog.findViewById(R.id.translationList);
                    final ItemAdapter translationAdapter = new ItemAdapter(MainActivity.this);
                    translationList.setAdapter(translationAdapter);
                    translationAdapter.setItems(getResources().getStringArray(R.array.languages));
                    translationAdapter.setSelected(sharedPreferences.getConvertLanguageIndex());
                    mLanguageTranslation = Constants.LANGUAGES[sharedPreferences.getConvertLanguageIndex()];

                    translationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mLanguageTranslation = Constants.LANGUAGES[position];
                            sharedPreferences.updateConvertLanguageIndex(position);
                            translationAdapter.setSelected(position);
                        }
                    });

                    Button translateButton = (Button) dialog.findViewById(R.id.translate_button);
                    Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);

                    translateButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            mResultText.setText("");
                            new TranslationTask(Constants.LANGUAGES[sharedPreferences.getBaseLanguageIndex()],
                                    mLanguageTranslation,
                                    mItemAdapter.getItem(position).toString()).execute();
                        }
                    });
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            });
        }
    }

    @Override
    public void onIntentReceived(String s) {

    }

    @Override
    public void onError(int errorCode, String response) {
        mFloatingActionButton.setEnabled(true);
        mFloatingActionButton.setImageDrawable(onlineIcon);
        Snackbar.make(findViewById(R.id.activity_main), getString(R.string.internet_error_text), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        mResultText.append("Error: " + errorCode + " : " + response + "\n");
        mMicrophoneRecognitionClient = null;
        mKey = mSecKey;
    }

    @Override
    public void onAudioEvent(boolean isRecording) {
        mHasStartedRecording = isRecording;
        if (!isRecording) {
            if (mMicrophoneRecognitionClient != null) {
                mMicrophoneRecognitionClient.endMicAndRecognition();
            }
            mFloatingActionButton.setEnabled(true);
            mFloatingActionButton.setImageDrawable(onlineIcon);
        } else {
            if (mSpeechRecognitionMode == SpeechRecognitionMode.ShortPhrase) ;
            mFloatingActionButton.setEnabled(false);
        }
        mFloatingActionButton.setImageDrawable(busyIcon);
        mResultText.append(isRecording ? getString(R.string.recording_start) : getString(R.string.recording_end));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mResultText", mResultText.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mResultText.setText(savedInstanceState.getString("mResultText"));
    }

    private class TranslationTask extends AsyncTask<Void, Void, Boolean> {
        private final Language baseLanguage;
        private final Language targetLanguage;
        private final String sentence;
        private String translatedText = "";

        public TranslationTask(Language baseLanguage, Language targetLanguage, String sentence) {
            this.baseLanguage = baseLanguage;
            this.targetLanguage = targetLanguage;
            this.sentence = sentence;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mResultText.append("Sentence selected: \n" + sentence);
            mResultText.append(getString(R.string.translation_start));
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Translate.setClientId(Constants.CLIENT_ID_VALUE);
            Translate.setClientSecret(Constants.CLIENT_SECRET_VALUE);
            try {
                translatedText = Translate.execute(sentence, baseLanguage, targetLanguage);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                mResultText.setVisibility(View.VISIBLE);
                mResultText.setText(getString(R.string.translation_heading));
                mResultText.append(translatedText);
                speakButton.setVisibility(View.VISIBLE);
                speakButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Get language code and call TTS
                        String speechLanguage = Constants.LANGUAGE_CODES[sharedPreferences.getConvertLanguageIndex()];
                        Log.d(LOG_TAG, "Speech language is: " + speechLanguage);
                        Synthesizer synthesizer = new Synthesizer(getString(R.string.app_name), Constants.PRIMARY_SUBSCRIPTION_KEY);
                        Voice voice = Voices.getVoice(speechLanguage, 0);
                        if (voice != null) {
                            Log.d(LOG_TAG, "Voice name: " + voice.voiceName);
                            synthesizer.SetVoice(voice, voice);
                            synthesizer.SpeakToAudio(translatedText);
                        }
                    }
                });
            } else {
                Log.d(LOG_TAG, "Error executing translation service");
            }
        }
    }

}
