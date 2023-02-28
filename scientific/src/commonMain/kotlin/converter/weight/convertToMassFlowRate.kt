/*
 Copyright 2022 Splendo Consulting B.V. The Netherlands

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

package com.splendo.kaluga.scientific.converter.weight

import com.splendo.kaluga.scientific.PhysicalQuantity
import com.splendo.kaluga.scientific.ScientificValue
import com.splendo.kaluga.scientific.converter.massFlowRate.massFlowRate
import com.splendo.kaluga.scientific.unit.ImperialWeight
import com.splendo.kaluga.scientific.unit.Kilogram
import com.splendo.kaluga.scientific.unit.MetricWeight
import com.splendo.kaluga.scientific.unit.Time
import com.splendo.kaluga.scientific.unit.UKImperialWeight
import com.splendo.kaluga.scientific.unit.USCustomaryWeight
import com.splendo.kaluga.scientific.unit.Weight
import com.splendo.kaluga.scientific.unit.per
import kotlin.jvm.JvmName

@JvmName("metricWeightDivTime")
infix operator fun <WeightUnit : MetricWeight, TimeUnit : Time> ScientificValue<PhysicalQuantity.Weight, WeightUnit>.div(
    time: ScientificValue<PhysicalQuantity.Time, TimeUnit>
) = (unit per time.unit).massFlowRate(this, time)

@JvmName("imperialWeightDivTime")
infix operator fun <WeightUnit : ImperialWeight, TimeUnit : Time> ScientificValue<PhysicalQuantity.Weight, WeightUnit>.div(
    time: ScientificValue<PhysicalQuantity.Time, TimeUnit>
) = (unit per time.unit).massFlowRate(this, time)

@JvmName("ukImperialWeightDivTime")
infix operator fun <WeightUnit : UKImperialWeight, TimeUnit : Time> ScientificValue<PhysicalQuantity.Weight, WeightUnit>.div(
    time: ScientificValue<PhysicalQuantity.Time, TimeUnit>
) = (unit per time.unit).massFlowRate(this, time)

@JvmName("usCustomaryWeightDivTime")
infix operator fun <WeightUnit : USCustomaryWeight, TimeUnit : Time> ScientificValue<PhysicalQuantity.Weight, WeightUnit>.div(
    time: ScientificValue<PhysicalQuantity.Time, TimeUnit>
) = (unit per time.unit).massFlowRate(this, time)

@JvmName("weightDivTime")
infix operator fun <WeightUnit : Weight, TimeUnit : Time> ScientificValue<PhysicalQuantity.Weight, WeightUnit>.div(
    time: ScientificValue<PhysicalQuantity.Time, TimeUnit>
) = (Kilogram per time.unit).massFlowRate(this, time)
