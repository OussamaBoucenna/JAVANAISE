package org.example.Models;


import org.example.interfaces.ICounter;

import java.io.Serializable;

public class Counter implements Serializable , ICounter {
    private int value = 0;

    public void increment() { value++; }

    public int read() { return value; }

}