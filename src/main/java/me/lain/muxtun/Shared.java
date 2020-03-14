package me.lain.muxtun;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.net.ssl.SSLContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public final class Shared
{

    public static final class ConcurrentDigests
    {

        private static final Map<String, Queue<MessageDigest>> m = new HashMap<>();

        static
        {
//          init0("MD5");
//          init0("SHA-1");
            init0("SHA-256");
            init0("SHA3-256");
        }

        public static boolean contains(String algorithm)
        {
            return m.containsKey(algorithm);
        }

        public static byte[] digest(String algorithm, byte[]... input)
        {
            return digest(algorithm, 1, input);
        }

        public static byte[] digest(String algorithm, int rounds, byte[]... input)
        {
            Queue<MessageDigest> q = m.get(algorithm);
            if (q == null)
                throw new IllegalStateException();
            MessageDigest md = q.poll();
            if (md == null)
            {
                try
                {
                    md = MessageDigest.getInstance(algorithm);
                }
                catch (NoSuchAlgorithmException e)
                {
                    throw new IllegalStateException();
                }
            }
            for (byte[] bytes : input)
                md.update(bytes);
            byte[] result = md.digest();
            if (rounds > 1)
                for (int i = 1; i < rounds; i++)
                    result = md.digest(result);
            q.add(md);
            return result;
        }

        public static void init(String algorithm) throws NoSuchAlgorithmException
        {
            synchronized (m)
            {
                if (!m.containsKey(algorithm))
                {
                    MessageDigest md = MessageDigest.getInstance(algorithm);
                    Queue<MessageDigest> q = new ConcurrentLinkedQueue<>();
                    q.add(md);
                    m.put(algorithm, q);
                }
            }
        }

        private static void init0(String algorithm)
        {
            try
            {
                init(algorithm);
            }
            catch (NoSuchAlgorithmException e)
            {
            }
        }

        private ConcurrentDigests()
        {
        }

    }

    public static final class NettyObjects
    {

        public static final EventLoopGroup bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : KQueue.isAvailable() ? new KQueueEventLoopGroup(1) : new NioEventLoopGroup(1);
        public static final EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : KQueue.isAvailable() ? new KQueueEventLoopGroup() : new NioEventLoopGroup();
        public static final Class<? extends SocketChannel> classSocketChannel = Epoll.isAvailable() ? EpollSocketChannel.class : KQueue.isAvailable() ? KQueueSocketChannel.class : NioSocketChannel.class;
        public static final Class<? extends DatagramChannel> classDatagramChannel = Epoll.isAvailable() ? EpollDatagramChannel.class : KQueue.isAvailable() ? KQueueDatagramChannel.class : NioDatagramChannel.class;
        public static final Class<? extends ServerSocketChannel> classServerSocketChannel = Epoll.isAvailable() ? EpollServerSocketChannel.class : KQueue.isAvailable() ? KQueueServerSocketChannel.class : NioServerSocketChannel.class;

        private NettyObjects()
        {
        }

    }

    public static final class TLS
    {

        public static final List<String> defaultProtocols;
        public static final List<String> defaultCipherSuites;

        static
        {
            List<String> protocols = new ArrayList<>(Arrays.asList("TLSv1.3:TLSv1.2".split(":")));
            List<String> cipherSuites = new ArrayList<>(Arrays.asList("TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256:TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256".split(":")));
            try
            {
                protocols.retainAll(Arrays.asList(SSLContext.getDefault().getSupportedSSLParameters().getProtocols()));
                cipherSuites.retainAll(Arrays.asList(SSLContext.getDefault().getSupportedSSLParameters().getCipherSuites()));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                defaultProtocols = Collections.unmodifiableList(protocols);
                defaultCipherSuites = Collections.unmodifiableList(cipherSuites);
            }
        }

        private TLS()
        {
        }

    }

    public static final byte[] magic = "a8a2be845c22bd60f105cd710649bcfabca1dd156f58112fb28dc5f289422a19".getBytes(StandardCharsets.UTF_8);
    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final OutputStream voidStream = new OutputStream()
    {

        @Override
        public void close() throws IOException
        {
        }

        @Override
        public void flush() throws IOException
        {
        }

        @Override
        public void write(byte b[]) throws IOException
        {
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException
        {
        }

        @Override
        public void write(int b) throws IOException
        {
        }

    };

    public static byte[] digestSHA256(byte[]... input)
    {
        return ConcurrentDigests.digest("SHA-256", input);
    }

    public static byte[] digestSHA256(int rounds, byte[]... input)
    {
        return ConcurrentDigests.digest("SHA-256", rounds, input);
    }

    public static byte[] digestSHA256_3(byte[]... input)
    {
        return ConcurrentDigests.digest("SHA3-256", input);
    }

    public static byte[] digestSHA256_3(int rounds, byte[]... input)
    {
        return ConcurrentDigests.digest("SHA3-256", rounds, input);
    }

    public static boolean isSHA3Available()
    {
        return ConcurrentDigests.contains("SHA3-256");
    }

    public static String printNow()
    {
        return dateFormat.format(LocalDateTime.now());
    }

    private Shared()
    {
    }

}
