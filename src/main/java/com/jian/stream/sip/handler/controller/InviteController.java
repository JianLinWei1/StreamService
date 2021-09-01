package com.jian.stream.sip.handler.controller;

import com.jian.stream.sip.bean.*;
import com.jian.stream.sip.handler.HandlerController;
import com.jian.stream.sip.utils.CharsetUtils;
import com.jian.stream.sip.utils.SendErrorResponseUtil;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.extern.log4j.Log4j2;
import org.dom4j.DocumentException;

/**
 * @author dongxinping
 */
@Log4j2
public class InviteController implements HandlerController {



    @Override
    public SipMethod method() {
        return SipMethod.INVITE;
    }

    @Override
    public void handler(FullSipRequest request, Channel channel) throws DocumentException {
        AbstractSipHeaders headers = request.headers();
        String type = headers.get(SipHeaderNames.CONTENT_TYPE);
        if (SipHeaderValues.APPLICATION_SDP.contentEqualsIgnoreCase(type)) {
            //todo.. sdp解析。
            log.info("sdp: {}", request.content().toString(CharsetUtils.US_ASCII));
        } else {
            SendErrorResponseUtil.err400(request, channel, "message content_type must be Application/sdp");
        }
    }
}
