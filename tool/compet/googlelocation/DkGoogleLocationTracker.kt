/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */
package tool.compet.googlelocation

import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import tool.compet.core.DkLogcats

/**
 * Differentiate with [tool.compet.location.DkLocationTracker], this location tracker
 * use Google location service to tracking user's location.
 *
 * Ref: https://github.com/android/location-samples
 */
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

	private var locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000)
		.setMinUpdateIntervalMillis(5_000)
		.setMaxUpdateDelayMillis(15_000)
		.build()

	init {
		listeners.add(listener)
		locationProviderClient = LocationServices.getFusedLocationProviderClient(host)
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

	fun checkLocationSetting(
		context: Context,
		onDisabled: (e: Exception) -> Unit = {},
		onEnabled: () -> Unit = {}
	) {
		val settingsClient = LocationServices.getSettingsClient(context)
		val builder = LocationSettingsRequest
			.Builder()
			.addLocationRequest(locationRequest)

		val gpsSettingTask = settingsClient.checkLocationSettings(builder.build())
		gpsSettingTask.addOnSuccessListener { onEnabled() }
		gpsSettingTask.addOnFailureListener(onDisabled)

		//// For failure case:
		//if (e is ResolvableApiException) {
		//	try {
		//		val intentSenderRequest = IntentSenderRequest
		//			.Builder(e.resolution)
		//			.build()
		//
		//		onDisabled(intentSenderRequest)
		//	}
		//	catch (sendEx: IntentSender.SendIntentException) {
		//		// ignore here
		//	}
		//}
	}

	/**
	 * Request last known location of user.
	 */
	@RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
	fun requestLastLocation(host: Activity) {
		locationProviderClient.lastLocation.addOnCompleteListener(host) { task ->
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

	/**
	 * Start location updates.
	 */
	@RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
	fun start(interval: Long = 5_000, fastestInterval: Long = 3_000, maxUpdateDelayMillis: Long = 10_000) {
		locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, interval)
			.setMinUpdateIntervalMillis(fastestInterval)
			.setMaxUpdateDelayMillis(maxUpdateDelayMillis)
			.build()

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
}