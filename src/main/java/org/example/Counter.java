package org.example;


import java.io.Serializable;

public class Counter implements Serializable {
    private int value = 0;

    public void increment() { value++; }
    public int read() { return value; }
}