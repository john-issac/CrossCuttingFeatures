/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logging;

/**
 *
 * @author john
 */
public class LogProvider {
    private static ILogger logger;
    private static final Object lock = new Object();
    
    public static ILogger getLogger() {
        synchronized (lock) {
            if(logger != null)
                return logger;
            logger = new DefaultLogger();
            return logger;
        }
    }
    
    public static void setLogger(ILogger customLogger) {
        logger = customLogger;
    }
}
