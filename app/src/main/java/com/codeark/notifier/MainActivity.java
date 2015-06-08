package com.codeark.notifier;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.inject.Inject;

import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_main)
public class MainActivity extends RoboActionBarActivity implements OnRegister {

    private GcmService gcm;

    @InjectResource(R.string.missing_google_play)
    private String missingGooglePlayMessage;

    @InjectResource(R.string.missing_google_project_number)
    private String missingGoogleProjectNumber;

    @InjectResource(R.string.not_registered)
    private String notRegisteredMessage;

    @InjectView(R.id.console)
    private TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Check device for Play Services APK.
        if (!gcm.checkPlayServices(this)) {
            append(missingGooglePlayMessage);
            return;
        }

        if (gcm.getRegistrationId().isEmpty()) {
            append(notRegisteredMessage);
            return;
        }
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
            this.showSettings();
            return true;
        }

        if (id == R.id.action_register) {
            try {
                gcm.registerInBackground(this);
            } catch (GcmService.MissingGoogleProjectNumberException e) {
                Util.toast(getApplicationContext(), missingGoogleProjectNumber);
            }
        }

        if (id == R.id.action_deregister) {
            gcm.deregisterInBackground(this);
        }

        return super.onOptionsItemSelected(item);
    }

    @Inject
    @SuppressWarnings("unused")
    public void setGcm(GcmService gcm) {
        this.gcm = gcm;
    }

    private void showSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        this.startActivity(i);
    }

    void append(String msg) {
        console.append(msg + "\n\n");
    }

    void appendAsync(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                append(msg);
            }
        });
    }


    @Override
    public void dispatch(String msg) {
        append(msg);
    }
}
