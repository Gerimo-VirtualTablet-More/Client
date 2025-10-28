package com.antozstudios.drawnow;

import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.TOOL_TYPE_FINGER;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.View.inflate;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.motion.widget.MotionLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.antozstudios.drawnow.Helper.HelperMethods;
import com.antozstudios.drawnow.Helper.KeyHelper;
import com.antozstudios.drawnow.databinding.ActivityDrawBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DrawActivity extends AppCompatActivity {

    public static final String EXTRA_SERVER_IP = "com.antozstudios.drawnow.SERVER_IP";
    private static final String TAG = "UDPClient";
    private String SERVER_IP;
    private static final int SERVER_PORT = 5001;
    private ActivityDrawBinding binding;

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;

    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private Thread senderThread;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.0f;

    boolean lastTouchMode,touchMode, showLastPointMode;

    PinchClass pinchClass = new PinchClass();

    ViewPropertyAnimator anim1,anim2;



    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;

    public String buildJsonPayload(String prompt) {
        return "{\n" +
                "  \"model\": \"deepseek/deepseek-chat-v3.1:free\",\n" +
                "  \"response_format\": {\"type\": \"json_object\"},\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"role\": \"system\",\n" +
                "      \"content\": \"You are Gerimo AI. Output only valid JSON. The user can ask you to create known or specific shortcuts or sequences of shortcuts for a given program on Windows 11. Rules: Only prompts related to shortcuts or sequences of shortcuts can be answered. Use only the official C# KeyCodes from System.Windows.Forms.Keys for all responses, e.g. Control=0x11, A=0x41, etc. Output a clean JSON format as shown in: {\\\"Program_Name\\\":{\\\"Example_Name_For_Sequence\\\":\\\"LControl;A;LControl;C;LControl;F4\\\",\\\"Example_Name_For_Shortcut\\\":\\\"LControl;N\\\"}}\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \""+prompt+"\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    Dialog open_AI_Dialog;
    Dialog open_private_config;


    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        open_AI_Dialog = new Dialog(this);
        open_private_config = new Dialog(this);



sharedPreferences = getSharedPreferences("PRIVATE_CONFIG",MODE_PRIVATE);
editor = getSharedPreferences("PRIVATE_CONFIG",MODE_PRIVATE).edit();

Log.d("HALLO",sharedPreferences.getString("KEY","").toString());


        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(binding.aiModeButton, "rotation", 0f, 360f);
        rotationAnimator.setDuration(1000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.start();

        binding.aiModeButton.setOnClickListener((View)->{

if(sharedPreferences.getString("KEY","").isBlank()){

    open_private_config(open_private_config);

}else{

    open_AI_Dialog(open_AI_Dialog);


}



        });


        binding.aiSettings.setOnClickListener((View)->{
          open_private_config(open_AI_Dialog);
        });


        // Manually adding of shortcuts
        binding.addShortCutButton.setOnClickListener((View)->{
            Dialog dialog = new Dialog(this);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setContentView(R.layout.popup_create_shortcuts);
            dialog.show();


            TextView shortCutResultTextview = dialog.findViewById(R.id.shortCutResultTextview);
            LinearLayout shortCutLinearLayout = dialog.findViewById(R.id.shortCutLinearLayout);
            FloatingActionButton addSpinner = dialog.findViewById(R.id.addSpinnerButton);
            FloatingActionButton deleteSpinnerButton = dialog.findViewById(R.id.deleteSpinnerButton);
            FloatingActionButton saveCommandsButton = dialog.findViewById(R.id.saveCommandsButton);







            addSpinner.setOnClickListener((v)->{

                Spinner spinner = new Spinner(this);
                SpinnerAdapter spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, KeyHelper.KeyCode.getAllKeys());

                spinner.setAdapter(spinnerAdapter);


                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {



                    final List<String>  values = shortCutLinearLayout.getTouchables()
                                .stream()
                                .filter(view -> view instanceof Spinner)
                                .map(view -> ((Spinner) view).getSelectedItem().toString())
                                .toList();




                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                       shortCutResultTextview.setText(HelperMethods.GetSplitedString('+', values));

                        Log.d("Antonucio",";");
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });


                shortCutLinearLayout.addView(spinner);




            });

            deleteSpinnerButton.setOnClickListener((v)->{
                if(shortCutLinearLayout.getChildAt(shortCutLinearLayout.getChildCount()-1) instanceof Spinner){
                    shortCutLinearLayout.removeViewAt(shortCutLinearLayout.getChildCount()-1);
                }

           //     shortCutResultTextview.setText(HelperMethods.GetSplitedString('+', values));

            });




        });



        MotionLayout motionLayout = binding.drawActivity;
         anim1= binding.pinchCursor1.animate();
         anim2= binding.pinchCursor2.animate();

        initDefaultFloatingButtons();
        binding.SettingsButton.setOnClickListener((View) -> {
            if (motionLayout.getProgress() == 0) {
                motionLayout.transitionToEnd();
            } else {
                motionLayout.transitionToStart();
            }
        });



        String serverIpFromIntent = getIntent().getStringExtra(EXTRA_SERVER_IP);
        if (serverIpFromIntent != null && !serverIpFromIntent.isEmpty()) {
            SERVER_IP = serverIpFromIntent;
        } else {
            Log.e(TAG, "DrawActivity started without a server IP. Finishing activity.");
            finish();
            return;
        }

        mScaleDetector = new ScaleGestureDetector(this, pinchClass);
        HelperMethods.setUI(this);

        initUDP();



    }

    /// //
    private void open_private_config(Dialog dialog) {

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setContentView(R.layout.popup_private_config);
        dialog.setCancelable(true);
        dialog.show();

        TextView title1 = dialog.findViewById(R.id.title1);
        title1.setText(!sharedPreferences.getString("KEY","").isBlank()?"":title1.getText());

        TextView title2 = dialog.findViewById(R.id.title2);
        title2.setText(!sharedPreferences.getString("KEY","").isBlank()?"Change your API-Key:":title1.getText());

        TextInputEditText api_key_input = dialog.findViewById(R.id.api_key_input);
        api_key_input.setText(!sharedPreferences.getString("KEY","").isBlank()?sharedPreferences.getString("KEY",""):"");

        Button saveButton = dialog.findViewById(R.id.saveButton);
        FloatingActionButton goToOpenRouter = dialog.findViewById(R.id.goToOpenRouter);

        saveButton.setOnClickListener((v)->{

            if(Objects.requireNonNull(api_key_input.getText()).length()>0){
                editor.putString("KEY",api_key_input.getText().toString());
                editor.apply();
                Toast.makeText(this,"API Key saved.",Toast.LENGTH_SHORT).show();
                dialog.cancel();

                open_AI_Dialog(open_AI_Dialog);
            }else{
                Toast.makeText(this,"Please enter a valid API Key.",Toast.LENGTH_SHORT).show();
            }
        });

        goToOpenRouter.setOnClickListener((v)->{
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/")));
        });


    }

    private void open_AI_Dialog(Dialog dialog) {
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setContentView(R.layout.popup_ai);
        dialog.show();



        // PopUp
        Button createButton = dialog.findViewById(R.id.create);
        LinearLayout layout = dialog.findViewById(R.id.ai_shortcut_linearLayout);
        TextInputEditText textInputEditText = dialog.findViewById(R.id.textInputEditText);
        TextInputLayout textInputLayout = dialog.findViewById(R.id.textInputLayout);
        //




        createButton.setOnClickListener((v) -> {
            if (textInputEditText.getText().length() > 0) {

                dialog.setCancelable(false);
                LottieAnimationView lottieAnimationView = layout.findViewById(R.id.LoadingBar);
                lottieAnimationView.setVisibility(VISIBLE);
                lottieAnimationView.playAnimation();

                String jsonBody = buildJsonPayload(textInputEditText.getText().toString());

                OkHttpClient client = new OkHttpClient();


                textInputEditText.setText("");
                textInputLayout.setVisibility(INVISIBLE);
                createButton.setVisibility(INVISIBLE);


                Request request = new Request.Builder()
                        .url("https://openrouter.ai/api/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + sharedPreferences.getString("KEY",""))
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("PPP", "Fehler beim Senden: " + e.getMessage());
                    }


                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String responseString = response.body().string();
                        try {
                            JSONObject rawObject = new JSONObject(responseString);
                            JSONArray choices = rawObject.getJSONArray("choices");
                            JSONObject messageObject = choices.getJSONObject(0).getJSONObject("message");
                            String reasoning = messageObject.getString("reasoning")
                                    .trim()
                                    .replace("```json", "")
                                    .replace("```", "");

                            add_checkBoxShortCuts(reasoning, layout);


                            Log.d("PPP", reasoning);
                        } catch (JSONException e) {
                            Log.e("PPP", "Response kein gültiges JSON: " + responseString);
                        } finally {
                            runOnUiThread(()->{
                                lottieAnimationView.cancelAnimation();
                                lottieAnimationView.setVisibility(GONE);


                                textInputLayout.setVisibility(VISIBLE);
                                createButton.setVisibility(VISIBLE);
                            });
                            dialog.setCancelable(true);
                        }


                    }
                });
            }
        });

    }
    /// //

    public void add_checkBoxShortCuts(String jsonReponse, ViewGroup view) {



        try {
            JSONObject jsonObject = new JSONObject(jsonReponse);


            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Log.d("ProgramName", key);

                Object value = jsonObject.get(key);

                if (value instanceof JSONObject) {
                    JSONObject children = (JSONObject) value;
                    JSONArray shortcutNames = children.names();

                    if (shortcutNames != null) {
                        for (int i = 0; i < shortcutNames.length(); i++) {
                            String shortcutName = shortcutNames.getString(i);
                            String shortcutValues = children.getString(shortcutName).replace(";","+");

                            View checkbox_shortcut = inflate(this,R.layout.checkbox_shortcut,null);
                            TextView textView = checkbox_shortcut.findViewById(R.id.textview);
                            runOnUiThread(()->{
                                textView.setText(new StringBuilder(shortcutName+ " "+ shortcutValues));
                                view.addView(checkbox_shortcut);

                            });
                            Log.d("ShortCut",shortcutValues);

                        }
                    }
                } else {
                    Log.w("JSON_WARNING", key + " ist kein JSONObject: " + value);
                }
            }




        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


    }

    private void initUDP() {
        try {
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_IP);
            senderThread = new Thread(this::sendLoop);
            senderThread.start();
            Log.i(TAG, "UDP-Client gestartet. Ziel: " + SERVER_IP + ":" + SERVER_PORT);
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Initialisieren von UDP: ", e);
        }
    }

    private void sendLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String msg = sendQueue.take();
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
                udpSocket.send(packet);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Senden: ", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeUDP();
    }

    private void closeUDP() {
        if (senderThread != null) senderThread.interrupt();
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        Log.i(TAG, "UDP-Client geschlossen");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        pinchClass.onScale(mScaleDetector);

        if (event.getToolType(0) == TOOL_TYPE_STYLUS) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            float pressure = event.getPressure();

            int action = event.getActionMasked();



            if (showLastPointMode) {
                binding.cursorImage.setVisibility(VISIBLE);
                binding.cursorImage.setX(x - ((float) binding.cursorImage.getWidth() / 2));
                binding.cursorImage.setY(y - ((float) binding.cursorImage.getHeight() / 2));
                binding.cursorImage.setScaleX((float) Math.max(0.5, pressure));
                binding.cursorImage.setScaleY((float) Math.max(0.5, pressure));
            }

            sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.CLICK, x,y, pressure));
        }

if(touchMode){



    if(event.getPointerCount()>=2){

        if (event.getToolType(0) == TOOL_TYPE_FINGER  && event.getToolType(1)==TOOL_TYPE_FINGER) {




            int x1 = (int) event.getX(0);
            int x2 = (int) event.getX(1);






            int y1 = (int) event.getY(0);
            int y2 = (int) event.getY(1);

            binding.pinchCursor1.setVisibility(VISIBLE);
            binding.pinchCursor2.setVisibility(VISIBLE);


            anim1.scaleX(1.5F);
            anim1.scaleY(1.5F);
            anim1.setDuration(100);
            anim1.start();


            anim2.scaleX(1.5F);
            anim2.scaleY(1.5F);
            anim2.setDuration(100);
            anim2.start();




            binding.pinchCursor1.setX(x1 - ((float) binding.pinchCursor1.getWidth() / 2));
            binding.pinchCursor1.setY(y1 - ((float) binding.pinchCursor1.getHeight() / 2));


            binding.pinchCursor2.setX(x2 - ((float) binding.pinchCursor2.getWidth() / 2));
            binding.pinchCursor2.setY(y2 - ((float) binding.pinchCursor2.getHeight() / 2));





            Log.d("PINCH",String.valueOf("Pointer1 "+x1)+String.valueOf("Pointer2 "+x2) );


            int action = event.getActionMasked();

            if(action==ACTION_POINTER_UP){
                sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.PINCH, 0, 0, 0, 0));
            }else{
                sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.PINCH, x1, x2, y1, y2));
            }




        }


    }else{
        binding.pinchCursor1.setVisibility(INVISIBLE);
        binding.pinchCursor2.setVisibility(INVISIBLE);
anim1.cancel();
anim2.cancel();
anim1.scaleX(1.0F);
anim1.scaleY(1.0F);
anim2.scaleX(1.0F);
anim2.scaleY(1.0F);


    }

}



        return super.onTouchEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        HelperMethods.setUI(this);

        if (event.getToolType(0) == TOOL_TYPE_STYLUS) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_HOVER_MOVE) {
                touchMode = false;
                Log.d("Enter", "enter");
                if (showLastPointMode) {
                    binding.cursorImage.setX(x - ((float) binding.cursorImage.getWidth() / 2));
                    binding.cursorImage.setY(y - ((float) binding.cursorImage.getHeight() / 2));
                    binding.cursorImage.setVisibility(VISIBLE);
                }
                float x1 = x* ((float) 1920 /2560);
                float y1 = y* ((float) 1080 /1600);
                sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.HOVER, (int) x1, (int) y1, 0.01f));
            }else{
                touchMode = touchMode && lastTouchMode;
            }
        }
        return super.onGenericMotionEvent(event);
    }



    private class PinchClass extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
            return true;
        }
    }



    void initDefaultFloatingButtons(){
        binding.undoButton.setOnClickListener((View)->{
            sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.HOTKEY, new KeyHelper.KeyCode[]{KeyHelper.KeyCode.LControl,KeyHelper.KeyCode.Z}));
        });
        binding.redoButton.setOnClickListener((View)->{
            sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.HOTKEY, new KeyHelper.KeyCode[]{KeyHelper.KeyCode.LControl,KeyHelper.KeyCode.Y}));
        });
        binding.fingerModeButton.setOnClickListener((View)->{
            touchMode =!touchMode;
            lastTouchMode = touchMode;

        });
       binding.showCursorButton.setOnClickListener((View)->{
           showLastPointMode=!showLastPointMode;
       });

    }





}
