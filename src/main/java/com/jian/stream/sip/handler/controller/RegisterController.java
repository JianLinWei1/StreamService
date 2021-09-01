package com.jian.stream.sip.handler.controller;


import com.jian.stream.sip.bean.*;
import com.jian.stream.sip.handler.HandlerController;
import com.jian.stream.sip.utils.SipHeaderUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.log4j.Log4j2;
import org.dom4j.DocumentException;

/**
 * @author carzy
 * @date 2020/8/14
 */
@Log4j2
public class RegisterController implements HandlerController {

    @Override
    public SipMethod method() {
        return SipMethod.REGISTER;
    }

    @Override
    public void handler(FullSipRequest request, Channel channel) throws DocumentException {
        AbstractSipHeaders headers = request.headers();
        DefaultFullSipResponse response = new DefaultFullSipResponse(SipResponseStatus.UNAUTHORIZED);
        response.setRecipient(request.recipient());
        AbstractSipHeaders h = response.headers();
        if (!headers.contains(SipHeaderNames.AUTHORIZATION)) {
            String wwwAuth = "Digest realm=\"3402000000\", nonce=\"" +
                    "b700dc7cb094478503a21148184a3731";

            String ipPort = channel.localAddress().toString();
            h.set(SipHeaderNames.VIA, SipHeaderUtil.via(channel, request.recipient()))
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
        log.info("***************注册回复结束*************结果：{}" ,channel1.isSuccess());
    }
}