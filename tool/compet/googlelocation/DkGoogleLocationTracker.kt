/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */
package tool.compet.googlelocation

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import tool.compet.core.DkConst
import tool.compet.core.DkLogcats
import tool.compet.core.DkUtils

/**
 * Differentiate with [tool.compet.location.DkLocationTracker], this location tracker
 * use Google location service to tracking user's location.
 *
 * Ref: https://github.com/android/location-samples
 */
@SuppressLint("MissingPermission")
class DkGoogleLocationTracker(host: Activity, listener: Listener) {
	interface Listener {
		fun onLocationAvailability(result: LocationAvailability)
		fun onLocationResult(locations: List<Location>)

		fun onLastLocationUpdated(location: Location)
		fun onLocationChanged(location: Location)
	}

	private val locationCallback = object : LocationCallback() {
		override fun onLocationAvailability(result: LocationAvailability) {
			for (caller in listeners) {
				caller.onLocationAvailability(result)
			}
		}

		override fun onLocationResult(result: LocationResult) {
			for (caller in listeners) {
				caller.onLocationResult(result.locations)
			}
		}
	}

	private val locationListener = LocationListener { location ->
		for (caller in listeners) {
			caller.onLocationChanged(location)
		}
	}

	private val locationProviderClient: FusedLocationProviderClient
	private val listeners = mutableListOf<Listener>()
	private val locationRequest = LocationRequest.create().apply {
		//this.interval = 30_000 // default 30 s
		//this.fastestInterval = 10_000 // default 10 s
		//this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
	}

	init {
		listeners.add(listener)
		locationProviderClient = LocationServices.getFusedLocationProviderClient(host)
		requestLastLocation(host)
	}

	fun register(listener: Listener): DkGoogleLocationTracker {
		if (!listeners.contains(listener)) {
			listeners.add(listener)
		}
		return this
	}

	fun unregister(listener: Listener): DkGoogleLocationTracker {
		listeners.remove(listener)
		return this
	}

	/**
	 * Start location updates.
	 */
	fun start(host: Activity) {
		if (!DkUtils.checkPermission(host, DkConst.ACCESS_FINE_LOCATION, DkConst.ACCESS_COARSE_LOCATION)) {
			DkLogcats.warning(this, "Skip request location updates since NO permission granted !")
			return
		}
		locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
		locationProviderClient.requestLocationUpdates(locationRequest, locationListener, Looper.getMainLooper())
	}

	/**
	 * Stop location updates.
	 */
	fun stop() {
		locationProviderClient.removeLocationUpdates(locationCallback)
		locationProviderClient.removeLocationUpdates(locationListener)
	}

	/**
	 * Request last known location of user.
	 */
	fun requestLastLocation(host: Activity) {
		if (!DkUtils.checkPermission(host, DkConst.ACCESS_FINE_LOCATION, DkConst.ACCESS_COARSE_LOCATION)) {
			DkLogcats.warning(this, "Skip request last location since NO permission granted !")
			return
		}
		locationProviderClient.lastLocation.addOnCompleteListener(host) { task: Task<Location?> ->
			if (task.isSuccessful) {
				task.result?.let { location ->
					for (caller in listeners) {
						caller.onLastLocationUpdated(location)
					}
				}
			}
			else {
				DkLogcats.warning(this, "Failed to get last location, task: $task")
			}
		}
	}
}