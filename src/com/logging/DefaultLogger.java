/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logging;

/**
 *
 * @author john
 */
public class DefaultLogger implements ILogger {

    @Override
    public void info(String message) {
        System.out.println("[info]: " + message);
    }

    @Override
    public void debug(String message) {
        System.out.println("[debug]: " + message);
    }
    
    @Override
    public void error(String message) {
        System.out.println("[error]: " + message);
    }

    @Override
    public void error(String message, Exception e) {
        System.out.println("[error]: " + message + " - " + e);
    }
}
