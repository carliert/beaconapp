package beacon.critizr.com.beaconapp;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;


public class MainActivity extends Activity implements
        DelayedConfirmationView.DelayedConfirmationListener, SensorEventListener {
    public static final String CALL_MANAGER_ACTION = "CALL_MANAGER";

    private static final String LIKE_MESSAGE = "like_message/";
    private static final String DISLIKE_MESSAGE = "dislike_message/";

    private DelayedConfirmationView mDelayedView;

    GoogleApiClient mGoogleApiClient;

    private SensorManager sensorManager;

    boolean hasLiked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mDelayedView =
                (DelayedConfirmationView) findViewById(R.id.delayed_confirmation);
        mDelayedView.setListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity","intent action : "+getIntent().getAction());
        // Two seconds to cancel the action
        mDelayedView.setTotalTimeMs(8000);
        // Start the timer
        mDelayedView.start();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);

        hasLiked = false;
    }


    @Override
    public void onTimerFinished(View view) {
        if (hasLiked) {

        } else {
            //DataLayerListenerService.detectPlate = true;
            finish();
        }
    }

    @Override
    public void onTimerSelected(View view) {
        // User canceled, abort the action
    }

    protected void onNewIntent (Intent intent){
        Log.d("MainActivity","OnNewIntent");
    }

    @Override
    public void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            float roll = event.values[1];
            float pitch = event.values[2];
            if (pitch < 10 && pitch > -10) {
                if (roll < -80) {
                    //DISLIKE !
                   //
                   // NotificationCenter.showLikeNotification(this,false,DataLayerListenerService.currentRegion);
                    //Log.d(TAG, "Sensor DisLike");
                    if (!hasLiked) {
                        mDelayedView.setImageResource(R.drawable.dislike);
                        new SendMessageActivityTask().execute(DISLIKE_MESSAGE);
                    }
                    hasLiked = true;
                } else if (roll > 70) {
                    //LIKE !
                    //new SendMessageActivityTask().execute(LIKE_MESSAGE);
                    //NotificationCenter.showLikeNotification(this,true,DataLayerListenerService.currentRegion);
                    if (!hasLiked) {
                        mDelayedView.setImageResource(R.drawable.like);
                        new SendMessageActivityTask().execute(LIKE_MESSAGE);
                    }
                    hasLiked = true;
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private class SendMessageActivityTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... args) {
            String path = (String )args[0];
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                Wearable.MessageApi.sendMessage(mGoogleApiClient, node, path,
                        null);
            }
            return null;
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }
}
