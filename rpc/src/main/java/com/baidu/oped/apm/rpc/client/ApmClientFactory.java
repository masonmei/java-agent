/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.oped.apm.rpc.client;


import com.baidu.oped.apm.common.util.ApmThreadFactory;
import com.baidu.oped.apm.rpc.MessageListener;
import com.baidu.oped.apm.rpc.ApmSocketException;
import com.baidu.oped.apm.rpc.StateChangeEventListener;
import com.baidu.oped.apm.rpc.cluster.ClusterOption;
import com.baidu.oped.apm.rpc.cluster.Role;
import com.baidu.oped.apm.rpc.stream.DisabledServerStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.stream.ServerStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.util.AssertUtils;
import com.baidu.oped.apm.rpc.util.LoggerFactorySetup;
import com.baidu.oped.apm.rpc.util.TimerFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientBossPool;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.util.*;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author emeroad
 * @author koo.taejin
 */
public class ApmClientFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String CONNECT_TIMEOUT_MILLIS = "connectTimeoutMillis";
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final long DEFAULT_TIMEOUTMILLIS = 3 * 1000;
    private static final long DEFAULT_PING_DELAY = 60 * 1000 * 5;
    private static final long DEFAULT_ENABLE_WORKER_PACKET_DELAY = 60 * 1000 * 1;

    private final AtomicInteger socketId = new AtomicInteger(1);
    
    private volatile boolean released;
    private ClientBootstrap bootstrap;
    private Map<String, Object> properties = Collections.emptyMap();
    
    private long reconnectDelay = 3 * 1000;
    private final Timer timer;

    // it's better to be a long value. even though keeping ping period from client to server short,
    // disconnection between them dose not be detected quickly.
    // rather keeping it from server to client short help detect disconnection as soon as possible.
    private long pingDelay = DEFAULT_PING_DELAY;
    private long enableWorkerPacketDelay = DEFAULT_ENABLE_WORKER_PACKET_DELAY;
    private long timeoutMillis = DEFAULT_TIMEOUTMILLIS;

    private ClusterOption clusterOption = ClusterOption.DISABLE_CLUSTER_OPTION;

    private MessageListener messageListener = SimpleMessageListener.INSTANCE;
    private List<StateChangeEventListener> stateChangeEventListeners = new ArrayList<StateChangeEventListener>();
    private ServerStreamChannelMessageListener serverStreamChannelMessageListener = DisabledServerStreamChannelMessageListener.INSTANCE;

    
    static {
        LoggerFactorySetup.setupSlf4jLoggerFactory();
    }

    public ApmClientFactory() {
        this(1, 1);
    }

    public ApmClientFactory(int bossCount, int workerCount) {
        if (bossCount < 1) {
            throw new IllegalArgumentException("bossCount is negative: " + bossCount);
        }

        // create a timer earlier because it is used for connectTimeout
        Timer timer = createTimer();
        ClientBootstrap bootstrap = createBootStrap(bossCount, workerCount, timer);
        setOptions(bootstrap);
        addPipeline(bootstrap);

        this.bootstrap = bootstrap;
        this.timer = timer;
    }

    private Timer createTimer() {
        HashedWheelTimer timer = TimerFactory.createHashedWheelTimer("Apm-SocketFactory-Timer", 100, TimeUnit.MILLISECONDS, 512);
        timer.start();
        return timer;
    }

    private void addPipeline(ClientBootstrap bootstrap) {
        ApmClientPipelineFactory apmClientPipelineFactory = new ApmClientPipelineFactory(this);
        bootstrap.setPipelineFactory(apmClientPipelineFactory);
    }

    private void setOptions(ClientBootstrap bootstrap) {
        // connectTimeout
        bootstrap.setOption(CONNECT_TIMEOUT_MILLIS, DEFAULT_CONNECT_TIMEOUT);
        // read write timeout needed?  isn't it needed because of nio?

        // tcp setting
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        // buffer setting
        bootstrap.setOption("sendBufferSize", 1024 * 64);
        bootstrap.setOption("receiveBufferSize", 1024 * 64);

    }

    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("connectTimeout cannot be a negative number");
        }
        bootstrap.setOption(CONNECT_TIMEOUT_MILLIS, connectTimeout);
    }

    public int getConnectTimeout() {
        return (Integer) bootstrap.getOption(CONNECT_TIMEOUT_MILLIS);
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelay(long reconnectDelay) {
        if (reconnectDelay < 0) {
            throw new IllegalArgumentException("reconnectDelay cannot be a negative number");
        }
        this.reconnectDelay = reconnectDelay;
    }

    public long getPingDelay() {
        return pingDelay;
    }

    public void setPingDelay(long pingDelay) {
        if (pingDelay < 0) {
            throw new IllegalArgumentException("pingDelay cannot be a negative number");
        }
        this.pingDelay = pingDelay;
    }
    
    public long getEnableWorkerPacketDelay() {
        return enableWorkerPacketDelay;
    }

    public void setEnableWorkerPacketDelay(long enableWorkerPacketDelay) {
        if (enableWorkerPacketDelay < 0) {
            throw new IllegalArgumentException("EnableWorkerPacketDelay cannot be a negative number");
        }
         this.enableWorkerPacketDelay = enableWorkerPacketDelay;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis cannot be a negative number");
        }
        this.timeoutMillis = timeoutMillis;
    }

    private ClientBootstrap createBootStrap(int bossCount, int workerCount, Timer timer) {
        // profiler, collector,
        logger.debug("createBootStrap boss:{}, worker:{}", bossCount, workerCount);
        NioClientSocketChannelFactory nioClientSocketChannelFactory = createChannelFactory(bossCount, workerCount, timer);
        return new ClientBootstrap(nioClientSocketChannelFactory);
    }

    private NioClientSocketChannelFactory createChannelFactory(int bossCount, int workerCount, Timer timer) {
        ExecutorService boss = Executors.newCachedThreadPool(new ApmThreadFactory("Apm-Client-Boss", true));
        NioClientBossPool bossPool = new NioClientBossPool(boss, bossCount, timer, ThreadNameDeterminer.CURRENT);

        ExecutorService worker = Executors.newCachedThreadPool(new ApmThreadFactory("Apm-Client-Worker", true));
        NioWorkerPool workerPool = new NioWorkerPool(worker, workerCount, ThreadNameDeterminer.CURRENT);
        return new NioClientSocketChannelFactory(bossPool, workerPool);
    }

    public ApmClient connect(String host, int port) throws ApmSocketException {
        InetSocketAddress connectAddress = new InetSocketAddress(host, port);
        return connect(connectAddress);
    }

    public ApmClient connect(InetSocketAddress connectAddress) throws ApmSocketException {
        ChannelFuture connectFuture = bootstrap.connect(connectAddress);
        ApmClientHandler apmClientHandler = getSocketHandler(connectFuture, connectAddress);

        ApmClient apmClient = new ApmClient(apmClientHandler);
        traceSocket(apmClient);
        return apmClient;
    }

    public ApmClient reconnect(String host, int port) throws ApmSocketException {
        SocketAddress address = new InetSocketAddress(host, port);
        ChannelFuture connectFuture = bootstrap.connect(address);
        ApmClientHandler apmClientHandler = getSocketHandler(connectFuture, address);

        ApmClient apmClient = new ApmClient(apmClientHandler);
        traceSocket(apmClient);
        return apmClient;
    }

    /*
        trace mechanism is needed in case of calling close without closing socket
        it is okay to make that later because this is a exceptional case.
     */
    private void traceSocket(ApmClient apmClient) {

    }

    public ApmClient scheduledConnect(String host, int port) {
        InetSocketAddress connectAddress = new InetSocketAddress(host, port);
        return scheduledConnect(connectAddress);
    }

    public ApmClient scheduledConnect(InetSocketAddress connectAddress) {
        ApmClient apmClient = new ApmClient(new ReconnectStateClientHandler());
        reconnect(apmClient, connectAddress);
        return apmClient;
    }

    ApmClientHandler getSocketHandler(ChannelFuture channelConnectFuture, SocketAddress address) {
        if (address == null) {
            throw new NullPointerException("address");
        }

        ApmClientHandler apmClientHandler = getSocketHandler(channelConnectFuture.getChannel());
        apmClientHandler.setConnectSocketAddress(address);
        
        ConnectFuture handlerConnectFuture = apmClientHandler.getConnectFuture();
        handlerConnectFuture.awaitUninterruptibly();
        
        if (ConnectFuture.Result.FAIL == handlerConnectFuture.getResult()) {
            throw new ApmSocketException("connect fail to " + address + ".", channelConnectFuture.getCause());
        }

        return apmClientHandler;
    }

    public ChannelFuture reconnect(final SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        ChannelPipeline pipeline;
        final ClientBootstrap bootstrap = this.bootstrap;
        try {
            pipeline = bootstrap.getPipelineFactory().getPipeline();
        } catch (Exception e) {
            throw new ChannelPipelineException("Failed to initialize a pipeline.", e);
        }
        ApmClientHandler apmClientHandler = (DefaultApmClientHandler) pipeline.getLast();
        apmClientHandler.initReconnect();


        // Set the options.
        Channel ch = bootstrap.getFactory().newChannel(pipeline);
        boolean success = false;
        try {
            ch.getConfig().setOptions(bootstrap.getOptions());
            success = true;
        } finally {
            if (!success) {
                ch.close();
            }
        }

        // Connect.
        return ch.connect(remoteAddress);
    }

    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        return this.timer.newTimeout(task, delay, unit);
    }


    private ApmClientHandler getSocketHandler(Channel channel) {
        return (ApmClientHandler) channel.getPipeline().getLast();
    }

    void reconnect(final ApmClient apmClient, final SocketAddress socketAddress) {
        ConnectEvent connectEvent = new ConnectEvent(apmClient, socketAddress);
        timer.newTimeout(connectEvent, reconnectDelay, TimeUnit.MILLISECONDS);
    }

    private class ConnectEvent implements TimerTask {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final ApmClient apmClient;
        private final SocketAddress socketAddress;

        private ConnectEvent(ApmClient apmClient, SocketAddress socketAddress) {
            if (apmClient == null) {
                throw new NullPointerException("apmClient must not be null");
            }
            if (socketAddress == null) {
                throw new NullPointerException("socketAddress must not be null");
            }

            this.apmClient = apmClient;
            this.socketAddress = socketAddress;
        }

        @Override
        public void run(Timeout timeout) {
            if (timeout.isCancelled()) {
                return;
            }

            // Just return not to try reconnection when event has been fired but apmClient already closed.
            if (apmClient.isClosed()) {
                logger.debug("apmClient is already closed.");
                return;
            }

            logger.warn("try reconnect. connectAddress:{}", socketAddress);
            final ChannelFuture channelFuture = reconnect(socketAddress);
            Channel channel = channelFuture.getChannel();
            final ApmClientHandler apmClientHandler = getSocketHandler(channel);
            apmClientHandler.setConnectSocketAddress(socketAddress);
            apmClientHandler.setApmClient(apmClient);

            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        Channel channel = future.getChannel();
                        logger.warn("reconnect success {}, {}", socketAddress, channel);
                        apmClient.reconnectSocketHandler(apmClientHandler);
                    } else {
                         if (!apmClient.isClosed()) {

                         /*
                            // comment out because exception message can be taken at exceptionCaught
                            if (logger.isWarnEnabled()) {
                                Throwable cause = future.getCause();
                                logger.warn("reconnect fail. {} Caused:{}", socketAddress, cause.getMessage());
                            }
                          */
                            reconnect(apmClient, socketAddress);
                        } else {
                            logger.info("apmClient is closed. stop reconnect.");
                        }
                    }
                }
            });
        }
    }


    public void release() {
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
        }

        if (bootstrap != null) {
            bootstrap.releaseExternalResources();
        }
        Set<Timeout> stop = this.timer.stop();
        if (!stop.isEmpty()) {
            logger.info("stop Timeout:{}", stop.size());
        }

        // stop, cancel something?
    }

    Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> agentProperties) {
        AssertUtils.assertNotNull(properties, "agentProperties must not be null");

        this.properties = Collections.unmodifiableMap(agentProperties);
    }

    public ClusterOption getClusterOption() {
        return clusterOption;
    }

    public void setClusterOption(String id, List<Role> roles) {
        this.clusterOption = new ClusterOption(true, id, roles);
    }

    public void setClusterOption(ClusterOption clusterOption) {
        this.clusterOption = clusterOption;
    }

    public MessageListener getMessageListener() {
        return messageListener;
    }
    
    public MessageListener getMessageListener(MessageListener defaultMessageListener) {
        if (messageListener == null) {
            return defaultMessageListener;
        }
        
        return messageListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        AssertUtils.assertNotNull(messageListener, "messageListener must not be null");

        this.messageListener = messageListener;
    }

    public ServerStreamChannelMessageListener getServerStreamChannelMessageListener() {
        return serverStreamChannelMessageListener;
    }
    
    public ServerStreamChannelMessageListener getServerStreamChannelMessageListener(ServerStreamChannelMessageListener defaultStreamMessageListener) {
        if (serverStreamChannelMessageListener == null) {
            return defaultStreamMessageListener;
        }
        
        return serverStreamChannelMessageListener;
    }
    
    public void setServerStreamChannelMessageListener(ServerStreamChannelMessageListener serverStreamChannelMessageListener) {
        AssertUtils.assertNotNull(messageListener, "messageListener must not be null");

        this.serverStreamChannelMessageListener = serverStreamChannelMessageListener;
    }

    public List<StateChangeEventListener> getStateChangeEventListeners() {
        return new ArrayList<StateChangeEventListener>(stateChangeEventListeners);
    }

    public void addStateChangeEventListener(StateChangeEventListener stateChangeEventListener) {
        this.stateChangeEventListeners.add(stateChangeEventListener);
    }

    boolean isReleased() {
        return released;
    }

    int issueNewSocketId() {
        return socketId.getAndIncrement();
    }

}
