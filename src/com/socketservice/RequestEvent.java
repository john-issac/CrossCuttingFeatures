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
public class RequestEvent {
    public SocketService server;
    public SocketChannel socket;
    public byte[] data;

    public RequestEvent(SocketService server, SocketChannel socket, byte[] data) {
            this.server = server;
            this.socket = socket;
            this.data = data;
    }
}
