package shortcut.gdd.android.com.shortcut;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    private static final int SHORTCUT_NOTIFICATION_ID = 3000;
    private static ListView mListView;

    private ShortcutService myService;

    /** Messenger for communicating with the service. */
    static Messenger mService = null;

    /** Flag indicating whether we have called bind on the service. */
    static boolean mBound;

    static View rootView;

    private boolean serverStatus = false;

    private boolean startAfterBind = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

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
        if (id == R.id.action_settings) {
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

    public void startServerClickButton(View view) {
        Button button = (Button) rootView.findViewById(R.id.startButton);

        if(!serverStatus){
            serverStatus = true;
            startServer();
            button.setText(getString(R.string.btn_text_stop));
        }else{
            serverStatus = false;
            stopServer();
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
            String[] data = {
                    getString(R.string.pref_messages_label),
                    getString(R.string.pref_cam_label),
                    getString(R.string.pref_silent_phone_label),
                    getString(R.string.pref_control_wifi_label),
                    getString(R.string.pref_control_3g_label),
                    getString(R.string.pref_set_alarm_label)
            };
            List<String> labels = new ArrayList<String>(Arrays.asList(data));

            String[] data2 = {
                    "0",
                    "1",
                    "2",
                    "3",
                    "4",
                    "5"
            };
            List<String> commands = new ArrayList<String>(Arrays.asList(data2));

            List<String> prefs = loadPrefs();
            ArrayList<Options> options = new ArrayList<Options>();
            for (int i=0; i < labels.size();i++){
                Options option = new Options(labels.get(i),
                                             Boolean.parseBoolean(prefs.get(i)),
                                             commands.get(i));
                options.add(option);
            }

            return options;
        }

        public ArrayList<String> loadPrefs(){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String cam_light = prefs.getString(getString(R.string.pref_cam_key),
                                        getString(R.string.pref_cam_default));
            String messages = prefs.getString(getString(R.string.pref_messages_key),
                    getString(R.string.pref_messages_default));
            String control_3g = prefs.getString(getString(R.string.pref_control_3g_key),
                    getString(R.string.pref_control_3g_default));
            String wifi = prefs.getString(getString(R.string.pref_control_wifi_key),
                    getString(R.string.pref_control_wifi_default));
            String silent_phone = prefs.getString(getString(R.string.pref_silent_phone_key),
                    getString(R.string.pref_silent_phone_default));

            String alarm = prefs.getString(getString(R.string.pref_set_alarm_key),
                    getString(R.string.pref_set_alarm_default));

            String[] data = {messages,cam_light,silent_phone,wifi,control_3g,alarm};
            return new ArrayList<String>(Arrays.asList(data));
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
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        removeNotification();

        bindService(new Intent(this, ShortcutService.class), mConnection,
                Context.BIND_AUTO_CREATE);

    }


    private void removeNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(SHORTCUT_NOTIFICATION_ID);
    }

    @Override
    protected void onStop() {
        super.onStop();

        startNotification();
    }


    public void startNotification(){

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_mic_48)
                        .setContentTitle("Touchless")
                        .setContentText("Show Options")
                        .setOngoing(true);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // SHORTCUT_ID allows you to update the notification later on.
        mNotificationManager.notify(SHORTCUT_NOTIFICATION_ID, mBuilder.build());
    }

}
