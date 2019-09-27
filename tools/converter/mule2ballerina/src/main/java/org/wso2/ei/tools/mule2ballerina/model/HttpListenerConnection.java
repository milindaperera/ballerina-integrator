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

import org.wso2.ei.tools.converter.common.BallerinaConverterException;
import org.wso2.ei.tools.mule2ballerina.dto.DataCarrierDTO;

/**
 * {@code HttpListenerConnection} represents mule http:listener-connection element
 *
 * <http:listener-config name="helloWorldListenerConfig" basePath="/helloworld">
 *     <http:listener-connection host="0.0.0.0" port="9091" connectionIdleTimeout="60000"/>
 * </http:listener-config>
 */
public class HttpListenerConnection extends BaseObject {

    private String host;
    private String port;
    private long connectionIdleTimeout;

    @Override
    public void buildTree(DataCarrierDTO dataCarrierDTO) {
        Root root = dataCarrierDTO.getRootObject();
        GlobalConfiguration listnerConfig = root.getGlobalConfigStack().peek();
        if (listnerConfig instanceof HttpListenerConfig) {
            ((HttpListenerConfig) listnerConfig).setListenerConnection(this);
        } else {
            throw new BallerinaConverterException("http:listener-connection is not wrapper within http:listener-config");
        }
    }

    public String getHost() {

        return host;
    }

    public void setHost(String host) {

        this.host = host;
    }

    public String getPort() {

        return port;
    }

    public void setPort(String port) {

        this.port = port;
    }

    public long getConnectionIdleTimeout() {

        return connectionIdleTimeout;
    }

    public void setConnectionIdleTimeout(long connectionIdleTimeout) {

        this.connectionIdleTimeout = connectionIdleTimeout;
    }
}
