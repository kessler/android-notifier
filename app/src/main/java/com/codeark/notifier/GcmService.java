package com.codeark.notifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.inject.Inject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import roboguice.inject.ContextSingleton;
import roboguice.util.Ln;

/**
 * Interaction and initialization of GCM service
 */
@ContextSingleton
public class GcmService {

    private final GoogleCloudMessaging gcm;
    private final SharedPreferences prefs;
    private final PackageInfo packageInfo;
    private String regId;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String GOOGLE_PROJECT_NUMBER = "718602014193";

    @Inject
    public GcmService(Context context, SharedPreferences prefs, PackageInfo packageInfo) {

        this.gcm = GoogleCloudMessaging.getInstance(context);
        this.prefs = prefs;
        this.packageInfo = packageInfo;
    }

    public void lazyInit(OnInit oi) {
        regId = getRegistrationId();

        if (regId.isEmpty()) {
            registerInBackground(oi);
        }
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public boolean checkPlayServices(Activity activity) {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Ln.i("This device is not supported.");
                activity.finish();
            }
            return false;
        }
        return true;
    }

    private String getRegistrationId() {
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");

        if (registrationId.isEmpty()) {
            Ln.i("Registration Id not found");
            return "";
        }

        Ln.i("Registration Id exist in device");

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = packageInfo.versionCode;

        if (registeredVersion != currentVersion) {
            Ln.i("App version changed from %s to %s", registeredVersion, currentVersion);
            return "";
        }

        return registrationId;
    }

    public void registerInBackground(final OnInit onInit) {
        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    regId = gcm.register(GOOGLE_PROJECT_NUMBER);
                    msg = "Device registered with gcm, registration ID=" + regId;
                    Ln.i(msg);
                    sendRegistrationIdToBackend();
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                onInit.execute(msg);
            }
        }.execute(null, null, null);
    }

    private void storeRegistrationId() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, packageInfo.versionCode);
        editor.commit();
    }


    private void sendRegistrationIdToBackend() {
        if (send("http://192.168.5.175:8080/api/0.0.1/push_reg?id=" + regId)) {
            storeRegistrationId();
        }
    }

    private static boolean send(String url) {

        HttpClient httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpGet httpget = new HttpGet(url);

        // Execute the request
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);
            // Examine the response status

            if (response.getStatusLine().getStatusCode() != 200) {
                Ln.e("send failed " + response.getStatusLine().toString());
                return false;
            }

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release

            if (entity != null) {

                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                String result = convertStreamToString(instream);
                Ln.d(result);
                // now you have the string representation of the HTML request
                instream.close();
                return true;
            }


        } catch (Exception e) {
            Ln.e(e);
        }

        return false;
    }

    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            Ln.e(e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Ln.e(e);
            }
        }
        return sb.toString();
    }

    public String getMessageType(Intent intent) {
        return gcm.getMessageType(intent);
    }
}
