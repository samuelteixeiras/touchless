package shortcut.gdd.android.com.shortcut;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Samuel PC on 06/04/2015.
 */
public class Utility extends Activity {

    final String RINGER_MODE_SILENT = " ringer mode silent";

    public void ringerModeSilent(AudioManager mobilemode,TextToSpeech tts){
        textToSpeech(RINGER_MODE_SILENT,mobilemode,tts);
        mobilemode.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }

    public void ringerModeNormal(AudioManager mobilemode) {
        mobilemode.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    public void wifiChange(boolean status,Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(status);
    }

    public void connectionChange(Boolean status){
        ConnectivityManager dataManager;
        dataManager  = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        Method dataMtd = null;
        try {
            dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        dataMtd.setAccessible(true);
        try {
            dataMtd.invoke(dataManager, status);        //True - to enable data connectivity .
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void textToSpeech(String speakThis,AudioManager mAudioManager,TextToSpeech tts){

        // speak from listening
        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);

        HashMap<String, String> hash = new HashMap<String,String>();
        hash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                String.valueOf(AudioManager.STREAM_NOTIFICATION));

        tts.setLanguage(Locale.UK);
        if(!tts.isSpeaking()) {
            tts.speak(speakThis, TextToSpeech.QUEUE_FLUSH, hash);
        }else{
            tts.stop();
        }

    }

    public void readMessagens(AudioManager mAudioManager,TextToSpeech tts){
        Uri uriSMSURI = Uri.parse("content://sms/inbox");
        Cursor cur = getContentResolver().query(uriSMSURI, null, null, null,null);
        String sms = "";
        while (cur.moveToNext()) {
            sms="";
            sms += "From :" + cur.getString(2) + " : " + cur.getString(12)+"\n";
            textToSpeech(sms,mAudioManager,tts);
        }
    }


    public void turnOnFlashLight(PackageManager pm,Camera cam) {
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {

                Camera.Parameters p = cam.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                cam.setParameters(p);
                cam.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnOffFlashLight(PackageManager pm,Camera cam) {
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                cam.stopPreview();
                cam.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void capturePhoto() {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent,123);
        }
    }


}
