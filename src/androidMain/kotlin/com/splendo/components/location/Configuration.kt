package com.splendo.components.location

//import android.app.Application
//import android.content.Context

actual data class Configuration (
    val context: String//Context
) {
    actual companion object {
        actual val default = Configuration(context = "context")
    }
}

//private var instance: Application? = null
//private fun Application.onCreate() {
//    instance = this
//    println(" 🗿 - app onCreate")
//}