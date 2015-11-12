/*
 * Copyright 2015 NAVER Corp.
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

package com.baidu.oped.apm.common.util;

import java.io.UnsupportedEncodingException;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;

import com.baidu.oped.apm.thrift.io.DeserializerFactory;
import com.baidu.oped.apm.thrift.io.HeaderTBaseDeserializer;
import com.baidu.oped.apm.thrift.util.SerializationUtils;

/**
 * @author HyunGil Jeong
 */
public class AgentEventMessageDeserializer {
    
    private final DeserializerFactory<HeaderTBaseDeserializer> tBaseDeserializerFactory;
    
    public AgentEventMessageDeserializer(DeserializerFactory<HeaderTBaseDeserializer> tBaseDeserializerFactory) {
        this.tBaseDeserializerFactory = tBaseDeserializerFactory;
    }
    
    public Object deserialize(AgentEventType agentEventType, byte[] eventBody) throws UnsupportedEncodingException {
        if (agentEventType == null) {
            throw new NullPointerException("agentEventType must not be null");
        }
        Class<?> eventMessageType = agentEventType.getMessageType();
        if (eventMessageType == Void.class) {
            return null;
        }
        if (TBase.class.isAssignableFrom(eventMessageType)) {
            try {
                return SerializationUtils.deserialize(eventBody, this.tBaseDeserializerFactory);
            } catch (TException e) {
                throw new UnsupportedEncodingException(e.getMessage());
            }
        } else if (String.class.isAssignableFrom(eventMessageType)) {
            return BytesUtils.toString(eventBody);
        }
        throw new UnsupportedEncodingException("Unsupported event message type [" + eventMessageType.getName() + "]");
    }

}
