package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

@Sharable
class UDPStreamInitializer extends ChannelInitializer<DatagramChannel>
{

    static final UDPStreamInitializer DEFAULT = new UDPStreamInitializer();

    @Override
    protected void initChannel(DatagramChannel ch) throws Exception
    {
        ch.pipeline().addLast(new IdleStateHandler(0, 0, 60));
        ch.pipeline().addLast(new ChunkedWriteHandler());
        ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
        ch.pipeline().addLast(UDPStreamInboundHandler.DEFAULT);
        ch.pipeline().addLast(UDPStreamEventHandler.DEFAULT);
        ch.pipeline().addLast(UDPStreamExceptionHandler.DEFAULT);
    }

}
