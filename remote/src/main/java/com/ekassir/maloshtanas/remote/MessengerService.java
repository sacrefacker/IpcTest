package com.ekassir.maloshtanas.remote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.ekassir.maloshtanas.commonlibrary.Constants;

import java.util.ArrayList;

public class MessengerService extends Service
{
// MARK: - Public functions

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

// MARK: - Private functions

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.remote_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, RemoteActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_sample)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.remote_service_started, notification);
    }

// MARK: - Inner types

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case Constants.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case Constants.MSG_SET_VALUE:
                    mValue = msg.arg1;
                    for (int i = mClients.size() - 1; i >= 0; i--) {
                        try {
                            mClients.get(i).send(Message.obtain(null,
                                    Constants.MSG_SET_VALUE, mValue, 0));
                        }
                        catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

//    /**
//     * <p>Example of explicitly starting and stopping the remove service.
//     * This demonstrates the implementation of a service that runs in a different
//     * process than the rest of the application, which is explicitly started and stopped
//     * as desired.</p>
//     *
//     * <p>Note that this is implemented as an inner class only keep the sample
//     * all together; typically this code would appear in some separate class.
//     */
//    public static class Controller extends Activity
//    {
//        @Override
//        protected void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//
//            setContentView(R.layout.remote_service_controller);
//
//            // Watch for button clicks.
//            Button button = (Button)findViewById(R.id.start);
//            button.setOnClickListener(mStartListener);
//            button = (Button)findViewById(R.id.stop);
//            button.setOnClickListener(mStopListener);
//        }
//
//        private OnClickListener mStartListener = new OnClickListener() {
//            public void onClick(View v) {
//                // Make sure the service is started.  It will continue running
//                // until someone calls stopService().
//                // We use an action code here, instead of explictly supplying
//                // the component name, so that other packages can replace
//                // the service.
//                startService(new Intent(Controller.this, RemoteService.class));
//            }
//        };
//
//        private OnClickListener mStopListener = new OnClickListener() {
//            public void onClick(View v) {
//                // Cancel a previous call to startService().  Note that the
//                // service will not actually stop at this point if there are
//                // still bound clients.
//                stopService(new Intent(Controller.this, RemoteService.class));
//            }
//        };
//    }

// MARK: - Variables

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * For showing and hiding our notification.
     */
    NotificationManager mNM;

    /**
     * Keeps track of all current registered clients.
     */
    ArrayList<Messenger> mClients = new ArrayList<>();

    /**
     * Holds last value set by a client.
     */
    int mValue = 0;
}
