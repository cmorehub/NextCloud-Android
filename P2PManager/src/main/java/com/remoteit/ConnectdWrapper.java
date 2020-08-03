package com.remoteit;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConnectdWrapper {
    private final String TAG = this.getClass().getSimpleName();
    private final String connectdFilename = "connectd";
    public final AssetManager assetManager;
    public final File connectdPath;
    public final String connectdLocation;
    HashMap<String, ConnectdInstance> instances = null;

    public interface ConnectdEventListener {
        void connectdStatus(String address, String line);
    }

    private ConnectdWrapper.ConnectdEventListener listener;

    public void setConnectdEventListener(ConnectdWrapper.ConnectdEventListener listener) {
        this.listener = listener;
    }

    public void clearConnectdEventListener() {
        this.listener = null;
    }

    public ConnectdWrapper(Context context) {
        assetManager = context.getAssets();
        connectdPath = context.getFilesDir();
        connectdLocation = connectdPath.toString() + "/" + connectdFilename;
        instances = new HashMap<String, ConnectdInstance>();
    }

    public String getName() {
        return "ConnectdWrapper";
    }

    public void init() throws IOException {
        Log.d(TAG, "XXX Attempting to load connectd executable...");
        if (!confirmConnectd()) {
            Log.e(TAG, "XXX Could not load connectd executable");
            throw new IOException("Could not start because suitable connectd executable was not found.");
        } else {
            Log.d(TAG, "XXX Loaded connectd executable");
        }
    }

    public void exec(final String address, String[] argArray) {
        String argString = "";

        List<String> argList = Arrays.asList(argArray);

        for (Object as : argList) {
            argString += " ";
            argString += as;
        }
        execString(address, argString);
    }

    public void execString(final String address, final String argString)
    {
        Log.d(TAG, "XXX attempting to execute: connectd " + argString);

        Process p = null;
        try {
            p = Runtime.getRuntime().exec(connectdLocation + " " + argString);
        } catch(IOException e) {
            Log.e(TAG, "IOException " + e);
        }
        final Process finalProc = p;

        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                try {
                    BufferedReader dataReader = new BufferedReader(new InputStreamReader(finalProc.getInputStream()));
                    String line = "";
//                    ReactContext reactContext = getReactApplicationContext();
                    while ((line = dataReader.readLine()) != null) {
                        // emit event for each line
                        listener.connectdStatus(address, line);
                        Log.d(TAG, "XXX " + line);
                    }
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(finalProc.getErrorStream()));
                    while ((line = errReader.readLine()) != null) {
                        Log.e(TAG, "XXX " + line);
                    }
                    finalProc.waitFor();

                    Log.d(TAG, "XXX done running connectd");
                }
                catch(IOException e) {
                    Log.e(TAG, "IOException " + e);
                }
                catch (Exception ex) {
                    Thread t = Thread.currentThread();
                    t.getUncaughtExceptionHandler().uncaughtException(t, ex);
                }
            }
        });

        ConnectdInstance connectd = new ConnectdInstance(address, p);
        instances.put(address, connectd);

        thread.start();
        Log.d(TAG, "XXX ran connectd");
    }

    public void disconnect(final String address)
    {
        ConnectdInstance connectd = instances.get(address);

        if (connectd != null) {
            Log.d(TAG, "XXX kill process for " + address);

            connectd.kill();

            instances.remove(address);

            listener.connectdStatus(address, "!!exit - process killed");
        } else {
            Log.e(TAG, "XXX Error disconnecting " + address);
        }
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    private boolean confirmConnectdExistsAndExecutable() {
        File connectdFile = new File(connectdLocation);
        if (connectdFile.exists()) {
            if (connectdFile.canExecute()) {
                Log.d(TAG, "XXX connectd exists and is executable");
                return true;
            }
            Log.d(TAG, "XXX connectd exists but is not yet executable");
            return connectdFile.setExecutable(true);
        }

        Log.e(TAG, "connectd does not exist at this location: " + connectdLocation);
        return false;
    }

    private boolean confirmConnectd()
    {
        File connectdFile = new File(connectdLocation);
        connectdFile.delete(); // don't always need to delete and re-copy file, but let's be safe for now

        if (confirmConnectdExistsAndExecutable()) {
            return true;
        } else {
            try {
                File file = new File(connectdPath, connectdFilename);
                boolean success = connectdPath.mkdirs();
                if (success) {
                    Log.d(TAG, "XXX created dirs for file " + file);
                } else {
                    Log.e(TAG, "XXX could not create dirs for file. maybe it exists? " + file);
                }

                Log.d(TAG, "XXX trying to write to file " + file);

                InputStream in = null;
                String list[] = assetManager.list("connectd");
                if (isEmulator()) {
                    in = assetManager.open("connectd/connectd_x86");
                } else {
                    // determine correct binary for device and copy that one to destination
                    // the vast majority of devices at this time use armebi (specifically armeabiv7)
                    in = assetManager.open("connectd/connectd_armeabi");
                }
                if (in != null) {
                    FileOutputStream out = new FileOutputStream(file);
                    int read;
                    byte[] buffer = new byte[4096];
                    while ((read = in.read(buffer)) > 0)
                        out.write(buffer, 0, read);
                    out.close();
                    in.close();
                } else {
                    Log.e(TAG, "XXX confirmConnectd no connectd executable found for architecture ");
                    return false;
                }

                return confirmConnectdExistsAndExecutable();
            }
            catch(IOException e) {
                Log.e(TAG, "XXX confirmConnectd got IOException " + e);
                return false;
            }
        }
    }

    private class ConnectdInstance extends Object
    {
        private String address;
        private Process process;

        // store thread/process
        public ConnectdInstance(String address, Process p) {
            super();

            this.address = address;
            this.process = p;
        }

        public void kill() {
            this.process.destroy();
            this.process = null;
        }
    }
}
