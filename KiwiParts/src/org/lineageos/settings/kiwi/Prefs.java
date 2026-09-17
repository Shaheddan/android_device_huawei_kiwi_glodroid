// SPDX-License-Identifier: Apache-2.0
package org.lineageos.settings.kiwi;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

final class Prefs {
    static final String KEY_BYPASS_NOW = "bypass_now";
    static final String KEY_AUTO = "auto_bypass";
    static final String KEY_LEVEL = "auto_bypass_level";

    /** Percent below the limit at which charging is allowed to resume. */
    static final int HYSTERESIS = 2;

    static final int DEFAULT_LEVEL = 80;

    private Prefs() {
    }

    static SharedPreferences get(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    static boolean bypassNow(Context c) {
        return get(c).getBoolean(KEY_BYPASS_NOW, false);
    }

    static boolean auto(Context c) {
        return get(c).getBoolean(KEY_AUTO, false);
    }

    static int level(Context c) {
        return get(c).getInt(KEY_LEVEL, DEFAULT_LEVEL);
    }
}
