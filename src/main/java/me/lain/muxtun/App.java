package me.lain.muxtun;

import java.io.BufferedReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class App
{

    private static SocketAddress bindAddress = null;
    private static Map<UUID, SocketAddress> targetAddresses = new HashMap<>();
    private static Path pathCert = null;
    private static Path pathKey = null;
    private static SslContext sslCtx = null;
    private static MirrorPoint theServer = null;

    public static void main(String[] args) throws Exception
    {
        try (BufferedReader in = Files.newBufferedReader(FileSystems.getDefault().getPath(args.length > 0 ? args[0] : "MuxTunnel.cfg")))
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
            if (failed)
                System.exit(1);

            sslCtx = SslContextBuilder.forServer(Files.newInputStream(pathCert, StandardOpenOption.READ), Files.newInputStream(pathKey, StandardOpenOption.READ)).build();
        }

        System.out.println(String.format("%s > Starting %s...", Shared.printNow(), "MirrorPoint"));
        theServer = new MirrorPoint(bindAddress, targetAddresses, sslCtx);
        System.out.println(String.format("%s > Done.", Shared.printNow()));
        Runtime.getRuntime().addShutdownHook(new Thread()
        {

            @Override
            public void run()
            {
                System.out.println(String.format("%s > Shutting down...", Shared.printNow()));
                Shared.bossGroup.shutdownGracefully().syncUninterruptibly();
                Shared.workerGroup.shutdownGracefully().syncUninterruptibly();
                theServer.getChannels().close().syncUninterruptibly();
                System.out.println(String.format("%s > %s is now offline.", Shared.printNow(), "MirrorPoint"));
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
