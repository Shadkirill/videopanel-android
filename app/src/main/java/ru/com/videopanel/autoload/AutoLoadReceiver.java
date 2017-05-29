package ru.com.videopanel.autoload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.com.videopanel.activities.StartActivity;

/**
 * Auto load ACTION_BOOT_COMPLETED receiver.
 * <p>
 * Start video panel after reboot.
 */
public class AutoLoadReceiver extends BroadcastReceiver {
    public AutoLoadReceiver() {
    }

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, StartActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}