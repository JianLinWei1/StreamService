package com.jian.stream.common.utils;

import java.util.UUID;

public class IDUtils {
	public static String id() {
		return UUID.randomUUID().toString().replace("-", "").toUpperCase();
	}

	public static String idLow() {
		return UUID.randomUUID().toString().replace("-", "").toLowerCase();
	}
	
}
