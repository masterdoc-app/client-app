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
          const blob = await response.blob();
          const typed = blob.type ? blob : new Blob([blob], { type: mimeType });
          const objectUrl = URL.createObjectURL(typed);
          const opened = window.open(objectUrl, '_blank');
          if (!opened) {
            const a = document.createElement('a');
            a.href = objectUrl;
            a.target = '_blank';
            a.rel = 'noopener';
            a.download = filename;
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
