package com.webtoapp.ui.webview

import android.content.Context
import android.webkit.WebView
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.util.DownloadHelper
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

internal fun handleWebViewDownload(
    context: Context,
    scope: CoroutineScope,
    url: String,
    userAgent: String,
    contentDisposition: String,
    mimeType: String,
    contentLength: Long,
    webViewProvider: () -> WebView?
) {
    DownloadHelper.handleDownload(
        context = context,
        url = url,
        userAgent = userAgent,
        contentDisposition = contentDisposition,
        mimeType = mimeType,
        contentLength = contentLength,
        method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
        scope = scope,
        onBlobDownload = createBlobDownloadCallback(webViewProvider)
    )
}

private fun createBlobDownloadCallback(
    webViewProvider: () -> WebView?
): (String, String) -> Unit = { blobUrl, filename ->
    val safeBlobUrl = JSONObject.quote(blobUrl)
    val safeFilename = JSONObject.quote(filename)
    webViewProvider()?.evaluateJavascript(
        buildBlobDownloadJavascript(safeBlobUrl, safeFilename),
        null
    )
}

private fun buildBlobDownloadJavascript(
    safeBlobUrl: String,
    safeFilename: String
): String {
    return """
        (function() {
            try {
                const blobUrl = $safeBlobUrl;
                const filename = $safeFilename;
                const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024;
                const CHUNK_SIZE = 1024 * 1024;
                
                function uint8ToBase64(u8) {
                    const S = 8192; const p = [];
                    for (let i = 0; i < u8.length; i += S) p.push(String.fromCharCode.apply(null, u8.subarray(i, i + S)));
                    return btoa(p.join(''));
                }
                
                function processChunked(blob, fname) {
                    const mimeType = blob.type || 'application/octet-stream';
                    if (!window.AndroidDownload || !window.AndroidDownload.startChunkedDownload) {
                        processSmall(blob, fname); return;
                    }
                    const did = window.AndroidDownload.startChunkedDownload(fname, mimeType, blob.size);
                    let off = 0, ci = 0; const tc = Math.ceil(blob.size / CHUNK_SIZE);
                    function next() {
                        if (off >= blob.size) { window.AndroidDownload.finishChunkedDownload(did); return; }
                        blob.slice(off, off + CHUNK_SIZE).arrayBuffer().then(function(ab) {
                            window.AndroidDownload.appendChunk(did, uint8ToBase64(new Uint8Array(ab)), ci, tc);
                            off += CHUNK_SIZE; ci++;
                            setTimeout(next, 0);
                        });
                    }
                    next();
                }
                
                function processSmall(blob, fname) {
                    const reader = new FileReader();
                    reader.onloadend = function() {
                        const base64Data = reader.result.split(',')[1];
                        const mimeType = blob.type || 'application/octet-stream';
                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64Data, fname, mimeType);
                        }
                    };
                    reader.readAsDataURL(blob);
                }
                
                if (blobUrl.startsWith('data:')) {
                    const parts = blobUrl.split(',');
                    const meta = parts[0];
                    const base64Data = parts[1];
                    const mimeMatch = meta.match(/data:([^;]+)/);
                    const mimeType = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
                    if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                        window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                    }
                } else if (blobUrl.startsWith('blob:')) {
                    fetch(blobUrl)
                        .then(function(r) { return r.blob(); })
                        .then(function(blob) {
                            if (blob.size > LARGE_FILE_THRESHOLD) {
                                processChunked(blob, filename);
                            } else {
                                processSmall(blob, filename);
                            }
                        })
                        .catch(function(err) {
                            console.error('[DownloadHelper] Blob fetch failed:', err);
                            if (window.AndroidDownload && window.AndroidDownload.showToast) {
                                window.AndroidDownload.showToast('${AppStringsProvider.current().downloadFailedWithReason}' + err.message);
                            }
                        });
                }
            } catch(e) {
                console.error('[DownloadHelper] Error:', e);
            }
        })();
    """.trimIndent()
}
