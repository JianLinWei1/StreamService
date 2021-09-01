package com.jian.stream.sip.handler;



import com.jian.stream.sip.bean.FullSipRequest;
import com.jian.stream.sip.bean.SipMethod;
import io.netty.channel.Channel;
import org.dom4j.DocumentException;

/**
 * @author carzy
 * @date 2020/8/14
 */
public interface HandlerController {

    SipMethod method();

    /**
     * 处理具体的 sip 请求
     *
     * @param request 请求携带的信息.
     * @param channel 通道
     * @throws DocumentException 解析XML失败.
     */
    void handler(FullSipRequest request , Channel channel) throws DocumentException;
}