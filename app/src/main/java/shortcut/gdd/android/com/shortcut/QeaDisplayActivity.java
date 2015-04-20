package shortcut.gdd.android.com.shortcut;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import shortcut.gdd.android.com.shortcut.data.QeaContract;

/**
 * Created by Samuel PC on 19/04/2015.
 */
public class QeaDisplayActivity extends ActionBarActivity {

    Context mContext;
    static ArrayAdapter mAdapter = null;
    static ArrayList<String> questions = new ArrayList<String>();
    static ArrayList<String> answers = new ArrayList<String>();
    static ArrayList<Integer> ids = new ArrayList<Integer>();

    private static final String[] QEA_COLUMNS = {
            QeaContract.QEAEntry.TABLE_NAME + "." + QeaContract.QEAEntry._ID,
            QeaContract.QEAEntry.COLUMN_DATE,
            QeaContract.QEAEntry.COLUMN_QUESTION,
            QeaContract.QEAEntry.COLUMN_ANSWER
    };
    
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


    public void addQea(View view){

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_qea);
        dialog.setTitle(getString(R.string.action_qea));

        Button dialogButtonAdd = (Button) dialog.findViewById(R.id.qea_btnadd);
        // if button is clicked, close the custom dialog
        dialogButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView quest = (TextView) dialog.findViewById(R.id.question);
                String question = quest.getText().toString();

                TextView answ = (TextView) dialog.findViewById(R.id.answer);
                String answer = answ.getText().toString();

                if(!question.isEmpty() && !answer.isEmpty()){
                    //insert on list
                    Time time = new Time();
                    time.setToNow();

                    ContentValues qeaValues = new ContentValues();
                    qeaValues.put(QeaContract.QEAEntry.COLUMN_DATE, Long.toString(time.toMillis(false)));
                    qeaValues.put(QeaContract.QEAEntry.COLUMN_QUESTION, question);
                    qeaValues.put(QeaContract.QEAEntry.COLUMN_ANSWER, answer);
                    mContext = getApplicationContext();

                    if(mContext!=null) {
                        Uri qeaUri = mContext.getContentResolver().insert(QeaContract.QEAEntry.CONTENT_URI, qeaValues);
                        questions.add(question);
                        answers.add(answer);
                    }
                    mAdapter.notifyDataSetChanged();
                    //view.setAlpha(1);

                    dialog.dismiss();
                }
            }
        });

        Button dialogButtonCancel = (Button) dialog.findViewById(R.id.qea_btncancel);
        // if button is clicked, close the custom dialog
        dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        Context mContext;
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            questions = new ArrayList<>();
            answers = new ArrayList<>();
            ids = new ArrayList<>();
            mContext = getActivity();

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
                    int id = qeaCursor.getColumnIndex(QeaContract.QEAEntry._ID);
                    questions.add(qeaCursor.getString(qId));
                    answers.add(qeaCursor.getString(aId));
                    ids.add(qeaCursor.getInt(id));
                    qeaCursor.moveToNext();
                }
                qeaCursor.close();
            }
            mAdapter =
                    new ArrayAdapter<String>(
                            getActivity(), // The current context (this activity)
                            R.layout.list_item_qea, // The name of the layout ID.
                            R.id.list_item_qea_textview, // The ID of the textview to populate.
                            questions);

            View rootView = inflater.inflate(R.layout.qea_list, container, false);

            ListView listView = (ListView) rootView.findViewById(R.id.listview_qea);
            listView.setAdapter(mAdapter);


            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        final int position, long id) {

                    mContext = getActivity();
                    final String item = (String) parent.getItemAtPosition(position);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            mContext);

                    // set title
                    alertDialogBuilder.setTitle(getString(R.string.qea_dialog_item_title));

                    String aux = answers.get(position);
                    // set dialog message
                    alertDialogBuilder
                            .setMessage("Answer:"+aux+".\n\nClick yes to remove item!")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    answers.remove(position);
                                    view.animate().setDuration(1000).alpha(0)
                                            .withEndAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                    questions.remove(item);
                                                    mAdapter.notifyDataSetChanged();
                                                    view.setAlpha(1);
                                                }
                                            });
                                    int index = ids.get(position);
                                    ids.remove(position);

                                    mContext = getActivity();
                                    mContext.getContentResolver().delete(QeaContract.QEAEntry.CONTENT_URI,
                                            QeaContract.QEAEntry._ID + " = ?", new String[]{String.valueOf(index)});

                            }
                            })
                            .setNegativeButton("No",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, just close
                                    // the dialog box and do nothing
                                    dialog.cancel();
                                }
                            });

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
                }





            });


            return rootView;
        }
    }
}
