/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socketservice;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 *
 * @author john
 */
public class RequestHandler {
    public UUID id;
    public Pattern pattern;
    public HandlerCallback callback;

    public RequestHandler(UUID id, Pattern pattern, HandlerCallback callback) {
        this.id = id;
        this.pattern = pattern;
        this.callback = callback;
    }
}
