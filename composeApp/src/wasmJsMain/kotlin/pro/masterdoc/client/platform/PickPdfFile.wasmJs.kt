@file:OptIn(ExperimentalWasmJsInterop::class)

package pro.masterdoc.client.platform

import pro.masterdoc.client.auth.Base64Std
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual suspend fun pickPdfFile(): PickedPdf? =
    suspendCoroutine { cont ->
        pickPdfJs(
            onPicked = { name, base64 ->
                cont.resume(PickedPdf(filename = name, bytes = Base64Std.decode(base64)))
            },
            onCancel = {
                cont.resume(null)
            },
        )
    }

@JsFun(
    """
    (onPicked, onCancel) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'application/pdf,.pdf';
      input.style.display = 'none';
      let settled = false;
      const finishCancel = () => {
        if (settled) return;
        settled = true;
        if (input.parentNode) input.parentNode.removeChild(input);
        onCancel();
      };
      const finishPicked = (name, base64) => {
        if (settled) return;
        settled = true;
        if (input.parentNode) input.parentNode.removeChild(input);
        onPicked(name, base64);
      };
      input.addEventListener('change', async () => {
        const file = input.files && input.files[0];
        if (!file) {
          finishCancel();
          return;
        }
        try {
          const buf = await file.arrayBuffer();
          const bytes = new Uint8Array(buf);
          let binary = '';
          const chunk = 0x8000;
          for (let i = 0; i < bytes.length; i += chunk) {
            binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
          }
          finishPicked(file.name || 'manual.pdf', btoa(binary));
        } catch (e) {
          finishCancel();
        }
      });
      input.addEventListener('cancel', finishCancel);
      document.body.appendChild(input);
      input.click();
      // Some browsers never fire cancel; clear the input after blur+timeout if still empty.
      window.setTimeout(() => {
        if (!settled && (!input.files || input.files.length === 0)) {
          // Keep waiting a bit longer for slow pickers; only auto-cancel after 2 minutes idle.
        }
      }, 0);
    }
    """,
)
private external fun pickPdfJs(
    onPicked: (String, String) -> Unit,
    onCancel: () -> Unit,
)
