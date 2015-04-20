package shortcut.gdd.android.com.shortcut;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.speech.tts.TextToSpeech;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;

import shortcut.gdd.android.com.shortcut.data.QeaContract;

/**
 * Utility :
 * methods used on service
 * Created by Samuel PC on 06/04/2015.
 */
public class Utility  {

    final String RINGER_MODE_SILENT = " ringer mode silent";

    /**
     * ringerModeSilent : set ringer phone silent mode
     * @param mobilemode
     * @param tts
     */
    public void ringerModeSilent(AudioManager mobilemode,TextToSpeech tts){
        textToSpeech(RINGER_MODE_SILENT,mobilemode,tts);
        mobilemode.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }

    /**
     * ringerModeNormal : set ringer phone normal mode
     * @param mobilemode
     */
    public void ringerModeNormal(AudioManager mobilemode) {
        mobilemode.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    /**
     * wifiChange : enable and disable wifi
     * @param status
     * @param context
     */
    public void wifiChange(boolean status,Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(status);
    }

    /**
     * connectionChange : enable and disable connection
     * @param status
     */
    public void connectionChange(Boolean status,ConnectivityManager dataManager){
        //ConnectivityManager dataManager;
        //dataManager  = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        Method dataMtd = null;
        try {
            dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        if(dataMtd != null)
            dataMtd.setAccessible(true);
        else
         return;

        try {
            dataMtd.invoke(dataManager, status);        //True - to enable data connectivity .
        } catch (IllegalAccessException  | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * textToSpeech: speak msg
     * @param speakThis
     * @param mAudioManager
     * @param tts
     */
    public void textToSpeech(String speakThis,AudioManager mAudioManager,TextToSpeech tts){

        // speak from listening
        tts.setPitch(0.7f);
        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);

        HashMap<String, String> hash = new HashMap<>();
        hash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                String.valueOf(AudioManager.STREAM_NOTIFICATION));

        tts.setLanguage(Locale.UK);
        if(!tts.isSpeaking()) {
            tts.speak(speakThis, TextToSpeech.QUEUE_FLUSH, hash);
        }else{
            tts.stop();
        }

    }

    /**
     * readMessagens : read unread messages
     * @param mAudioManager
     * @param tts
     */
    public void readMessagens(AudioManager mAudioManager,TextToSpeech tts,ContentResolver contentResolver){
        Uri uriSMSURI = Uri.parse("content://sms/inbox");
        Cursor cur = contentResolver.query(uriSMSURI, null, null, null,null);
        String sms = "";
        while (cur.moveToNext()) {
            sms += "From :" + cur.getString(2) + " : " + cur.getString(12)+"\n";
            textToSpeech(sms,mAudioManager,tts);
        }
    }


    /**
     * turnOnFlashLight: turn on the flash light
     * @param pm
     * @param cam
     */
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

    /**
     * turnOffFlashLight: turn off the flash light
     * @param pm
     * @param cam
     */
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

    public HashMap<String,String> getQea(Context mContext){

        HashMap<String,String> hashMap = new HashMap<>();

        String[] QEA_COLUMNS = {
                QeaContract.QEAEntry.TABLE_NAME + "." + QeaContract.QEAEntry._ID,
                QeaContract.QEAEntry.COLUMN_DATE,
                QeaContract.QEAEntry.COLUMN_QUESTION,
                QeaContract.QEAEntry.COLUMN_ANSWER
        };

        Cursor qeaCursor = mContext.getContentResolver().query(
                QeaContract.QEAEntry.CONTENT_URI,
                QEA_COLUMNS,
                null,
                null,
                null
        );

        if(qeaCursor!=null && qeaCursor.getCount()>0) {
            qeaCursor.moveToFirst();
            for (int i = 0; qeaCursor.getCount() > i; i++) {
                int qId = qeaCursor.getColumnIndex(QeaContract.QEAEntry.COLUMN_QUESTION);
                int aId = qeaCursor.getColumnIndex(QeaContract.QEAEntry.COLUMN_ANSWER);
                hashMap.put(qeaCursor.getString(qId),qeaCursor.getString(aId));
                qeaCursor.moveToNext();
            }
            qeaCursor.close();
        }
        return hashMap;
    }


    /*
        testing
     */
    /*
    public void capturePhoto() {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent,123);
        }
    }
    */

}
