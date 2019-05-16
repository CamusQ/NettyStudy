package com.mashibing.netty;

import com.mashibing.io.aio.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

public class HelloNetty {
    public static void main(String[] args) {
        new NettyServer(8888).serverStart();
    }
}

class NettyServer {


    int port = 8888;

    public NettyServer(int port) {
        this.port = port;
    }

    public void serverStart() {
        //这两个group可以理解为两个线程池
        //这里的bossGroup处理客户端来的连接，连接来了之后它先接受，接收完了之后任何一个都看作我的一的child，
        // 对每一个child的处理我都给加上另外的一个处理的Handler
        // 使用netty的好处就是 连接可以和业务分开（new Handler()）
        EventLoopGroup bossGroup = new NioEventLoopGroup();//相当与NIO里面的大管家selector
        EventLoopGroup workerGroup = new NioEventLoopGroup();//相当于工人

        //通过Bootstrap（解鞋带的意思），通过它来进行启动之前的一些配置
        ServerBootstrap b = new ServerBootstrap();

        b.group(bossGroup, workerGroup)//指定进去，第一个group负责连接，第二个group负责进行连接后的io处理
                .channel(NioServerSocketChannel.class)//建立完连接之后的通道是什么类型的
                //childHandler 的意思是，当我们每一个客户端连上来之后，我给他一个监听器，让他来处理
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    //一旦这个通道初始化了
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //这个处理的过程是：在这个通道上加一个对这个通道的处理器（又是一个监听器）
                        ch.pipeline().addLast(new Handler());
                    }
                });

        try {
            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }


    }
}

class Handler extends ChannelInboundHandlerAdapter {
    //处理过程
    @Override                                       //数据都不用自己读，都读好了
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //super.channelRead(ctx, msg);
        System.out.println("server: channel read");
        ByteBuf buf = (ByteBuf) msg;

        System.out.println(buf.toString(CharsetUtil.UTF_8));

        ctx.writeAndFlush(msg);

        ctx.close();

        //buf.release();
    }

    //netty 所有的异常处理都在这个方法中
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }
}
