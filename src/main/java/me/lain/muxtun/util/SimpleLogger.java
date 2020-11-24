package me.lain.muxtun.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class SimpleLogger {

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(SimpleLogger::newWorkerThread);
    private static final AtomicReference<Optional<PrintWriter>> fileOut = new AtomicReference<>(Optional.empty());

    private SimpleLogger() {
        throw new IllegalStateException("NoInstance");
    }

    public static void execute(Runnable command) {
        WORKER.execute(command);
    }

    private static PrintWriter newPrintWriter(Path path) {
        if (path == null)
            return null;

        try {
            return new PrintWriter(Channels.newWriter(FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE), StandardCharsets.UTF_8.newEncoder(), -1), true);
        } catch (IOException e) {
            return null;
        }
    }

    private static Thread newWorkerThread(Runnable r) {
        Thread t = new Thread(r, "SimpleLogger.WORKER");
        if (!t.isDaemon())
            t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

    public static void print(boolean b) {
        print(String.valueOf(b));
    }

    public static void print(char c) {
        print(String.valueOf(c));
    }

    public static void print(char[] s) {
        print(String.valueOf(s));
    }

    public static void print(double d) {
        print(String.valueOf(d));
    }

    public static void print(float f) {
        print(String.valueOf(f));
    }

    public static void print(int i) {
        print(String.valueOf(i));
    }

    public static void print(Locale l, String format, Object... args) {
        print(String.format(l, format, args));
    }

    public static void print(long l) {
        print(String.valueOf(l));
    }

    public static void print(Object obj) {
        print(String.valueOf(obj));
    }

    public static void print(String s) {
        execute(() -> {
            System.out.print(s);
            fileOut.get().ifPresent(w -> w.print(s));
        });
    }

    public static void print(String format, Object... args) {
        print(String.format(Locale.getDefault(), format, args));
    }

    public static void println() {
        execute(() -> {
            System.out.println();
            fileOut.get().ifPresent(PrintWriter::println);
        });
    }

    public static void println(boolean b) {
        println(String.valueOf(b));
    }

    public static void println(char c) {
        println(String.valueOf(c));
    }

    public static void println(char[] s) {
        println(String.valueOf(s));
    }

    public static void println(double d) {
        println(String.valueOf(d));
    }

    public static void println(float f) {
        println(String.valueOf(f));
    }

    public static void println(int i) {
        println(String.valueOf(i));
    }

    public static void println(Locale l, String format, Object... args) {
        println(String.format(l, format, args));
    }

    public static void println(long l) {
        println(String.valueOf(l));
    }

    public static void println(Object obj) {
        println(String.valueOf(obj));
    }

    public static void println(String s) {
        execute(() -> {
            System.out.println(s);
            fileOut.get().ifPresent(w -> w.println(s));
        });
    }

    public static void println(String format, Object... args) {
        println(String.format(Locale.getDefault(), format, args));
    }

    public static void printStackTrace(Throwable t) {
        execute(t::printStackTrace);
    }

    public static boolean setFileOut(Path path) {
        fileOut.getAndSet(Optional.ofNullable(newPrintWriter(path))).ifPresent(PrintWriter::close);
        return fileOut.get().isPresent() || path == null;
    }

}
