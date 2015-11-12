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

package com.baidu.oped.apm.thrift.io;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.thrift.io.Header;

/**
 * @author emeroad
 */
public class HeaderTest {

    private final Logger logger = LoggerFactory.getLogger(Header.class.getName());

    @Test
    public void testGetSignature() throws Exception {
        Header header = new Header();
        byte signature = header.getSignature();
        short type = header.getType();
        byte version = header.getVersion();
    }
}