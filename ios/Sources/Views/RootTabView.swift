import SwiftUI

struct RootTabView: View {
    var body: some View {
        TabView {
            NavigationStack { AlarmListView() }
                .tabItem { Label("알람", systemImage: "alarm.fill") }

            NavigationStack { PetView() }
                .tabItem { Label("펫", systemImage: "pawprint.fill") }

            NavigationStack { ReportView() }
                .tabItem { Label("리포트", systemImage: "chart.bar.xaxis") }
        }
    }
}
