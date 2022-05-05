/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */

package tool.compet.googlelocation;

import android.app.Activity;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import tool.compet.core.DkConst;
import tool.compet.core.DkLogcats;
import tool.compet.core.DkUtils;

/**
 * Differentiate with {@link tool.compet.location.DkLocationTracker}, this location tracker
 * use Google location service to tracking user's location.
 *
 * Ref: https://github.com/android/location-samples
 */
public class DkGoogleLocationTracker extends LocationCallback implements LocationListener {
	public interface Listener {
		void onLastLocationUpdated(Location location);
		void onLocationResult(@NonNull List<Location> locations);
		void onLocationChanged(Location location);
	}

	private final FusedLocationProviderClient locationProviderClient;
	private final LocationRequest locationRequest;
	private final ArrayList<Listener> listeners = new ArrayList<>();

	public DkGoogleLocationTracker(Activity host, Listener listener) {
		final LocationRequest locationRequest = LocationRequest.create();
		locationRequest.setInterval(30_000); // default 30 s
		locationRequest.setFastestInterval(10_000); // default 10 s
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		this.listeners.add(listener);
		this.locationRequest = locationRequest;
		this.locationProviderClient = LocationServices.getFusedLocationProviderClient(host);

		requestLastLocation(host);
	}

	@Override
	public void onLocationResult(@NonNull LocationResult result) {
		for (Listener listener : listeners) {
			listener.onLocationResult(result.getLocations());
		}
	}

	@Override
	public void onLocationChanged(@NonNull Location location) {
		for (Listener listener : listeners) {
			listener.onLocationChanged(location);
		}
	}

	public DkGoogleLocationTracker register(Listener listener) {
		if (! listeners.contains(listener)) {
			listeners.add(listener);
		}
		return this;
	}

	public DkGoogleLocationTracker unregister(Listener listener) {
		listeners.remove(listener);
		return this;
	}

	/**
	 * Start location updates.
	 */
	public void startLocationUpdates(Activity host) {
		if (! DkUtils.checkPermission(host, DkConst.ACCESS_FINE_LOCATION, DkConst.ACCESS_COARSE_LOCATION)) {
			DkLogcats.warning(this, "Skip request location updates since NO permission granted !");
			return;
		}
		locationProviderClient.requestLocationUpdates(locationRequest, this, Looper.getMainLooper());
	}

	/**
	 * Stop location updates.
	 */
	public void stopLocationUpdates() {
		locationProviderClient.removeLocationUpdates(this);
	}

	/**
	 * Request last known location of user.
	 */
	public void requestLastLocation(Activity host) {
		if (! DkUtils.checkPermission(host, DkConst.ACCESS_FINE_LOCATION, DkConst.ACCESS_COARSE_LOCATION)) {
			DkLogcats.warning(this, "Skip request last location since NO permission granted !");
			return;
		}
		locationProviderClient.getLastLocation().addOnCompleteListener(host, (Task<Location> task) -> {
			if (task.isSuccessful() && task.getResult() != null) {
				for (Listener listener : listeners) {
					listener.onLastLocationUpdated(task.getResult());
				}
			}
			else {
				DkLogcats.warning(this, "Failed to get last location, task: " + task);
			}
		});
	}

	/**
	 * Obtain location request instance for more setting, for eg,. interval, fastestInterval, priority,...
	 */
	public LocationRequest getLocationRequest() {
		return locationRequest;
	}
}
