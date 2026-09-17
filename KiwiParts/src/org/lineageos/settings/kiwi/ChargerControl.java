// SPDX-License-Identifier: Apache-2.0
package org.lineageos.settings.kiwi;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Charge enable/disable on the TI BQ24296M.
 *
 * WHY THIS NODE: f_chg_config is REG01 CHG_CONFIG, exposed by mainline's
 * bq24190_charger driver. Writing 0 stops charging while leaving the input
 * rail up, so the phone runs from the charger and the battery is left alone.
 * That is the same register operation the downstream 3.10 driver performed
 * through its custom "factory_diag" node on LineageOS 19.1 - verified there
 * against the chargelog as REG01 CHG_CONFIG 01 -> 00 with EN_HIZ staying 0.
 *
 * DO NOT use charging_enabled / EN_HIZ for this. On this hardware EN_HIZ cuts
 * the input entirely, so the phone runs off the battery and DRAINS while
 * plugged in - the opposite of bypass. f_en_hiz is only read here, never
 * written, so that state can be reported if something else sets it.
 */
final class ChargerControl {
    private static final String TAG = "KiwiBypass";

    private static final String BASE = "/sys/class/power_supply/bq24190-charger/";
    static final String CHG_CONFIG = BASE + "f_chg_config";
    static final String EN_HIZ = BASE + "f_en_hiz";

    static final int CHARGE_ENABLE = 1;
    static final int CHARGE_DISABLE = 0;

    private ChargerControl() {
    }

    static boolean isAvailable() {
        return read(CHG_CONFIG) != null;
    }

    /** @return true when the charger is currently allowed to charge. */
    static boolean isCharging() {
        String v = read(CHG_CONFIG);
        return v != null && !v.equals("0");
    }

    /** @return true if the input has been cut (should never be us). */
    static boolean isInputCut() {
        String v = read(EN_HIZ);
        return v != null && !v.equals("0");
    }

    static boolean setCharging(boolean enable) {
        return write(CHG_CONFIG, enable ? CHARGE_ENABLE : CHARGE_DISABLE);
    }

    /**
     * Best-effort restore. Called from every failure and teardown path so a
     * crash or a disabled feature can never leave the battery held.
     */
    static void restore() {
        if (!write(CHG_CONFIG, CHARGE_ENABLE)) {
            Log.e(TAG, "could not restore charging");
        }
    }

    private static String read(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line = r.readLine();
            return line == null ? null : line.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean write(String path, int value) {
        try (FileWriter w = new FileWriter(path)) {
            w.write(Integer.toString(value));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "write " + value + " -> " + path + " failed: " + e);
            return false;
        }
    }
}
