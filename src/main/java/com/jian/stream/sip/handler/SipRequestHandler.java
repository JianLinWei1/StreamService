package com.jian.stream.sip.handler;

import com.jian.stream.sip.bean.*;
import com.jian.stream.sip.utils.SipHeaderUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.AttributeKey;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by MrJan on 2021/8/26
 */

@Log4j2

public class SipRequestHandler  extends SimpleChannelInboundHandler<FullSipRequest> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端{}已连接服务器，通道ID:{} , 协议：{}", ctx.channel().remoteAddress(), ctx.channel().id() );

    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (channel instanceof DatagramChannel) {
            // UDP不处理.
            log.info("UDP在TCPHandler不处理");
            return;
        }
        SocketAddress remote = channel.remoteAddress();
        SocketAddress local = channel.localAddress();

        String remoteIpPort = remote.toString().substring(1);
        String[] clientAddr = remoteIpPort.split(":");
        String localIpPort = local.toString().substring(1);

        String via = String.format("%s/%s %s;rport=%s;received=%s;branch=z9hG4bk--",
                SipVersion.SIP_2_0_STRING, "TCP", localIpPort,  clientAddr[1], clientAddr[0]);

        channel.attr(AttributeKey.newInstance("VIA")).set(via);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullSipRequest msg) {

        AbstractSipHeaders headers = msg.headers();
        Channel channel = ctx.channel();
        log.info("********************消息****************");
        log.info(msg);
        // 启动的时候已经声明了. TCP为NioSocketChannel, UDP为NioDatagramChannel
        if (channel instanceof NioDatagramChannel) {
            log.info("[{}{}] rec udp request msg", channel.id().asShortText(), msg.recipient().toString());
        } else {
            log.info("[{}{}] rec tcp request msg", channel.id().asShortText(), msg.recipient().toString());
        }
        if (SipMethod.BAD == msg.method()) {
            log.error("收到一个错误的SIP消息");
            StringBuilder builder = new StringBuilder();
            log.error(SipMessageUtil.appendFullRequest(builder, msg).toString());
        }


        // 异步执行
       //DispatchHandler.INSTANCE.handler(msg, ctx.channel());

        DefaultFullSipResponse response = new DefaultFullSipResponse(SipResponseStatus.UNAUTHORIZED);
        response.setRecipient(msg.recipient());
        AbstractSipHeaders h = response.headers();
        if (!headers.contains(SipHeaderNames.AUTHORIZATION)) {
            String wwwAuth = "Digest realm=\"3402000000\", nonce=\"" +
                    "b700dc7cb094478503a21148184a3731";

            String ipPort = channel.localAddress().toString();
            h.set(SipHeaderNames.VIA, SipHeaderUtil.via(channel, msg.recipient()))
                    .set(SipHeaderNames.FROM, headers.get(SipHeaderNames.FROM))
                    .set(SipHeaderNames.TO, headers.get(SipHeaderNames.TO))
                    .set(SipHeaderNames.CSEQ, headers.get(SipHeaderNames.CSEQ))
                    .set(SipHeaderNames.CALL_ID, headers.get(SipHeaderNames.CALL_ID))
                    .set(SipHeaderNames.USER_AGENT, SipHeaderValues.USER_AGENT)
                    .set(SipHeaderNames.WWW_AUTHENTICATE, wwwAuth)
                    .set(SipHeaderNames.CONTENT_LENGTH, SipHeaderValues.EMPTY_CONTENT_LENGTH);
        } else {
            //todo.. 检验是否存在等等问题.
            h.set(SipHeaderNames.FROM, headers.get(SipHeaderNames.FROM))
                    .set(SipHeaderNames.TO, headers.get(SipHeaderNames.TO) + ";tag=" + System.currentTimeMillis())
                    .set(SipHeaderNames.CSEQ, headers.get(SipHeaderNames.CSEQ))
                    .set(SipHeaderNames.CALL_ID, headers.get(SipHeaderNames.CALL_ID))
                    .set(SipHeaderNames.USER_AGENT, SipHeaderValues.USER_AGENT)
                    .set(SipHeaderNames.CONTENT_LENGTH, SipHeaderValues.EMPTY_CONTENT_LENGTH);
            response.setStatus(SipResponseStatus.OK);
        }
        log.info("***************注册回复开始*************");
        log.info(response.toString());
        ChannelFuture channel1 =   channel.writeAndFlush(response);
        System.out.println(channel1.cause());
        log.info("***************注册回复结束*************结果：{}" ,channel1.isSuccess());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端{}已断开连接，通道ID:{}", ctx.channel().remoteAddress(), ctx.channel().id());


        // offLineService.offLine(HashMapUtil.getKey(ctxMap ,ctx));

    }






}
