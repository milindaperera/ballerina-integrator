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

import org.ballerinalang.model.elements.PackageID;
import org.wso2.ballerinalang.compiler.parser.BLangAnonymousModelHelper;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link AnonymousModelHelper} is a util for holding the number of anonymous constructs found so far
 * current package.
 *
 *
 */
public class AnonymousModelHelper {

    private Map<PackageID, Integer> anonTypeCount;
    private Map<PackageID, Integer> anonServiceCount;
    private Map<PackageID, Integer> anonFunctionCount;

    private static final String ANON_TYPE = "$anonType$";
    private static final String LAMBDA = "$lambda$";
    private static final String SERVICE = "$$service$";
    private static final String ANON_SERVICE = "$anonService$";
    private static final String BUILTIN_ANON_TYPE = "$anonType$builtin$";
    private static final String BUILTIN_LAMBDA = "$lambda$builtin$";

    private static AnonymousModelHelper helper;

    private AnonymousModelHelper() {
        anonTypeCount = new HashMap<>();
        anonServiceCount = new HashMap<>();
        anonFunctionCount = new HashMap<>();
    }

    public static AnonymousModelHelper getInstance() {
        if (helper == null) {
            helper = new AnonymousModelHelper();
        }
        return helper;
    }

    /*String getNextAnonymousTypeKey(PackageID packageID) {
        Integer nextValue = Optional.ofNullable(anonTypeCount.get(packageID)).orElse(0);
        anonTypeCount.put(packageID, nextValue + 1);
        if (PackageID.ANNOTATIONS.equals(packageID)) {
            return BUILTIN_ANON_TYPE + nextValue;
        }
        return ANON_TYPE + nextValue;
    }*/

    public String getNextAnonymousServiceTypeKey(PackageID packageID, String serviceName) {
        Integer nextValue = Optional.ofNullable(anonServiceCount.get(packageID)).orElse(0);
        anonServiceCount.put(packageID, nextValue + 1);
        return serviceName + SERVICE + nextValue;
    }

    /*String getNextAnonymousServiceVarKey(PackageID packageID) {
        Integer nextValue = Optional.ofNullable(anonServiceCount.get(packageID)).orElse(0);
        anonServiceCount.put(packageID, nextValue + 1);
        return ANON_SERVICE + nextValue;
    }

    public String getNextAnonymousFunctionKey(PackageID packageID) {
        Integer nextValue = Optional.ofNullable(anonFunctionCount.get(packageID)).orElse(0);
        anonFunctionCount.put(packageID, nextValue + 1);
        return LAMBDA + nextValue;
    }

    public boolean isAnonymousType(BSymbol symbol) {
        return symbol.name.value.startsWith(ANON_TYPE);
    }*/
}
