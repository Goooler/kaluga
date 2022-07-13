/*
 Copyright (c) 2020. Splendo Consulting B.V. The Netherlands

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package com.splendo.kaluga.permissions.storage

import co.touchlab.stately.freeze
import com.splendo.kaluga.logging.error
import com.splendo.kaluga.permissions.base.AuthorizationStatusHandler
import com.splendo.kaluga.permissions.base.AuthorizationStatusProvider
import com.splendo.kaluga.permissions.base.BasePermissionManager
import com.splendo.kaluga.permissions.base.IOSPermissionsHelper
import com.splendo.kaluga.permissions.base.PermissionContext
import com.splendo.kaluga.permissions.base.PermissionRefreshScheduler
import com.splendo.kaluga.permissions.base.requestAuthorizationStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSBundle
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import kotlin.time.Duration

const val NSPhotoLibraryUsageDescription = "NSPhotoLibraryUsageDescription"

actual class DefaultStoragePermissionManager(
    private val bundle: NSBundle,
    storagePermission: StoragePermission,
    settings: Settings,
    coroutineScope: CoroutineScope
) : BasePermissionManager<StoragePermission>(storagePermission, settings, coroutineScope) {

    private class Provider : AuthorizationStatusProvider {
        override suspend fun provide(): IOSPermissionsHelper.AuthorizationStatus = PHPhotoLibrary.authorizationStatus().toAuthorizationStatus()
    }

    private val provider = Provider()

    private val permissionHandler = AuthorizationStatusHandler(eventChannel, logTag, logger)
    private var timerHelper = PermissionRefreshScheduler(provider, permissionHandler, coroutineScope)

    override fun requestPermission() {
        super.requestPermission()
        if (IOSPermissionsHelper.missingDeclarationsInPList(bundle, NSPhotoLibraryUsageDescription).isEmpty()) {
            permissionHandler.requestAuthorizationStatus(timerHelper, CoroutineScope(coroutineContext)) {
                val deferred = CompletableDeferred<PHAuthorizationStatus>()
                val callback = { status: PHAuthorizationStatus ->
                    deferred.complete(status)
                    Unit
                }.freeze()
                PHPhotoLibrary.requestAuthorization(callback)

                deferred.await().toAuthorizationStatus()
            }
        } else {
            val permissionHandler = permissionHandler
            launch {
                permissionHandler.emit(IOSPermissionsHelper.AuthorizationStatus.Restricted)
            }
        }
    }

    override fun startMonitoring(interval: Duration) {
        super.startMonitoring(interval)
        timerHelper.startMonitoring(interval)
    }

    override fun stopMonitoring() {
        super.stopMonitoring()
        timerHelper.stopMonitoring()
    }
}

actual class StoragePermissionManagerBuilder actual constructor(private val context: PermissionContext) : BaseStoragePermissionManagerBuilder {

    override fun create(storagePermission: StoragePermission, settings: BasePermissionManager.Settings, coroutineScope: CoroutineScope): StoragePermissionManager {
        return DefaultStoragePermissionManager(context, storagePermission, settings, coroutineScope)
    }
}

private fun PHAuthorizationStatus.toAuthorizationStatus(): IOSPermissionsHelper.AuthorizationStatus {
    return when (this) {
        PHAuthorizationStatusAuthorized -> IOSPermissionsHelper.AuthorizationStatus.Authorized
        PHAuthorizationStatusDenied -> IOSPermissionsHelper.AuthorizationStatus.Denied
        PHAuthorizationStatusRestricted -> IOSPermissionsHelper.AuthorizationStatus.Restricted
        PHAuthorizationStatusNotDetermined -> IOSPermissionsHelper.AuthorizationStatus.NotDetermined
        else -> {
            error(
                "StoragePermissionManager",
                "Unknown StorageManagerAuthorization status={$this}"
            )
            IOSPermissionsHelper.AuthorizationStatus.NotDetermined
        }
    }
}
