package com.willbeddow.will;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.speech.SpeechRecognizer;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private String session_id;
    private TextView answer_view;
    private EditText command;
    private Spinner scroll_circle;
    private Button command_button;
    private FloatingActionButton speak_button;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle b = getIntent().getExtras();
        session_id = b.getString("session_id");
        Log.d("Command create", "Got session_id "+session_id);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        answer_view = (TextView) findViewById(R.id.answer);
        command = (EditText) findViewById(R.id.command);
        scroll_circle = (Spinner) findViewById(R.id.scroll_circle);
        command_button = (Button) findViewById(R.id.command_button);
        speak_button = (FloatingActionButton) findViewById(R.id.speakButton);
        speak_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeVoice();
            }
        });
        command_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand();
            }
        });
    }
    private Response.Listener<String> createCommandReqSuccessListener() {
        scroll_circle.setVisibility(View.INVISIBLE);
        command_button.setVisibility(View.VISIBLE);
        return new Response.Listener<String>() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onResponse(String response) {
                dialog.hide();
                Log.d("Send Command", "Got response "+response);
                try{
                    JSONObject response_json = new JSONObject(response);
                    String response_type = response_json.get("type").toString();
                    String response_text = response_json.get("text").toString();
                    Log.d("Send Command", "Got response type "+response_type+" and response text "+
                        response_text);
                    if (response_type.contains("success")){
                        Log.d("Send Command", "Request was successful");
                        String response_data_json = response_json.get("data").toString();
                        Log.d("Send Command", "Response data in JSON is "+response_data_json);
                        JSONObject response_data = new JSONObject(response_data_json);
                        if (response_data.has("url")){
                            //If the W.I.L.L server passed a url, open it as an intent
                            //Urls that I want to open with a specific intent
                            Log.d("Send Command", "Found url key in received command data");
                            String open_url = response_data.get("url").toString();
                            if (open_url.contains("netflix.com/watch")){
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setClassName("com.netflix.mediaclient", "com.netflix.mediaclient.ui.launch.UIWebViewActivity");
                                    intent.setData(Uri.parse(open_url));
                                    startActivity(intent);
                                }
                                catch(Exception e)
                                {
                                    // netflix app isn't installed, send to website.
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(open_url));
                                    startActivity(intent);
                                }
                            }
                            Log.d("Send Command", "Opening url "+open_url+" as intent");
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(open_url));
                            startActivity(i);
                        }
                        answer_view.setText(response_text);
                    }
                    else{
                        Log.d("Send Command", "Got non successful response type "+response_type);
                        answer_view.setError(response_text);
                    }
                }
                catch (JSONException error){
                    Log.d("LOGIN", "Received malformed JSON response "+response+" from server");
                    answer_view.setError("Received malformed response from server");
                }
            }
            };
    }
    private Response.ErrorListener createCommandReqErrorListener() {
        scroll_circle.setVisibility(View.INVISIBLE);
        command_button.setVisibility(View.VISIBLE);
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                Log.d("LOGIN", "Got error "+error);
                answer_view.setError(error.toString());
            }
        };
    }
    RecognitionListener commandListener = new RecognitionListener(){
        String TAG = "Command Listener";
        @Override
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "onReadyForSpeech");
        }
        @Override
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
        }
        @Override
        public void onRmsChanged(float rmsdB)
        {
            Log.d(TAG, "onRmsChanged");
        }
        @Override
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }
        @Override
        public void onEndOfSpeech()
        {
            dialog.hide();
            Log.d(TAG, "onEndofSpeech");
        }
        @Override
        public void onError(int error)
        {
            Log.d(TAG,  "error " +  error);
            answer_view.setText("error " + error);
        }
        @Override
        public void onResults(Bundle results)
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                Log.d(TAG, "result " + data.get(i));
                str += data.get(i);
            }
           setCommandFromVoice(str);
        }
        @Override
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults");
        }
        @Override
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }
    };
    public void takeVoice(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
               "com.willbeddow.will");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        SpeechRecognizer recognizer = SpeechRecognizer
                .createSpeechRecognizer(this);
        //Set the listener that will take the output and send the command
        recognizer.setRecognitionListener(commandListener);
        dialog = new ProgressDialog(this);
        dialog.setTitle("W.I.L.L");
        dialog.setMessage("Listening...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);
        dialog.show();
        recognizer.startListening(intent);
    }
    public void setCommandFromVoice(String recognized){
        String TAG = "Recognized Command";
        Log.d(TAG, "Setting command to "+recognized);
        command.setText(recognized);
        Log.d(TAG, "Set command, starting sendCommand");
        sendCommand();
    }
    public void sendCommand(){
        if (session_id == null){
            Log.d("Send Command", "Error: session_id is null");
            answer_view.setError("Couldn't find session id");
        }
        else{
            dialog = new ProgressDialog(this);
            dialog.setTitle("W.I.L.L");
            dialog.setMessage("Thinking...");
            dialog.setCancelable(false);
            dialog.setInverseBackgroundForced(false);
            dialog.show();
            //Show progress bar
            scroll_circle.setVisibility(View.VISIBLE);
            command_button.setVisibility(View.INVISIBLE);
            Log.d("Send Command", "Sending command "+command.getText().toString()+
                    " with session_id "+session_id);
            String url = "https://willbeddow.com/api/command";
            Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

                // Set up the network to use HttpURLConnection as the HTTP client.
                    Network network = new BasicNetwork(new HurlStack());
                    RequestQueue queue;
                    // Instantiate the RequestQueue with the cache and network.
                    queue = new RequestQueue(cache, network);
                    queue.start();
                    StringRequest commandReq = new StringRequest(Request.Method.POST,
                            url,
                    createCommandReqSuccessListener(),
                    createCommandReqErrorListener()) {

                protected Map<String, String> getParams() throws com.android.volley.AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("command", command.getText().toString());
                    params.put("session_id", session_id);
                    return params;
                };
            };
            Log.d("Send Command", "Starting request");
            commandReq.setRetryPolicy(new DefaultRetryPolicy(
                    20000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(commandReq);
            Log.d("Send Command", "Sent command "+command);
        }
    }
}
