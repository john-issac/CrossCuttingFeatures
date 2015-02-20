/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.servicewrapper;

/**
 *
 * @author john
 */
public interface IService {
    void init();
    void start();
    void stop();
    void destroy();
    boolean isInited();
    boolean isStarted();
}
