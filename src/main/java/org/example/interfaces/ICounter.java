package org.example.interfaces;

public interface ICounter {
    @JvnWrite
    public  void increment();

    @JvnRead
    public int read() ;
}
