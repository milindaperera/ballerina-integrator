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

package org.wso2.ei.tools.mule2ballerina.visitor;

import org.ballerinalang.model.tree.statements.StatementNode;
import org.ballerinalang.net.http.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ei.tools.converter.common.builder.BallerinaCompilationUnitBuilder;
import org.wso2.ei.tools.converter.common.builder.BuilderUtil;
import org.wso2.ei.tools.converter.common.builder.NameGenerator;
import org.wso2.ei.tools.converter.common.generator.Constants;
import org.wso2.ei.tools.mule2ballerina.model.AsynchronousTask;
import org.wso2.ei.tools.mule2ballerina.model.Comment;
import org.wso2.ei.tools.mule2ballerina.model.Flow;
import org.wso2.ei.tools.mule2ballerina.model.FlowReference;
import org.wso2.ei.tools.mule2ballerina.model.GlobalConfiguration;
import org.wso2.ei.tools.mule2ballerina.model.HttpListener;
import org.wso2.ei.tools.mule2ballerina.model.HttpListenerConfig;
import org.wso2.ei.tools.mule2ballerina.model.HttpRequest;
import org.wso2.ei.tools.mule2ballerina.model.HttpRequestConfig;
import org.wso2.ei.tools.mule2ballerina.model.Listener;
import org.wso2.ei.tools.mule2ballerina.model.Payload;
import org.wso2.ei.tools.mule2ballerina.model.PropertyRemover;
import org.wso2.ei.tools.mule2ballerina.model.PropertySetter;
import org.wso2.ei.tools.mule2ballerina.model.Root;
import org.wso2.ei.tools.mule2ballerina.model.VariableRemover;
import org.wso2.ei.tools.mule2ballerina.model.VariableSetter;
import org.wso2.ei.tools.mule2ballerina.util.ballerina.ModuleConstants;
import org.wso2.ei.tools.mule2ballerina.util.ballerina.MuleModuleUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

/**
 * {@code TreeVisitor} visits intermediate object stack and populate Ballerina AST
 */
public class TreeVisitor implements Visitor {

    private static final Logger LOG = LoggerFactory.getLogger(TreeVisitor.class);

    private Root root;
    private BallerinaCompilationUnitBuilder cuBuilder;

    private Map<String, String> listenerConfig2ServiceMap;
    private Map<String, BLangFunction> flow2FunctionMap;
    private Stack<Flow> flowStack;

    public TreeVisitor(Root mRoot) {
        this.root = mRoot;
        this.cuBuilder = new BallerinaCompilationUnitBuilder(mRoot.getName());
        this.listenerConfig2ServiceMap = new HashMap<>();
        this.flowStack = new Stack<>();
        this.flow2FunctionMap = new HashMap<>();
    }

    public BLangCompilationUnit visit() {
        visit(this.root);
        // Build compilation unit
        return cuBuilder.build();
    }

    /**
     * Visit Root. Main flows and private flows are visited separately as they serve two different purposes.
     *
     * @param root
     */
    @Override
    public void visit(Root root) {

        cuBuilder.addImportPackage(ModuleConstants.EI_ORG_NAME, ModuleConstants.CONTEXT_MODULE_NAME, null, null);
        cuBuilder.addImportPackage(ModuleConstants.EI_ORG_NAME, ModuleConstants.MULE_MODULE_NAME, null, null);

        // Visit Global configurations
        for (GlobalConfiguration globalConfiguration : root.getGlobalConfigurations().values()) {
            globalConfiguration.accept(this);
        }

        Map<String, Queue<Flow>> serviceMap = root.getServiceMap();

        //for (String listenerConfig : serviceMap)
        serviceMap.keySet().forEach(listenerConfig -> {
            serviceMap.get(listenerConfig).forEach(flow -> flow.accept(this));
        });
    }

    /**
     * Navigate flow processors. Flow is equivalent of a resource in Ballerina
     *
     * @param flow
     */
    @Override
    public void visit(Flow flow) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Visit - Flow : name : " + flow.getName());
        }
        flowStack.push(flow);

        if (flow.getListener() != null) {
            // Service resource flow
            flow.getListener().accept(this);
            // Create context
            addStatement(MuleModuleUtil.createContextStmt());
            // process each processor
            flow.getFlowProcessors().forEach(processor -> {
                // Avoid visiting listener since already visited while we reach here
                if (!(processor instanceof Listener)) {
                    processor.accept(this);
                }
            });
            addStatement(MuleModuleUtil.createRespondStmt());
        }
    }

    /**
     * Set the payload of the outbound message. Currently only String,JSON and XML types are supported. All the
     * other types are treated as Strings. First a variable
     * will be created to hold the value and then set the payload with that value.
     *
     * @param payload
     */
    @Override
    public void visit(Payload payload) {
        //BLangLiteral valueExpr = BuilderUtil.createLiteralValue(TypeTags.STRING, payload.getValue(),
                //payload.getValue());
        //BLangSimpleVariableDef statement = BuilderUtil.createSimpleVariableDef("message", valueExpr);
        //flow2FunctionMap.get(flowStack.peek().getName()).getBody().addStatement(statement);
        addStatement(MuleModuleUtil.createSetPayloadStmt(payload.getValue()));
    }

    /**
     * HTTP listener is equivalent of http:ServiceEndpointConfiguration in Ballerina
     *
     * @param listenerConfig
     */
    @Override
    public void visit(HttpListenerConfig listenerConfig) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Visit - HttpListenerConfig : name : " + listenerConfig.getName());
        }
        //TODO : retrieve configs from HttpListenerConfig and set default values if not available
        Map<String, Object> configs = new HashMap<>(2);
        configs.put("timeoutInMillis", listenerConfig.getListenerConnection().getConnectionIdleTimeout());

        //Create ballerina http:ServiceEndpointConfiguration with name of the http:listener-config
        cuBuilder.addHTTPServiceEndpointConfiguration(listenerConfig.getName(), configs);

        //Create ballerina http:Listener with generated name
        BLangSimpleVariable bLangListener =
                cuBuilder.addHTTPListener(Integer.parseInt(listenerConfig.getListenerConnection().getPort()),
                 listenerConfig.getName());

        //Create annotation
        Map<String, Object> annotationRecords = new HashMap<>();
        annotationRecords.put(HttpConstants.ANN_CONFIG_ATTR_BASE_PATH, listenerConfig.getBasePath());

        BLangAnnotationAttachment serviceConfigAnnotation =
                BuilderUtil.createAnnotationAttachment(Constants.PKG_HTTP, HttpConstants.ANN_NAME_HTTP_SERVICE_CONFIG,
                        annotationRecords);
        //Generate service name from basePath
        String genServiceName =
                NameGenerator.getInstance().generateServiceName(NameGenerator.cleanupPath(listenerConfig.getBasePath()));
        cuBuilder.addService(genServiceName, bLangListener.getName().value, serviceConfigAnnotation);
        //Need to keep track of the service name created to represent mule http:listener-config
        listenerConfig2ServiceMap.put(listenerConfig.getName(), genServiceName);
    }

    /**
     * When the inbound connector is encountered, first visit it's global configuration to start the service and then
     * start the resource definition.
     *
     * @param listener
     */
    @Override
    public void visit(HttpListener listener) {
        Map<String, Object> annotationConfigs = new HashMap<>();
        annotationConfigs.put(HttpConstants.ANN_RESOURCE_ATTR_PATH, listener.getPath());

        BLangAnnotationAttachment resourceConfigAnnotation = BuilderUtil.createAnnotationAttachment(Constants.PKG_HTTP,
                HttpConstants.ANN_NAME_RESOURCE_CONFIG, annotationConfigs);

        BLangFunction function = BuilderUtil.createResourceFunction(flowStack.peek().getName(), resourceConfigAnnotation);
        flow2FunctionMap.put(flowStack.peek().getName(), function);

        //Attach the function to the service
        cuBuilder.addFunctionToService(listenerConfig2ServiceMap.get(listener.getConfigName()), function);

    }

    /**
     * HttpRequest represents a http client connector in Ballerina. First visit this element's global config element to
     * populate required values necessary for client connector creation.
     *
     * @param request
     */
    @Override
    public void visit(HttpRequest request) {

    }

    /**
     * HttpRequestConfig contains attributes required to create http client connector in Ballerina.
     *
     * @param requestConfig
     */
    @Override
    public void visit(HttpRequestConfig requestConfig) {

    }

    /**
     * Add a comment in Ballerina code.
     *
     * @param comment
     */
    @Override
    public void visit(Comment comment) {

    }

    /**
     * Prints the logger message in correct log level. In mule, if the message property of logger is not set with any
     * value it print out the whole message property details. In Ballerina, since this is not directly available that
     * is not provided here.
     *
     * @param log
     */
    @Override
    public void visit(org.wso2.ei.tools.mule2ballerina.model.Logger log) {

    }

    /**
     * Add a header to outbound message
     *
     * @param propertySetter
     */
    @Override
    public void visit(PropertySetter propertySetter) {

    }

    /**
     * Remove header from outbound message
     *
     * @param propertyRemover
     */
    @Override
    public void visit(PropertyRemover propertyRemover) {

    }

    /**
     * Create a variable of type string in Ballerina with the mule variable value
     *
     * @param variableSetter
     */
    @Override
    public void visit(VariableSetter variableSetter) {

    }

    /**
     * Set the variable value in Ballerina to null
     *
     * @param variableRemover
     */
    @Override
    public void visit(VariableRemover variableRemover) {

    }

    /**
     * When a flow reference is called, it might either refer to a sub flow or a private flow. In case of a sub flow,
     * add the processors that's been referred in it, in the calling resource. But if it's a private flow call the
     * respective function.
     *
     * @param flowReference
     */
    @Override
    public void visit(FlowReference flowReference) {

    }

    /**
     * AsynchronousTask maps to Workers in Ballerina
     *
     * @param asynchronousTask
     */
    @Override
    public void visit(AsynchronousTask asynchronousTask) {

    }

    /**
     * Function to add ballerina statement to current building function
     *
     * @param statement
     */
    private void addStatement(StatementNode statement) {
        flow2FunctionMap.get(flowStack.peek().getName()).getBody().addStatement(statement);
    }
}
