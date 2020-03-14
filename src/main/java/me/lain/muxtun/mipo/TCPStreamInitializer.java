package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

@Sharable
class TCPStreamInitializer extends ChannelInitializer<SocketChannel>
{

    static final TCPStreamInitializer DEFAULT = new TCPStreamInitializer();

    @Override
    protected void initChannel(SocketChannel ch) throws Exception
    {
        ch.pipeline().addLast(new ChunkedWriteHandler());
        ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
        ch.pipeline().addLast(TCPStreamInboundHandler.DEFAULT);
        ch.pipeline().addLast(TCPStreamExceptionHandler.DEFAULT);
    }

}
