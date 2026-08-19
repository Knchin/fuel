package fuel.station.ios

import fuel.station.shared.domain.model.Coordinates
import fuel.station.shared.domain.repository.LocationProvider
import CoreLocation
import MapKit
import SwiftUI

// iOS Location Provider implementation using Core Location
// Handles: When In Use authorization, denied authorization, restricted location

class IOSLocationProvider: LocationProvider {

    private let locationManager = CLLocationManager()
    private var continuation: CheckedContinuation<Result<Coordinates>>?

    init {
        // Configure location manager
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.distanceFilter = 100.0 // meters

        // Request When In Use authorization
        locationManager.requestWhenInUseAuthorization()
    }

    deinit {
        locationManager.stopUpdatingLocation()
    }

    suspend func getCurrentLocation(): Result<Coordinates> {
        return withCheckedContinuation { continuation in
            self.continuation = continuation

            // Check authorization status
            let status = locationManager.authorizationStatus

            switch status {
            case .authorizedWhenInUse, .authorizedAlways:
                // Start updating location
                locationManager.startUpdatingLocation()
                // We'll get the result via delegate method
                // For now, use the most recent location
                if let location = locationManager.location {
                    deliverCoordinates(coordinates: location.coordinate)
                }
            case .denied, .restricted:
                // Permission denied or restricted
                deliverError(error: "Location permission denied or restricted")
            case .notDetermined:
                // Prompt for authorization
                locationManager.requestWhenInUseAuthorization()
                // After request, check status again
                // The continuation will be resumed when location is obtained
            @unknown default:
                deliverError(error: "Unknown location authorization status")
            }
        }
    }

    private func deliverCoordinates(coordinates: CLLocationCoordinate2D) {
        continuation?.resume(.success(Coordinates(
            latitude: coordinates.latitude,
            longitude: coordinates.longitude
        )))
        continuation = nil
    }

    private func deliverError(error: String) {
        continuation?.resume(.failure(NSError(domain: "Location", code: 1, userInfo: [NSLocalizedDescriptionKey: error])))
        continuation = nil
    }
}

// UIKit delegate for location updates (simplified)
class LocationManagerDelegate: NSObject, CLLocationManagerDelegate {

    private var continuation: CheckedContinuation<Result<Coordinates>>?

    func locationManager(_ manager: CLLocationManager,
                         didChangeAuthorization status: CLAuthorizationStatus) {

        switch status {
        case .authorizedWhenInUse, .authorizedAlways:
            // Start updating location
            manager.startUpdatingLocation()
        case .denied, .restricted:
            // Permission denied
            continuation?.resume(.failure(NSError(domain: "Location", code: 1)))
            continuation = nil
        case .notDetermined:
            // Continue waiting
            break
        @unknown default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager,
                         didUpdateLocations locations: [CLLocation]) {
        if let location = locations.last {
            deliverCoordinates(coordinates: location.coordinate)
            manager.stopUpdatingLocation()
        }
    }

    func locationManager(_ manager: CLLocationManager,
                         didFailWithError error: Error) {
        deliverError(error: error.localizedDescription)
        manager.stopUpdatingLocation()
    }

    private func deliverCoordinates(coordinates: CLLocationCoordinate2D) {
        continuation?.resume(.success(Coordinates(
            latitude: coordinates.latitude,
            longitude: coordinates.longitude
        )))
        continuation = nil
    }

    private func deliverError(error: String) {
        continuation?.resume(.failure(NSError(domain: "Location", code: 1, userInfo: [NSLocalizedDescriptionKey: error])))
        continuation = nil
    }
}