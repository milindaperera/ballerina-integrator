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

import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.TopLevelNode;
import org.ballerinalang.model.tree.types.ObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangServiceConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ei.tools.converter.common.generator.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code BallerinaCompilationUnitBuilder} is a High level wrapper API to build
 * {@link org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit}
 */
public class BallerinaCompilationUnitBuilder {

    private String name;
    private List<TopLevelNode> imports;
    private List<TopLevelNode> globalConfigurations;
    private List<TopLevelNode> services;

    // Temporary holding data structures
    private Map<String, BLangImportPackage> importDeclarations;
    private Map<String, TopLevelNode> globalConfigurationsMap;
    private Map<String, BLangService> serviceDefinitions; // Service name against BLangService
    private Map<String, BLangAnnotationAttachment> annotationAttachments; //

    private AnonymousModelHelper anonymousModelHelper;

    // TODO This is temporary package ID
    private PackageID pkgID;


    public BallerinaCompilationUnitBuilder(String name) {
        this.name = name;
        this.imports = new ArrayList<>();
        this.globalConfigurations = new ArrayList<>();
        this.services = new ArrayList<>();
        this.importDeclarations = new HashMap<>();
        this.globalConfigurationsMap = new HashMap<>();
        this.serviceDefinitions = new HashMap<>();
        this.anonymousModelHelper = AnonymousModelHelper.getInstance();
        this.pkgID = new PackageID(name);
    }

    /**
     * Function to add import package
     *
     * @param orgName
     * @param moduleName
     * @param version
     * @param alias
     */
    public void addImportPackage(String orgName, String moduleName, String version, String alias) {

        if (!importDeclarations.containsKey(orgName + "/" + moduleName)) {
            BLangImportPackage importPkg = BuilderUtil.createImportPackage(orgName, moduleName, version, alias);
            imports.add(importPkg);
            importDeclarations.put(orgName + "/" + moduleName, importPkg);
        }
    }

    /**
     * Function to add http:ServiceEndpointConfiguration (set of configurations for HTTP service endpoints)
     *
     * @param name name of the ServiceEndpointConfiguration
     * @param configurations configurations
     */
    public BLangSimpleVariable addHTTPServiceEndpointConfiguration(String name, Map<String, Object> configurations) {
        addImportPackage(Constants.ORG_BALLERINA, Constants.PKG_HTTP, null, null);

        BLangUserDefinedType serviceEPConfigType = BuilderUtil.createUserDefinedType(Constants.PKG_HTTP,
                "ServiceEndpointConfiguration");
        BLangRecordLiteral serviceConfigRecords = BuilderUtil.createRecordLiteralNode(configurations);
        BLangSimpleVariable serviceConfigVar = BuilderUtil.createSimpleVariableWithExpression(serviceEPConfigType,
                name, serviceConfigRecords);
        globalConfigurations.add(serviceConfigVar);
        globalConfigurationsMap.put(name, serviceConfigVar);

        return serviceConfigVar;
    }

    /**
     * Function to add HTTP listener config
     *
     * @param port port
     * @param configName name of the config
     */
    public BLangSimpleVariable addHTTPListener(int port, String configName) {
        addImportPackage(Constants.ORG_BALLERINA, Constants.PKG_HTTP, null, null);
        BLangLiteral portLiteral = BuilderUtil.createLiteralValue(TypeTags.INT, port, Integer.toString(port));
        BLangNamedArgsExpression listenerConfigArg =
                BuilderUtil.createNamedArg("config", BuilderUtil.createSimpleVariableReference(null, configName));

        BLangTypeInit initNode = (BLangTypeInit) TreeBuilder.createInitNode();
        initNode.argsExpr.add(portLiteral);
        initNode.argsExpr.add(listenerConfigArg);

        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        invocationNode.name = BuilderUtil.createIdentifier(Constants.NEW_STR);
        invocationNode.argExprs.add(portLiteral);
        invocationNode.argExprs.add(listenerConfigArg);

        initNode.initInvocation = invocationNode;

        BLangUserDefinedType listenerEPConfigType = BuilderUtil.createUserDefinedType(Constants.PKG_HTTP, "Listener");
        String listenerName = NameGenerator.getInstance().generateListenerName(configName);
        BLangSimpleVariable httpListener =
                BuilderUtil.createSimpleVariableWithExpression(listenerEPConfigType, listenerName, initNode);

        httpListener.flagSet.add(Flag.LISTENER);
        globalConfigurations.add(httpListener);

        return httpListener;
    }


    public BLangService addService(String serviceName, String attachedListenerName, BLangAnnotationAttachment annotation) {
        List<BLangExpression> expressions = new ArrayList<>(1);
        expressions.add(BuilderUtil.createSimpleVariableReference(null, attachedListenerName));

        BLangService bLangService = BuilderUtil.createService(serviceName, expressions,
                this.anonymousModelHelper.getNextAnonymousServiceTypeKey(pkgID, serviceName), annotation);
        serviceDefinitions.put(serviceName, bLangService);
        services.add(bLangService);

        return bLangService;
    }

    public void addFunctionToService(String serviceName, BLangFunction function) {
        BLangService service = serviceDefinitions.get(serviceName);
        ObjectTypeNode objectTypeNode = (ObjectTypeNode)service.getTypeDefinition().getTypeNode();
        objectTypeNode.addFunction(function);
    }

    public BLangService getServiceByName(String serviceName) {
        return serviceDefinitions.get(serviceName);
    }

    /*
     * Utility functions
     */

    public TopLevelNode getGlobalConfigurationByName(String name) {
        return globalConfigurationsMap.get(name);
    }







    /**
     * TODO Cleanup later
     * @param bLangAnnotationAttachment
     */
    public void creatingService(BLangAnnotationAttachment bLangAnnotationAttachment) {
        BLangSimpleVarRef varRef = (BLangSimpleVarRef) TreeBuilder.createSimpleVariableReferenceNode();
        varRef.pkgAlias = BuilderUtil.createIdentifier(null);
        varRef.variableName = BuilderUtil.createIdentifier("helloWorldEP");

        String serviceName = "helloService";
        BLangService serviceNode = (BLangService) TreeBuilder.createServiceNode();
        serviceNode.addAnnotationAttachment(bLangAnnotationAttachment);
        serviceNode.setName(BuilderUtil.createIdentifier(serviceName));
        serviceNode.isAnonymousServiceValue = false;
        serviceNode.getAttachedExprs().add(varRef);

        BLangObjectTypeNode objectTypeNode = (BLangObjectTypeNode) TreeBuilder.createObjectTypeNode();
        objectTypeNode.addFunction(BuilderUtil.createResourceFunction("sayHello", null));
        objectTypeNode.isAnonymous = false;
        objectTypeNode.isFieldAnalyseRequired = false;
        objectTypeNode.flagSet.add(Flag.SERVICE);

        // 1) Define type nodeDefinition for service type.
        String serviceTypeName = this.anonymousModelHelper.getNextAnonymousServiceTypeKey(pkgID, serviceName);
        BLangTypeDefinition serviceTypeDef = (BLangTypeDefinition) TreeBuilder.createTypeDefinition();
        serviceTypeDef.setName(BuilderUtil.createIdentifier(serviceTypeName));
        serviceTypeDef.flagSet.add(Flag.SERVICE);
        serviceTypeDef.typeNode = objectTypeNode;

        serviceNode.serviceTypeDefinition = serviceTypeDef;

        // 2) Create service constructor.
        BLangServiceConstructorExpr serviceConstructorNode =
                (BLangServiceConstructorExpr) TreeBuilder.createServiceConstructorNode();
        serviceConstructorNode.serviceNode = serviceNode;

        // Crate Global variable for service.
        BLangSimpleVariable serviceVar = BuilderUtil.createSimpleVariableWithoutType(serviceName, serviceConstructorNode);
        serviceVar.flagSet.add(Flag.FINAL);
        serviceVar.flagSet.add(Flag.SERVICE);
        serviceVar.typeNode = BuilderUtil.createUserDefinedType(null, serviceTypeDef.name.getValue());
        serviceNode.variableNode = serviceVar;

        services.add(serviceNode);
        services.add(serviceTypeDef);
        services.add(serviceVar);
    }

    public void test() {


    }

    public BLangCompilationUnit build() {

        BLangCompilationUnit cu = (BLangCompilationUnit) TreeBuilder.createCompilationUnit();
        cu.setName(this.name);

        // Add imports
        imports.forEach(cu::addTopLevelNode);

        // Add global configurations
        globalConfigurations.forEach(cu::addTopLevelNode);

        // Add services
        services.forEach(cu::addTopLevelNode);

        return cu;
    }
}
