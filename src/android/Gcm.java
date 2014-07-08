/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package com.sqisland.android.gcm_client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import com.google.android.gcm.GCMRegistrar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class Gcm extends CordovaPlugin {

    private static final String LOG_TAG = "GcmPlugin";
    private static GCMReceiver mGCMReceiver;
    private static IntentFilter mOnRegisteredFilter;
    private static TextView mStatus;

    private static String sender_id = null;
    private static String server_url = null;
    protected static Context context = null;
    private   static CordovaWebView webView = null;

    @Override
    public void initialize (CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Gcm.webView = super.webView;
        Gcm.context = super.cordova.getActivity().getApplicationContext();
    }
    
    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext command) throws JSONException {
        if ("start".equals(action)) {
          cordova.getThreadPool().execute( new Runnable() {
              public void run() {
                try {
          start(args.getString(0), args.getString(1), true);
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
                command.success();
              }
          });
        }
        return true;
    }

    public void start(String sender_id, String sever_url, boolean doFireEvent) {
        mGCMReceiver = new GCMReceiver();
        mOnRegisteredFilter = new IntentFilter();
        mOnRegisteredFilter.addAction(Constants.ACTION_ON_REGISTERED);

        sender_id = sender_id;
        server_url = sever_url;

        GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context);
        final String regId = GCMRegistrar.getRegistrationId(context);
        if (!regId.equals("")) {
          sendIdToServer(regId);
        } else {
          GCMRegistrar.register(context, sender_id);
        }
    }

    private static void sendIdToServer(String regId) {
      String status = "Got id from Google: " + regId;
      mStatus.setText(status);
      //TODO register id
//      (new SendRegistrationIdTask(regId)).execute();
    }

    private class GCMReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String regId = intent.getStringExtra(Constants.FIELD_REGISTRATION_ID);
      sendIdToServer(regId);
    }
  }

  private final class SendRegistrationIdTask extends
      AsyncTask<String, Void, HttpResponse> {
    private String mRegId;

    public SendRegistrationIdTask(String regId) {
      mRegId = regId;
    }

    @Override
    protected HttpResponse doInBackground(String... regIds) {
      String url = server_url + "/register";
      HttpPost httppost = new HttpPost(url);

      try {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("reg_id", mRegId));
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpClient httpclient = new DefaultHttpClient();
        return httpclient.execute(httppost);
      } catch (ClientProtocolException e) {
        Log.e(Constants.TAG, e.getMessage(), e);
      } catch (IOException e) {
        Log.e(Constants.TAG, e.getMessage(), e);
      }

      return null;
    }

    @Override
    protected void onPostExecute(HttpResponse response) {
      if (response == null) {
        Log.e(Constants.TAG, "HttpResponse is null");
        return;
      }

      StatusLine httpStatus = response.getStatusLine();
      if (httpStatus.getStatusCode() != 200) {
        Log.e(Constants.TAG, "Status: " + httpStatus.getStatusCode());
        mStatus.setText(httpStatus.getReasonPhrase());
        return;
      }

      String status = "Sent id to server: " + mRegId;
      mStatus.setText(status);
    }
  }
}