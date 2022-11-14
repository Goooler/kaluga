plugins {
    kotlin("multiplatform")
    id("jacoco")
    id("convention.publication")
    id("com.android.library")
    id("org.jlleitschuh.gradle.ktlint")
    id("kotlinx-atomicfu")
}

publishableComponent()

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":test-utils-base"))
                api(project(":architecture"))
            }
        }
        commonTest {
            dependencies { }
        }
    }
}
