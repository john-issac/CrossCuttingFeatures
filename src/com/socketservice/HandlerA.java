/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socketservice;

import com.logging.ILogger;
import com.logging.LogProvider;

/**
 *
 * @author john
 */
public class HandlerA extends HandlerCallback {
    static final ILogger logger = LogProvider.getLogger();

    @Override
    String dataHandler(String[] data) {
        logger.info("This is Handler A");
        return "response A";
    }
}
