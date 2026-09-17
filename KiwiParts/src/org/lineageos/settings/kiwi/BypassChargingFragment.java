// SPDX-License-Identifier: Apache-2.0
package org.lineageos.settings.kiwi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

/**
 * Two toggles, as on 19.1:
 *
 *   Bypass now  - hold the battery at its current level right away
 *   Auto bypass - charge to a limit, then hold there
 *
 * They are mutually exclusive, and the interlock is refreshed from three
 * places (onCreatePreferences, onResume and the preference listener) so the
 * greyed-out state is correct on entry as well as after a change.
 */
public class BypassChargingFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SwitchPreference mNow;
    private SwitchPreference mAuto;
    private SeekBarPreference mLevel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.bypass_prefs, rootKey);

        mNow = findPreference(Prefs.KEY_BYPASS_NOW);
        mAuto = findPreference(Prefs.KEY_AUTO);
        mLevel = findPreference(Prefs.KEY_LEVEL);

        if (!ChargerControl.isAvailable()) {
            // No charger node: show why rather than offering dead switches.
            mNow.setEnabled(false);
            mAuto.setEnabled(false);
            mLevel.setEnabled(false);
            mNow.setSummary(R.string.err_unavailable);
            return;
        }

        updateInterlock();
    }

    @Override
    public void onResume() {
        super.onResume();
        Prefs.get(getContext()).registerOnSharedPreferenceChangeListener(this);
        updateInterlock();
    }

    @Override
    public void onPause() {
        Prefs.get(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (getContext() == null) {
            return;
        }
        boolean ok = true;

        if (Prefs.KEY_BYPASS_NOW.equals(key)) {
            boolean on = Prefs.bypassNow(getContext());
            ok = ChargerControl.setCharging(!on);
            if (!ok) {
                // Roll the switch back rather than lie about the state.
                sp.edit().putBoolean(Prefs.KEY_BYPASS_NOW, false).apply();
                ChargerControl.restore();
            }
        } else if (Prefs.KEY_AUTO.equals(key)) {
            if (!Prefs.auto(getContext())) {
                ChargerControl.restore();
            }
            AutoBypassService.sync(getContext());
        }

        if (!ok) {
            Toast.makeText(getContext(), R.string.err_write, Toast.LENGTH_SHORT).show();
        }
        updateInterlock();
    }

    /** Each toggle greys the other out, and the limit follows Auto. */
    private void updateInterlock() {
        if (getContext() == null || mNow == null) {
            return;
        }
        boolean now = Prefs.bypassNow(getContext());
        boolean auto = Prefs.auto(getContext());

        mNow.setEnabled(!auto);
        mAuto.setEnabled(!now);
        mLevel.setEnabled(auto && !now);

        Preference footer = findPreference("footer");
        if (footer != null) {
            footer.setVisible(true);
        }
        if (auto) {
            mLevel.setSummary(getString(R.string.threshold_summary,
                    Prefs.level(getContext())));
        }
    }
}
