package me.lain.muxtun;

import io.netty.util.concurrent.Future;
import me.lain.muxtun.mipo.MirrorPoint;
import me.lain.muxtun.mipo.config.MirrorPointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static MirrorPoint theServer = null;

    public static void run(Path pathConfig) throws IOException {
        MirrorPointConfig theConfig;
        try (BufferedReader in = Files.newBufferedReader(pathConfig, StandardCharsets.UTF_8)) {
            theConfig = MirrorPointConfig.fromJson(in.lines()
                    .filter(line -> !line.trim().startsWith("#"))
                    .collect(Collectors.joining(System.lineSeparator())));
        }

        logger.info("Starting...");
        theServer = new MirrorPoint(theConfig);
        if (theServer.start().awaitUninterruptibly(60L, TimeUnit.SECONDS))
            logger.info("Done, theServer is up");
        else
            System.exit(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            List<Future<?>> futures = new ArrayList<>();
            futures.addAll(Shared.NettyObjects.shutdownGracefully());
            futures.add(theServer.stop());
            Shared.NettyUtils.combineFutures(futures).awaitUninterruptibly(60L, TimeUnit.SECONDS);
        }));
    }

}
