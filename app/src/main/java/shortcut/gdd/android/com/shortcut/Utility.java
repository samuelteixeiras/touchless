package shortcut.gdd.android.com.shortcut;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Samuel PC on 06/04/2015.
 */
public class Utility extends Activity {

    final String RINGER_MODE_SILENT = " ringer mode silent";
    Camera cam = null;

    public void ringerModeSilent(AudioManager mobilemode,TextToSpeech tts){
        textToSpeech(RINGER_MODE_SILENT,mobilemode,tts);
        mobilemode.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }

    public void ringerModeNormal(AudioManager mobilemode) {
        mobilemode.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
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


    public void turnOnFlashLight(PackageManager pm) {
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                cam = Camera.open();
                Camera.Parameters p = cam.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                cam.setParameters(p);
                cam.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnOffFlashLight(PackageManager pm) {
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                cam.stopPreview();
                cam.release();
                cam = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
