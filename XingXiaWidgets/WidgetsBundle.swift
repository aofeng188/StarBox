import WidgetKit
import SwiftUI

@main
struct XingXiaWidgetsBundle: WidgetBundle {
    var body: some Widget {
        FortuneWidget()
        HoroscopeWidget()
        CountdownWidget()
        QuickToolControl()
    }
}
