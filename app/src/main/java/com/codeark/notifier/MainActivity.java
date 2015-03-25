package com.codeark.notifier;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;

import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_main)
public class MainActivity extends RoboActionBarActivity {

    private GcmService gcm;

    private String missingGooglePlayMessage;

    private TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Check device for Play Services APK.
        if (!gcm.checkPlayServices(this)) {
            Toast.makeText(getApplicationContext(), missingGooglePlayMessage, Toast.LENGTH_LONG).show();
            return;
        }

        gcm.lazyInit(new OnInit() {
            @Override
            public void execute(String msg) {
                console.append(msg + "\n");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        gcm.checkPlayServices(this);
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

    @Inject
    @SuppressWarnings("unused")
    public void setGcm(GcmService gcm) {
        this.gcm = gcm;
    }

    @InjectResource(R.string.missing_google_play)
    @SuppressWarnings("unused")
    public void setMissingGooglePlayMessage(String message) {
        this.missingGooglePlayMessage = message;
    }

    @InjectView
    @SuppressWarnings("unused")
    public void setConsole(TextView console) {
        this.console = console;
    }
}
