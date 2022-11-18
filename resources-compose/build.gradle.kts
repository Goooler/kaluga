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

plugins {
    id("com.android.library")
    kotlin("android")
    id("jacoco")
    id("convention.publication")
    id("org.jlleitschuh.gradle.ktlint")
}

composeAndroidComponent()

dependencies {
    implementation(project(":base"))
    api(project(":resources"))
    implementationDependency(Dependencies.AndroidX.Compose.Foundation)
    implementationDependency(Dependencies.AndroidX.Compose.Material)
    implementationDependency(Dependencies.AndroidX.Compose.UI)
    implementationDependency(Dependencies.AndroidX.Compose.UITooling)
    implementationDependency(Dependencies.KotlinX.Coroutines.Core)
}
