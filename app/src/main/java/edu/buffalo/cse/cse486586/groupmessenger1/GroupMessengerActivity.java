package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final int SERVER_PORT = 10009;
    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private  Uri uri;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        try {

            Log.e(TAG,"check");

            ServerSocket serverSocket = new ServerSocket(); // <-- create an unbound socket first
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(SERVER_PORT));
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
         } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket" + e.getMessage().toString());
            return;
        }


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */


        /*
         * Register an OnKeyListener for the input box. OnKeyListener is an event handler that
         * processes each key event. The purpose of the following code is to detect an enter key
         * press event, and create a client thread so that the client thread can send the string
         * in the input box over the network.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)) {

                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        String msg = editText.getText().toString() + "\n";
                        editText.setText(""); // This is one way to reset the input box.

                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                        return true;
                    }
                }
                return false;
            }
        });


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         *
         */

        final Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG,"OnClickCalled");
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    class HandleClient extends ServerTask implements Runnable {
        private ServerSocket socS;
        private Socket soc;
        private  int ordering;
        HandleClient(ServerSocket socS, Socket soc, int ordering) {
            this.soc = soc;
            this.socS = socS;
            this.ordering = ordering;
            Thread thread = new Thread(this);
            thread.start();
        }

        public void run() {

            try {


                BufferedReader in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                String str = in.readLine();
                if (str != null) {
                    Log.d(TAG, "run: Message received " + str);
                    ContentValues content = new ContentValues();
                    content.put("key", Integer.toString(ordering));
                    content.put("value", str);
                    getContentResolver().insert(uri, content);
                    publishProgress(str);
                }
                PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
                String ser = "ACK";
                out.println(ser);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            int ordering = 0;
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
            try {
                serverSocket.setReuseAddress(true);
                while (true) {
                    Socket socket = serverSocket.accept();
                    new HandleClient(serverSocket, socket, ordering);
                    ordering++;
                }
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: Error occured", e.getCause());
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */

    class HandleSending implements Runnable {

        private String msgs;

        HandleSending(String msg) {
            this.msgs = msg;
            Thread thread = new Thread(this);
            thread.start();
        }

        public void run() {

            try {

                int i = 0;
                while (i < REMOTE_PORTS.length) {
                    Log.d(TAG, "doInBackground: remote " + REMOTE_PORTS[i]);

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORTS[i]));

                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */
                    if (msgs != null) {

                        try {

                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            Log.d(TAG, "doInBackground: sending message " + msgs);
                            out.println(msgs);
                            String ack = "ACK";
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String des = in.readLine();
                            if (des.equals(ack))
                            {
                                Log.e(TAG, "sendingALL: Confirmation recived");

                                socket.close();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "doInBackground: Error", e.getCause());
                        }
                    }
                    i++;
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException" + e.getMessage());
            }
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            new HandleSending(msgs[0]);
            return null;
        }
    }
}
