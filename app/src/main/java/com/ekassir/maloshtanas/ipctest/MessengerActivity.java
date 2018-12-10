package com.ekassir.maloshtanas.ipctest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.ekassir.maloshtanas.commonlibrary.Constants;

/**
 * Example of binding and unbinding to the remote service.
 * This demonstrates the implementation of a service which the client will
 * bind to, interacting with it through an aidl interface.
 * <p>
 * Note that this is implemented as an inner class only keep the sample
 * all together; typically this code would appear in some separate class.
 */
public class MessengerActivity extends Activity
{
// MARK: - Public functions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);

        mCallbackText = findViewById(R.id.label_callback);
        TextView label = findViewById(R.id.label_clickable);
        label.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v) {
                doBindService();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

// MARK: - Private functions

    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
//        bindService(new Intent(MessengerActivity.this, MessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
        Intent serviceIntent = new Intent().setClassName("com.ekassir.maloshtanas.remote", "com.ekassir.maloshtanas.remote.MessengerService");
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mCallbackText.setText("Binding.");
    }

    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, Constants.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            mCallbackText.setText("Unbinding.");
        }
    }

// MARK: - Inner types

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_SET_VALUE:
                    mCallbackText.setText("Received from service: " + msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private class MessengerServiceConnection implements ServiceConnection
    {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            mCallbackText.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, Constants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null, Constants.MSG_SET_VALUE, this.hashCode(), 0);
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
            Toast.makeText(MessengerActivity.this, R.string.remote_service_connected, Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mCallbackText.setText("Disconnected.");

            // As part of the sample, tell the user what happened.
            Toast.makeText(MessengerActivity.this, R.string.remote_service_disconnected, Toast.LENGTH_SHORT).show();
        }
    }

// MARK: - Variables

    private ServiceConnection mConnection = new MessengerServiceConnection();

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Messenger for communicating with service.
     */
    Messenger mService = null;

    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mIsBound;

    /**
     * Some text view we are using to show state information.
     */
    TextView mCallbackText;
}