/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socketservice;

import java.nio.channels.SocketChannel;

/**
 *
 * @author john
 */
public class ChannelChange {
    public SocketChannel socket;
    public static final int REGISTER = 1;
    public static final int CHANGEOPS = 2;    
    public int type;
    public int ops;

    public ChannelChange(SocketChannel socket, int type, int ops) {
        this.socket = socket;
        this.type = type;
        this.ops = ops;
    }
}
