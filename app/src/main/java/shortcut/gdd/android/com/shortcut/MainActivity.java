package shortcut.gdd.android.com.shortcut;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private static final int SHORTCUT_NOTIFICATION_ID = 3000;
    private static ListView mListView;

    /** Messenger for communicating with the service. */
    static Messenger mService = null;

    /** Flag indicating whether we have called bind on the service. */
    static boolean mBound;

    static View rootView;

    private static boolean serverStatus = false;

    private boolean startAfterBind = false;

    public static final String PREFS_NAME = "MyPrefsFile";

    public static ArrayList<Boolean> listPrefs;

    public String STOP_SERVER_KEY = "STOP_SERVER";

    BroadcastReceiver receiver;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        loadPrefs();
        registerIntentFilter();

    }

    /**
     * registerIntentFilter: used for stop server on notification button
     */
    public void registerIntentFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(STOP_SERVER_KEY);
        // Add other actions as needed

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(STOP_SERVER_KEY)) {
                    if(serverStatus){
                        stopServer();
                        removeNotification();
                    }
                }
            }
        };

        registerReceiver(receiver, filter);
    }

    public void loadPrefs(){

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        listPrefs = new ArrayList<Boolean>();
        listPrefs.add(settings.getBoolean(getString(R.string.pref_cam_on_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_cam_off_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_messages_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_silent_mode_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_normal_mode_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_set_alarm_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_control_wifi_on_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_control_wifi_off_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_control_connection_on_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_control_connection_off_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_control_time_key), true));
        listPrefs.add(settings.getBoolean(getString(R.string.pref_control_day_key), true));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        if (id == R.id.action_qea) {
            startActivity(new Intent(this, QeaDisplayActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void speakCommand(View view){

        Message msg = Message.obtain(null, ShortcutService.MSG_SPEAK, Integer.parseInt(view.getTag().toString()), 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * savePreference: save preference clicking on checkbox
     * @param view
     */
    public void savePreference(View view){

        boolean checked = ((CheckBox) view).isChecked();
        String index = view.getTag().toString();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(index, checked);
        // Commit the edits!
        editor.commit();
    }

    public void startServerClickButton(View view) {
        Button button = (Button) rootView.findViewById(R.id.startButton);

        if(!serverStatus){
            startServer();
            button.setText(getString(R.string.btn_text_stop));
        }else{
            stopServer();
            button.setText(getString(R.string.btn_text_start));
        }

    }

    private void restoreButtonState(){
        Button button = (Button) rootView.findViewById(R.id.startButton);

        if(serverStatus){
            button.setText(getString(R.string.btn_text_stop));
        }else{
            button.setText(getString(R.string.btn_text_start));
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        ArrayAdapter<String> mShortcutAdapter;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // Construct the data source
            ArrayList<Options> arrayOfOptions = populateOptions();
            // Create the adapter to convert the array to views
            OptionsAdapter adapter = new OptionsAdapter(getActivity(), arrayOfOptions);
            // Attach the adapter to a ListView
            mListView = (ListView) rootView.findViewById(R.id.listview_shorcut);
            mListView.setAdapter(adapter);
            return rootView;
        }


        public ArrayList<Options> populateOptions(){

            Resources res = getResources();
            String[] data = res.getStringArray(R.array.pref_labels);

            List<String> labels = new ArrayList<String>(Arrays.asList(data));

            String[] keys = res.getStringArray(R.array.pref_keys);

            List<String> listKeys = new ArrayList<String>(Arrays.asList(keys));

            ArrayList<Options> options = new ArrayList<Options>();
            for (int i=0; i < labels.size();i++){
                Options option = new Options(labels.get(i),
                                             listPrefs.get(i),
                                             String.valueOf(i),
                                             listKeys.get(i));
                options.add(option);
            }

            return options;
        }


    }



    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
            if(startAfterBind) startServer();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    public  void startServer() {

        serverStatus = true;

        if (!mBound) {
            bindService(new Intent(this, ShortcutService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            startAfterBind = true;
        } else{
            // Create and send a message to the service, using a supported 'what' value
            Message msg = Message.obtain(null, ShortcutService.MSG_RECOGNIZER_START_LISTENING, 0, 0);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopServer() {
        serverStatus = false;
        if(ShortcutService.mAudioManager!=null) {
            ShortcutService.mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            ShortcutService.mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        removeNotification();

    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreButtonState();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("serverStatus", serverStatus);
        savedInstanceState.putBoolean("mBound", mBound);
        savedInstanceState.putBoolean("startAfterBind", startAfterBind);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        serverStatus = savedInstanceState.getBoolean("serverStatus");
        mBound = savedInstanceState.getBoolean("mBound");
        startAfterBind = savedInstanceState.getBoolean("startAfterBind");
        restoreButtonState();

    }

    private void removeNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(SHORTCUT_NOTIFICATION_ID);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(serverStatus){
            startNotification();
        }
    }

    @Override
    protected void onDestroy() {
        removeNotification();

        if(ShortcutService.mAudioManager!=null) {
            ShortcutService.mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            ShortcutService.mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
        super.onDestroy();

    }

    public void startNotification(){

        Intent stopServerIntend = new Intent(STOP_SERVER_KEY);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopServerIntend, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_mic_48)
                .setContentTitle("Touchless")
                .setContentText("Show Options")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("press button to stop"))
                .addAction(R.drawable.ic_close,
                        getString(R.string.btn_text_stop), stopPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(SHORTCUT_NOTIFICATION_ID, mBuilder.build());

    }






}
