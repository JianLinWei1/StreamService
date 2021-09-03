package com.jian.stream.message.helper;

import com.jian.stream.common.utils.DateUtils;
import gov.nist.javax.sip.header.SIPHeader;

import java.util.Date;

public class CustomSIPDateHeader extends SIPHeader{

	@Override
	protected StringBuilder encodeBody(StringBuilder buffer) {
		return buffer.append(DateUtils.getGBFormatDate(new Date()));
	}

}
