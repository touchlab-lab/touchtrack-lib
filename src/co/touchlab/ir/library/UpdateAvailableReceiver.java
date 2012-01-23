package co.touchlab.ir.library;

import android.*;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import co.touchlab.ir.MemLog;
import co.touchlab.ir.process.UploadManagerService;

/**
 * Created by IntelliJ IDEA.
 * User: kgalligan
 * Date: 1/21/12
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateAvailableReceiver extends BroadcastReceiver
{

    public static final int UPDATE_NOTIFY_ID = 142356;

    public void onReceive(Context context, Intent intent)
    {
        MemLog.d(getClass().getName(), "UpdateAvailableReceiver called");

        String downloadLink = intent != null ? intent.getStringExtra(UploadManagerService.DOWNLOAD_LINK_KEY) : null;

        //WTF?
        if(downloadLink == null)
            return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int icon = R.drawable.ic_stat_example;
        CharSequence tickerText = context.getString(R.string.update_app_notification_text);
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        CharSequence contentTitle = context.getString(R.string.update_app_notification_title);
        CharSequence contentText = context.getString(R.string.update_app_notification_message);

        Intent notificationIntent = new Intent(context, UpdateDownloadActivity.class);
        notificationIntent.putExtra(UploadManagerService.DOWNLOAD_LINK_KEY, downloadLink);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        notificationManager.notify(UPDATE_NOTIFY_ID, notification);
    }
}
