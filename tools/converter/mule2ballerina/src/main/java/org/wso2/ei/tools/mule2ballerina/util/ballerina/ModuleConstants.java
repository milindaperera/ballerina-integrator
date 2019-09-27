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

package org.wso2.ei.tools.mule2ballerina.util.ballerina;

/**
 * Class to hold constants related to ballerina Mule Module
 */
public class ModuleConstants {
    public static final String EI_ORG_NAME = "ei";

    public static final String MULE_MODULE_NAME = "Mule";

    public static final String MULE_MODULE_FUNC_SET_PAYLOAD = "setPayload";
    public static final String MULE_MODULE_FUNC_RESPOND = "respond";

    public static final String CONTEXT_MODULE_NAME = "Context";
    public static final String CONTEXT_MODULE_TYPE_MESSAGECONTEXT = "MessageContext";

    /**
     * Ballerina variable names
     */
    public static final String BLANG_VAR_INCOMING_REQUEST = "request";
    public static final String BLANG_VAR_INCOMING_CALLER = "caller";
    public static final String BLANG_VAR_CONTEXT = "context";
    public static final String BLANG_VAR_RESULT = "result";

}
