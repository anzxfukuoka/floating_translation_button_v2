package jp.anzx.stsclient;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "M_________";

    public static int navigationBarHeight;
    public static int statusBarHeight;

    ListView listView;
    Integer selectedListViewItem;

    Spinner langFromSpinner;
    Spinner langToSpinner;
    Button saveButton;

    ArrayAdapter spinnerAdapter;

    Perms perms;
    TextRec textRec;

    Intent floatingButtonServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //
        getScreenSizes();

        //permissions
        perms = new Perms(this);

        perms.requestManifestPermission(Perms.WRITE_EXTERNAL_STORAGE);
        perms.requestManifestPermission(Perms.READ_EXTERNAL_STORAGE);
        perms.requestManifestPermission(Perms.FOREGROUND_SERVICE);

        perms.requestOverlay();
        perms.requestMediaProjection();

        //t
        textRec = new TextRec(this);

        //
        floatingButtonServiceIntent = new Intent(getApplicationContext(), FloatingButtonService.class);

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "☼", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                if(!isMyServiceRunning(FloatingButtonService.class, getApplicationContext())){

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(floatingButtonServiceIntent);
                    } else {
                        startService(floatingButtonServiceIntent);
                    }

                    fab.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);

                }else{
                    stopService(floatingButtonServiceIntent);

                    fab.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);

                }

            }
        });


        saveButton = findViewById(R.id.save);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedLangFrom = langFromSpinner.getSelectedItemPosition();
                int selectedLangTo = langToSpinner.getSelectedItemPosition();

                SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putInt("lang_from", selectedLangFrom);
                ed.putInt("lang_to", selectedLangTo);
                ed.apply();

                Toast.makeText(MainActivity.this, "saved.", Toast.LENGTH_SHORT).show();
            }
        });

        updateSpinners();
        updateListView();
    }

    void updateSpinners(){

        langFromSpinner = findViewById(R.id.lang_from);
        langToSpinner = findViewById(R.id.lang_to);

        spinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, textRec.getDownloadedList());
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        langFromSpinner.setAdapter(spinnerAdapter);
        langToSpinner.setAdapter(spinnerAdapter);

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        int langFromId = preferences.getInt("lang_from", 0);
        int langToId = preferences.getInt("lang_to", 1);

        langFromSpinner.setSelection(langFromId);
        langToSpinner.setSelection(langToId);

    }

    void updateListView(){

        listView = findViewById(R.id.listView);

        final int colorEnabled = ResourcesCompat.getColor(getResources(), R.color.colorAccent, null);
        final int colorDisabled = ResourcesCompat.getColor(getResources(), R.color.colorMetal, null);
        final int colorSelected = ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null);

        final ArrayList<String> downloadedList = textRec.getDownloadedList();
        final ArrayList<String> availableList = new ArrayList(Arrays.asList(Lang.LL3));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, availableList){

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);


                if(downloadedList.contains(availableList.get(position))){
                    text.setTextColor(colorEnabled);
                }else{
                    text.setTextColor(colorDisabled);
                    text.setText(text.getText() + " (download)");
                }

                return view;
            }

        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                Toast.makeText(getApplicationContext(), ((TextView) itemClicked).getText(),
                        Toast.LENGTH_SHORT).show();

                // какая же хуйня блять
                if(((TextView) itemClicked).getCurrentTextColor() == colorDisabled){

                    textRec.downloadTessData(availableList.get(position));

                    ((TextView) itemClicked).setTextColor(colorEnabled);
                    ((TextView) itemClicked).setText(availableList.get(position));

                }
                if(((TextView) itemClicked).getCurrentTextColor() == colorEnabled){

                    if(selectedListViewItem != null)
                        ((TextView) listView.getChildAt(selectedListViewItem)).setTextColor(colorEnabled);

                    ((TextView) itemClicked).setTextColor(colorSelected);
                    selectedListViewItem = position;

                }

            }
        });

    }

    @Override
    protected void onResume(){
        super.onResume();
        updateSpinners();
        updateListView();
    }

    void tesstest(){

        //textRec.downloadTessData( "rus");
        //textRec.downloadTessData( "eng");
        //textRec.downloadTessData( "jpn");

        //textRec.getDownloadedList();


        final Bitmap b = BitmapFactory.decodeResource(getResources(),
                R.drawable.test_ru2);

        //Log.i(TAG, textRec.extractText(b, "rus"));
    }

    void tstest(){
        TSClient tsClient = new TSClient();

        tsClient.setLangFrom("ja");
        tsClient.setLangTo("en");

        tsClient.setTsClientListener(new TSClient.TSClientListener() {
            @Override
            public void OnTranslated(String text) {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });

        tsClient.TS("木");
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        if(perms.IsManifestPermissionGot(requestCode, permissions, grantResults)){

        }else {
            perms.requestManifestPermission(requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        perms.onActivityResult(requestCode, resultCode, data);

    }

    void getScreenSizes()
    {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int height = size.y;

        Rect rectgle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectgle);

        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            MainActivity.navigationBarHeight = resources.getDimensionPixelSize(resourceId);
        }

        MainActivity.statusBarHeight = height - rectgle.bottom - navigationBarHeight;

        Log.i(TAG, navigationBarHeight + " " + statusBarHeight);

    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager)context. getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("Service already","running");
                return true;
            }
        }
        Log.i("Service not","running");
        return false;
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
}