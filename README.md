# Huawei Honor 5X (kiwi) — GloDroid / Android 13 device tree

Device tree for the Honor 5X (`kiwi`, MSM8939) running **Android 13 via
GloDroid**, on the **mainline 6.10 kernel** (`msm8916-mainline`), not the
downstream 3.10 vendor kernel.

This is a working but incomplete port. It boots, it is usable, and several
subsystems work well. Others do not. Everything below is what was actually
measured on the device, not what was hoped for.

## Related repositories

| Repo | Contents |
|---|---|
| [android_kernel_huawei_kiwi_mainlinenew](https://github.com/Shaheddan/android_kernel_huawei_kiwi_mainlinenew/tree/kiwi-glodroid-6.10) | the 6.10 kernel, with this port's patches as commits on top of upstream |
| `android_vendor_huawei_kiwi_glodroid` | the proprietary blobs this tree references |

Built against [GloDroidCommunity/qcom-msm8916-series](https://github.com/GloDroidCommunity/qcom-msm8916-series).

## What works

- **Boot**, to a usable Android 13 system
- **Display**, with the AUO OTM1901A panel driver, and **brightness**
- **Wifi**, including WPA3
- **Battery**, including bypass charging via the `KiwiParts` app
- **USB / adb**
- **GPS**
- **Quick-settings tiles and Material You** — GloDroid's `SystemUISlowGpu`
  RRO squares off the tiles for performance; removing it restores them

## What does not work

- **SIM / modem** — not started
- **Camera** — not started
- **Fingerprint** — see below; everything except the final session works
- **Display stability** — after roughly 1–2 minutes, or after screen sleep,
  the panel can scramble. Usable, but **parked, not solved**

## Fingerprint (route A) — how far it got

The approach was to run the **LineageOS 19.1 TrustZone stack unchanged** on
the mainline kernel, rather than reimplement fingerprint capture and matching
(there is no open-source matcher for this sensor, and templates would then
live outside TrustZone).

Working, and coming up automatically at boot:

- `kiwi_qseecom` — `/dev/qseecom` and a legacy `/dev/ion` for the 19.1 blobs:
  TrustZone app load/query/unload, commands, and all five listeners
- The downstream Qualcomm RPMB ioctl, without which `qseecomd` exits and
  **none** of its listeners start
- `qseecomd` with its five listeners registered (RPMB, SSD, time, file system,
  GP file system), `cmnlib` loaded, keymaster started
- `teecd` running
- `kiwi_fpc1020` — the sensor identifies itself over SPI as **FPC1021B**
  (hardware id `0x021b`), and exposes `/dev/fpc1020` plus the sysfs node the
  vendor HAL reads
- `fingerprint.msm8916.so` loads and reaches `TEEC_OpenSession`

Where it stops: **`teecd`'s GlobalPlatform session open returns
`TEEC_ERROR_ITEM_NOT_FOUND` (0xffff000e)**, so the HAL service never
registers and Settings shows no Fingerprint entry.

What was measured about that failure:

- The TrustZone app receives the session request and answers successfully at
  the QSEECOM level (`result=0x0`), setting the session id in the buffer
- It leaves the `returns` field holding the client's default. On a working
  19.1 system that field comes back as `04 00 00 00 / origin 0`
- Ruled out: the UUID (it is the app name padded to 16 bytes), request/reply
  buffer layout (`teecd` passes one buffer for both), a stale app-id cache,
  the `SEND_CMD` argument layout and the user-to-physical address conversion
  — all verified against the 19.1 driver's source
- `teecd` is statically linked, writes no logs (its liblog targets the
  pre-KitKat `/dev/log/*` interface, which no longer exists), and makes no
  `write()` calls at all

## Notes for anyone continuing this

- `/` is mounted **read-only**, so `/firmware` (which the blobs have compiled
  in) cannot be created by an init `symlink` command at any stage. It is built
  into the system image in `firmware/Android.mk`.
- ELF prebuilts are **not** allowed in `PRODUCT_COPY_FILES` on Android 13;
  they must be `cc_prebuilt_*` modules. See `Android.bp`.
- `hw_get_module()` builds the HAL filename from properties, so
  `ro.hardware.fingerprint=msm8916` is required or the service dies with
  `ENOENT`.
- Soong's `shared_libs` on a prebuilt does **not** add a `DT_NEEDED` entry.
  `fingerprint.msm8916.so` was patched with `patchelf --add-needed
  libshim_kiwi_fp.so` so it can find the two Huawei-only liblog symbols.
- `msm8939.dtsi` parks the SPI chip-select pin as a plain GPIO, so a board
  **must** declare `cs-gpios` or nothing is ever selected on the bus.
