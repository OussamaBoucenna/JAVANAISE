package org.example;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)   // lisible quand le programme tourne
@Target(ElementType.METHOD)           // se pose sur une méthode
public @interface JvnRead {

}
