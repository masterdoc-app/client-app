@file:OptIn(ExperimentalWasmJsInterop::class)

package pro.masterdoc.client.platform

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual suspend fun openAuthenticatedDocument(
    url: String,
    bearerToken: String,
    filename: String,
    mimeType: String,
) {
    suspendCoroutine { cont ->
        openAuthenticatedDocumentJs(
            url = url,
            bearerToken = bearerToken,
            filename = filename,
            mimeType = mimeType,
            onSuccess = { cont.resume(Unit) },
            onFailure = { message -> cont.resumeWithException(Exception(message)) },
        )
    }
}

@JsFun(
    """
    (url, bearerToken, filename, mimeType, onSuccess, onFailure) => {
      (async () => {
        try {
          const response = await fetch(url, {
            method: 'GET',
            headers: { 'Authorization': 'Bearer ' + bearerToken }
          });
          if (!response.ok) {
            onFailure('HTTP ' + response.status);
            return;
          }
          const buffer = await response.arrayBuffer();
          const bytes = new Uint8Array(buffer);
          const head = String.fromCharCode.apply(null, Array.from(bytes.slice(0, Math.min(8, bytes.length))));
          const tailStart = Math.max(0, bytes.length - 2048);
          let tail = '';
          for (let i = tailStart; i < bytes.length; i++) tail += String.fromCharCode(bytes[i]);
          const validPdf = head.startsWith('%PDF-') && tail.indexOf('%%EOF') !== -1;

          let objectUrl;
          let openName = filename;
          if (validPdf) {
            const typed = new Blob([bytes], { type: 'application/pdf' });
            objectUrl = URL.createObjectURL(typed);
          } else {
            // Legacy from-text fixtures: open as UTF-8 text so the user sees the manual.
            let text = new TextDecoder('utf-8').decode(bytes);
            if (text.startsWith('%PDF-')) {
              const nl = text.indexOf('\n');
              if (nl >= 0) text = text.slice(nl + 1);
            }
            const typed = new Blob([text], { type: 'text/plain;charset=utf-8' });
            objectUrl = URL.createObjectURL(typed);
            openName = filename.replace(/\.pdf$/i, '') + '.txt';
          }
          const opened = window.open(objectUrl, '_blank');
          if (!opened) {
            const a = document.createElement('a');
            a.href = objectUrl;
            a.target = '_blank';
            a.rel = 'noopener';
            a.download = openName;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
          }
          setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
          onSuccess();
        } catch (e) {
          onFailure(String(e));
        }
      })();
    }
    """,
)
private external fun openAuthenticatedDocumentJs(
    url: String,
    bearerToken: String,
    filename: String,
    mimeType: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
)
