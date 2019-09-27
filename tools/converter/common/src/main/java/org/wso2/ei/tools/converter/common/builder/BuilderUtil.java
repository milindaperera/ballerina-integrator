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
import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.model.tree.expressions.ExpressionNode;
import org.ballerinalang.model.tree.types.TypeNode;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNumericLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangServiceConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangVariableReference;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ei.tools.converter.common.generator.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BuilderUtil {

    private static SymbolTable symTable;

    static {
        symTable = SymbolTable.getInstance(new CompilerContext());
    }

    /**
     * Util function to create a {@code org.wso2.ballerinalang.compiler.tree.BLangIdentifier}
     * @param value
     * @return
     */
    public static BLangIdentifier createIdentifier(String value) {
        BLangIdentifier node = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        if (value == null) {
            return node;
        }
        node.setValue(value);
        node.setLiteral(false);

        return node;
    }

    public static BLangUserDefinedType createUserDefinedType(String pkgAlias, String name) {
        BLangUserDefinedType userDefinedType = (BLangUserDefinedType) TreeBuilder.createUserDefinedTypeNode();
        userDefinedType.pkgAlias = createIdentifier(pkgAlias);
        userDefinedType.typeName = createIdentifier(name);
        return userDefinedType;
    }

    public static BLangRecordLiteral createRecordLiteralNode(Map<String, Object> records) {
        BLangRecordLiteral record = (BLangRecordLiteral) TreeBuilder.createRecordLiteralNode();
        if (records != null) {
            records.forEach((name, object) -> {
                //Create record key
                BLangSimpleVarRef keyExpr = createSimpleVariableReference("", name);
                BLangRecordLiteral.BLangRecordKey recordKey = new BLangRecordLiteral.BLangRecordKey(keyExpr);

                //Create record value
                int type = TypeTags.STRING;
                String originalValue;
                if (object instanceof Integer) {
                    type = TypeTags.INT;
                    originalValue = String.valueOf(object);
                } else {
                    originalValue = Constants.QUOTE_STR + object.toString() + Constants.QUOTE_STR;
                }
                BLangLiteral recordValue = createLiteralValue(type, object, originalValue);

                record.keyValuePairs.add(createRecordKeyValue(recordKey, recordValue));
            });
        }

        return record;
    }

    public static BLangRecordLiteral.BLangRecordKeyValue createRecordKeyValue(BLangRecordLiteral.BLangRecordKey recordKey,
                                                                              BLangExpression valueExpr) {
        BLangRecordLiteral.BLangRecordKeyValue keyValue =
                (BLangRecordLiteral.BLangRecordKeyValue) TreeBuilder.createRecordKeyValue();
        keyValue.key = recordKey;
        keyValue.valueExpr = valueExpr;
        return keyValue;
    }


    /**
     * Function to create ballerina literal value
     *
     * @param type
     * @param value
     * @param originalValue
     * @return
     */
    public static BLangLiteral createLiteralValue(int type, Object value, String originalValue) {
        BLangLiteral litExpr;
        // If numeric literal create a numeric literal expression; otherwise create a literal expression
        if (type < TypeTags.DECIMAL) {
            litExpr = (BLangNumericLiteral) TreeBuilder.createNumericLiteralExpression();
        } else {
            litExpr = (BLangLiteral) TreeBuilder.createLiteralExpression();
        }
        litExpr.type = symTable.getTypeFromTag(type);
        litExpr.type.tag = type;
        litExpr.value = value;
        litExpr.originalValue = originalValue;

        return litExpr;
    }

    /**
     * Create declared Simple Variable
     *
     *  example: http:Caller caller
     *
     * @param identifier
     * @return
     */
    public static BLangSimpleVariable createSimpleVariable(BLangType type, String identifier) {
        BLangSimpleVariable var = (BLangSimpleVariable) TreeBuilder.createSimpleVariableNode();
        var.setName(createIdentifier(identifier));
        var.setTypeNode(type);
        return var;
    }

    /**
     * Create declared Simple Variable with expression
     *
     * @param identifier
     * @param expression
     * @return
     */
    public static BLangSimpleVariable createSimpleVariableWithExpression(BLangType type, String identifier,
                                                               BLangExpression expression) {
        BLangSimpleVariable var = (BLangSimpleVariable) TreeBuilder.createSimpleVariableNode();
        var.setName(createIdentifier(identifier));
        var.setTypeNode(type);
        var.setInitialExpression(expression);
        return var;
    }

    public static BLangSimpleVariable createSimpleVariableWithoutType(String identifier, BLangExpression expression) {
        BLangSimpleVariable var = (BLangSimpleVariable) TreeBuilder.createSimpleVariableNode();
        var.setName(createIdentifier(identifier));
        if (expression != null) {
            var.setInitialExpression(expression);
        }
        return var;
    }

    public static BLangSimpleVarRef createSimpleVariableReference(String pkgName, String variableName) {
        BLangSimpleVarRef varRef = (BLangSimpleVarRef) TreeBuilder.createSimpleVariableReferenceNode();
        varRef.pkgAlias = createIdentifier(pkgName);
        varRef.variableName = createIdentifier(variableName);
        return varRef;
    }

    /**
     *  Function to create simple variable definition statement
     *
     *      Grammar : VAR Identifier ASSIGN expression SEMICOLON
     *      Example 1 : var result = caller->respond(message);
     *      Example 2 : var message = "Hello World !!";
     *
     * @param identifier
     * @param expression
     */
    public static BLangSimpleVariableDef createSimpleVariableDef(String identifier, BLangExpression expression) {
        BLangSimpleVariable var = (BLangSimpleVariable) TreeBuilder.createSimpleVariableNode();
        BLangSimpleVariableDef varDefNode = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();

        var.setName(createIdentifier(identifier));
        var.isDeclaredWithVar = true;
        var.setInitialExpression(expression);

        varDefNode.setVariable(var);
        return varDefNode;
    }

    /**
     *  Function to create simple variable definition statement
     *
     *      Grammar : VAR Identifier ASSIGN expression SEMICOLON
     *      Example 1 : error result = caller->respond(message);
     *      Example 2 : string message = "Hello World !!";
     *
     * @param identifier
     * @param expression
     */
    public static BLangSimpleVariableDef createSimpleVariableDefWithType(TypeNode type, String identifier,
                                                                                BLangExpression expression) {
        BLangSimpleVariable var = (BLangSimpleVariable) TreeBuilder.createSimpleVariableNode();
        BLangSimpleVariableDef varDefNode = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();

        var.setName(createIdentifier(identifier));
        var.setInitialExpression(expression);
        var.setTypeNode(type);

        varDefNode.setVariable(var);
        return varDefNode;
    }

    /**
     * Function to create type conversion (expression) of variable reference using annotation
     *  Grammar: LT annotationAttachment GT nameReference
     *  Example: <@untained> request
     *
     * @param annotation
     * @param variable
     * @return
     */
    public static BLangTypeConversionExpr createTypeConversionExpr(BLangAnnotationAttachment annotation,
                                                                   BLangSimpleVarRef variable) {
        BLangTypeConversionExpr typeConversionNode = (BLangTypeConversionExpr) TreeBuilder.createTypeConversionNode();
        typeConversionNode.addAnnotationAttachment(annotation);
        typeConversionNode.expr = variable;

        return typeConversionNode;
    }

    /**
     * Function to create type init expression (without type)
     *  Grammar: NEW LEFT_PARENTHESIS invocationArgList? RIGHT_PARENTHESIS
     *  Example: new(<@untained> request)
     * @return
     */
    public static BLangTypeInit createTypeInitExpr(List<BLangExpression> invocationArgList) {
        BLangTypeInit initNode = (BLangTypeInit) TreeBuilder.createInitNode();
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        invocationNode.name = createIdentifier("new");
        invocationNode.pkgAlias = createIdentifier("");

        invocationArgList.forEach(expr -> {
            invocationNode.argExprs.add(expr);
            initNode.argsExpr.add(expr);
        });

        initNode.initInvocation = invocationNode;
        return initNode;
    }

    /**
     * Util function to create import package node
     *
     * @param orgName
     * @param moduleName
     * @param version
     * @param alias
     * @return
     */
    public static BLangImportPackage createImportPackage(String orgName, String moduleName, String version,
                                                         String alias) {

        List<BLangIdentifier> comps = new ArrayList<>();
        Arrays.stream(moduleName.split("\\.")).forEach(strPart -> comps.add(createIdentifier(strPart)));

        BLangImportPackage importNode = (BLangImportPackage) TreeBuilder.createImportPackageNode();
        importNode.pkgNameComps = comps;
        importNode.version = createIdentifier(version);
        importNode.orgName = createIdentifier(orgName);
        importNode.alias = (alias != null && !alias.isEmpty()) ? createIdentifier(alias) : comps.get(comps.size() - 1);

        return importNode;
    }

    /**
     * Function to create annotation
     *
     * @param annotationPkg
     * @param annotationName
     * @param annotationRecordEntries
     * @return
     */
    public static BLangAnnotationAttachment createAnnotationAttachment(String annotationPkg, String annotationName,
                                                                       Map<String, Object> annotationRecordEntries) {
        BLangRecordLiteral annotationRecord = BuilderUtil.createRecordLiteralNode(annotationRecordEntries);

        BLangAnnotationAttachment annotationAttachment =
                (BLangAnnotationAttachment) TreeBuilder.createAnnotAttachmentNode();
        annotationAttachment.setAnnotationName(BuilderUtil.createIdentifier(annotationName));
        annotationAttachment.setPackageAlias(BuilderUtil.createIdentifier(annotationPkg));
        annotationAttachment.setExpression(annotationRecord);
        return annotationAttachment;
    }

    /**
     * Create resource function for API resource
     *
     * @param name name of the resource function
     * @param annotationAttachment http:ResourceConfig annotation
     * @return
     */
    public static BLangFunction createResourceFunction(String name, BLangAnnotationAttachment annotationAttachment) {
        BLangFunction functionNode = (BLangFunction) TreeBuilder.createFunctionNode();
        functionNode.setName(BuilderUtil.createIdentifier(name));

        // Set return type
        BLangValueType nillTypeNode = (BLangValueType) TreeBuilder.createValueTypeNode();
        nillTypeNode.typeKind = TypeKind.NIL;
        functionNode.setReturnTypeNode(nillTypeNode);

        // Set function parameters
        BLangSimpleVariable funcParam1 = BuilderUtil.createSimpleVariable(
                        BuilderUtil.createUserDefinedType(Constants.PKG_HTTP, Constants.PKG_HTTP_CALLER), "caller");

        BLangSimpleVariable funcParam2 = BuilderUtil.createSimpleVariable(
                        BuilderUtil.createUserDefinedType(Constants.PKG_HTTP, Constants.PKG_HTTP_REQUEST), "request");
        functionNode.addParameter(funcParam1);
        functionNode.addParameter(funcParam2);

        // Set empty function body
        BLangBlockStmt bLangBlockStmt = (BLangBlockStmt) TreeBuilder.createBlockNode();
        functionNode.setBody(bLangBlockStmt);

        // Set annotation
        if (annotationAttachment != null) functionNode.addAnnotationAttachment(annotationAttachment);

        //Set flags
        functionNode.addFlag(Flag.ATTACHED);
        functionNode.addFlag(Flag.RESOURCE);

        return functionNode;
    }


    public static BLangNamedArgsExpression createNamedArg(String name, BLangExpression expr) {
        BLangNamedArgsExpression namedArg = (BLangNamedArgsExpression) TreeBuilder.createNamedArgNode();
        namedArg.name = createIdentifier(name);
        namedArg.expr = expr;
        return namedArg;
    }




    /**
     * Function to create invocation function
     *
     * @param variableReference
     * @param pkgName
     * @param functionName
     * @param invocationArgList
     * @param actionInvocation  Grammar :  variableReference RARROW functionInvocation
     * @return
     */
    public static BLangInvocation createFunctionInvocation(BLangVariableReference variableReference, String pkgName,
                                                           String functionName, List<BLangExpression> invocationArgList,
                                                           boolean actionInvocation) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        invocationNode.pkgAlias = BuilderUtil.createIdentifier(pkgName);
        invocationNode.name = BuilderUtil.createIdentifier(functionName);
        invocationNode.expr = variableReference;
        invocationNode.argExprs.addAll(invocationArgList);
        invocationNode.actionInvocation = actionInvocation;
        return invocationNode;
    }

    /**
     * Function to create simple function invocation
     *  Grammar:
     *      functionNameReference :   (Identifier COLON)? anyIdentifierName;
     *      functionInvocation :   functionNameReference LEFT_PARENTHESIS invocationArgList? RIGHT_PARENTHESIS;
     *
     *  Example: Mule:setPayload(context,"Hello World !!");
     *
     * @param pkgName
     * @param functionName
     * @param invocationArgList
     * @param actionInvocation  Grammar :  variableReference RARROW functionInvocation
     * @return
     */
    public static BLangInvocation createFunctionInvocation(String pkgName, String functionName,
                                                           List<BLangExpression> invocationArgList,
                                                           boolean actionInvocation) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        invocationNode.pkgAlias = BuilderUtil.createIdentifier(pkgName);
        invocationNode.name = BuilderUtil.createIdentifier(functionName);
        invocationNode.argExprs.addAll(invocationArgList);
        invocationNode.actionInvocation = actionInvocation;
        return invocationNode;
    }



    /**
     * Utility function to create Ballerina service
     * Grammar:
     *  serviceDefinition :   SERVICE Identifier? ON expressionList serviceBody;
     *  expressionList :   expression (COMMA expression)*;
     *
     * @param identifier name of the service
     * @param expressionList expressions list
     * @param serviceTypeName generated service type name. could use {@link AnonymousModelHelper} to generate name
     * @param bLangAnnotationAttachment annotations attachment for the service
     * @return
     */
    public static BLangService createService(String identifier, List<BLangExpression> expressionList,
                                             String serviceTypeName,
                                             BLangAnnotationAttachment bLangAnnotationAttachment) {

        BLangService serviceNode = (BLangService) TreeBuilder.createServiceNode();
        serviceNode.addAnnotationAttachment(bLangAnnotationAttachment);
        serviceNode.setName(BuilderUtil.createIdentifier(identifier));
        serviceNode.isAnonymousServiceValue = false;
        serviceNode.getAttachedExprs().addAll(expressionList);

        BLangObjectTypeNode objectTypeNode = (BLangObjectTypeNode) TreeBuilder.createObjectTypeNode();
        objectTypeNode.isAnonymous = false;
        objectTypeNode.isFieldAnalyseRequired = false;
        objectTypeNode.flagSet.add(Flag.SERVICE);

        // 1) Define type nodeDefinition for service type.
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
        BLangSimpleVariable serviceVar = BuilderUtil.createSimpleVariableWithoutType(identifier, serviceConstructorNode);
        serviceVar.flagSet.add(Flag.FINAL);
        serviceVar.flagSet.add(Flag.SERVICE);
        serviceVar.typeNode = BuilderUtil.createUserDefinedType(null, serviceTypeDef.name.getValue());
        serviceNode.variableNode = serviceVar;

        return serviceNode;
    }

    /**
     * Util function to insert a given function to a given service
     *
     * @param service target service
     * @param function function to insert
     * @return true if success, false otherwise
     */
    public static boolean addFunctionToService(BLangService service, BLangFunction function) {
        if (service.serviceTypeDefinition != null && service.serviceTypeDefinition.typeNode instanceof BLangObjectTypeNode) {
            ((BLangObjectTypeNode)service.serviceTypeDefinition.typeNode).addFunction(function);
            return true;
        }
        return false;
    }
}
