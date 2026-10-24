package com.example.ukilbhai.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class AirplaneModeReceiver extends BroadcastReceiver {

    public interface AirplaneModeListener {
        void onAirplaneModeChanged(boolean isAirplaneModeOn);
    }

    private final AirplaneModeListener listener;

    public AirplaneModeReceiver(AirplaneModeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
            boolean isAirplaneModeOn = isAirplaneModeOn(context);
            if (listener != null) {
                listener.onAirplaneModeChanged(isAirplaneModeOn);
            }
        }
    }

    public static boolean isAirplaneModeOn(Context context) {
        if (context == null) return false;
        return Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0;
    }
}
