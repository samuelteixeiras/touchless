package shortcut.gdd.android.com.shortcut;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Samuel PC on 02/04/2015.
 */
public class ShortcutService extends Service implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener
{
    private static final String TAG = ShortcutService.class.getSimpleName();
    protected static AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private static boolean mIsStreamSolo;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    static final int MSG_STOP_RECOGNIZER_CANCEL = 3;
    static final int MSG_SPEAK = 4;
    static final int MSG_RECOGNIZER_START_LISTENING_SOUND = 5;
    TextToSpeech tts;

    AudioManager mobilemode;
    Boolean firstTime = true;

    Utility utility;

    @Override
    public void onCreate()
    {
        super.onCreate();
        utility = new Utility();
        mobilemode = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        tts = new TextToSpeech(this, this);

        Log.d(TAG, "Creating listening");

    }



    protected  class IncomingHandler extends Handler
    {
        private WeakReference<ShortcutService> mtarget;

        IncomingHandler(ShortcutService target)
        {
            mtarget = new WeakReference<ShortcutService>(target);
        }


        @Override
        public void handleMessage(Message msg)
        {
            final ShortcutService target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        // turn off beep sound
                        if (!mIsStreamSolo)
                        {
                            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo)
                    {
                        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;

                case MSG_STOP_RECOGNIZER_CANCEL:
                    mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "message stop recognizer");
                    break;

                case MSG_SPEAK:
                    speakOut(msg.arg1);
                    break;

                case MSG_RECOGNIZER_START_LISTENING_SOUND:
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        Log.d(TAG, "message start listening sound"); //$NON-NLS-1$
                    }
                    break;
            }
        }
    }

    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000)
    {

        @Override
        public void onTick(long millisUntilFinished)
        {
            // TODO Auto-generated method stub
            Log.d(TAG, "onTick");
        }

        @Override
        public void onFinish()
        {
            mIsCountDownOn = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
                Log.d(TAG, "onFinish");
            }
            catch (RemoteException e)
            {
                Log.d(TAG, "onFinish"+e);
            }
        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(tts!=null) {
            tts.stop();
            tts.shutdown();
            tts=null;
        }

        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }

        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }

    private void speakOut(int command) {
        utility.textToSpeech(getCommand(command), mAudioManager, tts);
    }

    @Override
    public void onInit(int status) {
        tts.setOnUtteranceCompletedListener(this);
    }

    @Override
    public void onUtteranceCompleted(String utteranceId) {
        Log.d(TAG, "onUtteranceCompleted");

    }


    protected class SpeechRecognitionListener implements RecognitionListener
    {

        @Override
        public void onBeginningOfSpeech()
        {
            // speech input will be processed, so there is no need for count down anymore
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onError(int error)
        {
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try
            {
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
            Log.d(TAG, "error = " + error); //$NON-NLS-1$
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent" );
        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults" );
        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();

            }
            Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onResults(Bundle results)
        {
            ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d(TAG, "onResults"); //$NON-NLS-1$
            // get the audio and compare with waited strings

            Boolean resulOk = false;
            search:  for(String item : list){

                if(firstTime){
                    switch (item) {
                        case "ok google":
                            firstTime = false;
                            resulOk = true;
                            break search;
                    }
                }else{
                    switch (item) {
                        case "weather":
                            utility.textToSpeech("weather", mAudioManager, tts);
                            break search;

                        case "read messages":
                            utility.readMessagens(mAudioManager, tts);
                            break search;

                        case "lights on":
                        case "light on":
                            utility.turnOnFlashLight(getPackageManager());
                            break search;

                        case "lights off":
                        case "light off":
                            utility.turnOffFlashLight(getPackageManager());
                            break search;

                        case "silent mode":
                            utility.ringerModeSilent(mobilemode, tts);
                            break search;

                        case "normal mode":
                            utility.ringerModeNormal(mobilemode);
                            break search;
                    }

                }
            }
            // always restart the listening
            if(firstTime == false && resulOk == false){
                restartListening();
                firstTime = true;
            }else{
                if(firstTime == true && resulOk == false){
                    restartListening();
                }
                else
                Log.d(TAG, "dont restart");
            }

        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

    }

    /**
     * Restart the recognizer
     */
    public void restartListening(){

        mIsCountDownOn = false;
        Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
        try
        {
            mServerMessenger.send(message);
            message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            mServerMessenger.send(message);
        }
        catch (RemoteException e)
        {
            Log.d(TAG, "erro >" + e );
        }

    }

    /**
     * Restart the recognizer
     */
    public void startListening(){

        mIsCountDownOn = false;
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
        try
        {
            mServerMessenger.send(message);
            message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING_SOUND);
            mServerMessenger.send(message);
        }
        catch (RemoteException e)
        {
            Log.d(TAG, "erro >" + e );
        }

    }

    public String getCommand(int command){
        String[] data = {
                getString(R.string.pref_messages_command),
                getString(R.string.pref_cam_command),
                getString(R.string.pref_silent_phone_command),
                getString(R.string.pref_control_wifi_command),
                getString(R.string.pref_control_3g_command),
                getString(R.string.pref_set_alarm_command)
        };
        List<String> commads = new ArrayList<String>(Arrays.asList(data));

        return commads.get(command);
    }



}
