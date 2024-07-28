package org.example.utils;

import javax.swing.plaf.nimbus.State;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

    public static void log(String s) {
        System.out.println("  " + dtf.format(LocalDateTime.now()) + " ||| " + s);
    }

}


    //TODO NEXT FEATURE - of course we should use SLF4J/logback and other proper libraries for logging and rolling into file for stats
    // this was a quick hack for achieving the goal