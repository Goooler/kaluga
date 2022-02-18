/*
 Copyright 2021 Splendo Consulting B.V. The Netherlands

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

package com.splendo.kaluga.scientific.converter.angularVelocity

import com.splendo.kaluga.base.utils.Decimal
import com.splendo.kaluga.scientific.DefaultScientificValue
import com.splendo.kaluga.scientific.PhysicalQuantity
import com.splendo.kaluga.scientific.ScientificValue
import com.splendo.kaluga.scientific.byMultiplying
import com.splendo.kaluga.scientific.unit.AngularAcceleration
import com.splendo.kaluga.scientific.unit.AngularVelocity
import com.splendo.kaluga.scientific.unit.Time
import kotlin.jvm.JvmName

@JvmName("angularVelocityFromAngularAccelerationAndTimeDefault")
fun <
    TimeUnit : Time,
    > AngularVelocity.velocity(
    acceleration: ScientificValue<PhysicalQuantity.AngularAcceleration, AngularAcceleration>,
    time: ScientificValue<PhysicalQuantity.Time, TimeUnit>
) = velocity(acceleration, time, ::DefaultScientificValue)

@JvmName("angularVelocityFromAngularAccelerationAndTime")
fun <
    TimeUnit : Time,
    Value : ScientificValue<PhysicalQuantity.AngularVelocity, AngularVelocity>
    > AngularVelocity.velocity(
    acceleration: ScientificValue<PhysicalQuantity.AngularAcceleration, AngularAcceleration>,
    time: ScientificValue<PhysicalQuantity.Time, TimeUnit>,
    factory: (Decimal, AngularVelocity) -> Value
) = byMultiplying(acceleration, time, factory)
