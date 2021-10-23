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

package com.splendo.kaluga.scientific

import com.splendo.kaluga.base.utils.Decimal
import kotlinx.serialization.Serializable

@Serializable
sealed class AmountOfSubstance : AbstractScientificUnit<MeasurementType.AmountOfSubstance>(), MetricAndImperialScientificUnit<MeasurementType.AmountOfSubstance>

@Serializable
object Mole : AmountOfSubstance(), MetricBaseUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance> {
    override val symbol = "mol"
    override val system = MeasurementSystem.MetricAndImperial
    override val type = MeasurementType.AmountOfSubstance
    override fun fromSIUnit(value: Decimal): Decimal = value
    override fun toSIUnit(value: Decimal): Decimal = value
}

@Serializable
object NanoMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Nano(Mole)
@Serializable
object MicroMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Micro(Mole)
@Serializable
object MilliMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Milli(Mole)
@Serializable
object CentiMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Centi(Mole)
@Serializable
object DeciMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Deci(Mole)
@Serializable
object DecaMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Deca(Mole)
@Serializable
object HectoMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Hecto(Mole)
@Serializable
object KiloMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Kilo(Mole)
@Serializable
object MegaMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Mega(Mole)
@Serializable
object GigaMole : AmountOfSubstance(), MetricMultipleUnit<MeasurementSystem.MetricAndImperial, MeasurementType.AmountOfSubstance, Mole> by Giga(Mole)