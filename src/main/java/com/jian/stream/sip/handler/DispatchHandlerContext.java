package com.jian.stream.sip.handler;


import com.jian.stream.sip.bean.SipMethod;
import com.jian.stream.sip.utils.ClassScanner;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * gb-sip 请求分发
 *
 * @author dongxinping
 */
public class DispatchHandlerContext {
    private static final ConcurrentMap<SipMethod, HandlerController> CONTROLLER_MAP = new ConcurrentHashMap<>(256);

    private static String ALLOW_METHOD = "";

    public static HandlerController method(SipMethod method) {
       // System.out.println(CONTROLLER_MAP.get(method));
        return CONTROLLER_MAP.get(method);
    }

    public static String allowMethod() {
        return ALLOW_METHOD;
    }

    public static void init() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Set<Class<?>> classes = ClassScanner.doScanAllClasses("com.jian.stream.sip.handler.controller");
        for (Class<?> aClass : classes) {
            if (HandlerController.class.isAssignableFrom(aClass)) {
                addHandlerController((HandlerController) newClass(aClass));
            }
        }
        ALLOW_METHOD = CONTROLLER_MAP.keySet()
                .stream()
                .map(SipMethod::name)
                .collect(Collectors.joining(","));
    }

    private static void addHandlerController(HandlerController o) {
        if (CONTROLLER_MAP.containsKey(o.method())) {
            throw new IllegalArgumentException("handlerController has be created.");
        }
        CONTROLLER_MAP.put(o.method(), o);
    }

    private static <T> T newClass(Class<T> tClass) throws IllegalAccessException, InstantiationException {
        return tClass.newInstance();
    }

    static {
        try {
            init();
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
