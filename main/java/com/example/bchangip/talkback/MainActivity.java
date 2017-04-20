/*
 *  Copyright 2016 Zoraida Callejas, Michael McTear and David Griol
 *
 *  This file is part of the Conversandroid Toolkit, from the book:
 *  The Conversational Interface, Michael McTear, Zoraida Callejas and David Griol
 *  Springer 2016 <https://github.com/zoraidacallejas/ConversationalInterface/>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Example activity with speech input and output that implements the
 * speech management methods in VoiceActivity.
 * When the button is pressed, the user is asked to say something and
 * the system synthesizes it back.
 *
 * @author Zoraida Callejas, Michael McTear, David Griol, Bryan Chan
 * @version 3.0, 05/13/16
 */

package com.example.bchangip.talkback;

import android.content.Context;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MainActivity extends VoiceActivity implements SensorEventListener {

    private static final int MOUSE_COORDINATES_CODE = 0;
    private static final int COMMAND_CODE = 1;


    private static final String LOGTAG = "TALKBACK";
    private static Integer ID_PROMPT_QUERY = 0;
    private static Integer ID_PROMPT_INFO = 1;

    private long startListeningTime = 0; // To skip errors (see processAsrError method)

    private static final String host = "192.168.2.1";
//    private static final String host = "172.20.95.35";
    private static final int portnumber = 44444;
    private static final String debugString = "debug";
    Socket socket;
    private Sensor sensor;
    private SensorManager sensorManager;
    private MediaRecorder mRecorder;
    private TextView debugText;

    private boolean soundActivationPending;
    private Double currentAmplitude;
    private BufferedWriter bw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set layout
        setContentView(R.layout.activity_main);

        //Initialize the speech recognizer and synthesizer
        initSpeechInputOutput(this);

        //Set up the speech button
        setSpeakButton();


        //MotionMouse start------------------------------------------------------------------------------------------------->
        debugText = (TextView)findViewById(R.id.debugText);

        //Create sensor manager
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Accelerometer sensor
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Gyroscope sensor (Uncomment to enable the gyroscope version of the app)
        //sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


        //Register sensor listener (Add it)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

        //Microphone section, used to trigger the voice recognition
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile("/dev/null");

        try{
            mRecorder.prepare();
        } catch (IOException e){
            Log.e(debugString, e.getMessage());
        }
        mRecorder.start();


        //Separate thread used for conection to the server.
        new Thread() {
            @Override
            public void run(){
                try{
                    Log.i(debugString, "Attempting to connect to server");
                    socket = new Socket(host, portnumber);
                    Log.i(debugString, "Connection success");

//                    Sending message to server
//                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//                    bw.write("This is a message sent from the client");
//                    bw.newLine();
//                    bw.flush();

                    //Receiving message from server
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Log.i(LOGTAG, br.readLine());
//                    System.out.println("Message sent from the server: " + br.readLine());

                } catch(IOException e) {
                    Log.e(debugString, e.getMessage());
                }
            }
        }.start();
    }

    /**
     * Initializes the search button and its listener. When the button is pressed, a feedback is shown to the user
     * and the recognition starts
     */
    private void setSpeakButton() {
        // gain reference to speak button
        Button speak = (Button) findViewById(R.id.speech_btn);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(LOGTAG, "Stopping media recorder");
                mRecorder.stop();
                Log.i(LOGTAG, "Media recorder successfully stopped");

                //Ask the user to speak
                try {
                    speak(getResources().getString(R.string.initial_prompt), "EN", ID_PROMPT_QUERY);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }
        });
    }

    /**
     * Explain to the user why we need their permission to record audio on the device
     * See the checkASRPermission in the VoiceActivity class
     */
    public void showRecordPermissionExplanation(){
        Toast.makeText(getApplicationContext(), "TalkBack must access the microphone in order to perform speech recognition", Toast.LENGTH_SHORT).show();
    }

    /**
     * If the user does not grant permission to record audio on the device, a message is shown and the app finishes
     */
    public void onRecordAudioPermissionDenied(){
        Toast.makeText(getApplicationContext(), "Sorry, TalkBack cannot work without accessing the microphone", Toast.LENGTH_SHORT).show();
        System.exit(0);
    }

    /**
     * Starts listening for any user input.
     * When it recognizes something, the <code>processAsrResult</code> method is invoked.
     * If there is any error, the <code>onAsrError</code> method is invoked.
     */
    private void startListening(){

        if(deviceConnectedToInternet()){
            try {

				/*Start listening, with the following default parameters:
					* Language = English
					* Recognition model = Free form,
					* Number of results = 1 (we will use the best result to perform the search)
					*/
                startListeningTime = System.currentTimeMillis();
                listen(Locale.ENGLISH, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
            } catch (Exception e) {
                this.runOnUiThread(new Runnable() {  //Toasts must be in the main thread
                    public void run() {
                        Toast.makeText(getApplicationContext(),"ASR could not be started", Toast.LENGTH_SHORT).show();
                        changeButtonAppearanceToDefault();
                    }
                });

                Log.e(LOGTAG,"ASR could not be started");
                try { speak("Speech recognition could not be started", "EN", ID_PROMPT_INFO); } catch (Exception ex) { Log.e(LOGTAG, "TTS not accessible"); }

            }
        } else {

            this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                public void run() {
                    Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_SHORT).show();
                    changeButtonAppearanceToDefault();
                }
            });
            try { speak("Please check your Internet connection", "EN", ID_PROMPT_INFO); } catch (Exception ex) { Log.e(LOGTAG, "TTS not accessible"); }
            Log.e(LOGTAG, "Device not connected to Internet");

        }
    }

    /**
     * Invoked when the ASR is ready to start listening. Provides feedback to the user to show that the app is listening:
     * 		* It changes the color and the message of the speech button
     */
    @Override
    public void processAsrReadyForSpeech() {
        changeButtonAppearanceToListening();
    }

    /**
     * Provides feedback to the user to show that the app is listening:
     * 		* It changes the color and the message of the speech button
     */
    private void changeButtonAppearanceToListening(){
        Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
        button.setText(getResources().getString(R.string.speechbtn_listening)); //Changes the button's message to the text obtained from the resources folder
        button.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.speechbtn_listening),PorterDuff.Mode.MULTIPLY);  //Changes the button's background to the color obtained from the resources folder
    }

    /**
     * Provides feedback to the user to show that the app is idle:
     * 		* It changes the color and the message of the speech button
     */
    private void changeButtonAppearanceToDefault(){

    	//Microphone needs to be restarted after every speech recognition intent
        Log.i(LOGTAG, "Restarting media recorder");
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile("/dev/null");

        try{
            mRecorder.prepare();
        } catch (IOException e){
            Log.e(debugString, e.getMessage());
        }
        mRecorder.start();
        Log.i(LOGTAG, "Media recorder successfully restarted");


        Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
        button.setText(getResources().getString(R.string.speechbtn_default)); //Changes the button's message to the text obtained from the resources folder
        button.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.speechbtn_default),PorterDuff.Mode.MULTIPLY); 	//Changes the button's background to the color obtained from the resources folder
    }

    /**
     * Provides feedback to the user (by means of a Toast and a synthesized message) when the ASR encounters an error
     */
    @Override
    public void processAsrError(int errorCode) {

        changeButtonAppearanceToDefault();

        //Possible bug in Android SpeechRecognizer: NO_MATCH errors even before the the ASR
        // has even tried to recognized. We have adopted the solution proposed in:
        // http://stackoverflow.com/questions/31071650/speechrecognizer-throws-onerror-on-the-first-listening
        long duration = System.currentTimeMillis() - startListeningTime;
        if (duration < 500 && errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.e(LOGTAG, "Doesn't seem like the system tried to listen at all. duration = " + duration + "ms. Going to ignore the error");
            stopListening();
        }
        else {
            String errorMsg = "";
            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMsg = "Audio recording error";
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMsg = "Unknown client side error";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMsg = "Insufficient permissions";
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMsg = "Network related error";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    errorMsg = "Network operation timed out";
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMsg = "No recognition result matched";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMsg = "RecognitionService busy";
                case SpeechRecognizer.ERROR_SERVER:
                    errorMsg = "Server sends error status";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMsg = "No speech input";
                default:
                    errorMsg = ""; //Another frequent error that is not really due to the ASR, we will ignore it
            }
            if (errorMsg != "") {
                this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Speech recognition error", Toast.LENGTH_LONG).show();
                    }
                });

                Log.e(LOGTAG, "Error when attempting to listen: " + errorMsg);
                try { speak(errorMsg,"EN", ID_PROMPT_INFO); } catch (Exception e) { Log.e(LOGTAG, "TTS not accessible"); }
            }
        }


    }



    /**
     * Synthesizes the best recognition result
     */
    @Override
    public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {

        if(nBestList!=null){

            Log.d(LOGTAG, "ASR found " + nBestList.size() + " results");

            if(nBestList.size()>0){
                String bestResult = nBestList.get(0); //We will use the best result
                try {
                    Log.d(LOGTAG, "Speaking: "+bestResult);
                    //speak(bestResult, "EN", ID_PROMPT_INFO);
                    speak("OK", "EN", ID_PROMPT_INFO);

                    //Sending recognized speech to the server
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    bw.write(COMMAND_CODE+";"+bestResult);
                    bw.newLine();
                    bw.flush();

                } catch (Exception e) { Log.e(LOGTAG, "TTS not accessible"); }

                changeButtonAppearanceToDefault();
            }
        }
    }

    /**
     * Checks whether the device is connected to Internet (returns true) or not (returns false)
     * From: http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
     */
    public boolean deviceConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    /**
     * Shuts down the TTS engine when finished
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    /**
     * Invoked when the TTS has finished synthesizing.
     *
     * In this case, it starts recognizing if the message that has just been synthesized corresponds to a question (its id is ID_PROMPT_QUERY),
     * and does nothing otherwise.
     *
     * According to the documentation the speech recognizer must be invoked from the main thread. onTTSDone callback from TTS engine and thus
     * is not in the main thread. To solve the problem, we use Androids native function for forcing running code on the UI thread
     * (runOnUiThread).
     *
     * @param uttId identifier of the prompt that has just been synthesized (the id is indicated in the speak method when the text is sent
     * to the TTS engine)
     */

    @Override
    public void onTTSDone(String uttId) {
        if(uttId.equals(ID_PROMPT_QUERY.toString())) {
            runOnUiThread(new Runnable() {
                public void run() {
                    startListening();
                }
            });
        }
    }

    /**
     * Invoked when the TTS encounters an error.
     *
     * In this case it just writes in the log.
     */
    @Override
    public void onTTSError(String uttId) {
        Log.e(LOGTAG, "TTS error");
    }

    /**
     * Invoked when the TTS starts synthesizing
     *
     * In this case it just writes in the log.
     */
    @Override
    public void onTTSStart(String uttId) {
        Log.e(LOGTAG, "TTS starts speaking");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        currentAmplitude = getAmplitude();
        //Update debugText to show microphone level
        debugText.setText("Mic level: "+currentAmplitude);

        if(currentAmplitude>10000){
        	//Before running the speech recognition the media recorder (microphone) needs to be stoped.
            Log.i(LOGTAG, "Stopping media recorder");
            mRecorder.stop();
            Log.i(LOGTAG, "Media recorder successfully stopped");

            //Ask the user to speak
            try {
                speak(getResources().getString(R.string.initial_prompt), "EN", ID_PROMPT_QUERY);
            } catch (Exception e) {
                Log.e(LOGTAG, "TTS not accessible");
            }
        }

        try {
            //Sending mouse coordinates to the server based on the selected sensor
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bw.write(MOUSE_COORDINATES_CODE+";"+sensorEvent.values[0]+";"+sensorEvent.values[1]+";"+sensorEvent.values[2]+";"+currentAmplitude);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            Log.e(debugString, e.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public double getAmplitude() {
    	//Method used to retrieve the mic level at any moment
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude());
        else
            return 0;

    }
}