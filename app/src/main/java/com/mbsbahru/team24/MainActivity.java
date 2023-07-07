package com.mbsbahru.team24;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.imgproc.Imgproc;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import java.io.InputStream;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private String email;

    Uri URI = null;
    private static final int PICK_FROM_GALLERY = 101;

    private static final String TAG = BTConnectActivity.class.getName();
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final UUID MAGIC_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    private static List<String> classNames;
    private static List<Scalar> colors = new ArrayList<>();
    private Net net;
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean permissionGranted = false;
    
    public int keadaan = 0;

    private ProgressDialog pd;

    /////////////////////----Bluetooth----////////////////////////////////////

//    public static Handler mHandler;
    private BluetoothAdapter mBTAdapter;
    private String indikasi;
    private ArrayAdapter<String> mBTArrayAdapter;
    public static BluetoothSocket mBTSocket;
    private ConnectedThread mConnectedThread;
    private BTConnectionThread btConnectionThread;
    private BTConnectActivity mConnectBT;
    private Handler mHandler; // Our main handler that will receive callback notifications
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
//    private boolean isBluetoothUsed = false;
    /////////////////////----Bluetooth----////////////////////////////////////
    private int delayCounter = 0;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private boolean isBluetoothUsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!permissionGranted) {
            checkPermissions();
        }
        email = getIntent().getStringExtra("email_address");
        mOpenCvCameraView = findViewById(R.id.CameraView);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = (BluetoothDevice) getIntent().getParcelableExtra("device");
        if(device != null){
            connectToBTDevice2(device);
            indikasi = "Team 24";
            isBluetoothUsed = true;
        }else{
            Toast.makeText(this, "No Bluetooth, Just Squirrel Detect", Toast.LENGTH_SHORT).show();
//            Toast.makeText(this, email, Toast.LENGTH_SHORT).show();
            isBluetoothUsed = false;
        }


        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        classNames = readLabels("labels-squirrel.txt", this);
        for (int i = 0; i < classNames.size(); i++)
            colors.add(randomColor());

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
                }

                if(msg.what == CONNECTING_STATUS){
                    char[] sConnected;
                }
            }
        };

//        if (mConnectedThread != null) {
//            indikasi = "thread berhasil A";
//            mConnectedThread.write("<turn on>");
//            mConnectedThread.write_int(0);
//            mConnectedThread.write_int(centerY);
//                            btConnectionThread.write(("<turn on>").getBytes());
//                            btConnectionThread.write(charToByteArray(centerX));
//                            btConnectionThread.write(charToByteArray(centerY));
//        }

    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

//        String modelConfiguration = getAssetsFile("yolov3-tiny.cfg", this);
//        String modelWeights = getAssetsFile("yolov3-tiny.weights", this);
        String modelConfiguration = getAssetsFile("yolov3-squirrel-1810.cfg", this);
        String modelWeights = getAssetsFile("yolov3-squirrel-1810_final.weights", this);
        net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights);
    }


    @Override
    public void onCameraViewStopped() {
    }

    private static String getSupportedValue(List<String> supported,
                                            String value) {
        return (supported != null) && supported.contains(value) ? value
                : null;
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        String kondisi = "none";

        Mat frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

        Size frame_size = new Size(128, 128); //416 or288 or 96
        Scalar mean = new Scalar(0, 0, 0); //127.5

        Mat blob = Dnn.blobFromImage(frame, 1.0 / 255.0, frame_size, mean, true, false, CvType.CV_32F);
        net.setInput(blob);

        List<Mat> result = new ArrayList<>();
        List<String> outBlobNames = net.getUnconnectedOutLayersNames();

        net.forward(result, outBlobNames);
        float confThreshold = 0.5f;

        if (result.size() > 0) {
            for (int i = 0; i < result.size(); ++i) {
                // each row is a candidate detection, the 1st 4 numbers are
                // [center_x, center_y, width, height], followed by (N-4) class probabilities

                Mat level = result.get(i);
                for (int j = 0; j < level.rows(); ++j) {

                    Mat row = level.row(j);
                    Mat scores = row.colRange(5, level.cols()); //5
                    Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                    float confidence = (float) mm.maxVal;
                    Point classIdPoint = mm.maxLoc;

                    if (confidence > confThreshold) {


                        int centerX = (int) (row.get(0, 0)[0] * frame.cols());
                        int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                        int width = (int) (row.get(0, 2)[0] * frame.cols());
                        int height = (int) (row.get(0, 3)[0] * frame.rows());

                        int left = (int) (centerX - width * 0.5);
                        int top = (int) (centerY - height * 0.5);
                        int right = (int) (centerX + width * 0.5);
                        int bottom = (int) (centerY + height * 0.5);

                        Point left_top = new Point(left, top);
                        Point right_bottom = new Point(right, bottom);
                        Point label_left_top = new Point(left, top - 5);
                        DecimalFormat df = new DecimalFormat("#.##");

                        int class_id = (int) classIdPoint.x;
                        String label = classNames.get(class_id) + ": " + df.format(confidence);
                        Scalar color = colors.get(class_id);

                        Imgproc.rectangle(frame, left_top, right_bottom, color, 2, 2);
                        Imgproc.putText(frame, label, label_left_top, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 2);
                        Imgproc.putText(frame, label, label_left_top, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2);
                        Imgproc.putText(frame, "X: " + centerX + ", Y: " + centerY, new Point(1000, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 2);


                        kondisi = "SQ-detected: " + i;


                        if (isBluetoothUsed == true) {
                            byte[] buffer = new byte[8];
                            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                            byteBuffer.putInt(centerX);
                            byteBuffer.putInt(centerY);
                            mConnectedThread.write_byte(buffer);
                        }

                        if (delayCounter == 1) {
                            Imgproc.putText(frame, "sending picture via email...", new Point(30, 650), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 0), 2);
                            sendEmail(frame);
//                            save_mat(frame);
                        }

                        if (delayCounter == 50){
                            delayCounter = 0;
                        }
                        Imgproc.putText(frame, "delay counter for email (50): " + delayCounter, new Point(30, 700), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2);
                        delayCounter = delayCounter+1;
                    }
                }
            }
        }
        Imgproc.putText(frame, kondisi, new Point(30, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2);
        return frame;
    }

    public void sendEmail(Mat mat) {
//        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
//        File file = new File(path, "squirrel.jpg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.

        File file = new File(getExternalFilesDir(null), "image.jpg");
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);

        try {
            Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
//            Mat tmp = new Mat(mat.width(), mat.height(), CvType.CV_8UC1, new Scalar(4));
//            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_RGB2BGRA);
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            Utils.matToBitmap(mat, bmp);
            fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

//            email = etEmail.getText().toString();
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Squirrel Image");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello, this is image taken from the Squirrel Detection App (Team 24-CEE575)");
//            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
//            emailIntent.setAction(Intent.A).setAction(Intent.ACTION_GET_CONTENT);
//            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
//            if (URI != null) {
//                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
//            }
            emailIntent.setType("image/jpeg");
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(emailIntent, "Sending email..."));
//        } catch (Throwable t) {
//            Toast.makeText(this, "Request failed try again: "+ t.toString(), Toast.LENGTH_LONG).show();
//        }
    }

    private boolean checkPermissions() {

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
            return false;
        } else {
            return true;
        }

    }


    private static String getAssetsFile(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }


    private List<String> readLabels(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        List<String> labelsArray = new ArrayList<>();
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            Scanner fileScanner = new Scanner(new File(outFile.getAbsolutePath())).useDelimiter("\n");
            String label;
            while (fileScanner.hasNext()) {
                label = fileScanner.next();
                labelsArray.add(label);
            }
            fileScanner.close();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to read labels!");
        }
        return labelsArray;
    }


    private Scalar randomColor() {
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);
        return new Scalar(r, g, b);
    }


    private void save_mat(Mat mat) {
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        File file = new File(path, "screen.jpg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
        try {
            Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
            Mat tmp = new Mat(mat.width(), mat.height(), CvType.CV_8UC1, new Scalar(4));
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_RGB2BGRA);
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            Utils.matToBitmap(tmp, bmp);
            fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    ///////////////Bluetooth_Field//////////

    //    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
//    @SuppressLint("MissingPermission")
    protected void connectToBTDevice2(final BluetoothDevice device) {
        // Spawn a new thread to avoid blocking the GUI one
        new Thread() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                boolean fail = false;
                BluetoothDevice device2 = mBTAdapter.getRemoteDevice(device.getAddress());
                indikasi = device2.getAddress();

                try {
                    mBTSocket = createBluetoothSocket(device2);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

//                try {
////                    mBTSocket = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
//                } catch (Exception e) {
//                    Log.d(TAG,"Error creating socket");
//                }

                // Establish the Bluetooth socket connection.

                try {
                    mBTSocket.connect();
//                    indikasi = "thread berhasil";
                } catch (IOException e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        Toast.makeText(getBaseContext(), getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
                    }
                }
                if (!fail) {
                    mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                    mConnectedThread.start();


                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, device.getName())
                            .sendToTarget();
                }else{
//                    indikasi = device.getName();
                }
            }
        }.start();
    };

    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, MAGIC_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(MAGIC_UUID);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("MissingPermission")
    protected void connectToBTDevice(final BluetoothDevice device) {
        new AlertDialog.Builder(this).setMessage("Connect to " + device.getName() + " ?").setNegativeButton("Nay!", null).setPositiveButton("Yea!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                pd = new ProgressDialog(MainActivity.this);
                pd.setMessage("Loading....");
                pd.setCancelable(true);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (btConnectionThread != null) {
                            btConnectionThread.cancel();
                        }
                    }
                });
                pd.show();

                btConnectionThread = new BTConnectionThread(device);

                btConnectionThread.start();
            }
        }).create().show();

    }

    class BTChannelThread extends Thread {

        private boolean keepAlive = true;
        private BluetoothSocket mSocket;
        private OutputStream outStream;

        private InputStream inStream;

        public BTChannelThread(BluetoothSocket btSocket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            mSocket = btSocket;

//            try {
//                outStream = btSocket.getOutputStream();
//            } catch (IOException e) {
//            }

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) { }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inStream.available();
                    if(bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = inStream.available(); // how many bytes are ready to be read?
                        bytes = inStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(BTConnectActivity.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        public void sendCommandString(String inputString) {
            byte[] bytes = inputString.getBytes();           //converts entered String into bytes
            try {
                outStream.write(bytes);
            } catch (IOException e) { }
        }
        public void sendCommandInt(int inputInt) {
            try {
                outStream.write(inputInt);
            } catch (IOException e) { }
        }

        public void cancel() {

            keepAlive = false;

            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }

    class BTConnectionThread extends Thread {

        private BluetoothDevice device;
        private BluetoothSocket btSocket;
        private BTChannelThread btChannelThread;

        public BTConnectionThread(BluetoothDevice device) {
            this.device = device;

        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            if (mBTAdapter.isDiscovering()) {
                mBTAdapter.cancelDiscovery();
            }

            if(mBTAdapter.isDiscovering()){
                mBTAdapter.cancelDiscovery();
                Toast.makeText(getApplicationContext(),getString(R.string.DisStop),Toast.LENGTH_SHORT).show();
            }
            else{
                if(mBTAdapter.isEnabled()) {
//                    mBTArrayAdapter.clear(); // clear items
                    mBTAdapter.startDiscovery();
//                    Toast.makeText(getApplicationContext(), getString(R.string.DisStart), Toast.LENGTH_SHORT).show();
                    registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                }
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.BTnotOn), Toast.LENGTH_SHORT).show();
                }
            }

            try {
                btSocket = device.createRfcommSocketToServiceRecord(MAGIC_UUID);
                btChannelThread = new BTChannelThread(btSocket);

                btSocket.connect();

                if (pd != null) {
                    pd.dismiss();
                }

                btChannelThread.start();
            } catch (IOException e) {
                Log.e(TAG, "Cannot connect to Bluetooth " + device.getName(), e);
                try {
                    btSocket.close();
                } catch (IOException e1) {
                    // supress
                }
            }

        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                // supress
            }
        }

        public void writeString(String input) {
            btChannelThread.sendCommandString(input);
        }
        public void writeInt(int input) {
            btChannelThread.sendCommandInt(input);
        }
    }

    public static byte[] charToByteArray(int a)
    {
        byte[] ret = new byte[2];
        ret[1] = (byte) (a & 0xFF);
        ret[0] = (byte) ((a >> 8) & 0xFF);
        return ret;
    }
    public static byte[] oneByteToByteArray(int a)
    {
        byte[] ret = new byte[1];
        ret[0] = (byte) (a & 0xFF);
        return ret;
    }
}