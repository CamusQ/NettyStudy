package com.mashibing.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();//channel 是双向的，可以读，可以写
        ssc.socket().bind(new InetSocketAddress("127.0.0.1", 8888));
        ssc.configureBlocking(false);// 设定 非阻塞模型

        System.out.println("server started, listening on :" + ssc.getLocalAddress());
        Selector selector = Selector.open();//打开一个selector

        //对哪些事情感兴趣，首先对有连接的事件感兴趣
        //（再每一个等待连接的插座上放置一个key，这个key里面记录着我对扯个插座的那些事件感兴趣）
        //放置的第一个感兴趣的事就是 accept事件，就是有人想往这个插座上连接的时候我就进行处理
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        //接下来用while循环进行轮询，轮询一次有可能发现一堆的事件，把这些事件拿出来放到list里（set），然后挨着处理每一个事件
        //如果发现一个事件是acceptable，我们就建立一个通道，然后我们再这个通道上再注册一个感兴趣的事件read
        // （read的意思是 如果有客户端往服务端写数据，下次轮询的时候我就知道了，就可以处理这个read）
        while(true) {
            selector.select();//select也是阻塞方法，等着有事件发生
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while(it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                handle(key);
            }
        }

    }

    private static void handle(SelectionKey key) {
        if(key.isAcceptable()) {//说明有客户端想连上来
            try {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                //new Client
                //
                //String hostIP = ((InetSocketAddress)sc.getRemoteAddress()).getHostString();

			/*
			log.info("client " + hostIP + " trying  to connect");
			for(int i=0; i<clients.size(); i++) {
				String clientHostIP = clients.get(i).clientAddress.getHostString();
				if(hostIP.equals(clientHostIP)) {
					log.info("this client has already connected! is he alvie " + clients.get(i).live);
					sc.close();
					return;
				}
			}*/

                sc.register(key.selector(), SelectionKey.OP_READ );//监控通道上的read事件
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
        }
        //在nio中所有的通道都是和buffer绑在一起的，所谓的buffer就是一个字节数组，每次都把它填满或填到一定位置，再直接从子节数组读写
        // 在bio中是一个子节一个子节往出读，速度慢效率低
        else if (key.isReadable()) { //flip
            SocketChannel sc = null;
            try {
                sc = (SocketChannel)key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(512);
                buffer.clear();
                int len = sc.read(buffer);

                if(len != -1) {
                    System.out.println(new String(buffer.array(), 0, len));
                }

                //oracle再nio中设计了bytebuffer这个类，及其反人类
                ByteBuffer bufferToWrite = ByteBuffer.wrap("HelloClient".getBytes());
                sc.write(bufferToWrite);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(sc != null) {
                    try {
                        sc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
