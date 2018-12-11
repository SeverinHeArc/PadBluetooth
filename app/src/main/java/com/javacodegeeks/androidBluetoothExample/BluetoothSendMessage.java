package com.javacodegeeks.androidBluetoothExample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.*;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.List;

import com.javacodegeeks.R;

import androidRecyclerView.MessageAdapter;

/**
 * Acivité qui envoit les messages (coordonnées par bluetooth)
 */

public class BluetoothSendMessage extends Activity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{

    // machine d'etat de la connexion
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;


    // Key names received from the BluetoothGestionConnexion Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothGestionConnexion mChatService = null;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private MessageAdapter mAdapter;

    private GestureDetectorCompat mDetector;
    public int counter = 0;

    private List<androidRecyclerView.Message> messageList = new ArrayList<androidRecyclerView.Message>();

    private int x=0;
    private int y=0;
    private SendPosThread mSendPosThread;
    private int nbDoigts = 0;
    private boolean doubleTap = false;


    /**
     * Enable le bluetooth et regarde s'il existe.
     * Règle aussi le visuel des messages
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MessageAdapter(getBaseContext(), messageList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mRecyclerView.setOnTouchListener(handleTouch);
        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        // test si le bluetooth est supporté par l'appareil
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    /**
     * Démarre le chat entre client et serveur
     * gère aussi les états du chat/activité (pause, play, stop, etc..,)
     */

    //démarre le chat entre serveur et client
    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothGestionConnexion.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Chat avec prise du message (input + stocker dans buffer) et envoit du message
     * envoit message seulement quand le bouton envoit est clické
     */

    private void setupChat() {
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the BluetoothGestionConnexion to perform bluetooth connections
        mChatService = new BluetoothGestionConnexion(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }


    private void sendMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothGestionConnexion.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothGestionConnexion to write
            message = message + "\r";
            byte[] send = message.getBytes();
            mChatService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * réception des dimensions de l'ordi
     */


    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message);
                    }
                    return true;
                }
            };

    // The Handler that gets information back from the BluetoothGestionConnexion
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer

                    String writeMessage = new String(writeBuf);
                    mAdapter.notifyDataSetChanged();
                    messageList.add(new androidRecyclerView.Message(counter++, writeMessage, "Me"));
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mAdapter.notifyDataSetChanged();
                    messageList.add(new androidRecyclerView.Message(counter++, readMessage, mConnectedDeviceName));
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     * Rend notre appareil découvrable (visible)
     * Connexion
     */

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When PeripheralListBTActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(PeripheralListBTActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                    mSendPosThread = new SendPosThread();
                    mSendPosThread.start();
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void connect(View v) {
        Intent serverIntent = new Intent(this, PeripheralListBTActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public void discoverable(View v) {
        ensureDiscoverable();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)){
            return this.mDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDoubleTap (MotionEvent motionEvent){
        doubleTap = true;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent (MotionEvent motionEvent){
        doubleTap = true;
        return true;
    }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) { return true; }
            @Override
            public boolean onDown(MotionEvent motionEvent) { return true; }
            @Override
            public void onShowPress(MotionEvent motionEvent) { }
            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) { return true; }
            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) { return true; }
            @Override
            public void onLongPress(MotionEvent motionEvent) { }
            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) { return true; }


    /**
     * This thread sends the position of finger to PC.
     */
    private class SendPosThread extends Thread {


        public SendPosThread() {

        }

        public void run() {

            String message;
            while(mChatService.getState()!=mChatService.STATE_CONNECTED);
            try
            {
                Thread.sleep(30000);
            }
            catch(InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            while(true) {
                try
                {
                    Thread.sleep(1000);
                }
                catch(InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
                //message = ("X"+Integer.toString(x)+ "Y"+ Integer.toString(y)+"Z");
                //sendMessage(message);
            }
        }


        public void cancel() {
        }
    }

    private View.OnTouchListener handleTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            String message;
            x = (int) event.getRawX();
            y = (int) event.getRawY();
            String sClick;
            if(doubleTap)
            {
                sClick = "1";
            }
            else
            {
                sClick = "0";
            }
            message = ("X"+Integer.toString(x)+ "Y"+ Integer.toString(y)+"D"+Integer.toString(nbDoigts)+ "C" + sClick + "Z");
            sendMessage(message);
            int action = (event.getAction() & MotionEvent.ACTION_MASK);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Log.i("TAG", "touched down")
                        ++nbDoigts;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                        ++nbDoigts;
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Log.i("TAG", "moving: (" + x + ", " + y + ")");
                    break;
                case MotionEvent.ACTION_UP:
                    // Log.i("TAG", "touched up");
                    if(nbDoigts>=1)
                    {
                        --nbDoigts;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                        nbDoigts--;
                    break;
            }

            return true;
        }
    };
}