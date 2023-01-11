/*
 Copyright 2023 Splendo Consulting B.V. The Netherlands

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

package com.splendo.kaluga.example.shared.model.scientific.converters

import com.splendo.kaluga.scientific.DefaultScientificValue
import com.splendo.kaluga.scientific.PhysicalQuantity
import com.splendo.kaluga.scientific.converter.frequency.times
import com.splendo.kaluga.scientific.unit.Abfarad
import com.splendo.kaluga.scientific.unit.Abhenry
import com.splendo.kaluga.scientific.unit.ElectricCapacitance
import com.splendo.kaluga.scientific.unit.ElectricInductance
import com.splendo.kaluga.scientific.unit.Frequency

val PhysicalQuantity.Frequency.converters get() = listOf<QuantityConverter<PhysicalQuantity.Frequency, *, *>>(
    QuantityConverter("Electric Conductance from Electric Capacitance", QuantityConverter.Type.Multiplication, PhysicalQuantity.ElectricCapacitance) { (leftValue, leftUnit), (rightValue, rightUnit) ->
        when {
            leftUnit is Frequency && rightUnit is Abfarad -> DefaultScientificValue(leftValue, leftUnit) * DefaultScientificValue(rightValue, rightUnit)
            leftUnit is Frequency && rightUnit is ElectricCapacitance -> DefaultScientificValue(leftValue, leftUnit) * DefaultScientificValue(rightValue, rightUnit)
            else -> throw RuntimeException("Unexpected units: $leftUnit, $rightUnit")
        }
    },
    QuantityConverter("Electric Resistance from Electric Inductance", QuantityConverter.Type.Multiplication, PhysicalQuantity.ElectricInductance) { (leftValue, leftUnit), (rightValue, rightUnit) ->
        when {
            leftUnit is Frequency && rightUnit is Abhenry -> DefaultScientificValue(leftValue, leftUnit) * DefaultScientificValue(rightValue, rightUnit)
            leftUnit is Frequency && rightUnit is ElectricInductance -> DefaultScientificValue(leftValue, leftUnit) * DefaultScientificValue(rightValue, rightUnit)
            else -> throw RuntimeException("Unexpected units: $leftUnit, $rightUnit")
        }
    }
)
