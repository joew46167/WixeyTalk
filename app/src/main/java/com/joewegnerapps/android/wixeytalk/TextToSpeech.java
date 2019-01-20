package com.joewegnerapps.android.wixeytalk;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.Locale;

public class TextToSpeech {
    private final String TAG = this.getClass().getSimpleName();
    private android.speech.tts.TextToSpeech tts = null;

    //////////////////////////////
    //
    // Code to control talk
    //
    /////////////////////////////

    private boolean mTalk = false;
    public void talkSwitch(boolean talk) {
        mTalk = talk;
    }

    private boolean mRepeat = false;
    public void repeatSwitch(boolean repeat) {
        mRepeat = repeat;
    }

    //////////////////////////////
    //
    // Code to set up speech
    //
    /////////////////////////////

    public TextToSpeech() {
        if (tts == null) setupTts();
    }

    public void setupTts() {
        tts = new android.speech.tts.TextToSpeech(Constants.getApplicationContext(), new android.speech.tts.TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.getDefault());
                    if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "This Language is not supported");
                    }
                    speakTimer();
                }
                else {
                    Log.e(TAG, "Initilization Failed!");
                }
            }
        });
    }


    //////////////////////////////
    //
    // Code to talk
    //
    /////////////////////////////

    String mLastSpoken = null;
    String mNextSpoken = null;
    boolean mRepeatNext = true;
    public void speakData(String string, boolean repeatInfo) {
        mRepeatNext = repeatInfo;
        if (!string.equals(mLastSpoken)) {
            mNextSpoken = string;
        }
        else {
            mNextSpoken = null;
        }
        Log.v(TAG, "speak: " + string + " mLastSpoken: " + mLastSpoken + " mNextSpoken: " + mNextSpoken);
    }



    private void say() {
        if (mLastSpoken != null) {
            mLastSpokenTime = System.currentTimeMillis();
            ttsSpeak(mLastSpoken);
        }
    }

    long mLastSpokenTime = 0L;
    private void speakString() {
        if ((System.currentTimeMillis() - mLastSpokenTime) > 1000) {
            if (mNextSpoken != null) {
                mLastSpoken = mNextSpoken;
                mNextSpoken = null;
                say();
                if (!mRepeatNext) {
                    mLastSpoken = null;
                }
            }
        }
    }

    public void ttsSpeak(String text){
        if (mTalk) {
            Log.i(TAG, "with mTalk " + text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    //////////////////////////////
    //
    // Code trigger speaking
    //
    /////////////////////////////

    Handler mHandler = new Handler();
    private void speakTimer() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mNextSpoken != null) {
                    speakString();
                }
                if (mRepeat && mTalk && ((System.currentTimeMillis() - mLastSpokenTime) > 3000) ){
                    say();
                }
                speakTimer();
            }
        }, 500);
    }
}
