package me.lain.muxtun.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SimpleLogger
{

    private static final AtomicReference<Optional<Writer>> fileOut = new AtomicReference<>(Optional.empty());
    private static final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private static final AtomicBoolean done = new AtomicBoolean(true);
    private static final Runnable printer = new Runnable()
    {

        ManagedBlocker blocker = new ManagedBlocker()
        {

            @Override
            public boolean block() throws InterruptedException
            {
                Runnable task;
                while ((task = tasks.poll(50L, TimeUnit.MILLISECONDS)) != null)
                {
                    try
                    {
                        task.run();
                    }
                    catch (Throwable e)
                    {
                    }
                }
                return true;
            }

            @Override
            public boolean isReleasable()
            {
                return tasks.isEmpty();
            }

        };

        @Override
        public void run()
        {
            while (!tasks.isEmpty() && done.compareAndSet(true, false))
            {
                try
                {
                    ForkJoinPool.managedBlock(blocker);
                }
                catch (InterruptedException e)
                {
                }
                finally
                {
                    done.set(true);
                }
            }
        }

    };

    public static void ensureFlushed()
    {
        while (!tasks.isEmpty())
        {
            initiatePrinter();
            forceSleep(50L);
        }
    }

    public static void execute(Runnable runnable)
    {
        tasks.add(runnable);
        initiatePrinter();
    }

    private static void forceSleep(long millis)
    {
        long start = System.currentTimeMillis();

        for (;;)
        {
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed >= millis)
                break;

            try
            {
                Thread.sleep(millis - elapsed);
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    private static void initiatePrinter()
    {
        if (tasks.isEmpty() || !done.get())
            return;

        ForkJoinPool.commonPool().submit(printer);
    }

    private static Optional<Writer> newWriter(Path path)
    {
        if (path == null)
            return Optional.empty();

        try
        {
            return Optional.of(Channels.newWriter(FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE), StandardCharsets.UTF_8.newEncoder(), -1));
        }
        catch (IOException e)
        {
            return Optional.empty();
        }
    }

    public static void println()
    {
        tasks.add(() -> {
            System.out.println();

            fileOut.get().ifPresent(w -> {
                try
                {
                    w.write(System.lineSeparator());
                    w.flush();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    setFileOut(null);
                }
            });
        });

        initiatePrinter();
    }

    public static void println(boolean b)
    {
        println(String.valueOf(b));
    }

    public static void println(char c)
    {
        println(String.valueOf(c));
    }

    public static void println(char[] s)
    {
        println(String.valueOf(s));
    }

    public static void println(double d)
    {
        println(String.valueOf(d));
    }

    public static void println(float f)
    {
        println(String.valueOf(f));
    }

    public static void println(int i)
    {
        println(String.valueOf(i));
    }

    public static void println(Locale l, String format, Object... args)
    {
        println(String.format(l, format, args));
    }

    public static void println(long l)
    {
        println(String.valueOf(l));
    }

    public static void println(Object obj)
    {
        println(String.valueOf(obj));
    }

    public static void println(String s)
    {
        tasks.add(() -> {
            System.out.println(s);

            fileOut.get().ifPresent(w -> {
                try
                {
                    w.write(s);
                    w.write(System.lineSeparator());
                    w.flush();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    setFileOut(null);
                }
            });
        });

        initiatePrinter();
    }

    public static void println(String format, Object... args)
    {
        println(Locale.getDefault(), format, args);
    }

    public static void printStackTrace(Throwable t)
    {
        tasks.add(t::printStackTrace);
        initiatePrinter();
    }

    private static void safeClose(AutoCloseable autocloseable)
    {
        try
        {
            if (autocloseable != null)
                autocloseable.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static boolean setFileOut(Path path)
    {
        fileOut.getAndSet(newWriter(path)).ifPresent(SimpleLogger::safeClose);
        return fileOut.get().isPresent() || path == null;
    }

    private SimpleLogger()
    {
    }

}
