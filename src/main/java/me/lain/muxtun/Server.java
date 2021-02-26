package me.lain.muxtun;

import io.netty.util.concurrent.Future;
import me.lain.muxtun.mipo.MirrorPoint;
import me.lain.muxtun.mipo.config.MirrorPointConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger();

    private static MirrorPoint theServer = null;

    static {
        ShutdownTasks.register(() -> {
            if (theServer != null) {
                LOGGER.info("Shutting down...");
                shutdown(60L, TimeUnit.SECONDS);
                LOGGER.info("Done.");
            }

            LogManager.shutdown();
        });
    }

    public static void run(Path pathConfig) throws IOException {
        MirrorPointConfig theConfig;
        try (BufferedReader in = Files.newBufferedReader(pathConfig, StandardCharsets.UTF_8)) {
            theConfig = MirrorPointConfig.fromJson(in.lines()
                    .filter(line -> !line.trim().startsWith("#"))
                    .collect(Collectors.joining(System.lineSeparator())));
        }

        LOGGER.info("Starting...");
        theServer = new MirrorPoint(theConfig);
        Future<?> result;
        if ((result = theServer.start()).awaitUninterruptibly(60L, TimeUnit.SECONDS)) {
            if (result.isSuccess())
                LOGGER.info("Done, theServer is up.");
            else {
                LOGGER.fatal("error starting theServer", result.cause());
                LogManager.shutdown();
                System.exit(1);
            }
        } else {
            LOGGER.fatal("Took too long to start, terminating...");
            shutdown(10L, TimeUnit.SECONDS);
            LOGGER.info("Terminated.");
            LogManager.shutdown();
            System.exit(1);
        }
    }

    public static void shutdown(long timeout, TimeUnit unit) {
        List<Future<?>> futures = new ArrayList<>();
        futures.addAll(Shared.NettyObjects.shutdownGracefully());
        futures.add(theServer.stop());
        Shared.NettyUtils.combineFutures(futures).awaitUninterruptibly(timeout, unit);
    }

}
