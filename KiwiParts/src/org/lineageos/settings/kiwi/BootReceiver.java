// SPDX-License-Identifier: Apache-2.0
package org.lineageos.settings.kiwi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Re-applies the saved state after a reboot. The charger comes up charging by
 * default, so anything other than "off" has to be re-asserted here.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (!ChargerControl.isAvailable()) {
            return;
        }
        if (Prefs.bypassNow(context)) {
            ChargerControl.setCharging(false);
        } else {
            ChargerControl.setCharging(true);
        }
        AutoBypassService.sync(context);
    }
}
