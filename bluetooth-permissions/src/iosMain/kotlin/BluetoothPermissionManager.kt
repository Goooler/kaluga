/*

Copyright 2019 Splendo Consulting B.V. The Netherlands

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

package com.splendo.kaluga.permissions.bluetooth

import com.splendo.kaluga.base.IOSVersion
import com.splendo.kaluga.logging.error
import com.splendo.kaluga.permissions.base.BasePermissionManager
import com.splendo.kaluga.permissions.base.CurrentAuthorizationStatusProvider
import com.splendo.kaluga.permissions.base.DefaultAuthorizationStatusHandler
import com.splendo.kaluga.permissions.base.IOSPermissionsHelper
import com.splendo.kaluga.permissions.base.PermissionContext
import com.splendo.kaluga.permissions.base.PermissionRefreshScheduler
import kotlinx.coroutines.CoroutineScope
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorization
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.CoreBluetooth.CBManagerAuthorizationDenied
import platform.CoreBluetooth.CBManagerAuthorizationNotDetermined
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerAuthorizationStatus
import platform.CoreBluetooth.CBPeripheralManagerAuthorizationStatusAuthorized
import platform.CoreBluetooth.CBPeripheralManagerAuthorizationStatusDenied
import platform.CoreBluetooth.CBPeripheralManagerAuthorizationStatusNotDetermined
import platform.CoreBluetooth.CBPeripheralManagerAuthorizationStatusRestricted
import platform.Foundation.NSBundle
import platform.darwin.dispatch_queue_create
import kotlin.time.Duration

const val NSBluetoothAlwaysUsageDescription = "NSBluetoothAlwaysUsageDescription"
const val NSBluetoothPeripheralUsageDescription = "NSBluetoothPeripheralUsageDescription"

actual class DefaultBluetoothPermissionManager(
    private val bundle: NSBundle,
    settings: Settings,
    coroutineScope: CoroutineScope
) : BasePermissionManager<BluetoothPermission>(BluetoothPermission, settings, coroutineScope) {

    companion object {
        private fun checkAuthorization(): IOSPermissionsHelper.AuthorizationStatus {
            val version = IOSVersion.systemVersion
            return when {
                version >= IOSVersion(13) -> CBManager.authorization.toAuthorizationStatus()
                else -> CBPeripheralManager.authorizationStatus().toPeripheralAuthorizationStatus()
            }
        }
    }

    private class Provider : CurrentAuthorizationStatusProvider {
        override suspend fun provide(): IOSPermissionsHelper.AuthorizationStatus = checkAuthorization()
    }

    private val permissionsQueue = dispatch_queue_create("BluetoothPermissionsMonitor", null)
    private val provider = Provider()

    private val centralManager = lazy {
        val options = mapOf<Any?, Any>(CBCentralManagerOptionShowPowerAlertKey to false)
        CBCentralManager(null, permissionsQueue, options)
    }

    private val permissionHandler: DefaultAuthorizationStatusHandler
    private val timerHelper: PermissionRefreshScheduler

    init {
        permissionHandler = DefaultAuthorizationStatusHandler(eventChannel, logTag, logger)
        timerHelper = PermissionRefreshScheduler(provider, permissionHandler, coroutineScope)
    }

    override fun requestPermissionDidStart() {
        if (IOSPermissionsHelper.missingDeclarationsInPList(
                bundle,
                NSBluetoothAlwaysUsageDescription,
                NSBluetoothPeripheralUsageDescription
            ).isEmpty()
        ) {
            centralManager.value
        } else {
            val permissionHandler = permissionHandler
            permissionHandler.status(IOSPermissionsHelper.AuthorizationStatus.Restricted)
        }
    }

    override fun monitoringDidStart(interval: Duration) {
        val permissionHandler = permissionHandler
        permissionHandler.status(checkAuthorization())
        timerHelper.startMonitoring(interval)
    }

    override fun monitoringDidStop() {
        timerHelper.stopMonitoring()
    }
}

actual class BluetoothPermissionManagerBuilder actual constructor(
    private val context: PermissionContext
) : BaseBluetoothPermissionManagerBuilder {

    override fun create(
        settings: BasePermissionManager.Settings,
        coroutineScope: CoroutineScope
    ): BluetoothPermissionManager {
        return DefaultBluetoothPermissionManager(context, settings, coroutineScope)
    }
}

private fun CBPeripheralManagerAuthorizationStatus.toPeripheralAuthorizationStatus(): IOSPermissionsHelper.AuthorizationStatus {
    return when (this) {
        CBPeripheralManagerAuthorizationStatusAuthorized -> IOSPermissionsHelper.AuthorizationStatus.Authorized
        CBPeripheralManagerAuthorizationStatusDenied -> IOSPermissionsHelper.AuthorizationStatus.Denied
        CBPeripheralManagerAuthorizationStatusRestricted -> IOSPermissionsHelper.AuthorizationStatus.Restricted
        CBPeripheralManagerAuthorizationStatusNotDetermined -> IOSPermissionsHelper.AuthorizationStatus.NotDetermined
        else -> {
            error(
                "BluetoothPermissionManager",
                "Unknown CBPeripheralManagerAuthorizationStatus status={$this}"
            )
            IOSPermissionsHelper.AuthorizationStatus.NotDetermined
        }
    }
}

private fun CBManagerAuthorization.toAuthorizationStatus(): IOSPermissionsHelper.AuthorizationStatus {
    return when (this) {
        CBManagerAuthorizationAllowedAlways -> IOSPermissionsHelper.AuthorizationStatus.Authorized
        CBManagerAuthorizationDenied -> IOSPermissionsHelper.AuthorizationStatus.Denied
        CBManagerAuthorizationRestricted -> IOSPermissionsHelper.AuthorizationStatus.Restricted
        CBManagerAuthorizationNotDetermined -> IOSPermissionsHelper.AuthorizationStatus.NotDetermined
        else -> {
            error(
                "BluetoothPermissionManager",
                "Unknown CBManagerAuthorization status={$this}"
            )
            IOSPermissionsHelper.AuthorizationStatus.NotDetermined
        }
    }
}
