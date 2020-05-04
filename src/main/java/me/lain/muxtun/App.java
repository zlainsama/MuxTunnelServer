package me.lain.muxtun;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import io.netty.handler.ssl.SslContext;
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
    private static List<String> trustSha1 = new ArrayList<>();
    private static List<String> ciphers = new ArrayList<>();
    private static List<String> protocols = new ArrayList<>();
    private static SslContext sslCtx = null;
    private static MirrorPoint theServer = null;

    private static void discardOut()
    {
        System.setOut(new PrintStream(Shared.voidStream));
        System.setErr(new PrintStream(Shared.voidStream));
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
                else if ("trustSha1".equals(name))
                {
                    trustSha1.add(value);
                }
                else if ("ciphers".equals(name))
                {
                    ciphers.addAll(Arrays.asList(value.split(":")));
                }
                else if ("protocols".equals(name))
                {
                    protocols.addAll(Arrays.asList(value.split(":")));
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
            if (trustSha1.isEmpty())
                failed = true;
            if (failed)
                System.exit(1);

            sslCtx = MirrorPointConfig.buildContext(pathCert, pathKey, trustSha1, ciphers, protocols);
        }

        SimpleLogger.println("%s > Starting...", Shared.printNow());
        theServer = new MirrorPoint(new MirrorPointConfig(bindAddress, targetAddresses, sslCtx));
        theServer.start().syncUninterruptibly();
        SimpleLogger.println("%s > Done. [%s]", Shared.printNow(), theServer.toString());

        bindAddress = null;
        targetAddresses = null;
        pathCert = null;
        pathKey = null;
        trustSha1 = null;
        ciphers = null;
        protocols = null;
        sslCtx = null;

        Runtime.getRuntime().addShutdownHook(new Thread()
        {

            @Override
            public void run()
            {
                SimpleLogger.println("%s > Shutting down...", Shared.printNow());
                List<Future<?>> futures = new ArrayList<>();
                futures.addAll(Shared.NettyObjects.shutdownGracefully());
                futures.add(theServer.stop());
                futures.forEach(Future::syncUninterruptibly);
                SimpleLogger.println("%s > [%s] is now offline.", Shared.printNow(), theServer.toString());
                SimpleLogger.ensureFlushed();
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
