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

package com.splendo.kaluga.bluetooth.device

import com.splendo.kaluga.bluetooth.MTU
import kotlinx.coroutines.CoroutineScope

internal actual class DefaultDeviceConnectionManager(
    deviceWrapper: DeviceWrapper,
    settings: ConnectionSettings,
    coroutineScope: CoroutineScope
) : BaseDeviceConnectionManager(deviceWrapper, settings, coroutineScope) {

    class Builder() : BaseDeviceConnectionManager.Builder {

        override fun create(
            deviceWrapper: DeviceWrapper,
            settings: ConnectionSettings,
            coroutineScope: CoroutineScope
        ): DefaultDeviceConnectionManager {
            return DefaultDeviceConnectionManager(deviceWrapper, settings, coroutineScope)
        }
    }

    override fun getCurrentState(): DeviceConnectionManager.State = DeviceConnectionManager.State.DISCONNECTED

    override suspend fun connect() {}

    override suspend fun discoverServices() {}

    override suspend fun disconnect() {}

    override suspend fun readRssi() {}

    override suspend fun requestMtu(mtu: MTU) = false

    override suspend fun didStartPerformingAction(action: DeviceAction) {}

    override suspend fun didStartPairing() {}

    override suspend fun didStartUnpairing() {}
}
