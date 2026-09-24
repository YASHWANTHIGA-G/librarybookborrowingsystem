/**
 * Smart Campus Library - Industrial-Grade Multi-Engine Barcode & QR Scanner
 * Optimized for Mobile Phone Screen Display & Physical Books/Cards
 *
 * Supported Formats:
 * - 1D Barcodes: Code 128 (Student ID / Book barcode), EAN-13 (ISBN-13), UPC-A, UPC-E, Code 39, Code 93, ITF, Codabar
 * - 2D Matrix: QR Code (Instant Phone Screen Scan), Data Matrix
 *
 * Multi-Engine Architecture:
 * 1. Hardware Accelerated Chromium BarcodeDetector (<10ms when supported)
 * 2. Pure Core ZXing MultiFormatReader (TRY_HARDER = true, avoids buggy DOM canvas caching)
 * 3. Dynamic Center ROI (Region of Interest) Crop (eliminates glare & room lighting)
 * 4. Adaptive Phone Screen Contrast Enhancement (normalizes washed-out mobile backlights)
 * 5. Inverted / Dark Mode Barcode Decoding Pass
 * 6. High-Definition Zoom Crop for Dense 1D Barcodes
 */

class BarcodeScanner {
    constructor(readerElementId, options = {}) {
        this.readerId = readerElementId || 'reader';
        this.readerElement = document.getElementById(this.readerId);
        this.placeholder = document.getElementById(options.placeholderId || 'scannerPlaceholder');
        this.cameraSelect = document.getElementById(options.cameraSelectId || 'cameraSelect');
        this.statusText = document.getElementById(options.statusTextId || 'cameraStatusText');
        this.startBtn = document.getElementById(options.startBtnId || 'startScanBtn');
        this.stopBtn = document.getElementById(options.stopBtnId || 'stopScanBtn');
        this.viewportWrap = options.viewportWrapSelector 
            ? document.querySelector(options.viewportWrapSelector) 
            : (this.readerElement ? this.readerElement.closest('.scanner-viewport-wrap') : document.querySelector('.scanner-viewport-wrap'));

        this.onScanSuccess = options.onScanSuccess || null;
        this.onScanError = options.onScanError || null;

        this.stream = null;
        this.videoElement = null;
        this.multiFormatReader = null;
        this.nativeDetector = null;
        this.frameInterval = null;
        this.isScanning = false;
        this.selectedDeviceId = null;
        this.camerasList = [];

        this.lastScannedCode = null;
        this.lastScanTime = 0;
        this.debounceMs = options.debounceMs || 2200;
        this.autoClose = options.autoClose !== undefined ? options.autoClose : false;

        this.audioCtx = null;
        this.workCanvas = document.createElement('canvas');
        this.workCtx = this.workCanvas.getContext('2d', { willReadFrequently: true });
        this.frameCounter = 0;

        // Auto-wire button clicks if provided
        if (this.startBtn && !this.startBtn._scannerBound) {
            this.startBtn._scannerBound = true;
            this.startBtn.addEventListener('click', () => this.start());
        }
        if (this.stopBtn && !this.stopBtn._scannerBound) {
            this.stopBtn._scannerBound = true;
            this.stopBtn.addEventListener('click', () => this.stop());
        }

        this.init();
    }

    setStatus(message, isError = false) {
        if (this.statusText) {
            this.statusText.textContent = message;
            this.statusText.style.color = isError ? '#f43f5e' : '#38bdf8';
        }
        console.log(`[BarcodeScanner:${this.readerId}] ${message}`);
    }

    async init() {
        // 1. Initialize Core ZXing MultiFormatReader
        this.initZXing();

        // 2. Initialize native Chromium BarcodeDetector if available
        if ('BarcodeDetector' in window) {
            try {
                const supported = await BarcodeDetector.getSupportedFormats();
                const desired = ['code_128', 'code_39', 'code_93', 'ean_13', 'ean_8', 'upc_a', 'upc_e', 'itf', 'qr_code', 'data_matrix'];
                const active = desired.filter(f => supported.includes(f));
                if (active.length > 0) {
                    this.nativeDetector = new BarcodeDetector({ formats: active });
                    console.log('[BarcodeScanner] Hardware acceleration enabled for formats:', active);
                }
            } catch (e) {
                console.warn('[BarcodeScanner] Native BarcodeDetector warning:', e);
            }
        }

        // 3. Enumerate cameras
        await this.refreshDeviceList();
    }

    initZXing() {
        if (typeof ZXing === 'undefined') {
            console.warn('[BarcodeScanner] ZXing library not loaded yet, will retry on start.');
            return;
        }

        try {
            const hints = new Map();
            const formats = [
                ZXing.BarcodeFormat.CODE_128,
                ZXing.BarcodeFormat.EAN_13,
                ZXing.BarcodeFormat.CODE_39,
                ZXing.BarcodeFormat.UPC_A,
                ZXing.BarcodeFormat.UPC_E,
                ZXing.BarcodeFormat.EAN_8,
                ZXing.BarcodeFormat.ITF,
                ZXing.BarcodeFormat.CODE_93,
                ZXing.BarcodeFormat.CODABAR,
                ZXing.BarcodeFormat.QR_CODE,
                ZXing.BarcodeFormat.DATA_MATRIX
            ];
            hints.set(ZXing.DecodeHintType.POSSIBLE_FORMATS, formats);
            hints.set(ZXing.DecodeHintType.TRY_HARDER, true);

            this.multiFormatReader = new ZXing.MultiFormatReader();
            this.multiFormatReader.setHints(hints);
            console.log('[BarcodeScanner] Pure Core ZXing Reader ready with TRY_HARDER = true');
        } catch (e) {
            console.error('[BarcodeScanner] ZXing init error:', e);
        }
    }

    async refreshDeviceList() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices) {
            if (this.cameraSelect) {
                this.cameraSelect.innerHTML = '<option value="">Default Webcam</option>';
            }
            return;
        }

        try {
            const devices = await navigator.mediaDevices.enumerateDevices();
            const videoInputs = devices.filter(d => d.kind === 'videoinput');

            if (this.cameraSelect) {
                this.cameraSelect.innerHTML = '';
                if (videoInputs.length === 0) {
                    this.cameraSelect.innerHTML = '<option value="">Default Webcam</option>';
                } else {
                    this.camerasList = videoInputs;
                    videoInputs.forEach((dev, idx) => {
                        const opt = document.createElement('option');
                        opt.value = dev.deviceId;
                        opt.text = dev.label || `Webcam ${idx + 1}`;
                        if (idx === 0) opt.selected = true;
                        this.cameraSelect.appendChild(opt);
                    });
                    if (!this.selectedDeviceId && videoInputs[0]) {
                        this.selectedDeviceId = videoInputs[0].deviceId;
                    }
                }
            }
        } catch (err) {
            console.warn('[BarcodeScanner] Camera enumeration error:', err);
            if (this.cameraSelect) {
                this.cameraSelect.innerHTML = '<option value="">Default Webcam</option>';
            }
        }
    }

    playScanBeep() {
        try {
            if (!this.audioCtx) {
                this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            }
            if (this.audioCtx.state === 'suspended') {
                this.audioCtx.resume();
            }
            const osc = this.audioCtx.createOscillator();
            const gain = this.audioCtx.createGain();

            osc.type = 'sine';
            osc.frequency.setValueAtTime(2400, this.audioCtx.currentTime);
            gain.gain.setValueAtTime(0.35, this.audioCtx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, this.audioCtx.currentTime + 0.12);

            osc.connect(gain);
            gain.connect(this.audioCtx.destination);

            osc.start();
            osc.stop(this.audioCtx.currentTime + 0.12);
        } catch (e) {
            console.warn('Audio error:', e);
        }
    }

    playSuccessChime() {
        try {
            if (!this.audioCtx) {
                this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            }
            if (this.audioCtx.state === 'suspended') {
                this.audioCtx.resume();
            }
            const now = this.audioCtx.currentTime;
            const notes = [523.25, 659.25, 783.99, 1046.50]; // C5 - E5 - G5 - C6
            notes.forEach((freq, idx) => {
                const osc = this.audioCtx.createOscillator();
                const gain = this.audioCtx.createGain();
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(freq, now + idx * 0.07);
                gain.gain.setValueAtTime(0.25, now + idx * 0.07);
                gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.07 + 0.28);
                osc.connect(gain);
                gain.connect(this.audioCtx.destination);
                osc.start(now + idx * 0.07);
                osc.stop(now + idx * 0.07 + 0.28);
            });
        } catch (e) {
            console.warn('Chime error:', e);
        }
    }

    /**
     * Start the camera stream and decoding loops
     */
    async start() {
        if (this.isScanning) {
            console.log('[BarcodeScanner] Already scanning.');
            return;
        }

        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            this.setStatus('Camera API not supported or requires HTTPS/localhost.', true);
            if (this.onScanError) this.onScanError(new Error('getUserMedia not supported in this browser.'));
            return;
        }

        if (!this.multiFormatReader && typeof ZXing !== 'undefined') {
            this.initZXing();
        }

        this.setStatus('Requesting webcam access...');

        try {
            const selVal = this.cameraSelect ? this.cameraSelect.value : null;
            const targetDeviceId = (selVal && selVal !== '') ? selVal : this.selectedDeviceId;

            const videoConstraints = {
                width: { ideal: 1280, min: 640 },
                height: { ideal: 720, min: 480 }
            };

            if (targetDeviceId) {
                videoConstraints.deviceId = { exact: targetDeviceId };
            } else {
                videoConstraints.facingMode = { ideal: 'environment' };
            }

            const constraints = {
                video: videoConstraints,
                audio: false
            };

            try {
                this.stream = await navigator.mediaDevices.getUserMedia(constraints);
            } catch (errConstraint) {
                console.warn('[BarcodeScanner] Specific constraints failed, falling back to basic video:', errConstraint);
                this.stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
            }

            if (!this.readerElement) {
                throw new Error(`Reader container #${this.readerId} not found`);
            }

            this.readerElement.innerHTML = '';
            this.readerElement.style.display = 'block';

            // Container wrapper for video + targeting HUD
            const videoWrap = document.createElement('div');
            videoWrap.className = 'scanner-active-video-container';
            videoWrap.style.position = 'relative';
            videoWrap.style.width = '100%';
            videoWrap.style.height = '100%';
            videoWrap.style.minHeight = '240px';
            videoWrap.style.overflow = 'hidden';
            videoWrap.style.borderRadius = '8px';
            videoWrap.style.background = '#000';

            const video = document.createElement('video');
            video.setAttribute('playsinline', 'true');
            video.setAttribute('autoplay', 'true');
            video.setAttribute('muted', 'true');
            video.style.width = '100%';
            video.style.height = '100%';
            video.style.objectFit = 'cover';
            video.style.display = 'block';
            video.srcObject = this.stream;

            const hud = document.createElement('div');
            hud.className = 'scanner-hud-overlay';
            hud.innerHTML = `
                <div class="scanner-laser-line"></div>
                <div class="scanner-bracket top-left"></div>
                <div class="scanner-bracket top-right"></div>
                <div class="scanner-bracket bottom-left"></div>
                <div class="scanner-bracket bottom-right"></div>
                <div class="scanner-hud-text">Hold Phone Screen or Book Barcode Steady</div>
            `;

            videoWrap.appendChild(video);
            videoWrap.appendChild(hud);
            this.readerElement.appendChild(videoWrap);
            this.videoElement = video;

            await video.play();

            if (this.placeholder) this.placeholder.style.display = 'none';
            if (this.viewportWrap) this.viewportWrap.classList.add('scanning-active');
            if (this.startBtn) this.startBtn.disabled = true;
            if (this.stopBtn) this.stopBtn.disabled = false;

            this.isScanning = true;
            this.setStatus('📷 Camera Active: Hold phone screen 15-20cm in front of lens');

            await this.refreshDeviceList();

            // Start continuous high-performance frame processing
            this.startProcessingLoop();

        } catch (err) {
            console.error('[BarcodeScanner] Start failed:', err);
            this.setStatus(`Camera error: ${err.message || 'Access denied'}`, true);
            this.cleanupUiAfterStop();
            if (this.onScanError) this.onScanError(err);
        }
    }

    /**
     * Core decoding pipeline on a single grayscale Uint8ClampedArray buffer.
     * Tries multiple binarization & cropping strategies.
     */
    decodeGrayscaleBuffer(grayBuffer, width, height) {
        if (!this.multiFormatReader || !grayBuffer || width <= 0 || height <= 0) return null;

        try {
            const ls = new ZXing.RGBLuminanceSource(grayBuffer, width, height);

            // Strategy A: Hybrid Binarizer on full frame (excellent for clear barcodes and QR codes)
            try {
                const bitmap = new ZXing.BinaryBitmap(new ZXing.HybridBinarizer(ls));
                const res = this.multiFormatReader.decode(bitmap);
                if (res && res.getText()) return { text: res.getText(), pass: 'hybrid_full' };
            } catch (e) {}

            // Strategy B: Global Histogram Binarizer on full frame (handles phone screens with light gradients)
            try {
                const bitmap = new ZXing.BinaryBitmap(new ZXing.GlobalHistogramBinarizer(ls));
                const res = this.multiFormatReader.decode(bitmap);
                if (res && res.getText()) return { text: res.getText(), pass: 'global_full' };
            } catch (e) {}

            // Strategy C: Center Region-of-Interest (ROI) Crop (~65% width x 65% height)
            // Eliminates ambient office/room lights and phone edges!
            if (width > 60 && height > 60) {
                const cropW = Math.floor(width * 0.65);
                const cropH = Math.floor(height * 0.65);
                const cropX = Math.floor((width - cropW) / 2);
                const cropY = Math.floor((height - cropH) / 2);

                try {
                    const croppedLs = ls.crop(cropX, cropY, cropW, cropH);
                    try {
                        const bitmap = new ZXing.BinaryBitmap(new ZXing.HybridBinarizer(croppedLs));
                        const res = this.multiFormatReader.decode(bitmap);
                        if (res && res.getText()) return { text: res.getText(), pass: 'hybrid_center_crop' };
                    } catch (e) {}

                    try {
                        const bitmap = new ZXing.BinaryBitmap(new ZXing.GlobalHistogramBinarizer(croppedLs));
                        const res = this.multiFormatReader.decode(bitmap);
                        if (res && res.getText()) return { text: res.getText(), pass: 'global_center_crop' };
                    } catch (e) {}
                } catch (e) {}
            }

            // Strategy D: Tight Center Zoom Crop (~45% width x 45% height)
            // Zooms in on dense 1D barcodes (e.g. Code 128 / EAN-13) when phone is held at distance
            if (width > 120 && height > 120) {
                const zoomW = Math.floor(width * 0.45);
                const zoomH = Math.floor(height * 0.45);
                const zoomX = Math.floor((width - zoomW) / 2);
                const zoomY = Math.floor((height - zoomH) / 2);

                try {
                    const zoomedLs = ls.crop(zoomX, zoomY, zoomW, zoomH);
                    const bitmap = new ZXing.BinaryBitmap(new ZXing.HybridBinarizer(zoomedLs));
                    const res = this.multiFormatReader.decode(bitmap);
                    if (res && res.getText()) return { text: res.getText(), pass: 'zoomed_center_crop' };
                } catch (e) {}
            }

            // Strategy E: Inverted Luminance (for dark-mode phone screens or inverted barcodes)
            if (this.frameCounter % 3 === 0) {
                try {
                    const invertedLs = ls.invert();
                    const bitmap = new ZXing.BinaryBitmap(new ZXing.HybridBinarizer(invertedLs));
                    const res = this.multiFormatReader.decode(bitmap);
                    if (res && res.getText()) return { text: res.getText(), pass: 'inverted' };
                } catch (e) {}
            }

        } catch (e) {
            // Transient decoding error
        }

        return null;
    }

    /**
     * Decodes an HTMLCanvasElement
     */
    decodeCanvas(canvas) {
        if (!canvas || canvas.width <= 0 || canvas.height <= 0) return null;
        const w = canvas.width;
        const h = canvas.height;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        const imgData = ctx.getImageData(0, 0, w, h);
        const data = imgData.data;
        const len = w * h;

        const gray = new Uint8ClampedArray(len);
        for (let i = 0, j = 0; i < len; i++, j += 4) {
            gray[i] = (data[j] * 306 + data[j + 1] * 601 + data[j + 2] * 117) >> 10;
        }

        // Pass 1: Direct grayscale
        const res1 = this.decodeGrayscaleBuffer(gray, w, h);
        if (res1) return res1;

        // Pass 2: Adaptive contrast stretch (Phone Screen Glare Filter)
        let minLuma = 255, maxLuma = 0;
        for (let i = 0; i < len; i += 4) {
            const v = gray[i];
            if (v < minLuma) minLuma = v;
            if (v > maxLuma) maxLuma = v;
        }
        const range = maxLuma - minLuma;
        if (range > 25) {
            const factor = 255 / range;
            const enhanced = new Uint8ClampedArray(len);
            for (let i = 0; i < len; i++) {
                const val = (gray[i] - minLuma) * factor;
                enhanced[i] = val < 0 ? 0 : (val > 255 ? 255 : val);
            }
            const res2 = this.decodeGrayscaleBuffer(enhanced, w, h);
            if (res2) return res2;
        }

        return null;
    }

    /**
     * Continuous frame processing loop (~18 FPS)
     */
    startProcessingLoop() {
        if (!this.isScanning || !this.videoElement) return;

        let isFrameBusy = false;

        const processFrame = async () => {
            if (!this.isScanning || !this.videoElement) return;
            if (this.videoElement.readyState < 2 || this.videoElement.paused) return;
            if (isFrameBusy) return;

            isFrameBusy = true;
            this.frameCounter++;

            try {
                // Pass 1: Native Chromium BarcodeDetector (Hardware Accelerated, <10ms if available)
                if (this.nativeDetector) {
                    try {
                        const barcodes = await this.nativeDetector.detect(this.videoElement);
                        if (barcodes && barcodes.length > 0) {
                            for (const bc of barcodes) {
                                const raw = (bc.rawValue || '').trim();
                                if (raw) {
                                    this.handleScanResult(raw, 'native_hardware');
                                    isFrameBusy = false;
                                    return;
                                }
                            }
                        }
                    } catch (e) {
                        // Native detector skipped, proceed to ZXing engine
                    }
                }

                // Pass 2 & 3 & 4: Core ZXing Engine with Glare Normalization & Center ROI
                if (this.multiFormatReader && this.videoElement.videoWidth > 0) {
                    const vw = this.videoElement.videoWidth;
                    const vh = this.videoElement.videoHeight;

                    // Clamp to max 850px width for peak CPU efficiency and crisp edge definition
                    const targetW = Math.min(vw, 850);
                    const targetH = Math.round((vh / vw) * targetW);

                    if (this.workCanvas.width !== targetW || this.workCanvas.height !== targetH) {
                        this.workCanvas.width = targetW;
                        this.workCanvas.height = targetH;
                    }

                    this.workCtx.drawImage(this.videoElement, 0, 0, targetW, targetH);

                    // Decode using our robust, bug-free canvas decoder
                    const decoded = this.decodeCanvas(this.workCanvas);
                    if (decoded && decoded.text) {
                        this.handleScanResult(decoded.text.trim(), `zxing_${decoded.pass}`);
                        isFrameBusy = false;
                        return;
                    }
                }

            } catch (err) {
                // Transient frame error ignored
            } finally {
                isFrameBusy = false;
            }
        };

        if (this.frameInterval) clearInterval(this.frameInterval);
        this.frameInterval = setInterval(processFrame, 55);
    }

    /**
     * Decodes a barcode from an uploaded image file or dropped image
     */
    async scanImageFile(file) {
        if (!file) return null;
        this.setStatus('Analyzing uploaded barcode image...');

        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = async (e) => {
                const img = new Image();
                img.onload = async () => {
                    try {
                        // Pass 1: Native detector
                        if (this.nativeDetector) {
                            try {
                                const barcodes = await this.nativeDetector.detect(img);
                                if (barcodes && barcodes.length > 0) {
                                    for (const bc of barcodes) {
                                        const raw = (bc.rawValue || '').trim();
                                        if (raw) {
                                            this.handleScanResult(raw, 'file_native');
                                            resolve(raw);
                                            return;
                                        }
                                    }
                                }
                            } catch (err) {}
                        }

                        // Pass 2: Draw to canvas and decode with Core ZXing
                        const canvas = document.createElement('canvas');
                        canvas.width = img.naturalWidth || img.width;
                        canvas.height = img.naturalHeight || img.height;
                        const ctx = canvas.getContext('2d');
                        ctx.drawImage(img, 0, 0);

                        const decoded = this.decodeCanvas(canvas);
                        if (decoded && decoded.text) {
                            const raw = decoded.text.trim();
                            this.handleScanResult(raw, `file_zxing_${decoded.pass}`);
                            resolve(raw);
                            return;
                        }

                        this.setStatus('No barcode detected in image. Please try a clearer photo or enter manually.', true);
                        resolve(null);
                    } catch (e) {
                        this.setStatus(`Image decode failed: ${e.message}`, true);
                        reject(e);
                    }
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        });
    }

    /**
     * Handles decoded barcode with sound, visual flash, debounce, and callback
     */
    handleScanResult(rawCode, engine = 'unknown') {
        if (!rawCode) return;
        const cleanCode = String(rawCode).trim();
        if (!cleanCode) return;

        const now = Date.now();
        if (cleanCode === this.lastScannedCode && (now - this.lastScanTime) < this.debounceMs) {
            return;
        }

        this.lastScannedCode = cleanCode;
        this.lastScanTime = now;

        console.log(`[BarcodeScanner] SUCCESS [Engine: ${engine}]: Decoded [${cleanCode}]`);
        this.playScanBeep();
        this.playSuccessChime();

        if (this.viewportWrap) {
            this.viewportWrap.classList.add('scan-success-flash');
            setTimeout(() => {
                if (this.viewportWrap) this.viewportWrap.classList.remove('scan-success-flash');
            }, 800);
        }

        this.setStatus(`✅ Scanned: ${cleanCode}`);

        if (this.onScanSuccess) {
            this.onScanSuccess(cleanCode);
        }

        if (this.autoClose) {
            setTimeout(() => {
                this.stop().catch(err => console.warn('[BarcodeScanner] Auto-close error:', err));
            }, 400);
        }
    }

    /**
     * Stop the camera stream and restore UI - Guarantees hardware camera shuts completely OFF
     */
    async stop() {
        this.isScanning = false;

        if (this.frameInterval) {
            clearInterval(this.frameInterval);
            this.frameInterval = null;
        }

        // 1. Fully disable and stop all tracks in active stream
        if (this.stream) {
            try {
                this.stream.getTracks().forEach(track => {
                    try {
                        track.enabled = false;
                        track.stop();
                        console.log(`[BarcodeScanner] Camera track stopped: ${track.label}`);
                    } catch (tErr) {}
                });
            } catch (e) {
                console.warn('[BarcodeScanner] Error stopping tracks:', e);
            }
            this.stream = null;
        }

        // 2. Pause and release video element capture pipeline
        if (this.videoElement) {
            try {
                if (this.videoElement.srcObject && typeof this.videoElement.srcObject.getTracks === 'function') {
                    this.videoElement.srcObject.getTracks().forEach(track => {
                        try {
                            track.enabled = false;
                            track.stop();
                        } catch (tErr) {}
                    });
                }
                this.videoElement.pause();
                this.videoElement.srcObject = null;
                this.videoElement.removeAttribute('src');
                this.videoElement.load();
            } catch (vErr) {
                console.warn('[BarcodeScanner] Error releasing video element:', vErr);
            }
        }

        // 3. Clear container DOM and restore UI
        this.cleanupUiAfterStop();
        this.setStatus('Camera scanner stopped.');
    }

    cleanupUiAfterStop() {
        this.isScanning = false;
        if (this.frameInterval) {
            clearInterval(this.frameInterval);
            this.frameInterval = null;
        }
        if (this.videoElement) {
            try {
                this.videoElement.pause();
                this.videoElement.srcObject = null;
            } catch (e) {}
            this.videoElement = null;
        }
        if (this.readerElement) {
            this.readerElement.style.display = 'none';
            this.readerElement.innerHTML = '';
        }
        if (this.placeholder) this.placeholder.style.display = 'flex';
        if (this.viewportWrap) {
            this.viewportWrap.classList.remove('scanning-active');
            this.viewportWrap.classList.remove('scan-success-flash');
        }
        if (this.startBtn) this.startBtn.disabled = false;
        if (this.stopBtn) this.stopBtn.disabled = true;
    }

    static stopAll() {
        if (window.barcodeScanner && window.barcodeScanner.isScanning) window.barcodeScanner.stop();
        if (window.scanPageBarcodeScanner && window.scanPageBarcodeScanner.isScanning) window.scanPageBarcodeScanner.stop();
        if (window.catalogBarcodeScanner && window.catalogBarcodeScanner.isScanning) window.catalogBarcodeScanner.stop();
    }
}
