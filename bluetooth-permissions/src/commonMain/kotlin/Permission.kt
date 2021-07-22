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

package com.splendo.kaluga.permissions.bluetooth

import com.splendo.kaluga.permissions.Permission
import com.splendo.kaluga.permissions.PermissionsBuilder

/**
 * Permission to access the Bluetooth scanner
 */
object BluetoothPermission : Permission()

fun PermissionsBuilder.registerBluetoothPermission() =
    registerBluetoothBuilder(context).also { builder ->
        registerRepoFactory(BluetoothPermission::class) { _, coroutineContext ->
            BluetoothPermissionStateRepo(builder as BaseBluetoothPermissionManagerBuilder, coroutineContext)
        }
    }

internal expect fun PermissionsBuilder.registerBluetoothBuilder(context: Any?) : BluetoothPermissionManagerBuilder
