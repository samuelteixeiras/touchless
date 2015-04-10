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
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

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

    TextToSpeech tts;

    AudioManager mobilemode;

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



    protected static class IncomingHandler extends Handler
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
                        mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;

                case MSG_STOP_RECOGNIZER_CANCEL:
                    mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "message stop recognizer");
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
            }
            catch (RemoteException e)
            {

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
    }

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_LONG).show();
        return mServerMessenger.getBinder();
    }

    private void speakOut() {
        tts.speak("Test", TextToSpeech.QUEUE_FLUSH, null);
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

        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {

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
            search:  for(String item : list){

                switch (item){
                    case "ok google":
                        startListening();
                        break search;
                    // break;

                    case "weather":
                        utility.textToSpeech("weather",mAudioManager,tts);
                        break;

                    case "read messages":
                        utility.readMessagens(mAudioManager,tts);
                        break;

                    case "lights on":
                    case "light on":
                        utility.turnOnFlashLight(getPackageManager());
                        break;

                    case "lights off":
                    case "light off":
                        utility.turnOffFlashLight(getPackageManager());
                        break;

                    case "silent mode":
                        utility.ringerModeSilent(mobilemode,tts);
                        break;

                    case "normal mode":
                        utility.ringerModeNormal(mobilemode);
                        break;

                    default:
                        restartListening();
                        break;
                }
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

        mIsListening = false;
        Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);

        try
        {
            mServerMessenger.send(message);
        }
        catch (RemoteException e)
        {
            Log.d(TAG, "error = " + e);
        }

    }

    /**
     * Restart the recognizer
     */
    public void startListening(){

        mIsListening = false;
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);

        try
        {
            mServerMessenger.send(message);
        }
        catch (RemoteException e)
        {
            Log.d(TAG, "error = " + e);
        }

    }

}
