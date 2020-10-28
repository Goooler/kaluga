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

package com.splendo.kaluga.test.utils

import com.splendo.kaluga.architecture.observable.Observable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

const val DefaultTimeout = 2000L

suspend fun <T> List<T>.awaitElementAt(
    index: Int,
    timeOut: Long = DefaultTimeout
): T = withTimeout(timeOut) {
    while (size <= index) {
        delay(100)
    }
    get(index)
}

suspend fun <T> awaitEquals(
    expected: T,
    timeOut: Long = DefaultTimeout,
    current: (() -> T)
) = withTimeout(timeOut) {
    while (expected != current()) {
        delay(100)
    }
}

expect suspend fun <T> Observable<T>.awaitCurrentValue(
    timeOut: Long
): T

suspend fun <T> Observable<T>.awaitCurrentValue() = awaitCurrentValue(DefaultTimeout)
