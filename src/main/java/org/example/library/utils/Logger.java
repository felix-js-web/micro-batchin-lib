package org.example.library.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

    public static void log(String s) {
        // TODO Next Feature
        //  fix rolling logs to files
        System.out.println("  " + dtf.format(LocalDateTime.now()) + " ||| " + s);
    }

}


    //TODO NEXT FEATURE - of course we should use SLF4J/logback and other proper libraries for logging and rolling into file for stats and history
    // this was a quick hack for achieving the goal