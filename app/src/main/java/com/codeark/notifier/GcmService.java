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

import java.io.IOException;
import java.util.UUID;

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
    private Context context;

    @Inject
    public GcmService(Context context, SharedPreferences prefs, PackageInfo packageInfo) {

        this.context = context;
        this.gcm = GoogleCloudMessaging.getInstance(context);
        this.prefs = prefs;
        this.packageInfo = packageInfo;
        this.regId = getRegistrationId();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    boolean checkPlayServices(Activity activity) {

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

    String getRegistrationId() {
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

    void registerInBackground(final OnRegister onRegister) throws MissingGoogleProjectNumberException {
        if (getGoogleProjectNumber().isEmpty()) {
            throw new MissingGoogleProjectNumberException();
        }

        if (!getRegistrationId().isEmpty()) {
            sendRegistrationIdToBackend(onRegister);
            return;
        }

        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    regId = gcm.register(getGoogleProjectNumber());
                    msg = "Device registered with gcm, registration ID=" + regId;
                    Ln.i(msg);
                    sendRegistrationIdToBackend(onRegister);
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
                onRegister.dispatch(msg);
            }
        }.execute(null, null, null);
    }

    private void setRegistrationId() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, packageInfo.versionCode);
        editor.commit();
    }

    private void sendRegistrationIdToBackend(final OnRegister onRegister) {
        new AsyncTask<Void, Integer, String>() {

            @Override
            protected String doInBackground(Void... params) {
                try {
                    String url = getServerUrl() +
                            "api/register?handle=" + Util.encodeURIComponent(getHandle()) +
                            "&regid=" + Util.encodeURIComponent(regId) +
                            "&key=" + Util.encodeURIComponent(getHandleKey());

                    Ln.i("sending registration request to " + url);
                    String response = Util.httpGet(url);
                    Ln.d(response);
                    setHandleKey(response);
                    setRegistrationId();
                    return "registration complete";
                } catch (IOException e) {
                    Ln.e(e);
                    return "registration failed, due to an error " + e;
                } catch (Util.BadResponseCodeException e) {
                    return "registration failed, bad status code " + e.getCode();
                }
            }

            @Override
            protected void onPostExecute(String msg) {
                onRegister.dispatch(msg);
            }

        }.execute();
    }

    private String getServerUrl() {
        String serverUrl = prefs.getString("notification_server", "http://localhost:3000");

        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }

        return serverUrl;
    }

    private String getHandle() {
        String defaultHandle = UUID.randomUUID().toString();
        String handle = prefs.getString("registration_handle", defaultHandle);

        // user didn't set this so we set it for him
        if (defaultHandle.equals(handle)) {
            final SharedPreferences.Editor edit = prefs.edit();
            edit.putString("registration_handle", handle);
            edit.commit();
        }

        return handle;
    }

    private String getHandleKey() {
        return prefs.getString("handle_key", "");
    }

    private void setHandleKey(String key) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("handle_key", key);
        edit.commit();
    }

    private String getGoogleProjectNumber() {
        return prefs.getString("google_project_number", "");
    }

    public String getMessageType(Intent intent) {
        return gcm.getMessageType(intent);
    }

    static class MissingGoogleProjectNumberException extends Exception {

    }
}
