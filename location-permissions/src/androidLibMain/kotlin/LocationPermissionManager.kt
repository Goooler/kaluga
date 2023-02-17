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

package com.splendo.kaluga.permissions.location

import android.Manifest
import android.content.Context
import com.splendo.kaluga.permissions.base.AndroidPermissionsManager
import com.splendo.kaluga.permissions.base.BasePermissionManager
import com.splendo.kaluga.permissions.base.DefaultAndroidPermissionStateHandler
import com.splendo.kaluga.permissions.base.PermissionContext
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

actual class DefaultLocationPermissionManager(
    context: Context,
    locationPermission: LocationPermission,
    settings: Settings,
    coroutineScope: CoroutineScope
) : BasePermissionManager<LocationPermission>(locationPermission, settings, coroutineScope) {

    private val permissions: Array<String> get() = listOfNotNull(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (permission.precise) Manifest.permission.ACCESS_FINE_LOCATION else null,
        if (permission.background && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else null
    ).toTypedArray()

    private val permissionHandler = DefaultAndroidPermissionStateHandler(eventChannel, logTag, logger)
    private val permissionsManager = AndroidPermissionsManager(context, permissions, coroutineScope, logTag, logger, permissionHandler)

    override fun requestPermissionDidStart() {
        permissionsManager.requestPermissions()
    }

    override fun monitoringDidStart(interval: Duration) {
        permissionsManager.startMonitoring(interval)
    }

    override fun monitoringDidStop() {
        permissionsManager.stopMonitoring()
    }
}

actual class LocationPermissionManagerBuilder actual constructor(private val context: PermissionContext) : BaseLocationPermissionManagerBuilder {

    override fun create(
        locationPermission: LocationPermission,
        settings: BasePermissionManager.Settings,
        coroutineScope: CoroutineScope
    ): LocationPermissionManager {
        return DefaultLocationPermissionManager(context.context, locationPermission, settings, coroutineScope)
    }
}
