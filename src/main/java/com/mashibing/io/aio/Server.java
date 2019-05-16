package com.mashibing.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 *
 * AIO 的模型：
 *      我写好一段代码（A），把他钩到操作系统内核里面，什么时候你内核里面有个人要连我的时候，你帮我去执行这段代码A，
 *      一旦completed之后呢，我又下一个钩子（B）钩到连接好的通道上，当你这个通道上要读写的时候请你给我执行这段代码B
 */

//这已经差不多 类似于 netty 但是netty把bytebuffer封装的更好

//AIO asynchronous IO
public class Server {
    public static void main(String[] args) throws Exception {
        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open()
                .bind(new InetSocketAddress(8888));

        //用到了观察者模式，把这个方法交给操作系统，让操作系统调用
        //这里的accept方法不再阻塞，一调完程序马上就往下运行
        //completioinHandler 一个连接连上来了，由他来处理。
        /**
         * 相当于他做了这么一件事，整个主线程上来告诉操作系统，给操作系统下面这段代码，麻烦操作系统什么时候客户端连上来替我调度这段代码
         */
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            //已经连上来了
            @Override
            public void completed(AsynchronousSocketChannel client, Object attachment) {
                serverChannel.accept(null, this); //如果不写这行代码，下一个是连不上的

                try {
                    System.out.println(client.getRemoteAddress());
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    //读的操作原来是阻塞的，我也可以写成非阻塞，意思是我一读我就走了
                    //一旦我读完了，请你操作系统帮我调我读完之后处理的这段代码completed
                    client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                        //如果已经读完了，下一步应该怎么处理completed
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            attachment.flip();
                            System.out.println(new String(attachment.array(), 0, result));
                            client.write(ByteBuffer.wrap("HelloClient".getBytes()));
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            exc.printStackTrace();
                        }
                    });


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });

        while (true) {
            Thread.sleep(1000);
        }

    }
}
