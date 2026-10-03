package com.prijilevschi.library.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Reads a book's barcode with Google's code scanner (own camera UI, no CAMERA permission needed). */
object BarcodeScanner {

    /**
     * Opens the scanner and returns the raw barcode value, or null if the user cancelled.
     * Throws when Google Play services cannot provide the scanner.
     */
    suspend fun scan(context: Context): String? = suspendCancellableCoroutine { continuation ->
        val options = GmsBarcodeScannerOptions.Builder()
            // Books carry their ISBN as an EAN-13 (978/979...) barcode
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.rawValue) }
            .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    }
}
