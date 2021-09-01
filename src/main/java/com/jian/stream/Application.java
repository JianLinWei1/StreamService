package com.jian.stream;


import com.jian.stream.sip.bean.SipObjectAggregator;
import com.jian.stream.sip.coder.SipObjectTcpDecoder;
import com.jian.stream.sip.coder.SipObjectUdpDecoder;
import com.jian.stream.sip.coder.SipRequestEncoder;
import com.jian.stream.sip.coder.SipResponseEncoder;
import com.jian.stream.sip.handler.SipRequestHandler;
import com.jian.stream.sip.handler.SipResponseHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;

/**
 * Created by MrJan on 2021/8/24
 */

@Log4j2
public class Application {
    private final int port = 5060;


    // Configure the server.
    private static final EventLoopGroup BOSS_GROUP = new NioEventLoopGroup(1);
    private static final EventLoopGroup WORKER_GROUP = new NioEventLoopGroup();
    private static final EventLoopGroup UDP_GROUP = new NioEventLoopGroup();

    public void startUdp() {
        Bootstrap b = new Bootstrap();
        b.group(UDP_GROUP)
                // 关闭广播
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new SipResponseEncoder())
                                .addLast(new SipRequestEncoder())
                                .addLast(new SipObjectUdpDecoder())
                                .addLast(new SipObjectAggregator(8192))

                                .addLast(new SipRequestHandler())
                                .addLast(new SipResponseHandler());
                    }
                });
        try {
            ChannelFuture future = b.bind(new InetSocketAddress("0.0.0.0", port)).sync();
            log.info("udp port {} is running.", port);
            future.channel().closeFuture().addListener(f -> {
                if (f.isSuccess()) {
                    log.info("udp exit suc on port $port");
                } else {
                    log.error("udp exit err on port $port", f.cause());
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("udp run port $port err", e);
            UDP_GROUP.shutdownGracefully();
        }
    }

    public void startTcp() {
        ServerBootstrap b = new ServerBootstrap();
        b.group(BOSS_GROUP, WORKER_GROUP)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 512)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                               .addLast(new SipResponseEncoder())
                                .addLast(new SipRequestEncoder())
                                .addLast(new SipObjectTcpDecoder())
                               .addLast(new SipObjectAggregator(8192))

                                .addLast(new SipRequestHandler())
                                .addLast(new SipResponseHandler());
                    }
                });
        try {
            ChannelFuture future = b.bind(port).sync();
            log.info("tcp port {} is running.", port);
            future.channel().closeFuture().addListener(f -> {
                if (f.isSuccess()) {
                    log.info("tcp exit suc on port {}", port);
                } else {
                    log.error("tcp exit err on port {}", port, f.cause());
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("udp run port $port err", e);
            BOSS_GROUP.shutdownGracefully();
            WORKER_GROUP.shutdownGracefully();
        }
    }


    public static void main(String[] args) {
        Application application = new Application();
        new Thread(application::startTcp).start();
        new Thread(application::startUdp).start();
    }

}
