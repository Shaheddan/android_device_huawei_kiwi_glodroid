// SPDX-License-Identifier: Apache-2.0
package org.lineageos.settings.kiwi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

/**
 * Auto bypass: charge up to the chosen limit, then hold there.
 *
 * Runs only while "Auto bypass" is on. Charging is re-enabled once the level
 * falls HYSTERESIS percent below the limit, so the charger is not toggled on
 * and off repeatedly at the boundary.
 */
public class AutoBypassService extends Service {
    private static final String TAG = "KiwiBypass";

    private final BroadcastReceiver mBattery = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            evaluate(intent);
        }
    };

    private boolean mRegistered;

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(mBattery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mRegistered = true;
        Log.i(TAG, "auto bypass watching");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If auto was switched off while we were dead, stop and restore.
        if (!Prefs.auto(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            unregisterReceiver(mBattery);
            mRegistered = false;
        }
        // Never leave the battery held because this service went away.
        if (!Prefs.bypassNow(this)) {
            ChargerControl.restore();
        }
        Log.i(TAG, "auto bypass stopped");
        super.onDestroy();
    }

    private void evaluate(Intent batteryStatus) {
        if (!Prefs.auto(this)) {
            stopSelf();
            return;
        }
        // Manual bypass wins; do not fight it.
        if (Prefs.bypassNow(this)) {
            return;
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return;
        }
        int pct = Math.round(level * 100f / scale);
        int limit = Prefs.level(this);

        if (pct >= limit) {
            if (ChargerControl.isCharging()) {
                Log.i(TAG, "limit " + limit + "% reached at " + pct + "%, holding");
                ChargerControl.setCharging(false);
            }
        } else if (pct <= limit - Prefs.HYSTERESIS) {
            if (!ChargerControl.isCharging()) {
                Log.i(TAG, "fell to " + pct + "%, resuming charge");
                ChargerControl.setCharging(true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void sync(Context c) {
        Intent i = new Intent(c, AutoBypassService.class);
        if (Prefs.auto(c)) {
            c.startService(i);
        } else {
            c.stopService(i);
        }
    }
}
