package me.lain.muxtun.mipo;

import java.net.SocketAddress;
import java.util.Map;
import java.util.UUID;
import io.netty.handler.ssl.SslContext;

public class MirrorPointConfig
{

    final SocketAddress bindAddress;
    final Map<UUID, SocketAddress> targetAddresses;
    final SslContext sslCtx;
    final byte[] secret;
    final byte[] secret_3;
    final String name;

    public MirrorPointConfig(SocketAddress bindAddress, Map<UUID, SocketAddress> targetAddresses, SslContext sslCtx, byte[] secret, byte[] secret_3)
    {
        this(bindAddress, targetAddresses, sslCtx, secret, secret_3, "MirrorPoint");
    }

    public MirrorPointConfig(SocketAddress bindAddress, Map<UUID, SocketAddress> targetAddresses, SslContext sslCtx, byte[] secret, byte[] secret_3, String name)
    {
        if (bindAddress == null || targetAddresses == null || sslCtx == null || (secret == null && secret_3 == null) || name == null)
            throw new NullPointerException();
        if (targetAddresses.isEmpty() || !sslCtx.isServer() || (secret != null && secret.length == 0) || (secret_3 != null && secret_3.length == 0) || name.isEmpty())
            throw new IllegalArgumentException();

        this.bindAddress = bindAddress;
        this.targetAddresses = targetAddresses;
        this.sslCtx = sslCtx;
        this.secret = secret;
        this.secret_3 = secret_3;
        this.name = name;
    }

}
