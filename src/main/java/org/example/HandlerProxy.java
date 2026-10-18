package org.example;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


public class HandlerProxy implements InvocationHandler {

    private final JvnObject realObject;

    private HandlerProxy(JvnObject obj) {
        this.realObject = obj;
    }

    public static Object newInstance(JvnObject jo, Class<?> monInterface) {
        return Proxy.newProxyInstance(
                monInterface.getClassLoader(),
                new Class<?>[] { monInterface },
                new HandlerProxy(jo));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("invocation call ");
        // 1. Verrou AVANT le try : s'il échoue, on ne passe jamais par le finally
        if (method.getName().equals("increment")) {
            realObject.jvnLockWrite();
            System.out.println("incrementation");
        } else {
            realObject.jvnLockRead();
            System.out.println("read");
        }

        try {
            Object target = realObject.jvnGetSharedObject();
            return method.invoke(target, args);

        } catch (InvocationTargetException e) {
            throw e.getCause();

        } finally {
            realObject.jvnUnLock();
        }
    }
}