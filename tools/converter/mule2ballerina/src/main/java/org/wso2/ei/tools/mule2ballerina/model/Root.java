/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ei.tools.mule2ballerina.model;

import org.wso2.ei.tools.mule2ballerina.visitor.Visitable;
import org.wso2.ei.tools.mule2ballerina.visitor.Visitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@code Root} This is the root of the intermediate object stack
 */
public class Root extends BaseObject implements Visitable {

    private String name;

    private Map<String, GlobalConfiguration> globalConfigurations; //All global configurations against it's name
    private Map<String, Queue<Flow>> serviceMap; //Map of services and it's resources maintained as a queue
    private Map<String, SubFlow> subFlowMap; //All subflows against their names
    private Map<String, Flow> privateFlowMap; //Map of private flows against their names
    private List<AsynchronousTask> asyncTaskList;

    private Stack<Flow> flowStack; //Flows in LIFO order
    private Stack<SubFlow> subFlowStack; //Subflows maintained in FILO order
    private Stack<Flow> privateFlowStack; //Private flow list in FILO order
    private Stack<GlobalConfiguration> globalConfigStack;

    public Root(String name) {
        this.name = name;
        flowStack = new Stack<Flow>();
        globalConfigurations = new HashMap<>();
        serviceMap = new HashMap<>();
        subFlowMap = new HashMap<>();
        subFlowStack = new Stack<>();
        privateFlowMap = new HashMap<>();
        privateFlowStack = new Stack<>();
        globalConfigStack = new Stack<>();
        asyncTaskList = new CopyOnWriteArrayList<AsynchronousTask>();
    }

    public Stack<Flow> getFlowStack() {
        return flowStack;
    }

    public void addGlobalConfiguration(GlobalConfiguration globalConfiguration) {
        this.globalConfigurations.put(globalConfiguration.getName(), globalConfiguration);
        this.globalConfigStack.push(globalConfiguration);
    }

    public Map<String, GlobalConfiguration> getGlobalConfigurations() {
        return globalConfigurations;
    }

    public GlobalConfiguration getGlobalConfiguration(String name) {
        return globalConfigurations.get(name);
    }

    /* Maintain main flows in LIFO order */
    public void addMFlow(Flow flow) {
        this.flowStack.add(flow);
    }

    public void addToPrivateFlowStack(Flow privateFlow) {
        this.privateFlowStack.add(privateFlow);
    }

    public void addSubFlow(String name, SubFlow subFlow) {
        SubFlow subFlowRef = subFlowMap.get(name);
        if (subFlowRef == null) {
            subFlowMap.put(name, subFlow);
        }
        subFlowStack.add(subFlow);
    }

    public void addPrivateFlow(String name, Flow privateFlow) {
        SubFlow subFlowRef = subFlowMap.get(name);
        if (subFlowRef == null) {
            privateFlowMap.put(name, privateFlow);
        }
    }

    public void addAsynchronousTask(AsynchronousTask task) {
        asyncTaskList.add(task);
    }

    public Map<String, Queue<Flow>> getServiceMap() {
        return serviceMap;
    }

    public Map<String, SubFlow> getSubFlowMap() {
        return subFlowMap;
    }

    public Stack<SubFlow> getSubFlowStack() {
        return subFlowStack;
    }

    public Map<String, Flow> getPrivateFlowMap() {
        return privateFlowMap;
    }

    public Stack<Flow> getPrivateFlowStack() {
        return privateFlowStack;
    }

    public List<AsynchronousTask> getAsyncTaskList() {
        return asyncTaskList;
    }

    public String getName() {
        return name;
    }

    public Stack<GlobalConfiguration> getGlobalConfigStack() {
        return globalConfigStack;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
