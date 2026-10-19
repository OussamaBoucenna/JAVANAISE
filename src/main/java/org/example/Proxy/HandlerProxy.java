package org.example.Proxy;

import org.example.interfaces.JvnObject;
import org.example.interfaces.JvnRead;
import org.example.interfaces.JvnWrite;

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

        boolean write = method.isAnnotationPresent(JvnWrite.class);
        boolean read = method.isAnnotationPresent(JvnRead.class);


        if (!write && !read) {
            try {
                Object target = realObject.jvnGetSharedObject();
                return method.invoke(target, args);
            }catch (InvocationTargetException e){
                throw e.getCause();
            }
        }



        if (write) {
            realObject.jvnLockWrite();
        } else {
            realObject.jvnLockRead();
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