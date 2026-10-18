package org.example;

public interface ICounter {
    @JvnWrite
    public  void increment();

    @JvnRead
    public int read() ;
}
