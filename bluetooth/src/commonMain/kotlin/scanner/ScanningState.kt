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

package com.splendo.kaluga.bluetooth.scanner

import com.splendo.kaluga.base.flow.SpecialFlowValue
import com.splendo.kaluga.base.singleThreadDispatcher
import com.splendo.kaluga.bluetooth.UUID
import com.splendo.kaluga.bluetooth.device.BaseAdvertisementData
import com.splendo.kaluga.bluetooth.device.Device
import com.splendo.kaluga.bluetooth.device.Identifier
import com.splendo.kaluga.bluetooth.device.stringValue
import com.splendo.kaluga.state.ColdStateFlowRepo
import com.splendo.kaluga.state.HandleAfterCreating
import com.splendo.kaluga.state.HandleAfterNewStateIsSet
import com.splendo.kaluga.state.HandleAfterOldStateIsRemoved
import com.splendo.kaluga.state.HandleBeforeOldStateIsRemoved
import com.splendo.kaluga.state.KalugaState
import com.splendo.kaluga.state.StateRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

typealias Filter = Set<UUID>

sealed interface ScanningState : KalugaState {

    interface Permitted : HandleBeforeOldStateIsRemoved<ScanningState>, HandleAfterNewStateIsSet<ScanningState> {
        val revokePermission: suspend () -> NoBluetooth.MissingPermissions
    }

    interface Discovered {
        val devices: List<Device>
        val filter: Filter

        fun copyAndAdd(device: Device): Discovered
        fun discoveredForFilter(filter: Filter): Discovered
    }

    sealed interface Inactive : ScanningState, SpecialFlowValue.NotImportant
    interface NotInitialized : Inactive

    interface Deinitialized : Inactive {
        val previouslyDiscovered: Discovered
        val reinitialize: suspend () -> Initializing
    }

    sealed interface Active :
        ScanningState,
        HandleBeforeOldStateIsRemoved<ScanningState>,
        HandleAfterNewStateIsSet<ScanningState> {

        val discovered: Discovered
        val deinitialize: suspend () -> Deinitialized
    }

    interface Initializing: Active, SpecialFlowValue.NotImportant {
        fun initialized(hasPermission: Boolean, enabled: Boolean): suspend () -> Initialized
    }
    sealed interface Initialized : Active

    sealed interface Enabled : Initialized, Permitted {
        val disable: suspend () -> NoBluetooth.Disabled

        interface Idle : Enabled {

            fun startScanning(filter: Set<UUID> = discovered.filter): suspend () -> Scanning

            fun refresh(filter: Set<UUID> = discovered.filter): suspend () -> Idle
        }

        interface Scanning : Enabled,
            HandleAfterOldStateIsRemoved<ScanningState>,
            HandleAfterCreating<ScanningState> {

            suspend fun discoverDevice(
                identifier: Identifier,
                rssi: Int,
                advertisementData: BaseAdvertisementData,
                deviceCreator: () -> Device
            ): suspend () -> Scanning

            val stopScanning: suspend () -> Idle
        }
    }

    sealed interface NoBluetooth : Initialized {

        interface Disabled : NoBluetooth, Permitted {
            val enable: suspend () -> Enabled
        }

        interface MissingPermissions : NoBluetooth {
            fun permit(enabled: Boolean): suspend () -> ScanningState
        }
    }

    interface NoHardware : ScanningState
}

sealed class ScanningStateImpl {

    companion object {
        val nothingDiscovered = Discovered(emptySet())
    }

    data class Discovered(
        override val devices: List<Device>,
        override val filter: Filter,
    ) : ScanningState.Discovered {
        constructor(filter: Filter) : this(emptyList(), filter)

        override fun copyAndAdd(device: Device): Discovered =
            Discovered(listOf(*devices.toTypedArray(), device), filter)

        override fun discoveredForFilter(filter: Filter) =
            if (this.filter == filter)
                this
            else
                Discovered(filter)
    }

    sealed class Inactive : ScanningStateImpl()
    object NotInitialized : Inactive(), ScanningState.NotInitialized {

        fun startInitializing(
            scanner: Scanner
        ): suspend () -> ScanningState {
            return if (!scanner.isSupported) {
                { NoHardware }
            } else {
                { Initializing(nothingDiscovered, scanner) }
            }
        }
    }

    data class Deinitialized(override val previouslyDiscovered: ScanningState.Discovered, val scanner: Scanner) :
        Inactive(), ScanningState.Deinitialized {
        override val reinitialize = suspend { Initializing(previouslyDiscovered, scanner) }
    }

    sealed class Active : ScanningStateImpl() {
        open suspend fun beforeOldStateIsRemoved(oldState: ScanningState) {
            when (oldState) {
                is ScanningState.Inactive -> {
                    scanner.startMonitoringPermissions()
                }
                is Active, NoHardware -> {}
            }
        }

        open suspend fun afterNewStateIsSet(newState: ScanningState) {
            when (newState) {
                is ScanningState.Inactive -> {
                    scanner.stopMonitoringPermissions()
                }
                is Active, NoHardware -> {}
            }
        }
        abstract val scanner: Scanner
        abstract val discovered: ScanningState.Discovered
        val deinitialize: suspend () -> Deinitialized = { Deinitialized(discovered, scanner) }
    }

    class PermittedHandler(val scanner: Scanner) : ScanningState.Permitted {

        override val revokePermission = suspend { NoBluetooth.MissingPermissions(scanner) }

        override suspend fun afterNewStateIsSet(newState: ScanningState) {
            when (newState) {
                is ScanningState.Inactive,
                is ScanningState.Initializing,
                is ScanningState.NoHardware,
                is ScanningState.NoBluetooth.MissingPermissions -> scanner.stopMonitoringHardwareEnabled()
                is ScanningState.Active -> {}
            }
        }

        override suspend fun beforeOldStateIsRemoved(oldState: ScanningState) {
            when (oldState) {
                is ScanningState.Inactive,
                is ScanningState.Initializing,
                is ScanningState.NoHardware,
                is ScanningState.NoBluetooth.MissingPermissions -> scanner.startMonitoringHardwareEnabled()
                is ScanningState.Active -> {}
            }
        }
    }

    data class Initializing(
        override val discovered: ScanningState.Discovered,
        override val scanner: Scanner
    ) : Active(), ScanningState.Initializing {

        override fun initialized(hasPermission: Boolean, enabled: Boolean): suspend () -> ScanningState.Initialized =
            suspend {
                when {
                    !hasPermission -> NoBluetooth.MissingPermissions(scanner)
                    !enabled -> NoBluetooth.Disabled(scanner)
                    else -> Enabled.Idle(discovered, scanner)
                }
            }
    }

    sealed class Initialized : ScanningStateImpl()

    sealed class Enabled : Active() {

        protected abstract val permittedHandler: PermittedHandler

        val disable = suspend {
            NoBluetooth.Disabled(scanner)
        }

        val revokePermission: suspend () -> NoBluetooth.MissingPermissions get() = permittedHandler.revokePermission

        override suspend fun afterNewStateIsSet(newState: ScanningState) {
            super.afterNewStateIsSet(newState)
            permittedHandler.afterNewStateIsSet(newState)
        }

        override suspend fun beforeOldStateIsRemoved(oldState: ScanningState) {
            super.beforeOldStateIsRemoved(oldState)
            permittedHandler.beforeOldStateIsRemoved(oldState)
        }

        class Idle internal constructor(
            override val discovered: ScanningState.Discovered,
            override val scanner: Scanner
        ) : Enabled(), ScanningState.Enabled.Idle {

            override val permittedHandler: PermittedHandler = PermittedHandler(scanner)

            override fun startScanning(filter: Set<UUID>): suspend () -> Scanning = {
                Scanning(
                    discovered.discoveredForFilter(filter),
                    scanner
                )
            }

            override fun refresh(filter: Set<UUID>): suspend () -> Idle = {
                Idle(
                    discovered.discoveredForFilter(filter),
                    scanner
                )
            }
        }

        class Scanning internal constructor(
            override val discovered: ScanningState.Discovered,
            override val scanner: Scanner
        ) : Enabled(),
            ScanningState.Enabled.Scanning
        {

            override val permittedHandler: PermittedHandler = PermittedHandler(scanner)

            override suspend fun discoverDevice(
                identifier: Identifier,
                rssi: Int,
                advertisementData: BaseAdvertisementData,
                deviceCreator: () -> Device
            ): suspend () -> ScanningState.Enabled.Scanning {

                return discovered.devices.find { it.identifier == identifier }
                    ?.let { knownDevice ->
                        knownDevice.takeAndChangeState { state ->
                            state.advertisementDataAndRssiDidUpdate(advertisementData, rssi)
                        }
                        // TODO our contents have technically changed yet we remain()
                        // not storing Devices as repos, but rather storing their _state_
                        // would be better.
                        remain()
                    } ?: suspend { Scanning(discovered.copyAndAdd(deviceCreator()), scanner) }
            }

            override val stopScanning = suspend { Idle(discovered, scanner) }

            override suspend fun afterOldStateIsRemoved(oldState: ScanningState) {
                if (oldState !is Scanning)
                    scanner.scanForDevices(discovered.filter)
            }

            override suspend fun afterCreatingNewState(newState: ScanningState) {
                if (newState !is Scanning)
                    scanner.stopScanning()
            }
        }
    }

    sealed class NoBluetooth : Active() {

        override val discovered: Discovered = nothingDiscovered

        class Disabled internal constructor(
            override val scanner: Scanner
        ) : NoBluetooth(), ScanningState.NoBluetooth.Disabled {

            private val permittedHandler = PermittedHandler(scanner)

            override val enable: suspend () -> ScanningState.Enabled = {
                Enabled.Idle(nothingDiscovered, scanner)
            }

            override val revokePermission: suspend () -> MissingPermissions = permittedHandler.revokePermission

            override suspend fun afterNewStateIsSet(newState: ScanningState) {
                super.afterNewStateIsSet(newState)
                permittedHandler.afterNewStateIsSet(newState)
            }

            override suspend fun beforeOldStateIsRemoved(oldState: ScanningState) {
                super.beforeOldStateIsRemoved(oldState)
                permittedHandler.beforeOldStateIsRemoved(oldState)
            }
        }

        class MissingPermissions internal constructor(
            override val scanner: Scanner
        ) : NoBluetooth(), ScanningState.NoBluetooth.MissingPermissions {

            override fun permit(enabled: Boolean): suspend () -> ScanningState = {
                if (enabled) Enabled.Idle(nothingDiscovered, scanner)
                else Disabled(scanner)
            }
        }
    }

    object NoHardware : ScanningStateImpl(), ScanningState.NoHardware
}

typealias ScanningStateFlowRepo = StateRepo<ScanningState, MutableStateFlow<ScanningState>>

abstract class BaseScanningStateRepo(
    createNotInitializedState: () -> ScanningState.NotInitialized,
    createInitializingState: ColdStateFlowRepo<ScanningState>.(ScanningState.Inactive) -> suspend () -> ScanningState,
    createDeinitializingState: ColdStateFlowRepo<ScanningState>.(ScanningState.Active) -> suspend () -> ScanningState.Deinitialized,
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate
) : ColdStateFlowRepo<ScanningState>(
    coroutineContext = coroutineContext,
    initChangeStateWithRepo = { state, repo ->
        when (state) {
            is ScanningState.Inactive -> {
                repo.createInitializingState(state)
            }
            is ScanningState.Active, is ScanningState.NoHardware -> state.remain()
        }
    },
    deinitChangeStateWithRepo = { state, repo ->
        when (state) {
            is ScanningState.Active -> repo.createDeinitializingState(state)
            is ScanningState.Inactive, is ScanningState.NoHardware -> state.remain()
        }
    },
    firstState = createNotInitializedState
)

open class ScanningStateImplRepo(
    createScanner: () -> Scanner,
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate,
    private val contextCreator: CoroutineContext.(String) -> CoroutineContext = { this + singleThreadDispatcher(it) },
) : BaseScanningStateRepo(
    createNotInitializedState = { ScanningStateImpl.NotInitialized },
    createInitializingState = { state ->
        when (val stateImpl = state as ScanningStateImpl.Inactive) {
            is ScanningStateImpl.NotInitialized -> {
                val scanner = createScanner()
                (this as ScanningStateImplRepo).startMonitoringScanner(scanner)
                stateImpl.startInitializing(scanner)
            }
            is ScanningStateImpl.Deinitialized -> {
                (this as ScanningStateImplRepo).startMonitoringScanner(stateImpl.scanner)
                stateImpl.reinitialize
            }
        }
    },
    createDeinitializingState = { state ->
        (this as ScanningStateImplRepo).superVisorJob.cancelChildren()
        (state as ScanningStateImpl.Active).deinitialize
    }
) {

    private val superVisorJob = SupervisorJob(coroutineContext[Job])
    private fun startMonitoringScanner(scanner: Scanner) {
        CoroutineScope(coroutineContext + superVisorJob).launch {
            scanner.events.collect { event ->
                when (event) {
                    is Scanner.Event.PermissionChanged -> handlePermissionChangedEvent(event, scanner)
                    is Scanner.Event.BluetoothDisabled -> takeAndChangeState(remainIfStateNot = ScanningState.Enabled::class) { it.disable }
                    is Scanner.Event.BluetoothEnabled -> takeAndChangeState(remainIfStateNot = ScanningState.NoBluetooth.Disabled::class) { it.enable }
                    is Scanner.Event.FailedScanning -> takeAndChangeState(remainIfStateNot = ScanningState.Enabled.Scanning::class) { it.stopScanning }
                    is Scanner.Event.DeviceDiscovered -> handleDeviceDiscovered(event)
                    is Scanner.Event.DeviceConnected -> handleDeviceConnectionChanged(event.identifier, true)
                    is Scanner.Event.DeviceDisconnected -> handleDeviceConnectionChanged(event.identifier, false)
                }
            }
        }
    }

    private suspend fun handlePermissionChangedEvent(event: Scanner.Event.PermissionChanged, scanner: Scanner) = takeAndChangeState { state ->
        when (state) {
            is ScanningState.Initializing -> {
                state.initialized(event.hasPermission, scanner.isHardwareEnabled())
            }
            is ScanningState.Permitted -> {
                if (event.hasPermission)
                    state.remain()
                else
                    state.revokePermission
            }
            is ScanningState.NoBluetooth.MissingPermissions -> if (event.hasPermission) state.permit(scanner.isHardwareEnabled()) else state.remain()
            else -> { state.remain() }
        }
    }

    private suspend fun handleDeviceDiscovered(event: Scanner.Event.DeviceDiscovered) = takeAndChangeState(remainIfStateNot = ScanningState.Enabled.Scanning::class) { state ->
        state.discoverDevice(event.identifier, event.rssi, event.advertisementData) {
            event.deviceCreator(
                coroutineContext.contextCreator("Device ${event.identifier.stringValue}")
            )
        }
    }

    private suspend fun handleDeviceConnectionChanged(identifier: Identifier, connected: Boolean) = useState { state ->
        if (state is ScanningState.Enabled) {
            state.discovered.devices.find { it.identifier == identifier }?.let { device ->
                val connectionManager = device.first().connectionManager
                if (connected)
                    connectionManager.handleConnect()
                else
                    connectionManager.handleDisconnect()
            }
        }
    }
}

class ScanningStateRepo(
    settingsBuilder: (CoroutineContext) -> BaseScanner.Settings,
    builder: BaseScanner.Builder,
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate,
    contextCreator: CoroutineContext.(String) -> CoroutineContext = { this + singleThreadDispatcher(it) },
) : ScanningStateImplRepo(
    createScanner = {
        builder.create(
            settingsBuilder(coroutineContext.contextCreator("BluetoothPermissions")),
            CoroutineScope(coroutineContext.contextCreator("BluetoothScanner"))
        )
    },
    coroutineContext = coroutineContext,
    contextCreator = contextCreator
)
