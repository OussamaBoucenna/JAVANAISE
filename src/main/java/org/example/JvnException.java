package org.example;


public class JvnException extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    String message;

    public JvnException() {
    }

    public JvnException(String message) {
        this.message = message;
    }

    public String getMessage(){
        return message;
    }
}
