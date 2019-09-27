/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ei.tools.mule2ballerina.model;

import org.wso2.ei.tools.converter.common.builder.NameGenerator;

/**
 * Super class of listeners (x:listener) element (http:listener, jms:listener, etc.) for a flow.
 * All the listeners (example: {@link HttpListener}) are extends {@link Listener}
 *
 */
public abstract class Listener extends BaseObject implements Inbound, Processor {

    private String genName; //Generated name for the listener


    public String getGenName() {
        return genName;
    }

    public void setGenName(String genName) {
        this.genName = genName;
    }

    public void generateName() {
        if (genName == null) {
            genName = NameGenerator.getInstance().generateListenerName(getConfigName());
        }
    }
}
