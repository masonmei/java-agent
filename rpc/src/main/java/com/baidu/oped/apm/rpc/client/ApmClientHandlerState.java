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

import com.baidu.oped.apm.rpc.ApmSocket;
import com.baidu.oped.apm.rpc.StateChangeEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.common.SocketState;
import com.baidu.oped.apm.rpc.common.SocketStateChangeResult;
import com.baidu.oped.apm.rpc.common.SocketStateCode;

import java.util.List;

/**
 * @author Taejin Koo
 */
public class ApmClientHandlerState {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DefaultApmClientHandler clientHandler;
    private final List<StateChangeEventListener> stateChangeEventListeners;

    private final SocketState state;
    
    public ApmClientHandlerState(DefaultApmClientHandler clientHandler, List<StateChangeEventListener> stateChangeEventListeners) {
        this.clientHandler = clientHandler;
        this.stateChangeEventListeners = stateChangeEventListeners;

        this.state = new SocketState();
    }

    SocketStateChangeResult toBeingConnect() {
        SocketStateCode nextState = SocketStateCode.BEING_CONNECT;
        return to(nextState);
    }
    
    SocketStateChangeResult toConnected() {
        SocketStateCode nextState = SocketStateCode.CONNECTED;
        return to(nextState);
    }

    SocketStateChangeResult toConnectFailed() {
        SocketStateCode nextState = SocketStateCode.CONNECT_FAILED;
        return to(nextState);
    }

    SocketStateChangeResult toRunWithoutHandshake() {
        SocketStateCode nextState = SocketStateCode.RUN_WITHOUT_HANDSHAKE;
        return to(nextState);
    }

    SocketStateChangeResult toRunSimplex() {
        SocketStateCode nextState = SocketStateCode.RUN_SIMPLEX;
        return to(nextState);
    }

    SocketStateChangeResult toRunDuplex() {
        SocketStateCode nextState = SocketStateCode.RUN_DUPLEX;
        return to(nextState);
    }

    SocketStateChangeResult toBeingClose() {
        SocketStateCode nextState = SocketStateCode.BEING_CLOSE_BY_CLIENT;
        return to(nextState);
    }

    SocketStateChangeResult toBeingCloseByPeer() {
        SocketStateCode nextState = SocketStateCode.BEING_CLOSE_BY_SERVER;
        return to(nextState);
    }

    SocketStateChangeResult toClosed() {
        SocketStateCode nextState = SocketStateCode.CLOSED_BY_CLIENT;
        return to(nextState);
    }

    SocketStateChangeResult toClosedByPeer() {
        SocketStateCode nextState = SocketStateCode.CLOSED_BY_SERVER;
        return to(nextState);
    }

    SocketStateChangeResult toUnexpectedClosed() {
        SocketStateCode nextState = SocketStateCode.UNEXPECTED_CLOSE_BY_CLIENT;
        return to(nextState);
    }

    SocketStateChangeResult toUnexpectedClosedByPeer() {
        SocketStateCode nextState = SocketStateCode.UNEXPECTED_CLOSE_BY_SERVER;
        return to(nextState);
    }

    SocketStateChangeResult toErrorUnknown() {
        SocketStateCode nextState = SocketStateCode.ERROR_UNKNOWN;
        return to(nextState);
    }
    
    private SocketStateChangeResult to(SocketStateCode nextState) {
        String objectName = clientHandler.getObjectName();
        ApmSocket apmSocket = clientHandler.getApmClient();

        logger.debug("{} stateTo() started. to:{}", objectName, nextState);

        SocketStateChangeResult stateChangeResult = state.to(nextState);
        if (stateChangeResult.isChange()) {
            executeChangeEventHandler(apmSocket, nextState);
        }

        logger.info("{} stateTo() completed. {}", objectName, stateChangeResult);
        return stateChangeResult;
    }

    private void executeChangeEventHandler(ApmSocket apmSocket, SocketStateCode nextState) {
        for (StateChangeEventListener eachListener : this.stateChangeEventListeners) {
            try {
                eachListener.eventPerformed(apmSocket, nextState);
            } catch (Exception e) {
                eachListener.exceptionCaught(apmSocket, nextState, e);
            }
        }
    }

    boolean isBeforeConnected(SocketStateCode currentStateCode) {
        return SocketStateCode.isBeforeConnected(currentStateCode);
    }
    
    boolean isEnableCommunication() {
        return SocketStateCode.isRun(getCurrentStateCode());
    }
    
    boolean isEnableCommunication(SocketStateCode currentStateCode) {
        return SocketStateCode.isRun(currentStateCode);
    }

    boolean isEnableDuplexCommunication() {
        return SocketStateCode.isRunDuplex(getCurrentStateCode());
    }
    
    boolean isClosed() {
        return SocketStateCode.isClosed(getCurrentStateCode());
    }

    boolean isClosed(SocketStateCode currentStateCode) {
        return SocketStateCode.isClosed(currentStateCode);
    }
    
    boolean onClose(SocketStateCode currentStateCode) {
        return SocketStateCode.onClose(currentStateCode);
    }

    boolean isReconnect(SocketStateCode currentStateCode) {
        if (currentStateCode == SocketStateCode.BEING_CLOSE_BY_SERVER) {
            return true;
        }

        if (currentStateCode == SocketStateCode.CLOSED_BY_SERVER) {
            return true;
        }
        
        if (currentStateCode == SocketStateCode.UNEXPECTED_CLOSE_BY_SERVER) {
            return true;
        }
        
        return false;
    }

    SocketStateCode getCurrentStateCode() {
        return state.getCurrentState();
    }
    
    @Override
    public String toString() {
        return state.toString();
    }
    
}
