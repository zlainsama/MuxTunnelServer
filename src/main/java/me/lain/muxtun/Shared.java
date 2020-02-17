package me.lain.muxtun;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public final class Shared
{

    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final EventLoopGroup bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : KQueue.isAvailable() ? new KQueueEventLoopGroup(1) : new NioEventLoopGroup(1);
    public static final EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : KQueue.isAvailable() ? new KQueueEventLoopGroup() : new NioEventLoopGroup();
    public static final Class<? extends SocketChannel> classSocketChannel = Epoll.isAvailable() ? EpollSocketChannel.class : KQueue.isAvailable() ? KQueueSocketChannel.class : NioSocketChannel.class;
    public static final Class<? extends ServerSocketChannel> classServerSocketChannel = Epoll.isAvailable() ? EpollServerSocketChannel.class : KQueue.isAvailable() ? KQueueServerSocketChannel.class : NioServerSocketChannel.class;

    public static String printNow()
    {
        return dateFormat.format(LocalDateTime.now());
    }

    private Shared()
    {
    }

}
