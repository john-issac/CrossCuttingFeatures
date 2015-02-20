/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socketservice;

import com.logging.ILogger;
import com.logging.LogProvider;
import com.servicewrapper.IService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author john
 */
public class SocketService implements IService, Runnable{
    static final ILogger logger = LogProvider.getLogger();
    
    private InetAddress hostAddress;
    private int port;
    
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    private final List changeRequests = new LinkedList();
    private final Map pendingData = new HashMap();
    
    private ServiceWorker requestWorker;
    private Thread workerThread;
    private Thread serverThread;
    private volatile boolean running = true;
    
    public SocketService() {
        this.hostAddress = null;
        this.port = 24001;
    }
    
    /**
     * @param cmdArgs the parameters following the service name in the command line.
     * Either all items in the list follow the -key=value format or -value format.
     * in case of -key=value
    */
    public SocketService(List cmdArgs) throws IOException {
        boolean keyValue = false;
        for(int i=0; i<cmdArgs.size(); i++) {
            String arg = (String)cmdArgs.get(i);
            int eqPosition = arg.indexOf('=');
            if(eqPosition > 1) {
                keyValue = true;
                String[] kvParam = arg.split("=");
                switch (kvParam[0]) {
                    case "-host":
                        if(kvParam[1].equals("null"))
                            this.hostAddress = null;
                        else if(isValidIP(kvParam[1]))
                            this.hostAddress = InetAddress.getByName(kvParam[1]);
                        break;
                    case "-port":
                        if(kvParam[1].matches("\\d{4,5}") && Integer.parseInt(kvParam[1]) < 65535)
                            this.port = Integer.parseInt(kvParam[1]);
                        else
                            logger.error("Invalid Port Number.", new RuntimeException());
                        break;
                }
            }
        }
        
        if(!keyValue) {
            //exclude the - from the first argument
            String arg = ((String)cmdArgs.get(0)).substring(1);
            if(arg.equals("null"))
                this.hostAddress = null;
            else if(isValidIP(arg))
                this.hostAddress = InetAddress.getByName(arg);
            //exclude the - from the second argument
            arg = ((String)cmdArgs.get(1)).substring(1);
            if(arg.matches("\\d{4,5}") && Integer.parseInt(arg) < 65535)
                this.port = Integer.parseInt(arg);
            else
                logger.error("Invalid Port Number.", new RuntimeException());
        }
    }

    public SocketService(InetAddress hostAddress, int port, ServiceWorker worker) throws IOException {
        this.hostAddress = hostAddress;
        this.port = port;
        this.requestWorker = worker;
    }

    @Override
    public void init() {
        try {
            this.selector = this.initSelector();
        }catch(IOException ex) {
            logger.error("Service failed to start on IP: "+hostAddress +":"+port);
            logger.error(ex.toString(), ex);
            running = false;
        }
    }

    @Override
    public void start() {
        try {
            ServiceWorker worker = new ServiceWorker();
            // register key handlers
            HandlerA AH = new HandlerA();
            worker.register(Pattern.compile("A"), AH);
            // register reinstate key
            HandlerB BH = new HandlerB();
            worker.register(Pattern.compile("B"), BH);
            
            this.requestWorker = worker;
            workerThread = new Thread(worker);
            workerThread.start();
            
            serverThread = new Thread(this);
            serverThread.start();
        } catch (Exception e) {
            logger.error("Failed to start Service.");
        }
    }

    @Override
    public void stop() {
        if(running) {
            running = false;
            this.selector.wakeup();
            try {
                serverThread.join();
            } catch (InterruptedException ex) {
                logger.error(ex.toString(), ex);
            }
            logger.info("Service Stopped");
        } else {
            logger.info("Service was already stopped");
        }
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isInited() {
        if(selector == null)
            return false;
        else
            return true;
    }

    @Override
    public boolean isStarted() {
        return workerThread.isAlive();
    }
    
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        logger.info("Connection Accepted");
        Socket socket = socketChannel.socket();
        logger.info("IP: "+socket.getInetAddress()+" -- Port: "+socket.getPort()+", "+socket.getLocalPort());
        socketChannel.configureBlocking(false);
        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }
    
    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        this.readBuffer.clear();
        int numRead;
        try {
            numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            key.cancel();
            socketChannel.close();
            return;
        }

        if (numRead == -1) {
            key.channel().close();
            key.cancel();
            return;
        }
        // Pass data to worker thread
        this.requestWorker.processData(this, socketChannel, this.readBuffer.array(), numRead);
    }
    
    public void send(SocketChannel socket, byte[] data) {
        synchronized (this.changeRequests) {
            this.changeRequests.add(new ChannelChange(socket, ChannelChange.CHANGEOPS, SelectionKey.OP_WRITE));
            synchronized (this.pendingData) {
                List queue = (List) this.pendingData.get(socket);
                if (queue == null) {
                    queue = new ArrayList();
                    this.pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }
        logger.info("let's wake up");
        this.selector.wakeup();
    }
    
    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(socketChannel);
            while (!queue.isEmpty()) {
                ByteBuffer buf = (ByteBuffer) queue.get(0);
                socketChannel.write(buf);
                logger.info(new String(buf.array()));
                if (buf.remaining() > 0) { // socket's buffer full
                    break;
                }
                queue.remove(0);
            }

            if (queue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                // Process any pending changes
                synchronized (this.changeRequests) {
                    Iterator changes = this.changeRequests.iterator();
                    while (changes.hasNext()) {
                        ChannelChange change = (ChannelChange) changes.next();
                        switch (change.type) {
                            case ChannelChange.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(this.selector);
                                key.interestOps(change.ops);
                        }
                    }
                    this.changeRequests.clear();
                }

                // Wait for a channel
                this.selector.select();

                // Iterate over the set of keys for which events are available
                Iterator selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // Check what event is available and deal with it
                    if (key.isAcceptable()) {
                        logger.info("Handshake");
                        this.accept(key); //Establishing the connection happens here
                    } else if (key.isReadable()) {
                        this.read(key);   //Processing the request happens here
                    } else if (key.isWritable()) {
                        this.write(key);  
                    }
                }
            } catch (Exception e) {
                logger.error("Service crashed.", e);
            }
        }
        requestWorker.terminate();
        workerThread.interrupt();
        try {
            workerThread.join();
        } catch (InterruptedException ex) {
            logger.error(ex.toString(), ex);
        }
        requestWorker = null;
        logger.info("Service died Naturally.");
    }
    
    private boolean isValidIP(String IP) {
        Pattern pattern;
        pattern = Pattern.compile("^([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\."+
                "([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        Matcher matcher;
        matcher = pattern.matcher(IP);
        if(matcher.matches())
            return true;
        else {
            logger.error("Invalid IP", new RuntimeException());
            return false;
        }
    }
    
    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(isa);
        String host = "localhost";
        if(this.hostAddress != null)
            host = this.hostAddress.getHostAddress();
        logger.info("--------> Server started on "+host+":"+this.port);
        // Server stood up interested in accepting new connections (OP_ACCEPT)
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        return socketSelector;
    }
}
