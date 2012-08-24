package org.ncrmnt.utool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import java.lang.Thread;

public class AutoStartRecv extends BroadcastReceiver {
	private static final String TAG = "utool";

	@Override
		public void onReceive(Context context, Intent intent) {
			SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			boolean enable = mPrefs.getBoolean("enable", false);
			//handleEvent(intent);
			String action = "org.ncrmnt.utool.uToolService";
			if (enable)
			{
				Log.d(TAG,"Passing data to service");
				/* Sometimes, as a C coder, I want to put a bullet into the brains of whoever created this smoking shit */
				if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED))
				{
					action = "org.ncrmnt.utool.uToolService.MEDIA_MOUNTED";
				} else if (intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED) ||
					 (intent.getAction().equals(Intent.ACTION_MEDIA_BAD_REMOVAL))) {
					action = "org.ncrmnt.utool.uToolService.MEDIA_REMOVED";
				} 
					
				Intent i = new Intent();
				i.setAction(action);
				context.startService(i);
					
				/* this class gets created everytime some shit happens */
				/* Therefore, we need to bind the tSrv */
			}
		}
	/*
	 * if (enable) { 
	 * 
	 * 
	 *  { Log.d(TAG,
	 * "We have detected some new storage!11");
	 * 

	 */
	/*
	 * } else if ((intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED) ||
	 * (intent.getAction().equals(Intent.ACTION_MEDIA_BAD_REMOVAL)))) {
	 * Log.d(TAG, "Storage valid: " + storageValid); if (storageValid) { String
	 * mpoint = detectMountPoint(markerfile); if (mpoint == null) { storageValid
	 * = false; broadcast(UFLASH_REMOVED); } } Log.d(TAG, "Storage valid: " +
	 * storageValid);
	 * 
	 * }
	 * 
	 * }
	 */

}
