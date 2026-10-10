import CoreLocation
import MapKit

/// 获取一次当前位置，并搜索附近的餐饮店。
@MainActor
final class LocationService: NSObject, CLLocationManagerDelegate {
    static let shared = LocationService()

    private let manager = CLLocationManager()
    private var continuation: CheckedContinuation<CLLocation, Error>?

    enum LocationError: LocalizedError {
        case denied, unavailable

        var errorDescription: String? {
            switch self {
            case .denied: "没有定位权限。请在「设置 ▸ 隐私与安全性 ▸ 定位服务」中允许星匣使用位置。"
            case .unavailable: "暂时无法获取位置，请稍后再试。"
            }
        }
    }

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    var isDenied: Bool {
        manager.authorizationStatus == .denied || manager.authorizationStatus == .restricted
    }

    func currentLocation() async throws -> CLLocation {
        if isDenied { throw LocationError.denied }
        return try await withCheckedThrowingContinuation { continuation in
            self.continuation?.resume(throwing: LocationError.unavailable)
            self.continuation = continuation
            if manager.authorizationStatus == .notDetermined {
                manager.requestWhenInUseAuthorization()
            } else {
                manager.requestLocation()
            }
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            switch manager.authorizationStatus {
            case .authorizedWhenInUse, .authorizedAlways:
                if self.continuation != nil { manager.requestLocation() }
            case .denied, .restricted:
                self.continuation?.resume(throwing: LocationError.denied)
                self.continuation = nil
            default: break
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        Task { @MainActor in
            self.continuation?.resume(returning: location)
            self.continuation = nil
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in
            self.continuation?.resume(throwing: LocationError.unavailable)
            self.continuation = nil
        }
    }

    /// 附近 radius 米内的餐饮 POI
    func nearbyRestaurants(radius: CLLocationDistance = 1500) async throws -> (CLLocation, [MKMapItem]) {
        let location = try await currentLocation()
        let request = MKLocalPointsOfInterestRequest(center: location.coordinate, radius: radius)
        request.pointOfInterestFilter = MKPointOfInterestFilter(including: [.restaurant, .cafe, .bakery])
        var items = (try? await MKLocalSearch(request: request).start().mapItems) ?? []
        if items.isEmpty {
            let fallback = MKLocalSearch.Request()
            fallback.naturalLanguageQuery = "美食"
            fallback.region = MKCoordinateRegion(center: location.coordinate, latitudinalMeters: radius * 2, longitudinalMeters: radius * 2)
            items = (try? await MKLocalSearch(request: fallback).start().mapItems) ?? []
        }
        return (location, items)
    }
}
