package beacon.critizr.com.beaconapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;


/**
 * Created by obit on 18/10/2014.
 */
public class NotificationCenter {
    private static final int ENTER_REGION_NOTIFICATION = 1;
    private static final int QUIT_REGION_NOTIFICATION = 2;
    private static final int LIKE_NOTIFICATION = 3;

    private static final String[] regions = {"Caisse","Boucherie","Boulangerie"};
    private static final String[] texts   = {"dans notre magasin","dans le rayon boucherie","dans le rayon boulangerie"};

    private static Notification.Builder buildBasicNotification(Context context,String title,String text, int region) {
        int notificationId = 001;
        // Build intent for notification content

        Notification.Builder builder =new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setVibrate(new long[]{0, 1000, 50, 2000});
        builder.setDefaults(Notification.DEFAULT_ALL);
        int resource_id = R.drawable.caisse;
        if ( region== DataLayerListenerService.BEACON1) {
            resource_id = R.drawable.caisse;
        } else if (region == DataLayerListenerService.BEACON2) {
            resource_id = R.drawable.boucherie;
        }else if (region == DataLayerListenerService.BEACON3) {
            resource_id = R.drawable.boulangerie;
        }

        builder.extend(new Notification.WearableExtender()
                .setHintShowBackgroundOnly(true)
                .setBackground(BitmapFactory.decodeResource(context.getResources(),
                        resource_id)));

        return builder;
    }

    public static int showLikeNotification(Context context,boolean isLiking,int region ){
         String title = isLiking?"J'aime !":"J'aime pas !";
         String text = texts[region-1];

         Notification.Builder builder = buildBasicNotification(context,title,text,region);
       // builder.setGroup(regions[region-1]);
        ((NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE)).notify(1, builder.build());

        return 1;
    }

    public static int showEnterRegionNotification(Context context,int region ){
        String title = "Bienvenue !";
        String text = texts[region-1];

        Notification.Builder builder = buildBasicNotification(context,title,text,region);
        Intent call_intent = new Intent(context,CallManager.class);
        call_intent.setAction(MainActivity.CALL_MANAGER_ACTION);
        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(context, 0, call_intent, 0);



        builder.addAction(R.drawable.managercall,
                "Appeler un vendeur", viewPendingIntent);
       // builder.setGroup(regions[region-1]);
        ((NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE)).notify(1, builder.build());
        return 0;
    }

    public static int showQuitRegionNotification(Context context,int region ){
        String title = "A bientôt !";
        String text = texts[region-1];

        Notification.Builder builder = buildBasicNotification(context,title,text,region);
        //builder.setGroup(regions[region-1]);
        ((NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE)).notify(1, builder.build());
        return 0;
    }

}
