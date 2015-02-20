/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socketservice;

import com.logging.ILogger;
import com.logging.LogProvider;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author john
 */
public class ServiceWorker implements Runnable{
    static final ILogger logger = LogProvider.getLogger();
    
    private final List queue = new LinkedList();
    private volatile boolean running = true;
    
    private List<RequestHandler> handlers;

    public ServiceWorker() {
        handlers = new ArrayList();
    }
    
    public UUID register(Pattern pattern, HandlerCallback callback) {
        UUID handlerId = UUID.randomUUID();
        RequestHandler reg = new RequestHandler(handlerId, pattern, callback);
        handlers.add(reg);
        return handlerId;
    }
    
    public void terminate() {
        running = false;
        for(Iterator<RequestHandler> i = handlers.iterator(); i.hasNext();) {
            RequestHandler h = i.next();
            h = null;
        }
        handlers.clear();
        synchronized (queue) {
            queue.notify();
        }
    }
    
    private String processRequest(String input) throws IOException   {
        
        String[] inputArr = input.split(" ");
        
        RequestHandler handler = null;
        for(Iterator<RequestHandler> i = handlers.iterator(); i.hasNext();) {
            handler = i.next();
            Matcher m = handler.pattern.matcher(inputArr[0]);
            if(m.find())
                break;
        }
        
        if(handler == null) {
            logger.error("No handler found. Request is not supported");
            return null;
        }
        
        String output = handler.callback.dataHandler(inputArr);
        
        return output;
    }

    // used by server thread to pass data to worker thread
    public void processData(SocketService server, SocketChannel socket, byte[] data, int count) {
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        synchronized (queue) {
            queue.add(new RequestEvent(server, socket, dataCopy));
            queue.notify();
        }
    }
    
    @Override
    public void run() {
        RequestEvent reqEvent;
        String response;

        while (true) {
            response = null;
            try {
                // Wait for data to become available
                synchronized (queue) {
                    while (queue.isEmpty() && running) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    if(!running)
                        break;
                    reqEvent = (RequestEvent) queue.remove(0);
                    response = processRequest(new String(reqEvent.data));
                    logger.info("API Response... "+response);
                }
            } catch(IOException ex) {
                logger.error("Failed to Reach API: ", ex);
            }catch(Exception ex) {
                logger.error("General Failure: ", ex);
            }
        }
        logger.info("Service Worker Died Naturally");
    }
}
