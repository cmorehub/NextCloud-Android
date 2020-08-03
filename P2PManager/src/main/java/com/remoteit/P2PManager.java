package com.remoteit;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * Remoteit Peer to Peer Manager
 */
public class P2PManager {

    public interface P2PEventListener {
        void p2pSignedOut(String userName);
        void p2pConnectionSucceeded(String deviceAddress, String url);
        void p2pConnectionFailed(String deviceAddress, String reason);
        void p2pConnectionDestroyed(String deviceAddress, String reason);
    }

    private P2PEventListener listener;

    private final String TAG = this.getClass().getSimpleName();
    private final ConnectdWrapper connectdWrapper;

    // Declare default connection parameters to use if explicit options are not passed to connectDevice();
    private String defaultBindAddress = "127.0.0.1";
    private final String defaultRestrictAddress = "0.0.0.0";
    private int defaultEncryption = 2;
    private int defaultMaxOutstanding = 0;
    private int defaultProxyLifetimeSec = 0;
    private int defaultIdleLimitSec = 0;
    private final Context context;

    // Define name strings for P2PManager notifications for use in EventEmitter on() and emit() calls.
    static final String SIGNEDOUT = "p2pSignedOut";
    static final String CONNECTIONSUCCEEDED = "p2pConnectionSucceeded";
    static final String CONNECTIONFAILED = "p2pConnectionFailed";
    static final String CONNECTIONDESTROYED = "p2pConnectionDestroyed";

    // Set the valid port range to assign to a connectd instance.
    private final int minPort = 6000;
    private final int maxPort = 65535;
    private final int maxPortAttempts = maxPort - minPort + 1;

    private String userName;
    private String password;
    private String authHash;
    private boolean signInPending;
    private int encryption;
    private int signOutCount;
    private final HashMap<String, Object> activeBindings;
    private final HashMap<String, Connection> activeConnections;

    public P2PManager(Context context){
        Log.i(TAG, "Begin P2PManager Constructor");

        // Set the application context
        this.context = context;

        // Start with no Remot3.it userName, password, or autHash values. This effectively
        // starts the R3P2PManager with no active P2P sign-in.
        this.userName = "";
        this.password = "";
        this.authHash = "";

        // Start with the default encryption selected.
        this.encryption = defaultEncryption;

        // Start with no pending sign-out.
        this.signOutCount = 0;

        // Start with an empty a list of ports already in use.
        this.activeBindings = new HashMap<>();

        // Start with an empty list of active connections.
        this.activeConnections = new HashMap<>();

        // Also set up some instance aliases for default values passed to connectDevice()
        // as a convenience to the user.
        //
        this.defaultBindAddress = defaultBindAddress;
        this.defaultEncryption = defaultEncryption;
        this.defaultMaxOutstanding = defaultMaxOutstanding;
        this.defaultProxyLifetimeSec = defaultProxyLifetimeSec;
        this.defaultIdleLimitSec = defaultIdleLimitSec;
        this.connectdWrapper = new ConnectdWrapper(context);
        try {
            this.connectdWrapper.init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.listener = null;
        Log.i(TAG, "End P2PManager Constructor");
    }

    public void setP2PEventListener(P2PEventListener listener) {
        this.listener = listener;
    }

    public void signInWithPassword(String userName, String password, HashMap<String, Object> options) throws IOException {

        Log.i(TAG, "Start signInWithPassword");

        // If either the Remot3.it userName or password is blank, fail to sign in.
        if (userName == null || userName.isEmpty() || password == null || password.length() == 0) {
            Log.e(TAG, this.context.getString(R.string.invalid_user_password));
            throw new IOException(this.context.getString(R.string.invalid_user_password));
        }

        // If a Remot3.it userName is already set, then we can't log into a new account
        // until the caller signs out of the current account.
        if (this.userName != null && !this.userName.isEmpty()) {
            if (!this.userName.equals(userName)) {
                Log.e(TAG, String.format(this.context.getString(R.string.already_signed_in), this.userName));
                throw new IOException(String.format(this.context.getString(R.string.already_signed_in), this.userName));
            }
            // else already signed in with same account, not a failure
        }

        this.userName = userName;
        this.password = password;
        this.authHash = "";
        this.signInPending = true;

        // If no options object was specified, create an empty one.
        if (options.isEmpty()){
            options = new HashMap<>();
        }

        // Fill in defaults for any options not explicitly specified by the caller.
        if (options.get("encryption") == null){
            options.put("encryption", defaultEncryption);
        }

        // Set the encryption level to use when instantiating connections.
        this.encryption = (int) options.get("encryption");

        // After sign-in, flag that there is no pending sign-out.
        this.signOutCount = 0;

        Log.i(TAG, "End signInWithPassword");
    }

    public void signInWithAuthHash(String userName, String authHash, HashMap<String, Object> options) throws IOException {

        Log.i(TAG, "Start signInWithAuthHash");

        // If either the Remot3.it userName is blank, or authHash is wrong length, fail to sign in.
        if (userName == null || userName.isEmpty() || authHash == null || authHash.length() != 40) {
            Log.e(TAG, this.context.getString(R.string.invalid_user_hash));
            throw new IOException(this.context.getString(R.string.invalid_user_hash));
        }

        // If a Remot3.it userName is already set, then we can't sign into a new account
        // until the caller signs out of the current account.
        //
        if (this.userName != null && !this.userName.isEmpty()) {
            if (!this.userName.equals(userName)) {
                Log.e(TAG, String.format(this.context.getString(R.string.already_signed_in), this.userName));
                throw new IOException(String.format(this.context.getString(R.string.already_signed_in), this.userName));
            }
            // else already signed in with same account, not a failure
        }

        // Set the specified Remot3.it userName and authHash, but clear the password. This
        // tells the connectDevice() method to sign in using the authHash rather than the
        // password.
        //
        this.userName = userName;
        this.password = "";
        this.authHash = authHash;
        this.signInPending = true;

        // If no options object was specified, create an empty one.
        if (options.isEmpty()){
            options = new HashMap<>();
        }

        // Fill in defaults for any options not explicitly specified by the caller.
        if (options.get("encryption") == null){
            options.put("encryption", defaultEncryption);
        }

        // Set the encryption level to use when instantiating connections.
        this.encryption = (int) options.get("encryption");

        // After sign-in, flag that there is no pending sign-out.
        this.signOutCount = 0;

        Log.i(TAG, "End signInWithAuthHash");
    }

    public void connectDevice(String deviceID, HashMap<String, Object> options) {
        Log.i(TAG, "Begin connectDevice");

        // If a connection to the specified device already exists, post a p2pConnectionFailed
        // notification indicating the device was already connected.
        //
        if (this.activeConnections.containsKey(deviceID)) {
            if (listener != null) {
                listener.p2pConnectionFailed(deviceID, "Already connected to " + deviceID);
            }

            Log.i(TAG, "End connectDevice");
            return;
        }

        // If no options object was specified, create an empty one.
        if (options.isEmpty()){
            options = new HashMap<>();
        }

        // Fill in defaults for any options not explicitly specified by the caller.
        if (options.get("maxOutstanding") == null) {
            options.put("maxOutstanding", defaultMaxOutstanding);
        }

        if (options.get("proxyLifetimeSec") == null) {
            options.put("proxyLifetimeSec", defaultProxyLifetimeSec);
        }

        if (options.get("idleLimitSec") == null) {
            options.put("idleLimitSec", defaultIdleLimitSec);
        }

        // If no restrictAddress was specified, use the default.
        if (options.get("restrictAddress") == null) {
            options.put("restrictAddress", defaultRestrictAddress);
        }

        if (options.get("bindAddress") == null) {
            options.put("bindAddress", defaultBindAddress);
        }

        // If the caller didn't specify a bindPort, randomly select one.
        if (options.get("bindPort") == null) {
            options.put("bindPort", this.findFreePort((String) options.get("bindAddress")));
        }

        // If a free port couldn't be found, post a p2pConnectionFailed notification indicating
        // that no free ports are available.
        //
        if ((int) options.get("bindPort") == -1) {
            if (listener != null) {
                listener.p2pConnectionFailed(deviceID, "No free ports available for bindAddress " + options.get("bindAddress"));
            }

            Log.i(TAG, "End connectDevice");
            return;
        }

        // If the port is already in use by another device, fail to connect.
        String newBinding = options.get("bindAddress") + ":" + options.get("bindPort");
        if (this.activeBindings.containsKey("newBinding")) {
            if (listener != null) {
                listener.p2pConnectionFailed(deviceID, "Port " + options.get("bindPort") + " already in use by device " + this.activeBindings.get(newBinding));
            }

            Log.i(TAG, "End connectDevice");
            return;
        }

        // Create a new connection to the specified device.
        Connection newConnection = new Connection(
                this.connectdWrapper,
                this,
                deviceID,
                this.userName,
                this.password,
                this.authHash,
                (String) options.get("bindAddress"),
                (int) options.get("bindPort"),
                (String) options.get("restrictAddress"),
                this.encryption,
                (int) options.get("maxOutstanding"),
                (int) options.get("proxyLifetimeSec"),
                (int) options.get("idleLimitSec")
        );

        Log.i(TAG, "Connecting to device " + deviceID + ":");
        Log.i(TAG, "bindAddress = " + options.get("bindAddress"));
        Log.i(TAG, "bindPort = " + options.get("bindPort"));
        Log.i(TAG, "restrictAddress = " + options.get("restrictAddress"));
        Log.i(TAG, "encryption = " + this.encryption);
        Log.i(TAG, "maxOutstanding = " + options.get("maxOutstanding"));
        Log.i(TAG, "proxyLifetime = " + options.get("proxyLifetimeSec"));
        Log.i(TAG, "idleLimit = " + options.get("idleLimitSec"));

        // Record the new bindAddress:bindPort as in use and save the new connection
        // to the list of active connections. Note that this assumes we will succeed
        // in making the connection to the device. If later we get status back from
        // connectd indicating the connection failed, we'll remove the failed binding
        // and connection entries.
        //
        newBinding = options.get("bindAddress") + ":" + options.get("bindPort");
        this.activeBindings.put(newBinding, deviceID);
        this.activeConnections.put(deviceID, newConnection);

        Log.i(TAG, "End connectDevice");
    }

    public void disconnectDevice(String deviceID) {
        Log.i(TAG,"Begin disconnectDevice");

        // If the specified deviceID is not in the list of active connections, then post the
        // p2pConnectionDestroyed confirmation notification from here.
        //
        if (!this.activeConnections.containsKey(deviceID)) {
            if (listener != null) {
                listener.p2pConnectionDestroyed(deviceID, "Terminated by user.");
            }

            Log.i(TAG, "End disconnectDevice (early)");
            return;
        }

        // Disconnect from the specified device. This in turn will generate status output
        // from connectd confirming the disconnect, at which point the connectd status handler
        // will remove the associated connection and binding from the active lists, and post
        // the confirming p2pConnectionDestroyed notification.
        //
        this.activeConnections.get(deviceID).disconnect(deviceID, true);

        Log.i(TAG, "End disconnectDevice");
    }

    public void disconnectAllDevices() {
        Log.i(TAG, "Begin disconnectAllDevices");

        // Get a list of the deviceIDs with active connections. We do this now in one swell foop
        // because iterating the active connection list as we're removing entries from it seems
        // a bit sketchy.
        //
        Set connectedDevices = this.activeConnections.keySet();

        // Iterate through the deviceIDs, disconnecting them as we go. Doing so will cause a
        // p2pConnectionDestroyed notification to be issued each time a connectd instance for
        // a device terminates and outputs 'exit' sttus to the parseConnectdStatusLine() method.
        //
        Iterator iterator = connectedDevices.iterator();
        while (iterator.hasNext()) {
            String connection = (String) iterator.next();
            this.disconnectDevice(this.activeConnections.get(connection).deviceID);
        }

        Log.i(TAG, "End disconnectAllDevices");
    }

    public void signOut() {
        Log.i(TAG, "Begin signOut");

        // If we're not currently signed in or there are no active connections...
        if (this.userName.length() == 0 || this.activeConnections.size() == 0) {
            Log.e(TAG, "XXX skipping signOut " + this.userName.length() + " " + this.activeConnections.size());
            // Then post a p2p2pSignedOut notification here.
            if (listener != null) {
                listener.p2pSignedOut(this.userName);
            }

            // And reset the userName, password and autHash members.
            this.userName = "";
            this.password = "";
            this.authHash = "";

            Log.i(TAG, "End signOut early");

            return;
        } // if( this.userName.length == 0 || this.activeConnections.size == 0 )

        // Set a count of the number of p2pConnectionDestroyed notifications we need to
        // issue before posting the p2pSignedOut notification. We do this because we want
        // the sign-out notification to happen after the last connection has been terminated,
        // and we don't know which connection that will be because connectd terminations are
        // asynchronous.
        //
        this.signOutCount = this.activeConnections.size();

        // Disconnect all the active devices on the current account, marking the last
        // connection terminated to trigger an automatic sign-out.
        //
        this.disconnectAllDevices();

        Log.i(TAG,"End signOut");
    }

    private void parseConnectdStatusLine(String statusLine, Connection connection) {
        String deviceID = connection.deviceID;
        Log.i(TAG, statusLine);
        // If a connection was made...
        if (statusLine.startsWith("connected")) {
            // Post a p2pConnectionSucceeded notification, indicating an initial connection
            // to the device was successfully established.
            //
            Log.i(TAG, "XXX p2pConnectionSucceeded " + connection.bindAddress + " : " + connection.bindPort);
            if (listener != null) {
                listener.p2pConnectionSucceeded(deviceID, "http://" + connection.bindAddress + ":" + connection.bindPort);
            }

            // And we're done.
            return;
        } // if( statusLine.startsWith("connected") )

        if (statusLine.startsWith("status - server connection changed to state 5")) {
            // this means that the P2P sign-in succeeded
            //
            this.signInPending = false;
            //
        } // if( statusLine.startsWith("status - server connection changed to state 5") )

        // If sign in failed, get a timeout (may take more than a minute)
        if (statusLine.startsWith("status - Server Connection Timeout") ||
                        (statusLine.startsWith("exit") && statusLine.contains("authentication error"))
        ) {
            // For a failed sign-in we need to remove the binding and connection info from the
            // active lists that were added at connection time.
            //
            this.activeBindings.remove(connection.bindAddress + ":" + connection.bindPort);
            Log.i(TAG, "XXX removing listener (1) for " + deviceID);
            Connection info = this.activeConnections.get(deviceID);
            if (info != null) {
                info.connectdWrapper.clearConnectdEventListener();
            }
            this.activeConnections.remove(deviceID);

            if (this.signInPending) {
                // sign-in failed.
                this.signInPending = false;
                this.userName = "";

                if (listener != null) {
                    listener.p2pConnectionFailed(deviceID, "Server Connection Timeout");
                }

                // And we're done.
                return;
            }

            // this can happen if connection is lost after signIn.
            else {
                // TODO handle this case
                // NOTE If we're signed-in and connected to a device and then the connection
                //      goes down, the "exit - connection to peer closed or timed out."
                //      block below should handle connection loss. Maybe just need some code
                //      here to handle the case where the connection lost after sign-in but
                //      before device connection is established?
                // Need to return early here, or fall through to other test cases?
            }
        } // if( statusLine.startsWith("status - Server Connection Timeout") )

        // If connectd shut down due to the device going off-line, communication with the
        // device timing-out, or the internet connection going down...
        //
        if (statusLine.equals("exit - connection to peer closed or timed out.")) {
            // Remove the binding and connection info from the active lists for the
            // connection that failed.
            //
            this.activeBindings.remove(connection.bindAddress + ":" + connection.bindPort);
            Log.i(TAG, "XXX removing listener (2) for " + deviceID);
            Connection info = this.activeConnections.get(deviceID);
            if (info != null) {
                info.connectdWrapper.clearConnectdEventListener();
            }
            this.activeConnections.remove(deviceID);

            // Post the p2pConnection destroyed notification, with "reason" indicating
            // the connection to the device was lost.
            //
            if (listener != null) {
                listener.p2pConnectionDestroyed(deviceID, "Connection to device lost.");
            }

            // And I'm spent!
            return;
        } // if( statusLine ==  "exit - connection to peer closed or timed out." )

        // If connectd shut down due to proxy lifetime being exceeded...
        if (
                statusLine.startsWith("exit") &&
                        (statusLine.contains("process liftime") || // note "lifetime" is misspelled by connectd currently
                                statusLine.contains("process lifetime"))
        ) {
            // Remove the binding and connection info from the active lists for the
            // connection that was closed.
            //
            this.activeBindings.remove(connection.bindAddress + ":" + connection.bindPort);
            Log.i(TAG, "XXX removing listener (3) for " + deviceID);
            Connection info = this.activeConnections.get(deviceID);
            if (info != null) {
                info.connectdWrapper.clearConnectdEventListener();
            }
            this.activeConnections.remove(deviceID);

            // Post the p2pConnection destroyed notification, with "reason" indicating
            // that the connection lifetime was exceeded.
            //
            if (listener != null) {
                listener.p2pConnectionDestroyed(deviceID, "Connection lifetime exceeded.");
            }

            // And we're finished.
            return;
        } // if( statusLine.startsWith("exit - process liftime max hit.") )

        // If connectd was shut down due to the R3P2PManager issuing a kill message...
        if (statusLine.equals("exit - termination from signal 15") || statusLine.equals("exit - process killed")) {
            Log.i(TAG, "XXX killed connection " + deviceID + " @ " + connection.bindAddress + ":" + connection.bindPort);
            // Remove the binding and connection info from the active lists for the just
            // terminated connection.
            //
            this.activeBindings.remove(connection.bindAddress + ":" + connection.bindPort);
            Log.i(TAG, "XXX removing listener (4) for " + deviceID);
            Connection info = this.activeConnections.get(deviceID);
            if (info != null) {
                info.connectdWrapper.clearConnectdEventListener();
            }
            this.activeConnections.remove(deviceID);

            // Post the p2pConnectionDestroyed notification, with "reason" indicating that
            // the connection was terminated programmatically.
            //
            if (listener != null) {
                listener.p2pConnectionDestroyed(deviceID, "Terminated by user.");
            }

            // Finally, if we need to sign-out after the last connection has terminated...
            if (this.signOutCount > 0) {
                // And this termination was the last one expected...
                if (--this.signOutCount == 0) {
                    // Post a p2pSignedOut notification.
                    if (listener != null) {
                        listener.p2pSignedOut(this.userName);
                    }

                    // And reset the userName, password and autHash members, effectively
                    // signing out of the current Remot3.it account.
                    //
                    this.userName = "";
                    this.password = "";
                    this.authHash = "";

                    this.activeBindings.clear();
                    this.activeConnections.clear();
                } // if( --this.signOutCount == 0 )
            } // if( this.shouldSignOut )

            // And we're done.
            return;
        } // if( statusLine == "exit - termination from signal 15" )

        // If connectd was unable to establish a connection with the specified device...
        if (statusLine.startsWith("exit") && statusLine.endsWith("auto connect failed, exiting")) {
            // As with a destroyed connection, we need to remove its binding and connection
            // info from the active lists for connections that never succeeded to begin with.
            //
            this.activeBindings.remove(connection.bindAddress + ":" + connection.bindPort);
            Log.i(TAG, "XXX removing listener (5) for " + deviceID);
            Connection info = this.activeConnections.get(deviceID);
            if (info != null) {
                info.connectdWrapper.clearConnectdEventListener();
            }
            this.activeConnections.remove(deviceID);

            // Post a p2pConnectionFailed notification, with "reason" indicating that we could
            // not establish an initial connection to the device.
            //
            //  NOTE: If we can get more specific status (i.e., "device doesn't exist",
            //        "device disabled" or whatever), we should probably update the
            //        reason to be less generic.
            //
            if (listener != null) {
                listener.p2pConnectionFailed(deviceID, "Auto-connect failed.");
            }

            // And I'm spent!
            return;
        } // if( statusLine == "exit - auto connect failed, exiting" )

        // Ignore the generic "process closed" meesage that occurs after each of the
        // more specific exit conditions handled above.
        //
        if (statusLine.equals("exit - process closed.")) return;

        // If connectd shut down for some reason that wasn't specifically handled above...
        if (statusLine.startsWith("exit - ")) {
            // See if the device is still in the active connection list. If so, we need to
            // post a generic p2pConnectionDestroyed notification.
            //
            // If the device wasn't in the active connection list, that means a previous
            // (more specific) exit status already posted a notification, and we can
            // ignore this additional generic exit message.
            //
            if (this.activeConnections.containsKey(deviceID)) {
                // Remove the binding and connection info from the active lists for the just
                // terminated connection.
                //
                this.activeBindings.remove(connection.bindAddress + ":" + connection.bindPort);
                Log.i(TAG, "XXX removing listener (6) for " + deviceID);
                Connection info = this.activeConnections.get(deviceID);
                if (info != null) {
                    info.connectdWrapper.clearConnectdEventListener();
                }
                this.activeConnections.remove(deviceID);

                // Post the p2pConnectionDestroyed notification, with "reason" set to the
                // remainder of the unrecognized exit status string.
                //
                if (listener != null) {
                    listener.p2pConnectionDestroyed(deviceID, statusLine.substring("exit - ".length()));
                }
            }

            // And we're done!
            return;
        } // if( statusLine.startsWith("exit -") )

        // For now, just eat all throughput and status messages. In the future we
        // may want to tabulate their information for diagnostic purposes.
        //
        if (statusLine.startsWith("throughput") || statusLine.startsWith("status")) return;

        // DEBUG: Uncomment the following lines to log unhandled status from connectd.
        //if( statusLine.length > 1 )
        //    Log.i(TAG,
        //        "parseConnectdStatusLine: device = " + connection.deviceID + ", " +
        //        "status = [" + statusLine + "]"
        //    );
    }

    public int findFreePort(String bindAddress) {
        int i = 0;
        int port = 0;

        // Try to find a free port, up to the maximum number of ports available.
        for( i = 0; i < maxPortAttempts; i++ )
        {
            Random random = new Random();
            // Randomly select a port in the valid range.
            port = random.nextInt(maxPort - minPort + 1) + minPort;

            // Combine it with the specified bind address to come up with a composite binding.
            String binding = bindAddress + ":" + port;

            // If the port is already in use for the specified bind address, pick another port.
            if( this.activeBindings.containsKey( binding ) ) {
                continue;
            }

            // If we got to this point, the port is not already in use on the specified bind
            // address, so we can stop searching.
            //
            break;

        } // for( i = 0; i < maxPortAttempts; i++ )

        // If we got to this point because we couldn't find a free port, tell the caller
        // by returning -1.
        //
        if( i == maxPortAttempts ) {
            return -1;
        }


        Log.i(TAG,"findFreePort(" + bindAddress + ") returned " + port);

        // Otherwise return the port.
        return port;
    }

    private static class Connection {
        private final String TAG = this.getClass().getSimpleName();
        private final P2PManager owner;
        private final ConnectdWrapper connectdWrapper;
        private boolean signOut;
        private final String deviceID;
        private final String bindAddress;
        private final String bindPort;
        private final String restrictAddress;
        private final String encryption;
        private final String maxOutstanding;
        private final String proxyLifetimeSec;
        private final String idleLimitSec;

        public Connection(ConnectdWrapper connectdWrapper,
                          P2PManager owner,
                          String deviceID,
                          String userName,
                          String password,
                          String authHash,
                          String bindAddress,
                          int bindPort,
                          String restrictAddress,
                          int encryption,
                          int maxOutstanding,
                          int proxyLifetimeSec,
                          int idleLimitSec){
            // Save the P2PManager instance as the owner of this connection.
            this.owner = owner;
            this.connectdWrapper = connectdWrapper;

            // By default, closing a connection does not cause a Remot3.it account sign-out.
            // However, when the P2PManager's signOut() method is called, this flag will get
            // set for the last r3ConnectionInfo to be terminated so that a final sign-out
            // notifcation will be issued when the last connection has been terminated.
            //
            this.signOut = false;

            // Record the parameters we'll be using for this connection.
            this.deviceID = deviceID;
            this.bindAddress = bindAddress;
            this.bindPort = String.valueOf(bindPort);
            this.restrictAddress = restrictAddress;
            this.encryption = String.valueOf(encryption);
            this.maxOutstanding = String.valueOf(maxOutstanding);
            this.proxyLifetimeSec = String.valueOf(proxyLifetimeSec);
            this.idleLimitSec = String.valueOf(idleLimitSec);

            this.connectdWrapper.setConnectdEventListener(new ConnectdWrapper.ConnectdEventListener() {
                @Override
                public void connectdStatus(String address, String line) {
                    Connection.this.processConnectdOutput(line);
                }
            });
            // spawn connectd
            // If we're connecting using an authHash, use "connectd -p" to connect.
            if (authHash != null && authHash.length() > 0) {
                Log.i(TAG, "XXX calling exec w/ authhash");
                String[] args = {
                        "-s", // Output !! status messages
                        "-p", // Connect to device with authHash
                        Base64.encodeToString(userName.getBytes(), Base64.DEFAULT), // Base64 encoded user name
                        authHash, // Plain-text password hash
                        this.deviceID, // MAC Address of device to connect to
                        "T" + this.bindPort, // Port number to bind device to
                        this.encryption, // Encryption level to use
                        this.bindAddress, // Local IP Address to bind device to.
                        this.restrictAddress, // Local IP Address to restrict access to
                        this.maxOutstanding, //
                        this.proxyLifetimeSec, //
                        this.idleLimitSec //
                };
                this.connectdWrapper.exec(this.deviceID, args);
            } // if( authHash.length > 0 )

            // Otherwise, if we're connecting with a password, use "connectd -c" to connect.
            else {
                Log.i(TAG, "XXX calling exec w/ password");
                String[] args = {
                        "-s", // Output !! status messages
                        "-c", // ConnectToDevice with password
                        Base64.encodeToString(userName.getBytes(), Base64.DEFAULT), // Base64 encoded user name
                        Base64.encodeToString(password.getBytes(), Base64.DEFAULT), // Base64 encoded password
                        this.deviceID, // MAC Address of device to connect to
                        "T" + this.bindPort, // Port number to bind device to
                        this.encryption, // Encryption level to use
                        this.bindAddress, // Local IP Address to bind device to.
                        this.maxOutstanding, //
                        this.proxyLifetimeSec, //
                        this.idleLimitSec, //
                };
                this.connectdWrapper.exec(this.deviceID, args);
            } // else( autHash.length == 0 )
            Log.i(TAG, "XXX returned from exec");
        }
        public void disconnect(String deviceID,
                               boolean signOut) {
            if (this.deviceID.equals(deviceID)) {
                this.signOut = signOut;
                this.connectdWrapper.disconnect(deviceID);
            }
        }

        public void processConnectdOutput(String output) {
            String[] outputLines = new String(output.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).split("\n");

            // Then, step through the status lines...
            for (String outputLine : outputLines) {
                // And if the status line starts with "!!", route the  remainder of the status line
                // and this r3ConnectionInfo instance off to the P2PManager parseConnectdStatusLine()
                // method for parsing and posting of notifications if necessary.
                //
                if (outputLine.startsWith("!!")) {
                    this.owner.parseConnectdStatusLine(outputLine.substring("!!".length()), this);
                }
                // DEBUG: Un-comment the following lines to pass non-!! status up to the owning
                //        P2PManager instance's status parser for debugging purposes.
                else {
                    this.owner.parseConnectdStatusLine(outputLine, this);
                }
            }
        }
    }
}
