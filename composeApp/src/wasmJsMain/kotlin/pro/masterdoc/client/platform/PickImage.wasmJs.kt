@file:OptIn(ExperimentalWasmJsInterop::class)

package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import pro.masterdoc.client.auth.Base64Std

@Composable
actual fun rememberImagePickerLaunchers(
    onResult: (PickedImage?) -> Unit,
    onError: (String) -> Unit,
): ImagePickerLaunchers {
    val onResultState = rememberUpdatedState(onResult)
    val onErrorState = rememberUpdatedState(onError)
    val gallery = remember {
        {
            pickImageGalleryJs(
                onPicked = { name, type, base64 ->
                    onResultState.value(
                        PickedImage(Base64Std.decode(base64), name, type.ifBlank { "image/jpeg" }),
                    )
                },
                onCancel = { onResultState.value(null) },
            )
        }
    }
    val camera = remember {
        {
            captureImageCameraJs(
                onPicked = { name, type, base64 ->
                    onResultState.value(
                        PickedImage(Base64Std.decode(base64), name, type.ifBlank { "image/jpeg" }),
                    )
                },
                onError = {
                    onErrorState.value(if (it == "cancelled") "Камера недоступна" else "Камера недоступна")
                    onResultState.value(null)
                },
            )
        }
    }
    return remember(gallery, camera) {
        ImagePickerLaunchers(openGallery = gallery, openCamera = camera)
    }
}

@JsFun(
    """
    (onPicked, onCancel) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.style.display = 'none';
      let settled = false;
      const done = (fn) => {
        if (settled) return;
        settled = true;
        input.remove();
        fn();
      };
      input.onchange = async () => {
        const file = input.files && input.files[0];
        if (!file) { done(onCancel); return; }
        try {
          const bytes = new Uint8Array(await file.arrayBuffer());
          let binary = '';
          for (let i = 0; i < bytes.length; i += 0x8000) {
            binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
          }
          done(() => onPicked(file.name || 'photo.jpg', file.type || 'image/jpeg', btoa(binary)));
        } catch (e) { done(onCancel); }
      };
      input.addEventListener('cancel', () => done(onCancel));
      document.body.appendChild(input);
      input.click();
    }
    """,
)
private external fun pickImageGalleryJs(
    onPicked: (String, String, String) -> Unit,
    onCancel: () -> Unit,
)

@JsFun(
    """
    (onPicked, onError) => {
      if (!window.isSecureContext || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        onError('unavailable'); return;
      }
      const video = document.createElement('video');
      const button = document.createElement('button');
      const back = document.createElement('button');
      const overlay = document.createElement('div');
      overlay.style.cssText = 'position:fixed;inset:0;z-index:2147483647;background:#000;display:flex;align-items:center;justify-content:center';
      video.autoplay = true; video.muted = true; video.playsInline = true;
      video.style.cssText = 'width:100%;height:100%;object-fit:cover';
      button.textContent = 'Сделать снимок';
      button.style.cssText = 'position:absolute;bottom:32px;z-index:2;padding:16px 24px';
      back.textContent = 'Назад';
      back.style.cssText = 'position:absolute;top:24px;left:24px;z-index:2;padding:12px';
      overlay.append(video, button, back);
      document.body.appendChild(overlay);
      let stream = null, finished = false;
      const cleanup = () => {
        if (finished) return;
        finished = true;
        if (stream) stream.getTracks().forEach(t => t.stop());
        overlay.remove();
      };
      back.onclick = () => { cleanup(); onError('cancelled'); };
      navigator.mediaDevices.getUserMedia({video: {facingMode: {ideal: 'environment'}}, audio: false})
        .then(s => { stream = s; video.srcObject = s; return video.play(); })
        .catch(e => { cleanup(); onError(e && e.name ? e.name : 'unavailable'); });
      button.onclick = () => {
        if (!video.videoWidth || finished) return;
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth; canvas.height = video.videoHeight;
        canvas.getContext('2d').drawImage(video, 0, 0);
        canvas.toBlob(blob => {
          if (!blob) { cleanup(); onError('unavailable'); return; }
          const reader = new FileReader();
          reader.onload = () => {
            const base64 = String(reader.result).split(',')[1];
            cleanup(); onPicked('photo.jpg', 'image/jpeg', base64);
          };
          reader.readAsDataURL(blob);
        }, 'image/jpeg', 0.92);
      };
    }
    """,
)
private external fun captureImageCameraJs(
    onPicked: (String, String, String) -> Unit,
    onError: (String) -> Unit,
)
