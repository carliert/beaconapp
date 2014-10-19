package beacon.critizr.com.beaconapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RangedBeacon;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends Activity implements MessageApi.MessageListener, NodeApi.NodeListener, ConnectionCallbacks,
        OnConnectionFailedListener, BeaconConsumer {
    private static final int mNetworkRequestLocation = 1;
    private static final int mNetworkRequestLike = 2;
    private static final int mNetworkRequestCall = 3;

    private static final String TAG = "MainActivity";

    private static final String BEACON_ENTER_MESSAGE = "enter_region/";
    private static final String BEACON_QUIT_MESSAGE = "quit_region/";

    private static final String LIKE_MESSAGE = "like_message/";
    private static final String DISLIKE_MESSAGE = "dislike_message/";

    private static final String CALL_MANAGER = "call_manager/";


    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private boolean mResolvingError = false;

    private GoogleApiClient mGoogleApiClient;

    private BeaconManager beaconManager;

    private List distances = new ArrayList(Arrays.asList(0.0, 0.0, 0.0));

    private TextView tvBeacon1;
    private TextView tvBeacon2;
    private TextView tvBeacon3;
    private TextView mActionTv;

    private int currentRegion = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionTv= (TextView)this.findViewById(R.id.action_tv);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        verifyBluetooth();
        RangedBeacon.setSampleExpirationMilliseconds(5000);

        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb
        //
       /* beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=aabb,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
*/
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconManager.bind(this);
        beaconManager.debug = true;

        tvBeacon1 = (TextView) findViewById(R.id.textViewBeacon1);
        tvBeacon2 = (TextView) findViewById(R.id.textViewBeacon2);
        tvBeacon3 = (TextView) findViewById(R.id.textViewBeacon3);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "Google API Client was connected");
        mResolvingError = false;

        //Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "Connection to Google API client was suspended");
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        LOGD(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                .getRequestId() + " " + messageEvent.getPath());

        if(messageEvent.getPath().equalsIgnoreCase(MainActivity.DISLIKE_MESSAGE)){
            runOnUiThread(new Runnable() {
                public void run() {
                    mActionTv.setText("jaime pas");
                }
            });
            new NetworkTask().execute(new Integer(mNetworkRequestLike),new Boolean(false));

        }else if(messageEvent.getPath().equalsIgnoreCase(MainActivity.LIKE_MESSAGE)){
            runOnUiThread(new Runnable() {
                public void run() {
                    mActionTv.setText("jaime");
                }
            });
            new NetworkTask().execute(new Integer(mNetworkRequestLike),new Boolean(true));

        } else if (messageEvent.getPath().equalsIgnoreCase(MainActivity.CALL_MANAGER)){
            runOnUiThread(new Runnable() {
                public void run() {
                    mActionTv.setText("call manager");
                }
            });
            new NetworkTask().execute(new Integer(mNetworkRequestCall));
        }

    }



    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        LOGD(TAG, "onPeerConnected: " + peer);

    }

    @Override //NodeListener
    public void onPeerDisconnected(final Node peer) {
        LOGD(TAG, "onPeerDisconnected: " + peer);
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

    public void onActionClick(View view) {

    }

    public void onRadioButtonClicked(View view) {
        if (view.getId() == R.id.radio_no) {
            new SendMessageActivityTask().execute(BEACON_QUIT_MESSAGE);

        }else {
            if (view.getId() == R.id.radio_1) {
                currentRegion = 1;
            }else if (view.getId() == R.id.radio_2) {
                currentRegion = 2;
            }else if (view.getId() == R.id.radio_3) {
                currentRegion = 3;
            }
            new SendMessageActivityTask().execute(BEACON_ENTER_MESSAGE);
        }
        mActionTv.setText("");


    }


    private class SendMessageActivityTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... args) {

            String path = (String )args[0];
            path += currentRegion;

            if (path.startsWith(BEACON_QUIT_MESSAGE))
                currentRegion = -1;

            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                Wearable.MessageApi.sendMessage(mGoogleApiClient, node, path,
                        null);
            }
            return null;
        }
    }

    /**
     * Beacon Ranging
     */

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Beacon firstBeacon = beacons.iterator().next();
                    Double dist = firstBeacon.getDistance();
                    int index = 0;
                    if(firstBeacon.getId3().toInt() == 62208){ //rssi Hallalerie (00221)
                        index = 2;
                        tvBeacon3.setText("Beacon 3 (Hallalerie) : "+dist);
                    }else if (firstBeacon.getId3().toInt() == 50432){   //rssi Boucherie  (00284)
                        index = 1;
                        tvBeacon2.setText("Beacon 2 (Boucherie) : "+dist);
                    }else if (firstBeacon.getId3().toInt() == 11008){   //rssi Boulangerie (00234)
                        index = 0;
                        tvBeacon1.setText("Beacon 1 (Boulangerie) : "+dist);

                    }
                    distances.set( index, dist);
                    new NetworkTask().execute(new Integer(mNetworkRequestLocation),(Double)distances.get(0),(Double)distances.get(1),(Double)distances.get(2));

                    Log.d("DistanceTable"," Boulangerie " + distances.get(0) + " Boucherie " + distances.get(1) + " Hallalerie " + distances.get(2));
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("Hallalerie", new Identifier(Identifier.parse("60d4fcf0-507e-11e4-916c-0800200c9a66")) , new Identifier(Identifier.fromInt(1)), new Identifier(Identifier.fromInt(62208)))); /*hallalerie 00221*/
            beaconManager.startRangingBeaconsInRegion(new Region("Boucherie", new Identifier(Identifier.parse("60d4fcf0-507e-11e4-916c-0800200c9a66")) , new Identifier(Identifier.fromInt(1)), new Identifier(Identifier.fromInt(50432))));   /*Boucherie 00284*/
            beaconManager.startRangingBeaconsInRegion(new Region("Boulangerie", new Identifier(Identifier.parse("60d4fcf0-507e-11e4-916c-0800200c9a66")) , new Identifier(Identifier.fromInt(1)), new Identifier(Identifier.fromInt(11008))));  /*Boulangerie 00234*/
        } catch (RemoteException e) {   }
    }

    /**
     * Utils
     */

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                /*final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();*/
            }
        }
        catch (RuntimeException e) {
           /* final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();

            */

        }

    }

    /**
     * AsyncTask
     */

    private class NetworkTask extends AsyncTask<Object, Integer, Double>{

        @Override
        protected Double doInBackground(Object ...params) {
            InputStream inputStream = null;
            String result = "";
            try {


                String url  = "h";

                String json = "";

                Integer type = (Integer) params[0];

                if (type == MainActivity.mNetworkRequestCall ){
                    url += "call_manager/";
                } else  if (type == MainActivity.mNetworkRequestLike ){
                    Boolean isLiking = (Boolean) params[1];
                    url += "like/";
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.accumulate("liking", isLiking);
                    json = jsonObject.toString();

                } else  if (type == MainActivity.mNetworkRequestLocation ){
                    url += "location/";
                    Double b1 = (Double) params[1];
                    Double b2 = (Double) params[2];
                    Double b3 = (Double) params[3];

                    // 3. build jsonObject
                    JSONArray array = new JSONArray();

                    boolean in_region = (b1<1.0)?true:false;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.accumulate("identifier", "beacon_1");
                    jsonObject.accumulate("distance", b1);
                    jsonObject.accumulate("in_region", in_region);
                    array.put(jsonObject);

                    in_region = (b2<1.0)?true:false;
                    jsonObject = new JSONObject();
                    jsonObject.accumulate("identifier", "beacon_2");
                    jsonObject.accumulate("distance", b2);
                    jsonObject.accumulate("in_region", in_region);
                    array.put(jsonObject);

                    in_region = (b3<1.0)?true:false;
                    jsonObject = new JSONObject();
                    jsonObject.accumulate("identifier", "beacon_3");
                    jsonObject.accumulate("distance", b3);
                    jsonObject.accumulate("in_region", in_region);
                    array.put(jsonObject);

                    // 4. convert JSONObject to JSON to String
                    json = array.toString();

                }

                // Create a new HttpClient and Post Header
                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(url);

                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);

                // 6. set httpPost Entity
                httpPost.setEntity(se);

                // 7. Set some headers to inform server about the type of the content
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                Log.d("NRequest","Will execute request "+json);
                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);

                // 9. receive response as inputStream
                inputStream = httpResponse.getEntity().getContent();

                // 10. convert inputstream to string
                if(inputStream != null)
                    //result = convertInputStreamToString(inputStream);
                    Log.d("NRequest","Request Success");
                else
                    Log.d("NRequest","Request Failed");


            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }


            return null;
        }

    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

}
