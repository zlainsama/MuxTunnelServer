package me.lain.muxtun;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class EntryPoint {

    private static final OutputStream voidStream = new OutputStream() {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void write(byte[] b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }

        @Override
        public void write(int b) throws IOException {
        }

    };

    private static void discardOut() {
        System.setOut(new PrintStream(voidStream));
        System.setErr(new PrintStream(voidStream));
    }

    private static Path init(String... args) {
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
        Server.run(init(args));
    }

}
