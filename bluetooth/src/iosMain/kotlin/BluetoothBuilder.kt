package com.splendo.kaluga.bluetooth

import com.splendo.kaluga.bluetooth.scanner.BaseScanner
import com.splendo.kaluga.bluetooth.scanner.DefaultScanner
import com.splendo.kaluga.permissions.base.Permissions
import com.splendo.kaluga.permissions.base.PermissionsBuilder
import com.splendo.kaluga.permissions.bluetooth.registerBluetoothPermission
import platform.Foundation.NSBundle
import kotlin.coroutines.CoroutineContext

actual class BluetoothBuilder(
    private val bundle: NSBundle = NSBundle.mainBundle,
    private val permissionsBuilder: (CoroutineContext) -> Permissions = { context ->
        Permissions(
            PermissionsBuilder(bundle).apply {
                registerBluetoothPermission()
            },
            context
        )
    },
    private val scannerBuilder: DefaultScanner.Builder = DefaultScanner.Builder()
) : Bluetooth.Builder {

    override fun create(
        scannerSettingsBuilder: (Permissions) -> BaseScanner.Settings,
        coroutineContext: CoroutineContext,
        contextCreator: CoroutineContext.(String) -> CoroutineContext
    ): Bluetooth = Bluetooth(
        { scannerContext ->
            scannerSettingsBuilder(permissionsBuilder(scannerContext))
        },
        scannerBuilder,
        coroutineContext,
        contextCreator
    )
}
