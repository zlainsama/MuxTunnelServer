package me.lain.muxtun;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.util.concurrent.Future;
import me.lain.muxtun.mipo.MirrorPoint;
import me.lain.muxtun.mipo.MirrorPointConfig;
import me.lain.muxtun.util.SimpleLogger;

public class App
{

    private static SocketAddress bindAddress = null;
    private static Map<UUID, SocketAddress> targetAddresses = new HashMap<>();
    private static Path pathCert = null;
    private static Path pathKey = null;
    private static List<String> ciphers = new ArrayList<>();
    private static List<String> protocols = new ArrayList<>();
    private static Path pathSecret = null;
    private static Path pathSecret_3 = null;
    private static SslContext sslCtx = null;
    private static byte[] secret = null;
    private static byte[] secret_3 = null;
    private static MirrorPoint theServer = null;

    private static void discardOut()
    {
        System.setOut(new PrintStream(Shared.voidStream));
        System.setErr(new PrintStream(Shared.voidStream));
    }

    private static byte[] generateSecret(Path path, byte[] magic)
    {
        try (FileChannel fc = FileChannel.open(path, StandardOpenOption.READ))
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            fc.transferTo(0L, Long.MAX_VALUE, Channels.newChannel(new DigestOutputStream(Shared.voidStream, md)));

            return md.digest(magic);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static byte[] generateSecret_3(Path path, byte[] magic)
    {
        try (FileChannel fc = FileChannel.open(path, StandardOpenOption.READ))
        {
            MessageDigest md = MessageDigest.getInstance("SHA3-256");

            fc.transferTo(0L, Long.MAX_VALUE, Channels.newChannel(new DigestOutputStream(Shared.voidStream, md)));

            return md.digest(magic);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static Path init(String... args) throws Exception
    {
        int index = 0;
        boolean nolog = false;
        boolean silent = false;
        Optional<Path> pathLog = Optional.empty();
        Optional<Path> pathConfig = Optional.empty();

        for (String arg : args)
        {
            if (arg.startsWith("-"))
            {
                if ("--nolog".equalsIgnoreCase(arg))
                    nolog = true;
                else if ("--silent".equalsIgnoreCase(arg))
                    silent = true;
            }
            else
            {
                switch (index++)
                {
                    case 0:
                        pathConfig = Optional.of(FileSystems.getDefault().getPath(arg));
                        break;
                    case 1:
                        pathLog = Optional.of(FileSystems.getDefault().getPath(arg));
                        break;
                }
            }
        }

        if (silent)
            discardOut();
        if (!nolog)
            SimpleLogger.setFileOut(pathLog.orElse(FileSystems.getDefault().getPath("MuxTunnel.log")));

        return pathConfig.orElse(FileSystems.getDefault().getPath("MuxTunnel.cfg"));
    }

    public static void main(String[] args) throws Exception
    {
        try (BufferedReader in = Files.newBufferedReader(init(args)))
        {
            in.lines().map(String::trim).filter(App::nonCommentLine).filter(App::validConfigLine).forEach(line -> {
                int i = line.indexOf("=");
                String name = line.substring(0, i).trim();
                String value = line.substring(i + 1).trim();
                if ("bindAddress".equals(name))
                {
                    int i1 = value.lastIndexOf(":");
                    String host = value.substring(0, i1);
                    int port = Integer.parseInt(value.substring(i1 + 1));
                    bindAddress = new InetSocketAddress(host, port);
                }
                else if ("targetAddress".equals(name))
                {
                    int i1 = value.indexOf("=");
                    int i2 = value.lastIndexOf(":");
                    UUID streamId = UUID.fromString(value.substring(0, i1));
                    String host = value.substring(i1 + 1, i2);
                    int port = Integer.parseInt(value.substring(i2 + 1));
                    SocketAddress targetAddress = new InetSocketAddress(host, port);
                    targetAddresses.put(streamId, targetAddress);
                }
                else if ("pathCert".equals(name))
                {
                    pathCert = FileSystems.getDefault().getPath(value);
                }
                else if ("pathKey".equals(name))
                {
                    pathKey = FileSystems.getDefault().getPath(value);
                }
                else if ("ciphers".equals(name))
                {
                    ciphers.addAll(Arrays.asList(value.split(":")));
                }
                else if ("protocols".equals(name))
                {
                    protocols.addAll(Arrays.asList(value.split(":")));
                }
                else if ("pathSecret".equals(name))
                {
                    pathSecret = FileSystems.getDefault().getPath(value);
                }
                else if ("pathSecret_3".equals(name))
                {
                    pathSecret_3 = FileSystems.getDefault().getPath(value);
                }
            });

            boolean failed = false;
            if (bindAddress == null)
                failed = true;
            if (targetAddresses.isEmpty())
                failed = true;
            if (pathCert == null)
                failed = true;
            if (pathKey == null)
                failed = true;
            if (pathSecret == null && pathSecret_3 == null)
                failed = true;
            if (failed)
                System.exit(1);

            sslCtx = SslContextBuilder.forServer(Files.newInputStream(pathCert, StandardOpenOption.READ), Files.newInputStream(pathKey, StandardOpenOption.READ)).ciphers(!ciphers.isEmpty() ? ciphers : Shared.TLS.defaultCipherSuites, SupportedCipherSuiteFilter.INSTANCE).protocols(!protocols.isEmpty() ? protocols : Shared.TLS.defaultProtocols).build();
            secret = generateSecret(pathSecret, Shared.magic);
            secret_3 = generateSecret_3(pathSecret_3 != null ? pathSecret_3 : pathSecret, Shared.magic);
        }

        SimpleLogger.println("%s > Starting...", Shared.printNow());
        theServer = new MirrorPoint(new MirrorPointConfig(bindAddress, targetAddresses, sslCtx, secret, secret_3));
        theServer.start().syncUninterruptibly();
        SimpleLogger.println("%s > Done. [%s]", Shared.printNow(), theServer.toString());

        bindAddress = null;
        targetAddresses = null;
        pathCert = null;
        pathKey = null;
        ciphers = null;
        protocols = null;
        pathSecret = null;
        pathSecret_3 = null;
        sslCtx = null;
        secret = null;
        secret_3 = null;

        Runtime.getRuntime().addShutdownHook(new Thread()
        {

            @Override
            public void run()
            {
                SimpleLogger.println("%s > Shutting down...", Shared.printNow());
                Arrays.asList(Shared.NettyObjects.bossGroup.shutdownGracefully(), Shared.NettyObjects.workerGroup.shutdownGracefully(), theServer.stop()).forEach(Future::syncUninterruptibly);
                SimpleLogger.println("%s > [%s] is now offline.", Shared.printNow(), theServer.toString());
            }

        });
    }

    private static boolean nonCommentLine(String line)
    {
        return !line.startsWith("#");
    }

    private static boolean validConfigLine(String line)
    {
        return line.indexOf("=") != -1;
    }

}
