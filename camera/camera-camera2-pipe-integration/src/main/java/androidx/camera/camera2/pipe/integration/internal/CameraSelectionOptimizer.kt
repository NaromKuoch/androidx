/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.camera2.pipe.integration.internal

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CameraFactoryAdapter
import androidx.camera.camera2.pipe.integration.config.CameraAppComponent
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.InitializationException
import androidx.camera.core.impl.CameraInfoInternal
import kotlinx.coroutines.runBlocking

/**
 * The [CameraSelectionOptimizer] is responsible for available camera Ids based on passed CameraSelector
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraSelectionOptimizer {
    companion object {

        @Throws(InitializationException::class)
        fun getSelectedAvailableCameraIds(
            cameraFactory: CameraFactoryAdapter,
            availableCamerasSelector: CameraSelector?
        ): List<String> {
            try {
                val availableCameraIds = mutableListOf<String>()
                val cameraAppComponent = cameraFactory.cameraManager as CameraAppComponent
                val cameraDevices = cameraAppComponent.getCameraDevices()

                val cameraIdList =
                    runBlocking { cameraDevices.ids().map { it.value } }
                if (availableCamerasSelector == null) {
                    return cameraIdList
                }

                // Skip camera ID by heuristic: 0 is back lens facing, 1 is front lens facing.
                val skippedCameraId: String? = try {
                    decideSkippedCameraIdByHeuristic(
                        cameraDevices,
                        availableCamerasSelector.lensFacing
                    )
                } catch (e: IllegalStateException) {
                    // Device doesn't need to have front and/or back camera.
                    // This exception doesn't mean error.
                    Log.debug(e) { "Unable to get Metadata for cameraID 0 and/or 1" }
                    // Don't skip camera if there is any conflict in camera lens facing.
                    null
                }
                val cameraInfos = mutableListOf<CameraInfo>()
                for (id in cameraIdList) {
                    if (id == skippedCameraId) {
                        continue
                    }
                    val cameraInfo =
                        cameraAppComponent.cameraBuilder().config(CameraConfig(CameraId(id)))
                            .build()
                            .getCameraInternal().cameraInfoInternal
                    cameraInfos.add(cameraInfo)
                }
                val filteredCameraInfos = availableCamerasSelector.filter(cameraInfos)
                for (cameraInfo in filteredCameraInfos) {
                    val cameraId = (cameraInfo as CameraInfoInternal).cameraId
                    availableCameraIds.add(cameraId)
                }
                return availableCameraIds
            } catch (e: CameraAccessException) {
                Log.error(e) { "Error while accessing list of cameras." }
                throw InitializationException(e)
            }
        }

        // Returns the camera id that can be safely skipped.
        // Returns null if no camera ids can be skipped.
        private fun decideSkippedCameraIdByHeuristic(
            cameraDevices: CameraDevices,
            lensFacingInteger: Int?
        ): String? {
            var skippedCameraId: String? = null

            if (lensFacingInteger == null) { // Cannot skip any camera id.
                return null
            }
            if (lensFacingInteger.toInt() == CameraSelector.LENS_FACING_BACK) {
                val camera0Metadata = cameraDevices.awaitMetadata(CameraId("0"))
                if (camera0Metadata[CameraCharacteristics.LENS_FACING]?.equals(
                        CameraCharacteristics.LENS_FACING_BACK
                    )!!
                ) {
                    // If apps requires back lens facing,  and "0" is confirmed to be back
                    // We can safely ignore "1" as a optimization for initialization latency
                    skippedCameraId = "1"
                }
            } else if (lensFacingInteger.toInt() == CameraSelector.LENS_FACING_FRONT) {
                val camera1Metadata = cameraDevices.awaitMetadata(CameraId("1"))
                if (camera1Metadata[CameraCharacteristics.LENS_FACING]?.equals(
                        CameraCharacteristics.LENS_FACING_FRONT
                    )!!
                ) {
                    // If apps requires front lens facing,  and "1" is confirmed to be back
                    // We can safely ignore "0" as a optimization for initialization latency
                    skippedCameraId = "0"
                }
            }
            return skippedCameraId
        }
    }
}