/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logging;

/**
 *
 * @author john
 */
public interface ILogger {
    void info(String message);
    void debug(String message);
    void error(String message);
    void error(String message, Exception e);
}
