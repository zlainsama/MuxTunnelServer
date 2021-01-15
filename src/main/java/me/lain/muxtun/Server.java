package me.lain.muxtun;

import io.netty.util.concurrent.Future;
import me.lain.muxtun.mipo.MirrorPoint;
import me.lain.muxtun.mipo.config.MirrorPointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static MirrorPointConfig theConfig = null;
    private static MirrorPoint theServer = null;

    private static void discardOut() {
        System.setOut(new PrintStream(Shared.voidStream));
        System.setErr(new PrintStream(Shared.voidStream));
    }

    private static Path init(String... args) throws Exception {
        int index = 0;
        boolean discardOut = false;
        Optional<Path> pathConfig = Optional.empty();

        for (String arg : args) {
            if (arg.startsWith("-")) {
                if ("--discardOut".equalsIgnoreCase(arg))
                    discardOut = true;
            } else {
                switch (index++) {
                    case 0:
                        pathConfig = Optional.of(Paths.get(arg));
                        break;
                }
            }
        }

        if (discardOut)
            discardOut();

        return pathConfig.orElse(Paths.get("MuxTunnel.json"));
    }

    public static void main(String[] args) throws Exception {
        try (BufferedReader in = Files.newBufferedReader(init(args), StandardCharsets.UTF_8)) {
            theConfig = MirrorPointConfig.fromJson(in.lines()
                    .filter(line -> !line.trim().startsWith("#"))
                    .collect(Collectors.joining(System.lineSeparator())));
        }

        logger.info("Starting...");
        theServer = new MirrorPoint(theConfig);
        if (theServer.start().awaitUninterruptibly(60L, TimeUnit.SECONDS))
            logger.info("Done, theServer is up.");
        else
            System.exit(1);

        theConfig = null;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            List<Future<?>> futures = new ArrayList<>();
            futures.addAll(Shared.NettyObjects.shutdownGracefully());
            futures.add(theServer.stop());
            Shared.combineFutures(futures).awaitUninterruptibly(60L, TimeUnit.SECONDS);
        }));
    }

}
