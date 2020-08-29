package com.remoteit;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConnectdWrapper {
    private final String TAG = this.getClass().getSimpleName();
    private final String connectdFilename = "connectd.so";
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
        connectdLocation = context.getApplicationInfo().nativeLibraryDir + "/" + connectdFilename;
        instances = new HashMap<String, ConnectdInstance>();
    }

    public String getName() {
        return "ConnectdWrapper";
    }

    public void init() throws IOException {
        Log.d(TAG, "XXX Init connectd");
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
                    return;
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
