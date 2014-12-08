#cordova-plugin-android-gcm

Cordova plugin for Google Cloud Messaging for Android.

Base on [https://github.com/chiuki/android-gcm](https://github.com/chiuki/android-gcm).
#Installation
    cordova plugin add https://github.com/ZhichengChen/cordova-plugin-android-gcm
    
#Requirement

##Configure & Send Google Cloud Messaging Notifications

###Creating a Google API project

To create a Google API project:

* Open the [Google APIs Console](https://console.developers.google.com) page.
* Click `Create project`. 
* Take note of the value **Project Number**, it will be used later on as the GCM sender ID.

###Enabling the GCM Service

To Enable the GCM service:

* Click `Enable an API` button.
* Turn the `Google Cloud Messaging for Android` toggle to ON.
* In the Terms of Service page, accept the terms.

###Obtaining an API Key

To obtain an API key:

* Select the `Credentials` under the menu of `APIS & AUTH`.
* Click `Create new Key` button.
* Select the `Server key` in the prompt.
* Click `Create`.
* Take note of the **API KEY** value, as it will be used later on.

##The implementation of server

* [http://developer.android.com/google/gcm/server.html](http://developer.android.com/google/gcm/server.html)
* [https://github.com/chiuki/android-gcm/blob/master/server/gcm_server.py](https://github.com/chiuki/android-gcm/blob/master/server/gcm_server.py)
* [https://github.com/ToothlessGear/node-gcm](https://github.com/ToothlessGear/node-gcm)

This is a python example which can run on heorku.

	import os
	from flask import Flask
	from flask import request
	import requests
	import json

	app = Flask(__name__)

	@app.route('/')
	def index():
	  return 'GCM Server'
	
	@app.route('/send', methods=['POST'])
	def send():
	  id_str = request.form['id']
	  msg_str = request.form['msg']
	  data = {
	  'registration_ids' : [id_str],
    	'data' : {
      	'msg' : msg_str
	    }
	  }
	
	  headers = {
    	'Content-Type' : 'application/json',
	    'Authorization' : 'key=/**Your API KEY**/'
	  }
	
	  url = 'https://android.googleapis.com/gcm/send'
	
	  r = requests.post(url, data=json.dumps(data), headers=headers)  
	  return r.text


##Usage

 First copy `src\com.sqisland.android.gcm_client\GCMIntentServices.java` to your project path, for example `src\com.example.hello\GCMIntentServices.java`.

 Then excute as follow in your js file:

    plugin.gcm.start(<change it to your server address>, <change it to your prject number>, function(regId){
       console.log(regId);
    });

When the app starts, it checks if is already registered with GCM. If not, it registers the device with GCM, then send the registration id to the server.

 
#Support Platforms
* Android