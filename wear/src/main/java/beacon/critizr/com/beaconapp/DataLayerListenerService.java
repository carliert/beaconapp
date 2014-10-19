package beacon.critizr.com.beaconapp;

/**
 * Created by obit on 12/10/2014.
 */


import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Collection;
import java.util.HashSet;

public class DataLayerListenerService extends WearableListenerService implements SensorEventListener {
    public static boolean detectPlate;

    public static final int BEACON1 =  1;
    public static final int BEACON2 = 2;
    public static final int BEACON3 = 3;

    GoogleApiClient mGoogleApiClient;

    private static final String TAG = "ListenerService";

    private static final String BEACON_ENTER_MESSAGE = "enter_region/";
    private static final String BEACON_QUIT_MESSAGE = "quit_region/";

    private SensorManager sensorManager;

    public static int currentRegion = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged: " + dataEvents);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        LOGD(TAG, "onMessageReceived: " + messageEvent);
        // Check to see if the message is to start an activity
        if (messageEvent.getPath().startsWith(BEACON_ENTER_MESSAGE)) {
            int beacon_num = Integer.parseInt(messageEvent.getPath().split(BEACON_ENTER_MESSAGE)[1]);
            if (beacon_num == 1) {
                NotificationCenter.showEnterRegionNotification(this,BEACON1);
            }else if (beacon_num == 2) {
                NotificationCenter.showEnterRegionNotification(this,BEACON2);
            }else if (beacon_num == 3) {
                NotificationCenter.showEnterRegionNotification(this,BEACON3);
            }
            currentRegion = beacon_num;
            detectPlate = true;
            this.startSensor();

        } else if (messageEvent.getPath().startsWith(BEACON_QUIT_MESSAGE)) {
            int beacon_num = Integer.parseInt(messageEvent.getPath().split(BEACON_QUIT_MESSAGE)[1]);
            if (beacon_num == 1) {
                NotificationCenter.showQuitRegionNotification(this, BEACON1);
            }else if (beacon_num == 2) {
                NotificationCenter.showQuitRegionNotification(this, BEACON2);
            }else if (beacon_num == 3) {
                NotificationCenter.showQuitRegionNotification(this, BEACON3);
            }
            currentRegion = -1;
            detectPlate = false;
            this.stopSensor();
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        LOGD(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        LOGD(TAG, "onPeerDisconnected: " + peer);
    }

    public static void LOGD(final String tag, String message) {
        //if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(TAG, message);
       // }
    }

    public void startSensor(){
        Log.d(TAG,"Sensor On");
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopSensor(){
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (currentRegion>0) {
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                float roll = event.values[1];
                float pitch = event.values[2];
                if (pitch < 10 && pitch > -10) {
                        /*if (roll < -80) {
                            //DISLIKE !
                            new SendMessageActivityTask().execute(DISLIKE_MESSAGE);
                            NotificationCenter.showLikeNotification(this,false,currentRegion);
                            Log.d(TAG, "Sensor DisLike");
                            firstPositionFounded = false;
                        } else if (roll > 70) {
                            //LIKE !
                            new SendMessageActivityTask().execute(LIKE_MESSAGE);
                            NotificationCenter.showLikeNotification(this,true,currentRegion);
                            Log.d(TAG, "Sensor Like");
                            firstPositionFounded = false;
                        }*/
                    if (roll < 10 && roll > -10) {
                        if (detectPlate) {
                            detectPlate = false;
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                }

               /* float roll = event.values[1];
                float pitch = event.values[2];
                if (pitch < 10 && pitch > -10) {
                    if (firstPositionFounded) {
                        if (roll < -80) {
                            //DISLIKE !
                            new SendMessageActivityTask().execute(DISLIKE_MESSAGE);
                            NotificationCenter.showLikeNotification(this,false,currentRegion);
                            Log.d(TAG, "Sensor DisLike");
                            firstPositionFounded = false;
                        } else if (roll > 70) {
                            //LIKE !
                            new SendMessageActivityTask().execute(LIKE_MESSAGE);
                            NotificationCenter.showLikeNotification(this,true,currentRegion);
                            Log.d(TAG, "Sensor Like");
                            firstPositionFounded = false;
                        }
                    } else if (roll < 10 && roll > -10) {
                        if (detectPlate) {
                            detectPlate = false;
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        firstPositionFounded = true;
                    }
                }
                */
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
