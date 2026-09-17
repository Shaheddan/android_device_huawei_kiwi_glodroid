/*
 * libshim_kiwi_fp.c -- kiwi route A (fingerprint) shims.
 *
 * Huawei-only liblog symbols that fingerprint.msm8916.so imports:
 *      __android_janklog_print     (Huawei "jank"/UI-stall logging)
 *      __android_logPower_print    (Huawei power logging)
 *    Neither ever existed in AOSP, so the blob cannot load without them.
 *    Two more are included because other Huawei blobs in the same family
 *    import them; they cost nothing and save a later surprise:
 *      __android_log_exception_write
 *      isLogEnabled
 *
 *    These signatures are NOT guesses: they are the ones LineageOS 19.1 shipped
 *    for this exact device (device/huawei/kiwi/libshims/hw_log.c), which is the
 *    build where kiwi's fingerprint worked. All four take no arguments and do
 *    nothing; on arm64 a callee that ignores its arguments is safe whatever the
 *    caller passes, and dropping a log message changes nothing functionally.
 *
 * Not handled here: libstdc++.so, which five of the blobs still list
 * (libQSEEComAPI, libdrmfs, librpmb, libdrmtime, libssd). Bionic still ships a
 * real libstdc++ and it is vendor_available -- it just was not being built for
 * vendor, so the linker could not find it:
 *
 *   CANNOT LINK EXECUTABLE "/vendor/bin/qseecomd":
 *   library "libstdc++.so" not found: needed by /vendor/lib64/libQSEEComAPI.so
 *
 * device.mk pulls it in via PRODUCT_PACKAGES, so the blobs get the genuine
 * library rather than a stub.
 */

/* --- Huawei liblog stubs (libshim_kiwi_fp) ------------------------------- */

void __android_logPower_print(void)
{
}

int __android_janklog_print(void)
{
	return 0;
}

int __android_log_exception_write(void)
{
	return 0;
}

int isLogEnabled(void)
{
	return 0;
}
