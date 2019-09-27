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

package org.wso2.ei.tools.converter.common.builder;

import java.util.HashMap;
import java.util.Map;

public class NameGenerator {

    private static NameGenerator instance;
    private Map<String, Integer> genListenerCount;
    private Map<String, Integer> genServiceCount;

    public static final String PREFIX_LISTENER_ = "_LISTENER_";
    public static final String PREFIX_SERVICE_ = "_SERVICE_";

    private NameGenerator() {
        this.genListenerCount = new HashMap<>();
        this.genServiceCount = new HashMap<>();
    }

    public static NameGenerator getInstance() {
        if (instance == null) {
            instance = new NameGenerator();
        }
        return instance;
    }

    public String generateListenerName(String baseName) {
        String genBaseStr = PREFIX_LISTENER_.concat(baseName);
        return genBaseStr.concat(getNextInt(genListenerCount, genBaseStr).toString());
    }

    public String generateServiceName(String baseName) {
        String genBaseStr = PREFIX_SERVICE_.concat(baseName);
        return genBaseStr.concat(getNextInt(genServiceCount, genBaseStr).toString());
    }


    private static Integer getNextInt(Map<String, Integer> targetMap, String baseName) {
        Integer genInt = targetMap.get(baseName);
        if (genInt == null) {
            genInt = 0;
        }
        targetMap.put(baseName, genInt + 1);
        return genInt;
    }

    /*
    * Utilities functions for name generation
    * */

    /**
     * Function to clean up given context path by removing special characters
     * example: "/test/testpath" will be transformed to "_test_testpath" by replacing '/' by '_'
     * example usage: name generation for a service using basePath
     *
     * @param path path to cleanup
     * @return path removing special characters
     */
    public static String cleanupPath(String path) {
        return path.replace('/', '_');
    }
}
