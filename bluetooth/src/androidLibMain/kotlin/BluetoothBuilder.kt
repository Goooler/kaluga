package com.splendo.kaluga.bluetooth

import android.content.Context
import com.splendo.kaluga.base.ApplicationHolder
import com.splendo.kaluga.bluetooth.scanner.BaseScanner
import com.splendo.kaluga.bluetooth.scanner.DefaultScanner
import com.splendo.kaluga.permissions.base.PermissionContext
import com.splendo.kaluga.permissions.base.Permissions
import com.splendo.kaluga.permissions.base.PermissionsBuilder
import com.splendo.kaluga.permissions.bluetooth.registerBluetoothPermission
import com.splendo.kaluga.permissions.location.registerLocationPermission
import kotlin.coroutines.CoroutineContext

actual class BluetoothBuilder(
    private val applicationContext: Context = ApplicationHolder.applicationContext,
    private val permissionsBuilder: (CoroutineContext) -> Permissions = { context ->
        Permissions(
            PermissionsBuilder(PermissionContext(applicationContext)).apply {
                registerBluetoothPermission()
                registerLocationPermission()
            },
            coroutineContext = context
        )
    },
    private val scannerBuilder: BaseScanner.Builder = DefaultScanner.Builder(applicationContext = applicationContext)
) : Bluetooth.Builder {

    override fun create(
        scannerSettingsBuilder: (Permissions) -> BaseScanner.Settings,
        coroutineContext: CoroutineContext,
        contextCreator: CoroutineContext.(String) -> CoroutineContext
    ): Bluetooth = Bluetooth({ scannerContext ->
        scannerSettingsBuilder(permissionsBuilder(scannerContext))
    },
        scannerBuilder,
        coroutineContext,
        contextCreator
    )
}
