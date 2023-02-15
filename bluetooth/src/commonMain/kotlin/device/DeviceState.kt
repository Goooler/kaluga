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

import com.splendo.kaluga.base.state.HandleAfterOldStateIsRemoved
import com.splendo.kaluga.base.state.KalugaState
import com.splendo.kaluga.bluetooth.MTU
import com.splendo.kaluga.bluetooth.Service
import kotlinx.coroutines.CompletableDeferred

/**
 * An action a [Device] can execute on one of its [com.splendo.kaluga.bluetooth.Attribute]
 */
sealed class DeviceAction {

    /**
     * A Deferred that will be completed with
     * `true` if [DeviceAction] was completed successfully, or
     * `false` if [DeviceAction] failed
     * */
    val completedSuccessfully = CompletableDeferred<Boolean>()

    /**
     * A [DeviceAction] that attempts to read an [com.splendo.kaluga.bluetooth.Attribute]
     */
    sealed class Read : DeviceAction() {
        /**
         * A [DeviceAction.Read] on a [com.splendo.kaluga.bluetooth.Characteristic]
         * @property characteristic the [com.splendo.kaluga.bluetooth.Characteristic] to read the value of
         */
        class Characteristic(val characteristic: com.splendo.kaluga.bluetooth.Characteristic) : Read()

        /**
         * A [DeviceAction.Read] on a [com.splendo.kaluga.bluetooth.Descriptor]
         * @property descriptor the [com.splendo.kaluga.bluetooth.Descriptor] to read the value of
         */
        class Descriptor(val descriptor: com.splendo.kaluga.bluetooth.Descriptor) : Read()
    }

    /**
     * A [DeviceAction] that attempts to write a value to an [com.splendo.kaluga.bluetooth.Attribute]
     * @property newValue the [ByteArray] to write
     */
    sealed class Write(val newValue: ByteArray) : DeviceAction() {

        /**
         * A [DeviceAction.Write] on a [com.splendo.kaluga.bluetooth.Characteristic]
         * @param newValue the [ByteArray] to write
         * @property characteristic the [com.splendo.kaluga.bluetooth.Characteristic] to read the value of
         */
        class Characteristic(newValue: ByteArray, val characteristic: com.splendo.kaluga.bluetooth.Characteristic) : Write(newValue)

        /**
         * A [DeviceAction.Write] on a [com.splendo.kaluga.bluetooth.Descriptor]
         * @param newValue the [ByteArray] to write
         * @property descriptor the [com.splendo.kaluga.bluetooth.Descriptor] to read the value of
         */
        class Descriptor(newValue: ByteArray, val descriptor: com.splendo.kaluga.bluetooth.Descriptor) : Write(newValue)
    }

    /**
     * A [DeviceAction] that updates that notifying status of a [com.splendo.kaluga.bluetooth.Characteristic]
     * @property characteristic the [com.splendo.kaluga.bluetooth.Characteristic] to notify
     */
    sealed class Notification(val characteristic: com.splendo.kaluga.bluetooth.Characteristic) : DeviceAction() {

        /**
         * A [Notification] that starts notifying
         * @param characteristic the [com.splendo.kaluga.bluetooth.Characteristic] to notify
         */
        class Enable(characteristic: com.splendo.kaluga.bluetooth.Characteristic) : Notification(characteristic)

        /**
         * A [Notification] that stops notifying
         * @param characteristic the [com.splendo.kaluga.bluetooth.Characteristic] to no longer notify
         */
        class Disable(characteristic: com.splendo.kaluga.bluetooth.Characteristic) : Notification(characteristic)
    }
}

/**
 * The state of a [Device]
 */
sealed interface DeviceState

/**
 * A [DeviceState] indicating the [Device] cannot be connected to
 */
interface NotConnectableDeviceState : DeviceState

/**
 * A [DeviceState] for a [Device] that can be connected to
 */
sealed interface ConnectableDeviceState : DeviceState, KalugaState {

    /**
     * A [ConnectableDeviceState] where the [Device] is connected
     */
    sealed interface Connected : ConnectableDeviceState {

        /**
         * A [Connected] State where no [Service] has been discovered yet
         */
        interface NoServices : Connected {

            /**
             * Attempts to start discovering the list of [Service] of the [Device]
             */
            fun startDiscovering()

            /**
             * Transitions into a [Discovering] State
             */
            val discoverServices: suspend () -> Discovering
        }

        /**
         * A [Connected] State where the device is discovering the list of [Service]
         */
        interface Discovering : Connected {

            /**
             * Discovers a list of [Service] to transition into [Idle]
             * @param services the list of [Service] discovered
             * @return a transition into a [Idle] State
             */
            fun didDiscoverServices(services: List<Service>): suspend () -> Idle
        }

        /**
         * A [Connected] State indicating all [Service] have been discovered
         */
        sealed interface DiscoveredServices : Connected {
            /**
             * The list of [Service] hat where discovered
             */
            val services: List<Service>
        }

        /**
         * A [DiscoveredServices] State where no [DeviceAction] is currently being executed
         */
        interface Idle : DiscoveredServices {

            /**
             * Starts handling a [DeviceAction]
             * @param action the [DeviceAction] to execute
             * @return a transition into a [HandlingAction] state
             */
            fun handleAction(action: DeviceAction): suspend () -> HandlingAction
        }

        /**
         * A [DiscoveredServices] State where a [DeviceAction] is being executed
         */
        interface HandlingAction : DiscoveredServices {

            /**
             * The [DeviceAction] currently being executed
             */
            val action: DeviceAction

            /**
             * The list of [DeviceAction] to be executed when [action] has been handled
             */
            val nextActions: List<DeviceAction>

            /**
             * Adds an additional [DeviceAction] to the [nextActions]
             * @param newAction the [DeviceAction] to add to the queue
             * @return a transition into [HandlingAction] with [newAction] added to [nextActions]
             */
            fun addAction(newAction: DeviceAction): suspend () -> HandlingAction

            /**
             * Transitions into [DiscoveredServices] when [action] has completed
             */
            val actionCompleted: suspend () -> DiscoveredServices
        }

        /**
         * The current [MTU] size of the device
         */
        val mtu: MTU?

        /**
         * Starts transitioning to a [Disconnected] State
         */
        fun startDisconnected()

        /**
         * Transitions into a [Reconnecting] State
         */
        val reconnect: suspend () -> Reconnecting

        /**
         * Updates the [MTU] size of the device
         * @param mtu the new [MTU] value
         * @return a transition into a [Connected] State with the new MTU
         */
        fun didUpdateMtu(mtu: MTU): suspend () -> Connected

        /**
         * Reads the RSSI
         */
        suspend fun readRssi()

        /**
         * Requests an update to the [MTU] size of the device
         * @param mtu the new [MTU] size to request
         * @return `true` if the MTU update has been requested successfully
         */
        suspend fun requestMtu(mtu: MTU): Boolean

        /**
         * Attempts to pair this device
         */
        suspend fun pair()
    }

    /**
     * A [ConnectableDeviceState] where the [Device] is connecting
     */
    interface Connecting : ConnectableDeviceState {

        /**
         * Transitions into a [Disconnecting] State
         */
        val cancelConnection: suspend () -> Disconnecting

        /**
         * Transitions into a [Connected.NoServices] State
         */
        val didConnect: suspend () -> Connected.NoServices

        /**
         * Starts cancelling the connection
         */
        fun handleCancel()
    }

    /**
     * A [ConnectableDeviceState] where the [Device] is reconnecting after the connection was dropped
     */
    interface Reconnecting : ConnectableDeviceState {

        /**
         * The number of attempts to reconnect that have been made since the disconnect occurred
         */
        val attempt: Int

        /**
         * The list of [Service] discovered
         */
        val services: List<Service>?

        /**
         * Fail this attempt and transition into a new [Reconnecting] State
         */
        val raiseAttempt: suspend () -> Reconnecting

        /**
         * Transition into a [Connected] State
         */
        val didConnect: suspend () -> Connected

        /**
         * Transition into a [Disconnecting] State
         */
        val cancelConnection: suspend () -> Disconnecting

        /**
         * Starts cancelling the reconnection
         */
        fun handleCancel()

        /**
         * Retries according to [ConnectionSettings.ReconnectionSettings] and transitions into a new [ConnectableDeviceState]
         * @param reconnectionSettings the [ConnectionSettings.ReconnectionSettings] to use for reconnecting
         * @return a transition into the next [ConnectableDeviceState]. Can be either [Disconnected] or [Reconnecting].
         */
        fun retry(reconnectionSettings: ConnectionSettings.ReconnectionSettings): suspend () -> ConnectableDeviceState = when (reconnectionSettings) {
            is ConnectionSettings.ReconnectionSettings.Always -> raiseAttempt
            is ConnectionSettings.ReconnectionSettings.Never -> didDisconnect
            is ConnectionSettings.ReconnectionSettings.Limited -> {
                if (attempt + 1 < reconnectionSettings.attempts) {
                    raiseAttempt
                } else {
                    didDisconnect
                }
            }
        }
    }

    /**
     * A [ConnectableDeviceState] where the [Device] is disconnected
     */
    interface Disconnected : ConnectableDeviceState {

        /**
         * Transitions into a [Connecting] State
         */
        val connect: suspend () -> Connecting

        /**
         * Starts connecting the device
         */
        fun startConnecting()
    }

    /**
     * A [ConnectableDeviceState] where the [Device] is disconnecting
     */
    interface Disconnecting : ConnectableDeviceState

    /**
     * Transitions into a [Disconnected] State
     */
    val didDisconnect: suspend () -> Disconnected

    /**
     * Transitions into a [Disconnecting] State
     */
    val disconnecting: suspend () -> Disconnecting

    /**
     * Attempts to unpair the Device
     */
    suspend fun unpair()
}

internal object NotConnectableDeviceStateImpl : NotConnectableDeviceState

internal sealed class ConnectableDeviceStateImpl {

    protected abstract val deviceConnectionManager: DeviceConnectionManager

    sealed class Connected : ConnectableDeviceStateImpl() {

        data class NoServices constructor(
            override val mtu: MTU?,
            override val deviceConnectionManager: DeviceConnectionManager
        ) : Connected(), ConnectableDeviceState.Connected.NoServices {

            override fun startDiscovering() {
                deviceConnectionManager.startDiscovering()
            }

            override val discoverServices = suspend {
                Discovering(mtu, deviceConnectionManager)
            }

            override fun didUpdateMtu(mtu: MTU) = suspend { copy(mtu = mtu) }
        }

        data class Discovering constructor(
            override val mtu: MTU?,
            override val deviceConnectionManager: DeviceConnectionManager
        ) : Connected(),
            ConnectableDeviceState.Connected.Discovering,
            HandleAfterOldStateIsRemoved<ConnectableDeviceState> {

            override fun didDiscoverServices(services: List<Service>): suspend () -> Idle {
                return { Idle(mtu, services, deviceConnectionManager) }
            }

            override fun didUpdateMtu(mtu: MTU) = suspend { copy(mtu = mtu) }

            override suspend fun afterOldStateIsRemoved(oldState: ConnectableDeviceState) {
                deviceConnectionManager.discoverServices()
            }
        }

        data class Idle constructor(
            override val mtu: MTU?,
            override val services: List<Service>,
            override val deviceConnectionManager: DeviceConnectionManager
        ) : Connected(), ConnectableDeviceState.Connected.Idle {

            override fun handleAction(action: DeviceAction) = suspend { HandlingAction(action, emptyList(), mtu, services, deviceConnectionManager) }
            override fun didUpdateMtu(mtu: MTU) = suspend { copy(mtu = mtu) }
        }

        data class HandlingAction constructor(
            override val action: DeviceAction,
            override val nextActions: List<DeviceAction>,
            override val mtu: MTU?,
            override val services: List<Service>,
            override val deviceConnectionManager: DeviceConnectionManager
        ) : Connected(), ConnectableDeviceState.Connected.HandlingAction, HandleAfterOldStateIsRemoved<ConnectableDeviceState> {

            override fun addAction(newAction: DeviceAction) = suspend { HandlingAction(action, listOf(*nextActions.toTypedArray(), newAction), mtu, services, deviceConnectionManager) }
            override fun didUpdateMtu(mtu: MTU) = suspend { copy(mtu = mtu) }

            override val actionCompleted: suspend () -> ConnectableDeviceState.Connected.DiscoveredServices = suspend {
                when (action) {
                    is DeviceAction.Read.Characteristic -> action.characteristic.updateValue()
                    is DeviceAction.Read.Descriptor -> action.descriptor.updateValue()
                    is DeviceAction.Write.Characteristic -> action.characteristic.updateValue()
                    is DeviceAction.Write.Descriptor -> action.descriptor.updateValue()
                    is DeviceAction.Notification.Enable -> action.characteristic.updateValue()
                    is DeviceAction.Notification.Disable -> { }
                }
                if (nextActions.isEmpty()) {
                    Idle(mtu, services, deviceConnectionManager)
                } else {
                    val nextAction = nextActions.first()
                    val remainingActions = nextActions.drop(1)
                    HandlingAction(nextAction, remainingActions, mtu, services, deviceConnectionManager)
                }
            }

            override suspend fun afterOldStateIsRemoved(oldState: ConnectableDeviceState) {
                if (oldState is HandlingAction && oldState.action == action)
                    return
                deviceConnectionManager.performAction(action)
            }
        }

        fun startDisconnected() = deviceConnectionManager.startDisconnecting()

        val reconnect = suspend {
            // All services, characteristics and descriptors become invalidated after it disconnects
            Reconnecting(0, null, deviceConnectionManager)
        }

        suspend fun readRssi() {
            deviceConnectionManager.readRssi()
        }

        suspend fun requestMtu(mtu: MTU): Boolean {
            return deviceConnectionManager.requestMtu(mtu)
        }

        suspend fun pair() = deviceConnectionManager.pair()
    }

    data class Connecting constructor(
        override val deviceConnectionManager: DeviceConnectionManager
    ) : ConnectableDeviceStateImpl(), ConnectableDeviceState.Connecting, HandleAfterOldStateIsRemoved<ConnectableDeviceState> {

        override val cancelConnection = disconnecting

        override fun handleCancel() = deviceConnectionManager.cancelConnecting()

        override val didConnect = suspend {
            Connected.NoServices(null, deviceConnectionManager)
        }

        override suspend fun afterOldStateIsRemoved(oldState: ConnectableDeviceState) {
            when (oldState) {
                is Disconnected -> deviceConnectionManager.connect()
                else -> {
                    // do nothing: TODO check all these are correct, e.g. Disconnecting
                }
            }
        }
    }

    data class Reconnecting constructor(
        override val attempt: Int,
        override val services: List<Service>?,
        override val deviceConnectionManager: DeviceConnectionManager
    ) : ConnectableDeviceStateImpl(), ConnectableDeviceState.Reconnecting, HandleAfterOldStateIsRemoved<ConnectableDeviceState> {

        override fun handleCancel() = deviceConnectionManager.cancelConnecting()

        override val cancelConnection = disconnecting

        override val raiseAttempt = suspend { Reconnecting(attempt + 1, services, deviceConnectionManager) }
        override val didConnect: suspend () -> ConnectableDeviceState.Connected = suspend {
            services?.let { Connected.Idle(null, services, deviceConnectionManager) } ?: Connected.NoServices(null, deviceConnectionManager)
        }

        override suspend fun afterOldStateIsRemoved(oldState: ConnectableDeviceState) {
            when (oldState) {
                is Connected, is Reconnecting -> deviceConnectionManager.connect()
                else -> {}
            }
        }
    }

    data class Disconnected constructor(
        override val deviceConnectionManager: DeviceConnectionManager
    ) : ConnectableDeviceStateImpl(), ConnectableDeviceState.Disconnected {

        override fun startConnecting() = deviceConnectionManager.startConnecting()

        override val connect = suspend { Connecting(deviceConnectionManager) }
    }

    data class Disconnecting constructor(
        override val deviceConnectionManager: DeviceConnectionManager
    ) : ConnectableDeviceStateImpl(), ConnectableDeviceState.Disconnecting, HandleAfterOldStateIsRemoved<ConnectableDeviceState> {

        override suspend fun afterOldStateIsRemoved(oldState: ConnectableDeviceState) {
            when (oldState) {
                is Connecting, is Reconnecting, is Connected -> deviceConnectionManager.disconnect()
                else -> {}
            }
        }
    }

    val didDisconnect = suspend {
        Disconnected(deviceConnectionManager)
    }

    val disconnecting = suspend {
        Disconnecting(deviceConnectionManager)
    }

    suspend fun unpair() = deviceConnectionManager.unpair()
}
