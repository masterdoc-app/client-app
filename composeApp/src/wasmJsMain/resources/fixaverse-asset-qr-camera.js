(function () {
    var activeSession = null;
    var scanCameraCallbacks = null;

    function ensureCameraDom() {
        if (document.getElementById("fixaverse-camera-overlay")) return;
        var overlay = document.createElement("div");
        overlay.id = "fixaverse-camera-overlay";
        overlay.innerHTML =
            '<video id="fixaverse-camera-preview" autoplay muted playsinline webkit-playsinline></video>' +
            '<div id="fixaverse-camera-status">Подключение камеры…</div>' +
            '<div id="fixaverse-camera-hint">Наведите на QR оборудования</div>' +
            '<button id="fixaverse-camera-back" type="button" aria-label="Назад">‹</button>' +
            '<div id="fixaverse-camera-controls">' +
            '<button id="fixaverse-camera-capture" type="button" aria-label="Сделать снимок" disabled>' +
            '<span class="fixaverse-shutter-inner"></span>' +
            "</button>" +
            "</div>";
        document.body.appendChild(overlay);
    }

    function isMobileCaptureDevice() {
        var ua = navigator.userAgent || "";
        return /Android/i.test(ua) || /iPhone|iPad|iPod/i.test(ua);
    }

    function formatCameraError(error) {
        if (!error) return "camera unavailable";
        if (typeof error === "string") return error;
        return error.name ? error.name + (error.message ? ": " + error.message : "") : String(error);
    }

    function requestCameraStream() {
        var attempts = isMobileCaptureDevice()
            ? [
                  { video: { facingMode: { ideal: "environment" } }, audio: false },
                  { video: { facingMode: "environment" }, audio: false },
                  { video: { facingMode: "user" }, audio: false },
                  { video: true, audio: false },
              ]
            : [
                  { video: { width: { ideal: 1920 }, height: { ideal: 1080 } }, audio: false },
                  { video: { facingMode: "user" }, audio: false },
                  { video: true, audio: false },
              ];
        var lastError = null;
        function tryAt(index) {
            if (index >= attempts.length) {
                return Promise.reject(lastError || new Error("camera unavailable"));
            }
            return navigator.mediaDevices.getUserMedia(attempts[index]).catch(function (error) {
                lastError = error;
                return tryAt(index + 1);
            });
        }
        return tryAt(0);
    }

    function waitForVideoFrame(video) {
        return new Promise(function (resolve, reject) {
            var settled = false;
            function finish(ok, err) {
                if (settled) return;
                settled = true;
                video.removeEventListener("loadeddata", onLoaded);
                video.removeEventListener("playing", onLoaded);
                clearTimeout(timer);
                if (ok) resolve();
                else reject(err || new Error("camera preview timeout"));
            }
            function onLoaded() {
                if (video.videoWidth > 0 && video.videoHeight > 0) finish(true);
            }
            var timer = setTimeout(function () {
                finish(false, new Error("camera preview timeout"));
            }, 12000);
            video.addEventListener("loadeddata", onLoaded);
            video.addEventListener("playing", onLoaded);
            if (video.readyState >= 2 && video.videoWidth > 0) finish(true);
        });
    }

    function createBarcodeDetector() {
        if (typeof BarcodeDetector === "undefined") return null;
        try {
            return new BarcodeDetector({ formats: ["qr_code"] });
        } catch (e) {
            try {
                return new BarcodeDetector();
            } catch (e2) {
                return null;
            }
        }
    }

    function firstQrValue(barcodes) {
        if (!barcodes || !barcodes.length) return null;
        for (var i = 0; i < barcodes.length; i++) {
            var raw = barcodes[i].rawValue;
            if (raw) return String(raw);
        }
        return null;
    }

    /**
     * Live camera → continuous QR decode (BarcodeDetector) + shutter fallback.
     * onSuccess(rawQrString), onError(message).
     */
    window.fixaverseScanAssetQr = function (onSuccess, onError) {
        ensureCameraDom();
        if (activeSession) {
            activeSession.cleanup();
            activeSession = null;
        }

        if (!window.isSecureContext) {
            onError("insecure-context");
            return;
        }
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            onError("mediaDevices unavailable");
            return;
        }

        var overlay = document.getElementById("fixaverse-camera-overlay");
        var video = document.getElementById("fixaverse-camera-preview");
        var captureButton = document.getElementById("fixaverse-camera-capture");
        var backButton = document.getElementById("fixaverse-camera-back");
        var status = document.getElementById("fixaverse-camera-status");
        var stream = null;
        var ready = false;
        var detectTimer = null;
        var detector = createBarcodeDetector();
        var finished = false;

        document.body.classList.add("fixaverse-camera-open");
        document.body.appendChild(overlay);

        function cleanup() {
            ready = false;
            if (detectTimer) {
                clearInterval(detectTimer);
                detectTimer = null;
            }
            captureButton.disabled = true;
            captureButton.classList.remove("ready");
            if (stream) {
                stream.getTracks().forEach(function (track) {
                    track.stop();
                });
                stream = null;
            }
            video.srcObject = null;
            overlay.classList.remove("active");
            overlay.classList.remove("waiting");
            document.body.classList.remove("fixaverse-camera-open");
            captureButton.onclick = null;
            backButton.onclick = null;
        }

        function succeed(raw) {
            if (finished) return;
            finished = true;
            cleanup();
            activeSession = null;
            onSuccess(String(raw));
        }

        function fail(message) {
            if (finished) return;
            finished = true;
            cleanup();
            activeSession = null;
            onError(message);
        }

        function detectFromSource(source) {
            if (!detector || finished) return Promise.resolve(null);
            return detector.detect(source).then(firstQrValue).catch(function () {
                return null;
            });
        }

        function startDetectLoop() {
            if (!detector || detectTimer) return;
            detectTimer = setInterval(function () {
                if (!ready || finished || !video.videoWidth) return;
                detectFromSource(video).then(function (raw) {
                    if (raw) succeed(raw);
                });
            }, 350);
        }

        function captureAndDetect() {
            if (!ready || !stream || finished) {
                onError("camera not ready");
                return;
            }
            var width = video.videoWidth;
            var height = video.videoHeight;
            if (!width || !height) {
                onError("camera has no frame size");
                return;
            }
            var canvas = document.createElement("canvas");
            canvas.width = width;
            canvas.height = height;
            var ctx = canvas.getContext("2d");
            if (!ctx) {
                onError("canvas unavailable");
                return;
            }
            ctx.drawImage(video, 0, 0, width, height);
            detectFromSource(canvas).then(function (raw) {
                if (raw) {
                    succeed(raw);
                    return;
                }
                if (status) {
                    status.textContent = "QR не распознан — наведите ближе и снимите ещё раз";
                    status.style.display = "flex";
                    setTimeout(function () {
                        if (!finished && status) status.style.display = "none";
                    }, 1800);
                }
            });
        }

        activeSession = { cleanup: cleanup };
        overlay.classList.add("waiting");
        overlay.classList.add("active");
        captureButton.disabled = true;
        if (status) {
            status.textContent = "Подключение камеры…";
            status.style.display = "flex";
        }

        requestCameraStream()
            .then(function (activeStream) {
                stream = activeStream;
                video.srcObject = stream;
                video.setAttribute("playsinline", "true");
                video.setAttribute("webkit-playsinline", "true");
                video.muted = true;
                return video.play();
            })
            .then(function () {
                return waitForVideoFrame(video);
            })
            .then(function () {
                ready = true;
                overlay.classList.remove("waiting");
                if (status) status.style.display = "none";
                captureButton.disabled = false;
                captureButton.classList.add("ready");
                if (!detector) {
                    if (status) {
                        status.textContent = "Сделайте снимок QR (автоскан недоступен в этом браузере)";
                        status.style.display = "flex";
                    }
                } else {
                    startDetectLoop();
                }
            })
            .catch(function (error) {
                fail(formatCameraError(error));
            });

        captureButton.onclick = captureAndDetect;
        backButton.onclick = function () {
            fail("cancelled");
        };
    };

    function initScanShutterButton() {
        var btn = document.getElementById("fixaverse-scan-shutter");
        if (!btn || btn.dataset.fixaverseBound === "1") return;
        btn.dataset.fixaverseBound = "1";
        btn.addEventListener(
            "click",
            function (event) {
                if (!scanCameraCallbacks) return;
                event.preventDefault();
                event.stopPropagation();
                window.fixaverseScanAssetQr(scanCameraCallbacks.onSuccess, scanCameraCallbacks.onError);
            },
            true
        );
    }

    window.fixaverseActivateAssetQrCamera = function (onSuccess, onError) {
        scanCameraCallbacks = { onSuccess: onSuccess, onError: onError };
        initScanShutterButton();
        var btn = document.getElementById("fixaverse-scan-shutter");
        if (btn) btn.hidden = false;
    };

    window.fixaverseDeactivateAssetQrCamera = function () {
        scanCameraCallbacks = null;
        var btn = document.getElementById("fixaverse-scan-shutter");
        if (btn) btn.hidden = true;
    };

    window.fixaverseOpenAssetQrCamera = function () {
        if (!scanCameraCallbacks) return false;
        window.fixaverseScanAssetQr(scanCameraCallbacks.onSuccess, scanCameraCallbacks.onError);
        return true;
    };

    initScanShutterButton();
})();
