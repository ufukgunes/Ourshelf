import SwiftUI
import UIKit
import ComposeApp

/// Ortak Compose arayüzünü (Kotlin) SwiftUI içinde gösterir.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose kendi kenar boşluklarını (status bar, home indicator) yönetir.
        ComposeView().ignoresSafeArea(.all)
    }
}
