/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.camera.extensions.impl;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Stub implementation for night image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices.
 *
 * @since 1.0
 */
public final class NightImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    public NightImageCaptureExtenderImpl() {}

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Nullable
    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Nullable
    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public int getMaxCaptureStage() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void onInit(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull Context context) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void onDeInit() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Nullable
    @Override
    public CaptureStageImpl onPresetSession() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Nullable
    @Override
    public CaptureStageImpl onEnableSession() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Nullable
    @Override
    public CaptureStageImpl onDisableSession() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Nullable
    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size captureOutputSize) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @NonNull
    @Override
    public List<CaptureRequest.Key> getAvailableCaptureRequestKeys() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @NonNull
    @Override
    public List<CaptureResult.Key> getAvailableCaptureResultKeys() {
        throw new RuntimeException("Stub, replace with implementation.");
    }
}
