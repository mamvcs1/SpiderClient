import Foundation
import Network
import NetworkExtension
import Darwin
import LibXray

final class PacketTunnelProvider: NEPacketTunnelProvider {
    private var started = false

    override func startTunnel(options: [String : NSObject]?,
                              completionHandler: @escaping (Error?) -> Void) {
        guard
            let proto = protocolConfiguration as? NETunnelProviderProtocol,
            let config = proto.providerConfiguration,
            let shareLink = config["shareLink"] as? String,
            !shareLink.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            completionHandler(TunnelError.missingConfiguration)
            return
        }

        let settings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: "127.0.0.1")
        let ipv4 = NEIPv4Settings(addresses: ["198.18.0.1"], subnetMasks: ["255.255.255.0"])
        ipv4.includedRoutes = [NEIPv4Route.default()]
        settings.ipv4Settings = ipv4
        settings.dnsSettings = NEDNSSettings(servers: ["1.1.1.1", "1.0.0.1"])
        settings.mtu = 1500

        setTunnelNetworkSettings(settings) { [weak self] error in
            guard let self else {
                completionHandler(TunnelError.startupFailed("Tunnel provider was released."))
                return
            }
            if let error {
                completionHandler(error)
                return
            }

            do {
                let tunFD = try self.findUtunFileDescriptor()
                let converted = try self.invoke([
                    "apiVersion": 3,
                    "method": "convertShareLinksToXrayJson",
                    "payload": ["text": shareLink]
                ])
                guard
                    let data = converted["data"] as? [String: Any],
                    let outbounds = data["outbounds"] as? [[String: Any]],
                    !outbounds.isEmpty
                else {
                    throw TunnelError.startupFailed("libXray could not parse the server link.")
                }

                let xrayConfig: [String: Any] = [
                    "log": ["loglevel": "warning"],
                    "env": ["xray.tun.fd": String(tunFD)],
                    "inbounds": [[
                        "tag": "spider-tun-in",
                        "listen": "127.0.0.1",
                        "port": 0,
                        "protocol": "tun",
                        "settings": ["name": "spider0", "mtu": 1500]
                    ]],
                    "outbounds": outbounds
                ]
                let response = try self.invoke([
                    "apiVersion": 3,
                    "method": "runXray",
                    "payload": ["xrayJson": try self.jsonString(xrayConfig)]
                ])
                guard (response["success"] as? Bool) == true else {
                    throw TunnelError.startupFailed(response["error"] as? String ?? "Xray failed to start.")
                }
                self.started = true
                completionHandler(nil)
            } catch {
                completionHandler(error)
            }
        }
    }

    override func stopTunnel(with reason: NEProviderStopReason,
                             completionHandler: @escaping () -> Void) {
        if started {
            _ = try? invoke([
                "apiVersion": 3,
                "method": "stopXray",
                "payload": [String: String]()
            ])
            started = false
        }
        completionHandler()
    }

    private func invoke(_ request: [String: Any]) throws -> [String: Any] {
        let data = try JSONSerialization.data(withJSONObject: request, options: [.sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw TunnelError.startupFailed("Could not encode Xray request.")
        }
        let responsePointer = json.withCString { pointer in
            CGoInvoke(UnsafeMutablePointer(mutating: pointer))
        }
        guard let responsePointer else {
            throw TunnelError.startupFailed("libXray returned no response.")
        }
        let responseString = String(cString: responsePointer)
        CGoFree(responsePointer)
        guard let responseData = responseString.data(using: .utf8),
              let response = try JSONSerialization.jsonObject(with: responseData) as? [String: Any]
        else {
            throw TunnelError.startupFailed("libXray returned invalid JSON.")
        }
        if (response["success"] as? Bool) == false {
            throw TunnelError.startupFailed(response["error"] as? String ?? "libXray request failed.")
        }
        return response
    }

    private func jsonString(_ object: [String: Any]) throws -> String {
        let data = try JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])
        guard let string = String(data: data, encoding: .utf8) else {
            throw TunnelError.startupFailed("Could not encode Xray configuration.")
        }
        return string
    }

    private func findUtunFileDescriptor() throws -> Int32 {
        var buffer = [CChar](repeating: 0, count: 32)
        for fd in 0...1024 {
            var length = socklen_t(buffer.count)
            if getsockopt(Int32(fd), 2, 2, &buffer, &length) == 0,
               String(cString: buffer).hasPrefix("utun") {
                return Int32(fd)
            }
        }
        throw TunnelError.startupFailed("Could not locate the iOS utun file descriptor.")
    }

    private enum TunnelError: LocalizedError {
        case missingConfiguration
        case startupFailed(String)

        var errorDescription: String? {
            switch self {
            case .missingConfiguration:
                return "No server configuration was provided."
            case .startupFailed(let message):
                return message
            }
        }
    }
}
