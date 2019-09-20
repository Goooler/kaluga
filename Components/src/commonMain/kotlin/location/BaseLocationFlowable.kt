package com.splendo.mpp.location

import com.splendo.mpp.flow.BaseFlowable
import com.splendo.mpp.location.Location.UnknownReason
import com.splendo.mpp.log.debug

open class BaseLocationFlowable : BaseFlowable<Location>() {

    private var lastLocation: Location.KnownLocation? = null

    suspend fun set(location: Location.KnownLocation) {
        lastLocation = location
        super.set(location)
    }

    suspend fun setUnknownLocation(reason: UnknownReason = UnknownReason.NOT_CLEAR) {
        val location = lastLocation?.let {
            Location.UnknownLocationWithLastLocation(it, reason)
        } ?: Location.UnknownLocationWithNoLastLocation(reason)
        debug(TAG, "Send to channel: $location")
        set(location)
        debug(TAG, "unknown location sent")
    }

    companion object {
        const val TAG = "BaseLocationFlowable"
    }

}