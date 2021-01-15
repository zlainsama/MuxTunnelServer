package me.lain.muxtun.mipo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.FingerprintTrustManagerFactory;
import me.lain.muxtun.Shared;
import me.lain.muxtun.mipo.config.adapter.PathAdapter;
import me.lain.muxtun.mipo.config.adapter.SocketAddressAdapter;
import me.lain.muxtun.mipo.config.adapter.UUIDAdapter;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public final class MirrorPointConfig {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(SocketAddress.class, new SocketAddressAdapter())
            .registerTypeAdapter(UUID.class, new UUIDAdapter())
            .registerTypeAdapter(Path.class, new PathAdapter())
            .create();

    private SocketAddress bindAddress;
    private Map<UUID, SocketAddress> targetAddresses;
    private Path pathCert;
    private Path pathKey;
    private List<String> trusts;
    private List<String> ciphers;
    private List<String> protocols;

    public static SslContext buildContext(Path pathCert, Path pathKey, List<String> trustSha256s, List<String> ciphers, List<String> protocols) throws IOException {
        return buildContext(pathCert, pathKey, trustSha256s, "SHA-256", ciphers, protocols);
    }

    public static SslContext buildContext(Path pathCert, Path pathKey, List<String> trusts, String algorithm, List<String> ciphers, List<String> protocols) throws IOException {
        return SslContextBuilder.forServer(Files.newInputStream(pathCert, StandardOpenOption.READ), Files.newInputStream(pathKey, StandardOpenOption.READ))
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(FingerprintTrustManagerFactory.builder(algorithm).fingerprints(trusts).build())
                .ciphers(!ciphers.isEmpty() ? ciphers : !Shared.TLS.defaultCipherSuites.isEmpty() ? Shared.TLS.defaultCipherSuites : null, SupportedCipherSuiteFilter.INSTANCE)
                .protocols(!protocols.isEmpty() ? protocols : !Shared.TLS.defaultProtocols.isEmpty() ? Shared.TLS.defaultProtocols : null)
                .build();
    }

    public static MirrorPointConfig fromJson(String json) {
        return gson.fromJson(json, MirrorPointConfig.class);
    }

    public static List<MirrorPointConfig> fromJsonList(String json) {
        return gson.fromJson(json, new TypeToken<ArrayList<MirrorPointConfig>>() {
        }.getType());
    }

    public static Map<String, MirrorPointConfig> fromJsonMap(String json) {
        return gson.fromJson(json, new TypeToken<HashMap<String, MirrorPointConfig>>() {
        }.getType());
    }

    public static String toJson(MirrorPointConfig config) {
        return gson.toJson(config);
    }

    public static String toJsonList(List<MirrorPointConfig> configList) {
        return gson.toJson(configList);
    }

    public static String toJsonMap(Map<String, MirrorPointConfig> configMap) {
        return gson.toJson(configMap);
    }

    public SocketAddress getBindAddress() {
        return bindAddress;
    }

    public MirrorPointConfig setBindAddress(SocketAddress bindAddress) {
        this.bindAddress = bindAddress;
        return this;
    }

    public Map<UUID, SocketAddress> getTargetAddresses() {
        return targetAddresses;
    }

    public MirrorPointConfig setTargetAddresses(Map<UUID, SocketAddress> targetAddresses) {
        this.targetAddresses = targetAddresses;
        return this;
    }

    public Path getPathCert() {
        return pathCert;
    }

    public MirrorPointConfig setPathCert(Path pathCert) {
        this.pathCert = pathCert;
        return this;
    }

    public Path getPathKey() {
        return pathKey;
    }

    public MirrorPointConfig setPathKey(Path pathKey) {
        this.pathKey = pathKey;
        return this;
    }

    public List<String> getTrusts() {
        return trusts;
    }

    public MirrorPointConfig setTrusts(List<String> trusts) {
        this.trusts = trusts;
        return this;
    }

    public List<String> getCiphers() {
        return ciphers;
    }

    public MirrorPointConfig setCiphers(List<String> ciphers) {
        this.ciphers = ciphers;
        return this;
    }

    public List<String> getProtocols() {
        return protocols;
    }

    public MirrorPointConfig setProtocols(List<String> protocols) {
        this.protocols = protocols;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirrorPointConfig that = (MirrorPointConfig) o;
        return Objects.equals(bindAddress, that.bindAddress) && Objects.equals(targetAddresses, that.targetAddresses) && Objects.equals(pathCert, that.pathCert) && Objects.equals(pathKey, that.pathKey) && Objects.equals(trusts, that.trusts) && Objects.equals(ciphers, that.ciphers) && Objects.equals(protocols, that.protocols);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindAddress, targetAddresses, pathCert, pathKey, trusts, ciphers, protocols);
    }

    @Override
    public String toString() {
        return "MirrorPointConfig{" +
                "bindAddress=" + bindAddress +
                ", targetAddresses=" + targetAddresses +
                ", pathCert=" + pathCert +
                ", pathKey=" + pathKey +
                ", trusts=" + trusts +
                ", ciphers=" + ciphers +
                ", protocols=" + protocols +
                '}';
    }

}
