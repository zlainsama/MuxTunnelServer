package me.lain.muxtun;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Shared {

    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final OutputStream voidStream = new OutputStream() {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void write(byte b[]) throws IOException {
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
        }

        @Override
        public void write(int b) throws IOException {
        }

    };

    private Shared() {
    }

    public static String printNow() {
        return dateFormat.format(LocalDateTime.now());
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static final class NettyObjects {

        public static final Class<? extends SocketChannel> classSocketChannel = Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
        public static final Class<? extends DatagramChannel> classDatagramChannel = Epoll.isAvailable() ? EpollDatagramChannel.class : NioDatagramChannel.class;
        public static final Class<? extends ServerSocketChannel> classServerSocketChannel = Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
        private static final Map<String, EventLoopGroup> eventLoopGroups = new ConcurrentHashMap<>();
        private static final Map<String, EventLoopGroup> eventLoopGroupsView = Collections.unmodifiableMap(eventLoopGroups);
        private static final Map<String, EventExecutorGroup> eventExecutorGroups = new ConcurrentHashMap<>();
        private static final Map<String, EventExecutorGroup> eventExecutorGroupsView = Collections.unmodifiableMap(eventExecutorGroups);

        private NettyObjects() {
        }

        public static Map<String, EventExecutorGroup> getEventExecutorGroups() {
            return eventExecutorGroupsView;
        }

        public static Map<String, EventLoopGroup> getEventLoopGroups() {
            return eventLoopGroupsView;
        }

        public static EventExecutorGroup getOrCreateEventExecutorGroup(String name, int nThreads) {
            boolean[] created = new boolean[]{false};
            EventExecutorGroup group = eventExecutorGroups.computeIfAbsent(name, unused -> {
                created[0] = true;
                return new DefaultEventExecutorGroup(nThreads, new DefaultThreadFactory(name));
            });

            if (created[0]) {
                group.terminationFuture().addListener(future -> {
                    if (future.isSuccess())
                        eventExecutorGroups.remove(name, group);
                });
            }
            return group;
        }

        public static EventLoopGroup getOrCreateEventLoopGroup(String name, int nThreads) {
            boolean[] created = new boolean[]{false};
            EventLoopGroup group = eventLoopGroups.computeIfAbsent(name, unused -> {
                created[0] = true;
                return Epoll.isAvailable() ? new EpollEventLoopGroup(nThreads, new DefaultThreadFactory(name)) : new NioEventLoopGroup(nThreads, new DefaultThreadFactory(name));
            });

            if (created[0]) {
                group.terminationFuture().addListener(future -> {
                    if (future.isSuccess())
                        eventLoopGroups.remove(name, group);
                });
            }
            return group;
        }

        public static Collection<Future<?>> shutdownGracefully() {
            return Stream.concat(getEventLoopGroups().values().stream().map(EventLoopGroup::shutdownGracefully), getEventExecutorGroups().values().stream().map(EventExecutorGroup::shutdownGracefully)).collect(Collectors.toList());
        }

    }

    public static final class RoundRobinSupplier<T> implements Supplier<T> {

        private final List<Supplier<T>> suppliers;
        private final AtomicInteger index;

        private RoundRobinSupplier(Collection<Supplier<T>> suppliers) {
            this.suppliers = Collections.unmodifiableList(new ArrayList<>(suppliers));
            this.index = new AtomicInteger();
        }

        public static <T> RoundRobinSupplier<T> of(Collection<Supplier<T>> suppliers) {
            return new RoundRobinSupplier<>(suppliers);
        }

        @Override
        public T get() {
            return suppliers.get(Math.abs(index.getAndIncrement() % suppliers.size())).get();
        }

    }

    public static final class TLS {

        public static final List<String> defaultProtocols;
        public static final List<String> defaultCipherSuites;

        static {
            List<String> protocols = new ArrayList<>(Arrays.asList("TLSv1.3:TLSv1.2".split(":")));
            List<String> cipherSuites = new ArrayList<>(Arrays.asList("TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256:TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256".split(":")));
            try {
                protocols.retainAll(Arrays.asList(SSLContext.getDefault().getSupportedSSLParameters().getProtocols()));
                cipherSuites.retainAll(Arrays.asList(SSLContext.getDefault().getSupportedSSLParameters().getCipherSuites()));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                defaultProtocols = Collections.unmodifiableList(protocols);
                defaultCipherSuites = Collections.unmodifiableList(cipherSuites);
            }
        }

        private TLS() {
        }

    }

}
