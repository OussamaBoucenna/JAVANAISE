package org.example.interfaces;

/***
 * JAVANAISE API
 * Contact:
 *
 * Authors:
 */

import org.example.Exceptions.JvnException;

import java.io.Serializable;


public interface JvnLocalServer {
        public JvnObject jvnCreateObject(Serializable o) throws JvnException;
        public void jvnRegisterObject(String jon, JvnObject jo) throws JvnException;
        public JvnObject jvnLookupObject(String jon) throws JvnException;

        public Serializable jvnLockRead(int joi) throws JvnException;
        public Serializable jvnLockWrite(int joi) throws JvnException;
    //    public void jvnUnLock(int joi, Serializable state) throws JvnException; // temporaire

        public void jvnTerminate() throws JvnException;

}


