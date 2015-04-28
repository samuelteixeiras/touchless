package shortcut.gdd.android.com.shortcut;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.AlarmClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ShortcutService: executed in background
 * Created by Samuel PC on 02/04/2015.
 */
public class ShortcutService extends Service implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener
{
    private static final String TAG = ShortcutService.class.getSimpleName();
    public static AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private static boolean mIsStreamMute;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    static final int MSG_STOP_RECOGNIZER_CANCEL = 3;
    static final int MSG_SPEAK = 4;
    static final int MSG_RECOGNIZER_START_LISTENING_SOUND = 5;
    TextToSpeech tts;
    Camera cam = null;
    ConnectivityManager dataManager;


    AudioManager mobilemode;
    Boolean firstTime = true;

    Utility utility;

    Boolean lightStatus = false;

    static CountDownTimer mTimer;

    final static String MESSAGE_ALARM = "touchless alarm";

    private SoundPool soundPool;
    private int failSound;
    private int okSound;
    HashMap<String,String> qeaHashMap = new HashMap<>();
    Context mContext;
    SharedPreferences settings;
    @Override
    public void onCreate()
    {
        super.onCreate();
        utility = new Utility();
        mContext = getApplicationContext();
        mobilemode = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        if(tts == null)
            tts = new TextToSpeech(this, this);

        qeaHashMap = utility.getQea(mContext);
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        failSound = soundPool.load(this, R.raw.fail, 1);
        okSound = soundPool.load(this, R.raw.ok, 1);
        settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
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
                        if (!mIsStreamMute)
                        {
                            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                            mIsStreamMute = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        Log.d(TAG, "message start listening"); 

                    }else{
                        Log.d(TAG, "message not start listening"); 
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamMute)
                    {
                        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
                        mIsStreamMute = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "message canceled recognizer"); 
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
                        Log.d(TAG, "message start listening sound"); 
                    }else{
                        Log.d(TAG, "message not started"); 
                    }

                    mIsStreamMute = false;

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
                Log.d(TAG, ">>>>>>>>>>>  onFinish <<<<<<<<<<<<<<<<<<<<");
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


        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        if(cam != null){
            cam.release();
            cam = null;
        }

        if(tts!=null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, " tts destroy");
        }

        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }
        super.onDestroy();

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
            // speech input will be processed,
            // so there is no need for count down anymore
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }

            Log.d(TAG, "onBeginingOfSpeech"); 
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndOfSpeech"); 
        }

        @Override
        public void onError(int error)
        {

            playFailSound("onerror");

            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                    try
                    {
                        firstTime = true;
                        mServerMessenger.send(message);
                    }
                    catch (RemoteException e)
                    {
                        Log.d(TAG, "error = " + e);
                    }
                }
            }, 2500);


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

            }else{
                Log.d(TAG, "onReadyForSpeech ??"); 
            }
            Toast.makeText(mContext,"onReadyForSpeech",Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onReadyForSpeech"); 
        }

        @Override
        public void onResults(Bundle results)
        {
            ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d(TAG, "onResults");

            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);

            // get the audio and compare with waited strings
            String aux = "";
            Boolean resultOk = false;
            search:  for(String item : list){

                Log.d(TAG, "item  >"+ item +"<");

                if (item.startsWith("android") )
                {

                    item = item.replace("android","");
                    item = item.trim();

                    // modify item case set alarm found
                    if(item.contains("set an alarm for")){
                        aux = item;
                        item   = "set an alarm for";
                    } else if(item.contains("set timer for")){
                        aux = item;
                        item   = "set timer for";
                    }

                    Log.d(TAG, "item replaced  >"+ item +"<");

                    switch (item) {

                        case "what time is it":
                            if(!settings.getBoolean(getString(R.string.pref_control_time_key), true)){
                                continue;
                            }
                            resultOk = true;
                            SimpleDateFormat sdfHour = new SimpleDateFormat("HH");
                            SimpleDateFormat sdfMinutes = new SimpleDateFormat("mm");
                            long time = new Date().getTime();
                            String strHour = sdfHour.format(time);
                            String strMinutes = sdfMinutes.format(time);
                            String speakTime = "Time now: " + strHour + " hours and " + strMinutes + " minutes ";

                            utility.textToSpeech(speakTime, mAudioManager, tts);
                            firstTime = true;
                            break search;

                        case "what day is it":
                            if(!settings.getBoolean(getString(R.string.pref_control_day_key), true)){
                                continue;
                            }
                            resultOk = true;
                            SimpleDateFormat day = new SimpleDateFormat("dd");
                            SimpleDateFormat month = new SimpleDateFormat("MMMM");
                            SimpleDateFormat year = new SimpleDateFormat("yyyy");
                            String strDay = day.format(new Date().getTime());
                            String strMonth = month.format(new Date().getTime());
                            String strYear = year.format(new Date().getTime());
                            String speakDate = "Today is:" + strDay + " " + strMonth + " " + strYear;

                            utility.textToSpeech(speakDate, mAudioManager, tts);
                            firstTime = true;
                            break search;

                            /*
                            testing
                            case "take a picture":
                                resultOk = true;
                                utility.capturePhoto();
                                firstTime = true;
                                break search;*/

                        case "set an alarm for":
                            if(!settings.getBoolean(getString(R.string.pref_set_alarm_key), true)){
                                continue;
                            }
                            resultOk = true;
                            createAlarm(aux, item);
                            firstTime = true;
                            break search;

                        /* only in api lv 19 , current 16
                        case "set timer for":
                            resultOk = true;
                            startTimer(aux, item);
                            firstTime = true;
                            break search;
                        */
                        /* not implemented yet
                        case "weather":
                            resultOk = true;
                            utility.textToSpeech("weather", mAudioManager, tts);
                            firstTime = true;
                            break search;
                        */
                        case "read messages":
                            if(!settings.getBoolean(getString(R.string.pref_messages_key), true)){
                                continue;
                            }
                            resultOk = true;
                           // utility.readMessagens(mAudioManager, tts,getContentResolver());
                            firstTime = true;
                            break search;

                        case "lights on":
                        case "light on":
                            if(!settings.getBoolean(getString(R.string.pref_cam_on_key), true)){
                                continue;
                            }
                            resultOk = true;
                            if (!lightStatus) {
                                Log.d(TAG, "light on");
                                cam = Camera.open();
                                utility.turnOnFlashLight(getPackageManager(), cam);
                                lightStatus = true;
                            }
                            firstTime = true;
                            break search;

                        case "lights off":
                        case "light off":
                            if(!settings.getBoolean(getString(R.string.pref_cam_off_key), true)){
                                continue;
                            }
                            resultOk = true;
                            if (lightStatus) {
                                Log.d(TAG, "light off");
                                utility.turnOffFlashLight(getPackageManager(), cam);
                                lightStatus = false;
                            }
                            firstTime = true;
                            break search;

                        case "silent mode":
                            if(!settings.getBoolean(getString(R.string.pref_silent_mode_key), true)){
                                continue;
                            }
                            resultOk = true;
                            utility.ringerModeSilent(mobilemode, tts);
                            firstTime = true;
                            break search;

                        case "normal mode":
                            if(!settings.getBoolean(getString(R.string.pref_normal_mode_key), true)){
                                continue;
                            }
                            resultOk = true;
                            utility.ringerModeNormal(mobilemode);
                            firstTime = true;
                            break search;

                        case "wifi on":
                            if(!settings.getBoolean(getString(R.string.pref_control_wifi_on_key), true)){
                                continue;
                            }
                            resultOk = true;
                            firstTime = true;
                            utility.wifiChange(true, mContext);
                            break search;

                        case "wifi off":
                            if(!settings.getBoolean(getString(R.string.pref_control_wifi_off_key), true)){
                                continue;
                            }
                            resultOk = true;
                            firstTime = true;
                            utility.wifiChange(false, mContext);
                            break search;

                        case "connection on":
                            if(!settings.getBoolean(getString(R.string.pref_control_connection_on_key), true)){
                                continue;
                            }
                            dataManager  = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                            resultOk = true;
                            firstTime = true;
                            utility.connectionChange(true,dataManager);
                            break search;

                        case "connection off":
                            if(!settings.getBoolean(getString(R.string.pref_control_connection_off_key), true)){
                                continue;
                            }
                            resultOk = true;
                            firstTime = true;
                            dataManager  = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                            utility.connectionChange(false,dataManager);
                            break search;

                        default:
                            // command not recognized, restart
                            for(Map.Entry<String, String> entry : qeaHashMap.entrySet()) {
                                String key = entry.getKey();
                                String value = entry.getValue();

                                if(item.equals(key)){
                                    resultOk = true;
                                    utility.textToSpeech(value, mAudioManager, tts);
                                    firstTime = true;
                                    break search;
                                }

                            }

                            firstTime = true;
                            break;
                    }

                }
            }

            // delay for play sound
            long timeToWait = 1000;
            if(!resultOk) {
                playFailSound("result fail");
            }else{
                playOkSound();
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    restartListening();
                    Log.d(TAG, " 2 restartListening  >" +firstTime );
                }
            }, timeToWait);


        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

    }

    /**
     * Restart the recognizer , the original without sound
     */
    public void restartListening(){

        mNoSpeechCountDown.cancel();
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
     * Restart the recognizer using sound
     */
    public void startListening(){
        // for call again
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
        try
        {
            mServerMessenger.send(message);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING_SOUND);
                    try {
                        mServerMessenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }, 1000);

        }
        catch (RemoteException e)
        {
            Log.d(TAG, "erro >" + e );
        }

    }

    /**
     * getCommand
     * @param command
     * @return
     */
    public String getCommand(int command){
        Resources res = getResources();
        String[] data = res.getStringArray(R.array.pref_labels);
        List<String> commads = new ArrayList<>(Arrays.asList(data));
        return commads.get(command);
    }

    /**
     * createAlarm
     * @param aux string recognized
     * @param item string auxiliar
     */
    public void createAlarm(String aux,String item){
        int hourAlarm = 0;
        int minutesAlarm = 0;

        if(aux.isEmpty())
            return;

        aux = aux.replace(item,"");

        if(aux.contains("a.m.")){
            aux = aux.replace("a.m.","");
            aux = aux.replace(" ","");
            if(aux.isEmpty())
                return;

            if(aux.contains(":")){
                String[] splits =  aux.split(":");
                hourAlarm = Integer.parseInt(splits[0]);
                minutesAlarm = Integer.parseInt(splits[1]);
            }else {
                hourAlarm = Integer.parseInt(aux);
            }
        }else if(aux.contains("p.m.")){
            aux = aux.replace("p.m.","");
            aux = aux.replace(" ","");

            if(aux.isEmpty())
                return;

            if(aux.contains(":")){
                String[] splits =  aux.split(":");
                hourAlarm = Integer.parseInt(splits[0]);
                minutesAlarm = Integer.parseInt(splits[1]);
            }else {
                hourAlarm = Integer.parseInt(aux);
            }
            hourAlarm+=12;
        }
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_MESSAGE, MESSAGE_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hourAlarm)
                .putExtra(AlarmClock.EXTRA_MINUTES, minutesAlarm);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /**
     * startTimer : used only in api lv 19 , current is 16
     * @param aux
     * @param item
     */
    public void startTimer(String aux,String item) {
        int seconds = 0;
        int minutes;
        if(aux.isEmpty())
            return;

        aux = aux.replace(item,"");

        if(aux.contains("minutes")){
            aux = aux.replace("minutes","");
            aux = aux.replace(" ","");
            if(aux.isEmpty())
                return;
            minutes = Integer.parseInt(aux);
            // change for seconds
            seconds = minutes * 60;
        }

        Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_MESSAGE, MESSAGE_ALARM)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /**
     * playFailSound
     * @param msg only for debug
     */
    private void playFailSound(String msg){
        mIsStreamMute = false;
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        soundPool.stop(okSound);
        soundPool.play(failSound, 1f, 1f, 1, 0, 1f);
        Log.d("Test", "Played fail sound >>" + msg);

    }

    /**
     * playOkSound : play when the recognizer is ok
     */
    private void playOkSound(){
        mIsStreamMute = false;
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        soundPool.stop(failSound);
        soundPool.play(okSound, 1f, 1f, 1, 0, 1f);
        Log.d("Test", "Played ok sound");
    }

}
