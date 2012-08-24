package org.ncrmnt.utool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;



public class uToolService extends Service implements Runnable {


	SharedPreferences mPrefs;
	static boolean running = false;
	boolean storageValid = false; // true if our target volume is around
	private String mpoint, script, markerfile;
	private boolean asroot;
	private static final String TAG = "utool";
	private Context c;

	/* bcasts we send out. Gui will pick up those */
	public static final String UFLASH_MOUNTED = "org.ncrmnt.utool.actions.FLASH_DETECTED";
	public static final String UFLASH_REMOVED = "org.ncrmnt.utool.actions.FLASH_REMOVED";
	public static final String USCRIPT_DONE = "org.ncrmnt.utool.actions.USCRIPT_DONE";


	public void broadcast(String event) {
		Log.d(TAG, "BROADCAST: " + event);
		Intent i = new Intent();
		i.setAction(event);
		getApplicationContext().sendBroadcast(i);
	}

	public void RunAsRoot(String[] cmds) {
		Process p;
		Log.d(TAG, "Running root");
		try {
			p = Runtime.getRuntime().exec("su");

			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			for (String tmpCmd : cmds) {
				Log.d(TAG, "execRoot: " + tmpCmd);
				os.writeBytes(tmpCmd + "\n");
			}
			os.writeBytes("exit\n");
			os.flush();
			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void RunAsNonRoot(String[] cmds) {
		Process p;
		Log.d(TAG, "Running non-root");
		try {
			p = Runtime.getRuntime().exec("sh");

			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			for (String tmpCmd : cmds) {
				Log.d(TAG, "execNonRoot: " + tmpCmd);
				os.writeBytes(tmpCmd + "\n");
			}
			os.writeBytes("exit\n");
			os.flush();
			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runScript(String script, boolean root, String mpoint) {
		String commands[] = { "sh " + script + " " + mpoint };
		if (!root)
			RunAsRoot(commands);
		else
			RunAsNonRoot(commands);
	}

	public String detectMountPoint(String marker) {
		String ret = null;

		Process process;
		try {
			process = new ProcessBuilder().command("mount")
					.redirectErrorStream(true).start();
			InputStream in = process.getInputStream();
			OutputStream out = process.getOutputStream();
			BufferedReader inr = new BufferedReader(new InputStreamReader(in));
			String line = "/tmp";
			do {
				line = inr.readLine();
				if (line != null) {
					String tokens[] = line.split(" ");
					String markername = tokens[1] + "/" + marker;
					// Log.d(TAG, "checking marker: " + markername);
					File fl = new File(markername);
					if (fl.exists()) {
						Log.d(TAG, "Detected marker file: " + markername);
						ret = tokens[1];
						break;
					}
				}
			} while (line != null);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (ret != null)
			Log.d(TAG, "Detected mountpoint: " + ret);
		return ret;
	}

	public class LocalBinder extends Binder {
		uToolService getService() {
			return uToolService.this;
		}
	}

	public void onCreate() {
		super.onCreate();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		asroot = mPrefs.getBoolean("root", false); 
		script = mPrefs.getString("script", "/sdcard/iorunner.sh"); 
		markerfile = mPrefs.getString("markerfile", ".IOMARKER");
		Log.d(TAG, "Created uToolService");
		Log.d(TAG, "root: " + asroot);
		Log.d(TAG, "script: " + script);
		Log.d(TAG, "markerfile: " + markerfile);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "action:" + intent.getAction());
		if (intent.getAction().equals("org.ncrmnt.utool.uToolService.MEDIA_MOUNTED"))
		{
			 mpoint = !storageValid ? detectMountPoint(markerfile) : null;
			 if ((mpoint != null) && (!storageValid)) { 
				 broadcast(UFLASH_MOUNTED); 
				 storageValid=true; 
				 Thread t = new Thread(this); 
				 t.start(); 
				 } 
		}else if (intent.getAction().equals("org.ncrmnt.utool.uToolService.MEDIA_REMOVED"))
		{
			mpoint = detectMountPoint(markerfile);
			if ((null == mpoint) && (storageValid))
			{
				/* The removed flash was the marked one */
				storageValid = false; 
				broadcast(UFLASH_REMOVED);
				/* What if we remove while an op is in progress?  */
				/* Just don't do it! */
			}
		}
		
		return START_STICKY;
	}

	public void onDestroy() {

	}

	public void run() {
		Log.d(TAG, "Processing thread started");
		runScript(script, asroot, mpoint);
		broadcast(USCRIPT_DONE);
		Log.d(TAG, "Processing done");
	}

	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = (IBinder) new LocalBinder();

}