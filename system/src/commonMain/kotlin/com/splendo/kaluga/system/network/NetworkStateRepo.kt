/*
 Copyright 2020 Splendo Consulting B.V. The Netherlands

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

package com.splendo.kaluga.system.network

import com.splendo.kaluga.base.MainQueueDispatcher
import com.splendo.kaluga.state.ColdStateRepo
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkStateRepo(
    context: Any?,
    coroutineContext: CoroutineContext = MainQueueDispatcher
) : ColdStateRepo<NetworkState>(coroutineContext = coroutineContext) {

    private val networkManager: BaseNetworkManager = NetworkManager(this, context)

    override suspend fun deinitialize(state: NetworkState) { }

    override suspend fun initialValue(): NetworkState {
        return NetworkState.Available(Network.Wifi(), networkManager)
    }
}

fun Flow<NetworkState>.network(): Flow<Network> {
    return this.map { it.networkType }
}
