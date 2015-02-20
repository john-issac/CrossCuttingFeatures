/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logging;

/**
 *
 * @author john
 */
public class CustomLogger implements ILogger {
    
    static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomLogger.class.getName());

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }
    
    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Exception e) {
        logger.error(message, e);
    }
    
}
