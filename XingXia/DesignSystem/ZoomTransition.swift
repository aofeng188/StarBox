import SwiftUI

/// 从格子、卡片点进页面时的缩放转场（matchedTransitionSource + navigationTransition(.zoom)）。
/// 每个 tab 的 NavigationStack 上挂 zoomTransitionRoot()，来源视图用 zoomSource(_:)，目的页在 withAppRoutes 里统一加 zoomDestination(_:)。
/// 同一页面上一个路由只标一个来源；没有来源的推入（列表行、深链接）照常推入。
private struct ZoomNamespaceKey: EnvironmentKey {
    static let defaultValue: Namespace.ID? = nil
}

extension EnvironmentValues {
    var zoomNamespace: Namespace.ID? {
        get { self[ZoomNamespaceKey.self] }
        set { self[ZoomNamespaceKey.self] = newValue }
    }
}

private struct ZoomRootModifier: ViewModifier {
    @Namespace private var namespace

    func body(content: Content) -> some View {
        content.environment(\.zoomNamespace, namespace)
    }
}

private struct ZoomSourceModifier: ViewModifier {
    let route: AppRoute
    @Environment(\.zoomNamespace) private var namespace

    func body(content: Content) -> some View {
        if let namespace {
            content.matchedTransitionSource(id: route, in: namespace)
        } else {
            content
        }
    }
}

private struct ZoomDestinationModifier: ViewModifier {
    let route: AppRoute
    @Environment(\.zoomNamespace) private var namespace

    func body(content: Content) -> some View {
        if let namespace {
            content.navigationTransition(.zoom(sourceID: route, in: namespace))
        } else {
            content
        }
    }
}

extension View {
    /// 挂在每个 tab 的 NavigationStack 上
    func zoomTransitionRoot() -> some View {
        modifier(ZoomRootModifier())
    }

    /// 点进 route 的格子、卡片：推入时从这里放大
    func zoomSource(_ route: AppRoute) -> some View {
        modifier(ZoomSourceModifier(route: route))
    }

    /// route 对应的目的页
    func zoomDestination(_ route: AppRoute) -> some View {
        modifier(ZoomDestinationModifier(route: route))
    }
}
