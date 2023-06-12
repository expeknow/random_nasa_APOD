package com.expeknow.apodnasa;


import static android.content.ContentValues.TAG;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {
    JSONArray jsonArray;

    Boolean SwipeLeft = true;
    Boolean SwipeRight = false;
    TextView title;
    TextView details;
    Button next_btn;
    Button download_btn;
    ImageView imageView;
    JSONObject data;
    ConnectivityManager cm;
    GestureDetector gestureDetector;
    int index = -1;
    ConstraintLayout mainView;

    int SWIPE_MIN_DISTANCE = 120;
    int SWIPE_THRESHOLD_VELOCITY = 200;
    
    final String APODUrl = "https://api.nasa.gov/planetary/apod?api_key=3EmuuZMwHjcjyMUzc9rFWfCisCpdJrkifFUHlumj&count=15";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        title = findViewById(R.id.title_text);
        details = findViewById(R.id.details_box);
        imageView = findViewById(R.id.image);
        next_btn = findViewById(R.id.next_btn);
        download_btn = findViewById(R.id.download_btn);
        mainView = findViewById(R.id.mainView);
        /*
         * this getSystemService() returns a generic object that needs to be casted into ConnectivityManager
         * It is used to get a system level android service which helps in knowing the state of android
         * hardware and software like wifi, network, volume, etc.
         */
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        next_btn.setOnClickListener(view -> getData(SwipeLeft));

        download_btn.setOnClickListener(view -> {
            try {
                downloadConditionChecker();
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Error in downloading image. Please reopen the app.", Toast.LENGTH_LONG).show();
                throw new RuntimeException(e);
            }
        });

        getData(SwipeLeft);

        gestureDetector = new GestureDetector(this, new MyGestureListener());

    }
    /**
     * This code overrides the onTouchEvent() method of a View or Activity to pass touch events to
     * a GestureDetector for gesture recognition.
     * MotionEvent is received by this method whenever any touch happens and further it is sent
     * to onTouchEvent of gestureDetector Class for further processing
     * */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                getData(SwipeLeft);
                return true;
            }
            else if(e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY){
                getData(SwipeRight);
            }
            return false;
        }
    }

    private void getData(Boolean swipeDirection) {

        if(isNotConnected()){
            Snackbar.make(this,
                    mainView,
                    "No internet connection available!",
                    Snackbar.LENGTH_LONG).show();
            String lit = "No Internet Connection";
            title.setText(lit);
            details.setText(lit);
            imageView.setImageResource(R.drawable.no_internet);
            return;
        }

        if(jsonArray!=null && index < jsonArray.length()-1){
            if(swipeDirection == SwipeLeft){
                index++;
            }else if(swipeDirection == SwipeRight && index > 0){
                index--;
            }else{
                Snackbar.make(this,
                        mainView,
                        "No image present before this image!",
                        Snackbar.LENGTH_LONG).show();
                return;
            }

            try {
                data = jsonArray.getJSONObject(index);
            } catch (JSONException e){
                e.printStackTrace();
            }

            setDataOnScreen();
            return;
        }

        RequestQueue mRequestQueue = Volley.newRequestQueue(this);
        Glide.with(getApplicationContext())
                .asGif()
                .load(R.drawable.loading)
                .into(imageView);

        /*
         * this variable defines what request to make, how to handle the response and what to do
         * with the received response. It is from volley
         */
        StringRequest mStringRequest = new StringRequest(Request.Method.GET, APODUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONArray newJsonArray = new JSONArray(response);
                    if(jsonArray == null){
                        jsonArray = newJsonArray;
                    }else{
                        for(int i=0; i<newJsonArray.length(); i++){
                            jsonArray.put(newJsonArray.get(i));
                        }
                    }
                    data = jsonArray.getJSONObject(index > -1 ? index : ++index);
                    if(!data.getString("media_type").equals("image")){
                        getData(SwipeLeft);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                setDataOnScreen();
            }
        },
                new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error :" + error.toString());
            }
        });

        mRequestQueue.add(mStringRequest);
    }

    private void setDataOnScreen() {
        try {
            String newTitle = data.getString("title");
            String newDetails = data.getString("explanation");
            String date = data.getString("date");


            Glide.with(getApplicationContext())
                    .load(data.optString("url", data.optString("hdurl", "")))
                    .placeholder(R.drawable.wait)
                    .into(imageView);
            String detail = "Picture from: " + date+ "\n\n" + newDetails;
            details.setText(detail);
            title.setText(newTitle);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadConditionChecker() throws JSONException {

        if(data == null){
            Snackbar.make(this,
                    mainView,
                    "No download link present.",
                    Snackbar.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(), "No download link present. Please load any image and try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(isNotConnected()){
            Snackbar.make(this,
                    mainView,
                    "No Internet Connection.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        downloadImage();
    }


    public void downloadImage() throws JSONException {

        String img_link;
        if(data.isNull("hdurl")){
            img_link = data.getString("url");
        }else{
            img_link = data.getString("hdurl");
        }
        Snackbar.make(this,
                mainView,
                "Downloading Image...",
                Snackbar.LENGTH_LONG).show();

        //here we directly use DownloadManager to create a request for HTTP download
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(img_link));

        request.setTitle("NASA Image"); // Title for notification
        request.setDescription("Downloading...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Nasa_image.jpg");
        request.setMimeType("image/jpg");

        DownloadManager manager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            //download service requests for http downloads from given link
            manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        }

        //assertion is a way to throw Assertion error if the manager variable is null
        assert manager != null;
        manager.enqueue(request);
    }


    public boolean isNotConnected(){
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork == null || !activeNetwork.isConnectedOrConnecting();
    }


}