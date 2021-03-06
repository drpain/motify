/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.networkusage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;



/**
 * Main Activity for the sample application.
 *
 * This activity does the following:
 *
 * o Presents a WebView screen to users. This WebView has a list of HTML links to the latest
 *   questions tagged 'android' on stackoverflow.com.
 *
 * o Parses the StackOverflow XML feed using XMLPullParser.
 *
 * o Uses AsyncTask to download and process the XML feed.
 *
 * o Monitors preferences and the device's network connection to determine whether
 *   to refresh the WebView content.
 */
public class NetworkActivity extends Activity {
	
    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";
	
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
        
    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;

    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver receiver = new NetworkReceiver();       
    
    // Application specific vars
    public static String IPAddress 		= null;
    public static String IPRange 		= null;
    public static boolean manualConnect = false;
    public static String serverIp 		= null;
    public static boolean connected 	= false;
    public static String port			= "8080";
    private ProgressDialog pDialog;
    private float x1,x2;
    static final int MIN_DISTANCE 		= 150;
    		

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        
        this.registerReceiver(receiver, filter);   
        setContentView(R.layout.main);
    }
    
       

    // Refreshes the display if the network connection and the
    // pref settings allow it.
    @Override
    public void onStart() {
        super.onStart();

        // Gets the user's network preference settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);        
        updateConnectedFlags();
       
        // Check whether we should connect manually or scan the network
        manualConnect 	= sharedPrefs.getBoolean("manualConnect", false);   
        port 			= sharedPrefs.getString("serverPort", "8080");
        IPAddress 		= IPAddress();        
        IPRange 		= IPRange();
        
        // If we are to manually connect, then we can set the IP Right now
        if (manualConnect == true)
        {
        	// Get the IP From the preferences
        	serverIp = sharedPrefs.getString("serverIp", "192.168.1.4");
        }
        
        Log.d("motify", "IP ADDRESS: " + IPAddress);        
        Log.d("motify", "IP RANGE: " + IPRange);        
        Log.d("motify", "MANUAL CONNECT: " + String.valueOf(manualConnect));
        Log.d("motify", "SERVER IP: " + serverIp);
        Log.d("motify", "SERVER PORT: " + port);
        
        // Only loads the page if refreshDisplay is true. Otherwise, keeps previous
        // display. For example, if the user has set "Wi-Fi only" in prefs and the
        // device loses its Wi-Fi connection midway through the user using the app,
        // you don't want to refresh the display--this would force the display of
        // an error page instead of stackoverflow.com content.
        if (refreshDisplay) {
            loadPage();
        }
    } 
    
    
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
	   if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) { 
		  
		   if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		   {
			   //Log.d("motify", "Volume DOWN");
			   Command("vol-down");
		   }
		   
		   else {
			   //Log.d("motify", "Volume UP");
			   Command("vol-up");
		   }
		   return true;
	   } else {
	       return super.onKeyDown(keyCode, event); 
	   }
	}   
    
    // Get the IP Address of the phone    
    public String IPAddress() {    	
    	
    	WifiManager myWifiManager 	= (WifiManager)getSystemService(Context.WIFI_SERVICE);        
        WifiInfo myWifiInfo 		= myWifiManager.getConnectionInfo();
        int myIp 					= myWifiInfo.getIpAddress();
       
        int intMyIp3 				= myIp/0x1000000;
        int intMyIp3mod 			= myIp%0x1000000;
       
        int intMyIp2 				= intMyIp3mod/0x10000;
        int intMyIp2mod 			= intMyIp3mod%0x10000;
       
        int intMyIp1 				= intMyIp2mod/0x100;
        int intMyIp0 				= intMyIp2mod%0x100;
       
        // We would like the full one please
        return String.valueOf(intMyIp0)
  	          + "." + String.valueOf(intMyIp1)
  	          + "." + String.valueOf(intMyIp2)
  	          + "." + String.valueOf(intMyIp3);
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {     
        switch(event.getAction())
        {
          case MotionEvent.ACTION_DOWN:
              x1 = event.getX();                         
          break;         
          case MotionEvent.ACTION_UP:
              x2 = event.getX();
              float deltaX = x2 - x1;
              
              if (Math.abs(deltaX) > MIN_DISTANCE)
              {
            	  // Left to Right swipe action
            	  if (x2 > x1)
            	  {
            		  //Toast.makeText(this, "Left to Right swipe [Next]", Toast.LENGTH_SHORT).show ();
            		  Command("prev");
            	  }
            	  
            	  // Right to left swipe action            	  
            	  else 
            	  {
            		  //Toast.makeText(this, "Right to Left swipe [Previous]", Toast.LENGTH_SHORT).show ();
            		  Command("next");
            	  }
                
              }
              else
              {
                  // consider as something else - a screen tap for example
              }                          
          break;   
        }           
        return super.onTouchEvent(event);       
    }
    
    
    // Manually fire off command to the server
    public void Command(String input) {
    	Log.d("motify", "COMMAND: Running " + input);
    	//new SendCommand().execute(input);
    	
    	if (connected == true)
    	{
    		new ServerCommand().execute("command=" + input);
    	}
    	
    	else 
    	{
    		notice("Not connected to server!");
    		Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(settingsActivity);
    	}
    } 
    
    // Manually fire off command to the server
    public void button_command(View view) {
    	
    	String tag = (String) view.getTag();
    	Log.d("motify", "COMMAND: Running " + tag);
    	//new SendCommand().execute(tag);
    	
    	if (connected == true)
    	{
    		new ServerCommand().execute("command=" + tag);
    	}
    	
    	else 
    	{
    		notice("Not connected to server!");
    		Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(settingsActivity);
    	}
    }

    
    // Get the IP Address Range of the phone    
    public String IPRange() {    	
    	
    	WifiManager myWifiManager 	= (WifiManager)getSystemService(Context.WIFI_SERVICE);        
        WifiInfo myWifiInfo 		= myWifiManager.getConnectionInfo();
        int myIp 					= myWifiInfo.getIpAddress();
       
        int intMyIp3 				= myIp/0x1000000;
        int intMyIp3mod 			= myIp%0x1000000;
       
        int intMyIp2 				= intMyIp3mod/0x10000;
        int intMyIp2mod 			= intMyIp3mod%0x10000;
       
        int intMyIp1 				= intMyIp2mod/0x100;
        int intMyIp0 				= intMyIp2mod%0x100;
       
        // We would like the full one please
        return String.valueOf(intMyIp0)
  	          + "." + String.valueOf(intMyIp1)
  	          + "." + String.valueOf(intMyIp2);
    }

    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }
    

    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    private void updateConnectedFlags() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
        } else {
            wifiConnected = false;
        }
    }

    // Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
    // This avoids UI lock up. To prevent network operations from
    // causing a delay that results in a poor user experience, always perform
    // network operations on a separate thread from the UI.
    private void loadPage() {
        if (wifiConnected) {
        	
            // AsyncTask subclass
            //new DownloadXmlTask().execute(URL);
        	//SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);        
                        
            // If we are to manually connect, then we can set the IP Right now
            if (manualConnect == true)
            {
            	// Check the server connection
            	//new ServerStateCheck().execute(serverIp);
            	new ServerCommand().execute("alive=true");
            }
        	
        } else {
            error("failed");
        }
    }
    
    // This is a dummy class to indicate server responses
    private class IndicateFailure extends AsyncTask<String, Void, String> {
    	
    	protected void onPreExecute (){
    		ImageView imageView = (ImageView)findViewById(R.id.top_section);  
    		imageView.setImageResource(R.drawable.top_section_failed);
    	}
    	
        @Override
        protected String doInBackground(String... state) {        	
            try {  
                Thread.sleep(500); // no need for a loop
            } catch (InterruptedException e) {
                Log.e("motify", "Interrupted", e);
                return "Interrupted";
            }
            return "Executed";
        }      

        @Override
        protected void onPostExecute(String result) {  
        	
        	// Either way set the original image back
        	ImageView imageView = (ImageView)findViewById(R.id.top_section);
         	imageView.setImageResource(R.drawable.top_section_test);
        }
    }  
    
    // This is a dummy class to indicate server responses
    private class IndicateSuccess extends AsyncTask<String, Void, String> {
    	
    	protected void onPreExecute (){
    		ImageView imageView = (ImageView)findViewById(R.id.top_section);  
    		imageView.setImageResource(R.drawable.top_section_action);
    	}
    	
        @Override
        protected String doInBackground(String... state) {        	
            try {  
                Thread.sleep(150); // no need for a loop
            } catch (InterruptedException e) {
                Log.e("motify", "Interrupted", e);
                return "Interrupted";
            }
            return "Executed";
        }      

        @Override
        protected void onPostExecute(String result) {  
        	
        	// Either way set the original image back
        	ImageView imageView = (ImageView)findViewById(R.id.top_section);
         	imageView.setImageResource(R.drawable.top_section_test);
        }
    }
    
    

    // Populates the activity's options menu.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    // Handles the user's menu selection.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.settings:
                Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(settingsActivity);
                return true;
        /*case R.id.refresh:
                loadPage();
                return true;*/
        default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    
    // Uses AsyncTask to create a task away from the main UI thread. This task takes a 
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class ServerCommand extends AsyncTask<String, Void, String> {
       @Override
       protected String doInBackground(String... urls) {
             
           // params comes from the execute() call: params[0] is the url.
           try {
               return downloadUrl("http://" + serverIp + ":" + port + "/?" + urls[0]);
           } catch (IOException e) {
        	   return "Unable to connect to server";
           }
       }
       
       // onPostExecute displays the results of the AsyncTask.
       @Override
       protected void onPostExecute(String result) {	   
		   Log.d("motify", "RESULT: " + result);
		   
		   if (result == "SUCCESS") {
			   new IndicateSuccess().execute(result);
		   }
		   
		   else {
			   new IndicateFailure().execute(result);
		   }
      }
   }
    
    protected void handleException(Exception e) {
        e.printStackTrace();
    }
    
    
	 // Given a URL, establishes an HttpUrlConnection and retrieves
	 // the web page content as a InputStream, which it returns as
	 // a string.
	 private String downloadUrl(String myurl) throws IOException {
	     InputStream is = null;
	     // Only display the first 500 characters of the retrieved
	     // web page content.
	     int len = 1;	         
	     try {
	         URL url = new URL(myurl);
	         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	         conn.setReadTimeout(10000 /* milliseconds */);
	         conn.setConnectTimeout(15000 /* milliseconds */);
	         conn.setRequestMethod("GET");
	         conn.setDoInput(true);
	         // Starts the query
	         conn.connect();
	         int response = conn.getResponseCode();
	         len = conn.getContentLength();
	         //Log.d("motify", "Content Lenght: " + String.valueOf(len));	         
	         //Log.d("motify", "RESPONSE CODE: " + response);	         
	         is = conn.getInputStream();
	         
	         // Convert the InputStream into a string
	         String contentAsString = readIt(is, len);	 
	         	         
         	 if (response == 201) {				 
			    connected = true;
			    return "SUCCESS";
		     }
		   
         	 else {
			    error("connection");
			    connected = false;
			    return "FAILURE";	
         	 }         	  
	         
	     // Makes sure that the InputStream is closed after the app is
	     // finished using it.
	     } finally {
	         if (is != null) {
	             is.close();
	         } 
	     }
	 }
	 
	 
	 // Reads an InputStream and converts it to a String.
	 public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
	     Reader reader = null;
	     reader = new InputStreamReader(stream, "UTF-8");        
	     char[] buffer = new char[len];
	     reader.read(buffer);
	     return new String(buffer);
	 }    
    
    
    // I would like a generic way of dealing with errors
    public void error(String type) {
    	
    	// Manage a connection issue
    	if (type == "connection") {
    		Toast.makeText(NetworkActivity.this, "Failed to connect to server! " + serverIp, Toast.LENGTH_SHORT).show();
    	}
    }
    
    
    // I would like a generic way of dealing with errors
    public void notice(String notice) {
    	
    	Toast.makeText(NetworkActivity.this, notice, Toast.LENGTH_SHORT).show();    	
    }
   

    /**
     *
     * This BroadcastReceiver intercepts the android.net.ConnectivityManager.CONNECTIVITY_ACTION,
     * which indicates a connection change. It checks whether the type is TYPE_WIFI.
     * If it is, it checks whether Wi-Fi is connected and sets the wifiConnected flag in the
     * main activity accordingly.
     *
     */
    public class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
        	
            ConnectivityManager connMgr 	= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo 		= connMgr.getActiveNetworkInfo();

            // Checks the user prefs and the network connection. Based on the result, decides
            // whether
            // to refresh the display or keep the current display.
            // If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // If device has its Wi-Fi connection, sets refreshDisplay
                // to true. This causes the display to be refreshed when the user
                // returns to the app.
                refreshDisplay = true;
                Toast.makeText(context, R.string.wifi_connected, Toast.LENGTH_SHORT).show();

            // No WIFI Network connection
            } else {
                refreshDisplay = false;
                Toast.makeText(context, R.string.lost_connection, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
