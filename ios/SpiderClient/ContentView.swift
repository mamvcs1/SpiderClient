import SwiftUI
import NetworkExtension

struct ContentView: View {
    @State private var shareLink = ""
    @State private var status = "Not connected"
    @State private var isConnected = false
    @State private var isBusy = false
    @State private var errorMessage: String?

    private let red = Color(red: 1.0, green: 0.12, blue: 0.22)

    var body: some View {
        ZStack {
            Color(red: 0.045, green: 0.035, blue: 0.05).ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    HStack(spacing: 13) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 15)
                                .fill(red.opacity(0.16))
                                .frame(width: 54, height: 54)
                            Image(systemName: "spider")
                                .font(.system(size: 27, weight: .bold))
                                .foregroundStyle(red)
                        }
                        VStack(alignment: .leading, spacing: 4) {
                            Text("SPIDER CLIENT")
                                .font(.system(size: 22, weight: .black, design: .rounded))
                                .tracking(1.1)
                            Text("PRIVATE CONNECTION")
                                .font(.system(size: 10, weight: .semibold))
                                .tracking(2.0)
                                .foregroundStyle(.white.opacity(0.55))
                        }
                        Spacer()
                    }
                    .padding(.top, 24)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Your connection")
                            .font(.system(size: 29, weight: .bold, design: .rounded))
                        Text("Import a VLESS, VMess, Trojan or supported Xray share link.")
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.62))
                    }

                    VStack(spacing: 15) {
                        Image(systemName: isConnected ? "checkmark.shield.fill" : "shield.lefthalf.filled")
                            .font(.system(size: 48))
                            .foregroundStyle(isConnected ? .green : red)
                            .padding(.top, 8)
                        Text(status.uppercased())
                            .font(.system(size: 14, weight: .bold, design: .rounded))
                            .tracking(1.8)
                        Text(isConnected ? "Your VPN tunnel is active." : "Your traffic is not currently routed through Spider.")
                            .font(.footnote)
                            .foregroundStyle(.white.opacity(0.55))
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 25)
                    .background(Color.white.opacity(0.045), in: RoundedRectangle(cornerRadius: 25))
                    .overlay(RoundedRectangle(cornerRadius: 25).stroke(Color.white.opacity(0.08), lineWidth: 1))

                    VStack(alignment: .leading, spacing: 10) {
                        Text("SERVER CONFIGURATION")
                            .font(.system(size: 11, weight: .bold))
                            .tracking(1.4)
                            .foregroundStyle(red)
                        TextEditor(text: $shareLink)
                            .scrollContentBackground(.hidden)
                            .font(.system(.footnote, design: .monospaced))
                            .foregroundStyle(.white)
                            .frame(minHeight: 125)
                            .padding(8)
                            .background(Color.black.opacity(0.28), in: RoundedRectangle(cornerRadius: 15))
                            .overlay(alignment: .topLeading) {
                                if shareLink.isEmpty {
                                    Text("Paste your server share link here…")
                                        .font(.system(.footnote, design: .monospaced))
                                        .foregroundStyle(.white.opacity(0.28))
                                        .padding(.horizontal, 13)
                                        .padding(.vertical, 17)
                                        .allowsHitTesting(false)
                                }
                            }
                        Button {
                            Task { await toggleConnection() }
                        } label: {
                            HStack(spacing: 10) {
                                if isBusy { ProgressView().tint(.white) }
                                else { Image(systemName: isConnected ? "power" : "bolt.fill") }
                                Text(isBusy ? "PLEASE WAIT…" : (isConnected ? "DISCONNECT" : "CONNECT"))
                                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                                    .tracking(1.5)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 17)
                            .foregroundStyle(.white)
                            .background(isConnected ? Color.white.opacity(0.13) : red, in: RoundedRectangle(cornerRadius: 16))
                        }
                        .disabled(isBusy || (!isConnected && shareLink.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty))
                    }
                    .padding(18)
                    .background(Color.white.opacity(0.035), in: RoundedRectangle(cornerRadius: 23))

                    Text("VPN access requires an iOS build signed with the Network Extension entitlement. This source project does not itself install a profile or grant Apple capabilities.")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.42))
                        .padding(.bottom, 20)
                }
                .padding(.horizontal, 20)
            }
        }
        .task { await refreshStatus() }
        .onReceive(NotificationCenter.default.publisher(for: .NEVPNStatusDidChange)) { _ in
            Task { await refreshStatus() }
        }
        .alert("Spider Client", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    @MainActor
    private func refreshStatus() async {
        do {
            let manager = try await loadManager()
            switch manager.connection.status {
            case .connected:
                isConnected = true
                status = "Connected"
            case .connecting, .reasserting:
                isConnected = false
                status = "Connecting…"
            case .disconnecting:
                isConnected = false
                status = "Disconnecting…"
            default:
                isConnected = false
                status = "Not connected"
            }
        } catch {
            isConnected = false
            status = "Not connected"
        }
    }

    @MainActor
    private func toggleConnection() async {
        isBusy = true
        defer { isBusy = false }
        do {
            let manager = try await loadManager()
            if isConnected {
                manager.connection.stopVPNTunnel()
                isConnected = false
                status = "Disconnecting…"
                return
            }

            let link = shareLink.trimmingCharacters(in: .whitespacesAndNewlines)
            guard ["vless://", "vmess://", "trojan://", "ss://", "socks://"].contains(where: { link.lowercased().hasPrefix($0) }) else {
                throw ClientError.invalidLink
            }

            let tunnel = NETunnelProviderProtocol()
            tunnel.providerBundleIdentifier = "com.spiderclient.ios.tunnel"
            tunnel.serverAddress = "Spider Client"
            tunnel.providerConfiguration = ["shareLink": link]
            manager.protocolConfiguration = tunnel
            manager.localizedDescription = "Spider Client"
            manager.isEnabled = true
            try await save(manager)
            try manager.connection.startVPNTunnel()
            status = "Connecting…"
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func loadManager() async throws -> NETunnelProviderManager {
        let managers = try await NETunnelProviderManager.loadAllFromPreferences()
        return managers.first ?? NETunnelProviderManager()
    }

    private func save(_ manager: NETunnelProviderManager) async throws {
        try await withCheckedThrowingContinuation { continuation in
            manager.saveToPreferences { error in
                if let error { continuation.resume(throwing: error) }
                else { continuation.resume() }
            }
        }
    }

    private enum ClientError: LocalizedError {
        case invalidLink
        var errorDescription: String? {
            switch self {
            case .invalidLink:
                return "This doesn't look like a supported Xray share link. Check the link and try again."
            }
        }
    }
}
