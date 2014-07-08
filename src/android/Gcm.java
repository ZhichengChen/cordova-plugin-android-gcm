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

import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
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
import android.util.Log;
import android.widget.TextView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class Gcm extends CordovaPlugin {

    private static final String LOG_TAG = "GcmPlugin";
    private GCMReceiver mGCMReceiver;
    private IntentFilter mOnRegisteredFilter;
    private TextView mStatus;

    private static String sender_id = null;
    private static String server_url = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext command) throws JSONException {
        if ("start".equals(action)) {
          cordova.getThreadPool().execute( new Runnable() {
              public void run() {
                JSONObject arguments = args.optJSONObject(0);
                Options options      = new Options(context).parse(arguments);
                start(options, true);
                command.success();
              }
          });
        }
    }

    public static void start(Options options, boolean doFireEvent) {
        mGCMReceiver = new GCMReceiver();
        mOnRegisteredFilter = new IntentFilter();
        mOnRegisteredFilter.addAction(Constants.ACTION_ON_REGISTERED);

        sender_id = options.getJSON().project_num;
        server_url = options.getJSON().server_url;

        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);
        final String regId = GCMRegistrar.getRegistrationId(this);
        if (!regId.equals("")) {
          sendIdToServer(regId);
        } else {
          GCMRegistrar.register(this, sender_id);
        }
    }

    private void sendIdToServer(String regId) {
      String status = getString(R.string.gcm_registration, regId);
      mStatus.setText(status);
      (new SendRegistrationIdTask(regId)).execute();
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

      String status = getString(R.string.server_registration, mRegId);
      mStatus.setText(status);
    }
  }
}