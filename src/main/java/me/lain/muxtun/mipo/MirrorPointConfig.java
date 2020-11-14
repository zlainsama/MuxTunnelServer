package me.lain.muxtun.mipo;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.FingerprintTrustManagerFactory;
import me.lain.muxtun.Shared;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MirrorPointConfig {

    private final SocketAddress bindAddress;
    private final Map<UUID, SocketAddress> targetAddresses;
    private final SslContext sslCtx;
    private final String name;

    public MirrorPointConfig(SocketAddress bindAddress, Map<UUID, SocketAddress> targetAddresses, SslContext sslCtx) {
        this(bindAddress, targetAddresses, sslCtx, "MirrorPoint");
    }

    public MirrorPointConfig(SocketAddress bindAddress, Map<UUID, SocketAddress> targetAddresses, SslContext sslCtx, String name) {
        if (bindAddress == null || targetAddresses == null || sslCtx == null || name == null)
            throw new NullPointerException();
        if (targetAddresses.isEmpty() || !sslCtx.isServer() || name.isEmpty())
            throw new IllegalArgumentException();

        this.bindAddress = bindAddress;
        this.targetAddresses = targetAddresses;
        this.sslCtx = sslCtx;
        this.name = name;
    }

    public static SslContext buildContext(Path pathCert, Path pathKey, List<String> trustSha256, List<String> ciphers, List<String> protocols) throws IOException {
        return SslContextBuilder.forServer(Files.newInputStream(pathCert, StandardOpenOption.READ), Files.newInputStream(pathKey, StandardOpenOption.READ))
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(FingerprintTrustManagerFactory.builder("SHA-256").fingerprints(trustSha256).build())
                .ciphers(!ciphers.isEmpty() ? ciphers : !Shared.TLS.defaultCipherSuites.isEmpty() ? Shared.TLS.defaultCipherSuites : null, SupportedCipherSuiteFilter.INSTANCE)
                .protocols(!protocols.isEmpty() ? protocols : !Shared.TLS.defaultProtocols.isEmpty() ? Shared.TLS.defaultProtocols : null)
                .build();
    }

    public SocketAddress getBindAddress() {
        return bindAddress;
    }

    public String getName() {
        return name;
    }

    public SslContext getSslCtx() {
        return sslCtx;
    }

    public Map<UUID, SocketAddress> getTargetAddresses() {
        return targetAddresses;
    }

}
