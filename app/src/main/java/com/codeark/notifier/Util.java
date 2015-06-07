package com.codeark.notifier;

import android.content.Context;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import roboguice.util.Ln;

/**
 * Created by kessler on 6/6/15.
 */
public class Util {
    static String convertStreamToString(InputStream is) {
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

    static void toast(Context ctx, String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    static String httpGet(String url) throws IOException, BadResponseCodeException {

        HttpClient httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpGet httpget = new HttpGet(url);

        // Execute the request
        HttpResponse response;
        response = httpclient.execute(httpget);
        // Examine the response status

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            Ln.e("send failed " + response.getStatusLine().toString());
            throw new BadResponseCodeException(statusCode);
        }

        // Get hold of the response entity
        HttpEntity entity = response.getEntity();
        // If the response does not enclose an entity, there is no need
        // to worry about connection release

        if (entity != null) {

            // A Simple JSON Response Read
            InputStream instream = entity.getContent();
            String result = Util.convertStreamToString(instream);
            Ln.d(result);
            // now you have the string representation of the HTML request
            instream.close();
            return result;
        }

        return "";
    }

    static String encodeURIComponent(String component) {
        String result = null;

        try {
            result = URLEncoder.encode(component, "UTF-8")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = component;
        }

        return result;
    }

    static class BadResponseCodeException extends Exception {
        private int code;

        BadResponseCodeException(int code) { this.code = code; }

        int getCode() { return code; }
    }
}
