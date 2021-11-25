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

package com.splendo.kaluga.scientific.unit

import kotlin.test.Test
import kotlin.test.assertEquals

class MolarMassUnitTest {

    @Test
    fun metricMolarMassConversionTest() {
        assertScientificConversion(1.0, (Kilogram per Mole), 0.001, Tonne per Mole)
    }

    @Test
    fun imperialMolarMassConversionTest() {
        assertScientificConversion(1.0, (Pound per Mole), 16.0, Ounce per Mole)
    }

    @Test
    fun ukImperialMolarMassConversionTest() {
        assertScientificConversion(1.0, (ImperialTon per Mole), 2240.0, Pound per Mole)
    }

    @Test
    fun usCustomaryMolarMassConversionTest() {
        assertScientificConversion(1.0, (UsTon per Mole), 2000.0, Pound per Mole)
    }
}
