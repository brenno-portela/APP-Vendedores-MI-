import Shared
import SwiftUI

struct ContentView: View {
    private let sharedInfo = SharedAppInfo()

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 24) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(sharedInfo.appName())
                        .font(.largeTitle)
                        .fontWeight(.semibold)

                    Text("Shell iOS inicial")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 12) {
                    StatusRow(title: "Modulo compartilhado", value: sharedInfo.migrationStatus())
                    StatusRow(title: "Plataformas", value: sharedInfo.supportedPlatforms())
                    StatusRow(title: "Proximo passo", value: "Portar login e regras de negocio")
                }

                Spacer()
            }
            .padding(24)
            .navigationTitle("Rotas")
        }
    }
}

private struct StatusRow: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption)
                .textCase(.uppercase)
                .foregroundStyle(.secondary)

            Text(value)
                .font(.body)
                .fontWeight(.medium)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

#Preview {
    ContentView()
}
