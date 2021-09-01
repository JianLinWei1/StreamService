package com.jian.stream.sip.utils;

import io.netty.util.AttributeKey;

public class AttributeKeyUtil {

    private AttributeKeyUtil(){}

    // VIA头.
    public static final AttributeKey<String> ATTR_VIA = AttributeKey.newInstance("VIA");
}
