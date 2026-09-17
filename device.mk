# SPDX-License-Identifier: Apache-2.0
#
# Copyright (C) 2023 The GloDroid project

$(call inherit-product, glodroid/configuration/common/device-common.mk)

# Framework overlay: enables the auto-brightness toggle, which AOSP
# hides by default even though the APDS9930 ALS reports fine.
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# The gadget only binds when UsbDeviceManager writes the UDC name into
# /config/usb_gadget/g1/UDC, and it does that based on persist.sys.usb.config.
# That value ends up 'none' on this device, so nothing enumerates even though
# sys.usb.controller, ffs.ready and the linked function are all correct.
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.usb.config=adb

GD_NO_DEFAULT_FASTBOOTD := true
GD_NO_DEFAULT_BOOTCTL   := true
GD_NO_DEFAULT_CAMERA    := true
GD_NO_DEFAULT_MODEM     := true
GD_NO_DEFAULT_APPS      := true

# Files from linux-firmware
PRODUCT_COPY_FILES += \
    glodroid/linux-firmware/qcom/a420_pfp.fw:$(TARGET_COPY_OUT_VENDOR)/firmware/a420_pfp.fw \
    glodroid/linux-firmware/qcom/a420_pm4.fw:$(TARGET_COPY_OUT_VENDOR)/firmware/a420_pm4.fw \
    glodroid/linux-firmware/qcom/venus-1.8/venus.mbn:$(TARGET_COPY_OUT_VENDOR)/firmware/qcom/venus-1.8/venus.mbn \

# Symlink firmware files from device partitions
PRODUCT_PACKAGES += kiwi_firmware

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/etc/sensors.kiwi.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/sensors.kiwi.rc \
    $(LOCAL_PATH)/etc/init.kiwi.usb.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.kiwi.usb.rc \
    $(LOCAL_PATH)/etc/uevent.device.rc:$(TARGET_COPY_OUT_VENDOR)/etc/uevent.device.rc \
    glodroid/configuration/common/no_suspend.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/no_suspend.rc \

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/etc/audio.kiwi.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio.kiwi.xml \

# Lights HAL
PRODUCT_PACKAGES += \
    android.hardware.lights-service \

# Sensors HAL
PRODUCT_PACKAGES += \
    sensors.iio \
    android.hardware.sensors@1.0-impl:64 \
    android.hardware.sensors@1.0-service \

# Checked by android.opengl.cts.OpenGlEsVersionTest#testOpenGlEsVersion.
# Required to run correct set of dEQP tests.
# 131072 == 0x00020000 == GLES v2.0
PRODUCT_VENDOR_PROPERTIES += \
    ro.opengles.version=131072

# kiwi: fingerprint (route A). hw_get_module() builds the filename from
# properties -- ro.hardware.<id> first -- and the blob is
# fingerprint.msm8916.so while ro.hardware reads "kiwi". Without this the
# HAL service dies at startup with ENOENT.
PRODUCT_VENDOR_PROPERTIES += \
    ro.hardware.fingerprint=msm8916

# gatekeeper.msm8916.so has the same naming problem, but kiwi still uses
# the software gatekeeper (android.hardware.gatekeeper@1.0-service.software),
# so switching is a separate change:
#PRODUCT_VENDOR_PROPERTIES += \
#    ro.hardware.gatekeeper=msm8916

# kiwi: SystemUISlowGpu RRO removed. GloDroid's RRO zeroes SystemUI corner and
# blur radii for slow GPUs; that is what squared the QS tiles, notifications and
# dialogs. Undo with: kiwi_slowgpu_rro_patch.py --revert

# Bluetooth
PRODUCT_VENDOR_PROPERTIES += \
    bluetooth.device.class_of_device=90,2,12 \
    bluetooth.profile.asha.central.enabled=true \
    bluetooth.profile.a2dp.source.enabled=true \
    bluetooth.profile.avrcp.target.enabled=true \
    bluetooth.profile.bap.broadcast.assist.enabled=true \
    bluetooth.profile.bap.unicast.client.enabled=true \
    bluetooth.profile.bas.client.enabled=true \
    bluetooth.profile.csip.set_coordinator.enabled=true \
    bluetooth.profile.gatt.enabled=true \
    bluetooth.profile.hap.client.enabled=true \
    bluetooth.profile.hfp.ag.enabled=true \
    bluetooth.profile.hid.device.enabled=true \
    bluetooth.profile.hid.host.enabled=true \
    bluetooth.profile.map.server.enabled=true \
    bluetooth.profile.mcp.server.enabled=true \
    bluetooth.profile.opp.enabled=true \
    bluetooth.profile.pan.nap.enabled=true \
    bluetooth.profile.pan.panu.enabled=true \
    bluetooth.profile.pbap.server.enabled=true \
    bluetooth.profile.sap.server.enabled=true \
    bluetooth.profile.ccp.server.enabled=true \
    bluetooth.profile.vcp.controller.enabled=true \
    persist.bluetooth.a2dp_aac.vbr_supported=true \

# Bypass charging (KiwiParts) --------------------------------------------
PRODUCT_PACKAGES += KiwiParts

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/etc/init.kiwi.bypass.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.kiwi.bypass.rc \

# kiwi: fingerprint (route A) --------------------------------------------
# The 19.1 TrustZone stack: qseecomd + its five listener libraries, teecd
# (the GlobalPlatform relay lib_fpc_tac_shared.so talks to), and the FPC
# fingerprint HAL. The blobs are prebuilt Soong modules (see Android.bp);
# ELF files are not allowed in PRODUCT_COPY_FILES. libshim_kiwi_fp supplies
# the Huawei-only liblog symbols the HAL imports; libstdc++ is bionic's own,
# which five of these blobs still list. Needs kiwi_qseecom.ko
# plus the qcom_scm export and RPMB compat kernel patches.
PRODUCT_PACKAGES += \
    qseecomd \
    teecd \
    libQSEEComAPI \
    libteec \
    libdrmfs \
    libdrmtime \
    librpmb \
    libssd \
    libdiag \
    libtime_genoff \
    lib_fpc_tac_shared \
    fingerprint.msm8916 \
    gatekeeper.msm8916 \
    libshim_kiwi_fp \
    libstdc++ \

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/etc/init.kiwi.fingerprint.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.kiwi.fingerprint.rc \

# Without this the Settings app shows no Fingerprint entry at all.
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.fingerprint.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.fingerprint.xml \

# The hwbinder service the framework actually talks to. 19.1's sources,
# unchanged; it opens the HAL with hw_get_module("fingerprint").
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint@1.0-service.kiwi
