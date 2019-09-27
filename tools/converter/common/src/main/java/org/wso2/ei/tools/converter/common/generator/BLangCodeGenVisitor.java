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

package org.wso2.ei.tools.converter.common.generator;

import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.model.tree.expressions.ExpressionNode;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.compiler.util.TypeTags;

import java.util.Arrays;
import java.util.Stack;

/**
 * This class implements @{@link BLangNodeVisitor} to traverse through Ballerina model of the integration flow
 * and serialize to ballerina source
 */
public class BLangCodeGenVisitor extends BLangNodeVisitor {

    private StringBuilder sourceStrBuilder;
    private String name;
    private Stack<Integer> indentationStack;

    public BLangCodeGenVisitor() {
        this.sourceStrBuilder = new StringBuilder();
        this.indentationStack = new Stack<>();
        this.indentationStack.push(0);
    }

    public void visit(BLangCompilationUnit compUnit) {
        this.name = compUnit.getName();
        compUnit.getTopLevelNodes().forEach(topNode -> {
            if (topNode instanceof BLangImportPackage) {
                ((BLangImportPackage)topNode).accept(this);
            } else if (topNode instanceof BLangVariable) {
                // globalVariableDefinition
                serializeGlobalVariable((BLangVariable) topNode);
            } else if (topNode instanceof BLangService) {
                ((BLangService) topNode).accept(this);
            }
        });
    }

    public void visit(BLangImportPackage importPkgNode) {
        sourceStrBuilder.append(importPkgNode.toString()).append(Constants.STMTEND_STR).append(Constants.NEWLINE_CHAR);
    }

    /**
     * Grammar:
     * variableDefinitionStatement
     *     :   typeName Identifier SEMICOLON
     *     |   FINAL? (typeName | VAR) bindingPattern ASSIGN expression SEMICOLON
     *     ;
     * @param varNode
     */
    public void visit(BLangSimpleVariable varNode) {
        sourceStrBuilder.append(getIndentation());

        if (varNode.getInitialExpression() == null) {
            // variableDefinitionStatement : typeName Identifier SEMICOLON
            varNode.getTypeNode().accept(this);
            sourceStrBuilder.append(Constants.SPACE_CHAR).append(varNode.getName());

        } else {
            // variableDefinitionStatement : FINAL? (typeName | VAR) bindingPattern ASSIGN expression SEMICOLON;

            if (varNode.getTypeNode() != null) {
                varNode.getTypeNode().accept(this);
            } else {
                sourceStrBuilder.append(Constants.VAR_STR);
            }
            sourceStrBuilder.append(Constants.SPACE_STR).append(varNode.getName()).
                    append(Constants.SPACE_STR).append(Constants.EQUAL_STR).append(Constants.SPACE_STR);
            varNode.getInitialExpression().accept(this);
            sourceStrBuilder.append(Constants.STMTEND_STR).append(Constants.NEWLINE_CHAR);
        }
    }

    /**
     * Grammar:
     *  variableDefinitionStatement
     *     :   typeName Identifier SEMICOLON
     *     |   FINAL? (typeName | VAR) bindingPattern ASSIGN expression SEMICOLON
     *     ;
     * @param varDefNode
     */
    @Override
    public void visit(BLangSimpleVariableDef varDefNode) {
        BLangSimpleVariable simpleVariable = varDefNode.getVariable();
        simpleVariable.accept(this);
    }


    /**
     * Grammar:
     *  recordLiteral
     *     :   LEFT_BRACE (recordKeyValue (COMMA recordKeyValue)*)? RIGHT_BRACE
     *     ;
     *  recordKeyValue
     *     :   recordKey COLON expression
     *     ;
     *
     * @param recordLiteral
     */
    @Override
    public void visit(BLangRecordLiteral recordLiteral) {
        sourceStrBuilder.append(Constants.LEFT_BRACE).append(Constants.NEWLINE_CHAR);
        indentationStack.push(indentationStack.peek() + 1);

        int keyValueCount = recordLiteral.getKeyValuePairs().size();
        for (BLangRecordLiteral.BLangRecordKeyValue recordKeyValue : recordLiteral.getKeyValuePairs()) {
            recordKeyValue.accept(this);
            -- keyValueCount;
            if (keyValueCount > 0) {
                sourceStrBuilder.append(Constants.COMMA_STR).append(Constants.NEWLINE_CHAR);
            }
        }

        indentationStack.pop();
        sourceStrBuilder.append(Constants.NEWLINE_CHAR).append(getIndentation()).append(Constants.RIGHT_BRACE);
    }

    /**
     * Grammar:
     *  recordKeyValue
     *     :   recordKey COLON expression
     *     ;
     *  recordKey
     *     :   Identifier
     *     |   LEFT_BRACKET expression RIGHT_BRACKET
     *     |   expression
     *     ;
     *
     * @param recordKeyValue
     */
    @Override
    public void visit(BLangRecordLiteral.BLangRecordKeyValue recordKeyValue) {
        sourceStrBuilder.append(getIndentation());
        sourceStrBuilder.append(recordKeyValue.getKey()).append(Constants.SPACE_CHAR).append(Constants.COLON_STR)
                .append(Constants.SPACE_CHAR);
        recordKeyValue.getValue().accept(this);
    }

    /**
     * Grammar:
     *  simpleLiteral
     *     :   SUB? integerLiteral
     *     |   SUB? floatingPointLiteral
     *     |   QuotedStringLiteral
     *     |   BooleanLiteral
     *     |   nilLiteral
     *     |   blobLiteral
     *     |   NullLiteral
     *     ;
     *
     *  QuotedStringLiteral
     *     :   '"' StringCharacters? '"'
     *     ;
     * @param literalExpr
     */
    @Override
    public void visit(BLangLiteral literalExpr) {
        if (literalExpr.type.tag == TypeTags.STRING) {
            sourceStrBuilder.append(literalExpr.getOriginalValue());
        } else {
            sourceStrBuilder.append(literalExpr.getValue());
        }
    }

    @Override
    public void visit(BLangUserDefinedType userDefinedType) {
        sourceStrBuilder.append(userDefinedType);
    }

    /**
     * Grammar:
     *  typeInitExpr
     *     :   NEW (LEFT_PARENTHESIS invocationArgList? RIGHT_PARENTHESIS)?
     *     |   NEW userDefineTypeName LEFT_PARENTHESIS invocationArgList? RIGHT_PARENTHESIS
     *     ;
     * @param bLangTypeInit
     */
    public void visit(BLangTypeInit bLangTypeInit) {
        /*sourceStrBuilder.append(Constants.NEW_STR).append(Constants.SPACE_STR);

        if (bLangTypeInit.getType() != null) {
            BLangUserDefinedType type = (BLangUserDefinedType) bLangTypeInit.getType();
            sourceStrBuilder.append(type).append(Constants.SPACE_STR);
        }*/

        bLangTypeInit.initInvocation.accept(this);
    }

    /**
     * Grammar:
     *  functionInvocation
     *     :   functionNameReference LEFT_PARENTHESIS invocationArgList? RIGHT_PARENTHESIS
     *     ;
     *
     *  functionNameReference
     *     :   (Identifier COLON)? anyIdentifierName
     *     ;
     *
     *  invocationArgList
     *     :   invocationArg (COMMA invocationArg)*
     *     ;
     * @param bLangInvocation
     */
    @Override
    public void visit(BLangInvocation bLangInvocation) {
        sourceStrBuilder.append(getNameReference(bLangInvocation.getPackageAlias(), bLangInvocation.getName()))
                .append(Constants.SPACE_CHAR).append(Constants.PARENTHESES_START_CHAR);

        // serialize invocationArgList
        int argCount = bLangInvocation.getArgumentExpressions().size();
        for (ExpressionNode expr : bLangInvocation.getArgumentExpressions()) {
            ((BLangExpression)expr).accept(this);
            -- argCount;
            if (argCount > 0) {
                sourceStrBuilder.append(Constants.COMMA_CHAR).append(Constants.SPACE_CHAR);
            }
        }

        sourceStrBuilder.append(Constants.PARENTHESES_END_CHAR);
    }

    /**
     * Grammar:
     *  serviceDefinition
     *     :   SERVICE Identifier? ON expressionList serviceBody
     *     ;
     *  serviceBody
     *     :   LEFT_BRACE serviceBodyMember* RIGHT_BRACE
     *     ;
     *  serviceBodyMember
     *     :   objectFieldDefinition
     *     |   objectFunctionDefinition
     *     ;
     * @param bLangService
     */
    public void visit(BLangService bLangService) {
        bLangService.getAnnotationAttachments().forEach(annotationAttachment -> annotationAttachment.accept(this));
        sourceStrBuilder.append(getIndentation()).append(Constants.SERVICE_STR)
                                    .append(Constants.SPACE_CHAR).append(bLangService.getName())
                                    .append(Constants.SPACE_CHAR).append(Constants.ON_STR)
                                    .append(Constants.SPACE_CHAR);

        // serialize expressionList
        int expressionListCount = bLangService.getAttachedExprs().size();
        for (BLangExpression expr : bLangService.getAttachedExprs()) {
            expr.accept(this);
            -- expressionListCount;
            if (expressionListCount > 0) {
                sourceStrBuilder.append(Constants.COMMA_CHAR).append(Constants.SPACE_CHAR);
            }
        }
        sourceStrBuilder.append(Constants.SPACE_CHAR).append(Constants.LEFT_BRACE_CHAR).append(Constants.NEWLINE_CHAR);
        indentationStack.push(indentationStack.peek() + 1);
        // serialize serviceBody
        ((BLangObjectTypeNode) bLangService.serviceTypeDefinition.typeNode).getFunctions().forEach(bLangFunction -> {
            bLangFunction.accept(this);
        });
        indentationStack.pop();
        sourceStrBuilder.append(Constants.RIGHT_BRACE_CHAR).append(Constants.NEWLINE_CHAR);
    }

    /**
     * Grammar:
     *  objectFunctionDefinition
     *     :   documentationString? annotationAttachment* (PUBLIC | PRIVATE)? (REMOTE | RESOURCE)? FUNCTION
     *     callableUnitSignature (callableUnitBody | externalFunctionBody? SEMICOLON)
     *     ;
     *
     *  callableUnitSignature
     *     :   anyIdentifierName LEFT_PARENTHESIS formalParameterList? RIGHT_PARENTHESIS returnParameter?
     *     ;
     *
     *  formalParameterList
     *     :   (parameter | defaultableParameter) (COMMA (parameter | defaultableParameter))* (COMMA restParameter)?
     *     |   restParameter
     *     ;
     *
     *  parameterList
     *     :   parameter (COMMA parameter)*
     *     ;
     *
     *  parameter
     *     :   annotationAttachment* PUBLIC? typeName Identifier
     *     ;
     * @param funcNode
     */
    @Override
    public void visit(BLangFunction funcNode) {
        // Serialize annotations
        funcNode.getAnnotationAttachments().forEach(annotation -> annotation.accept(this));

        sourceStrBuilder.append(getIndentation());
        if (funcNode.getFlags().contains(Flag.RESOURCE)) {
            sourceStrBuilder.append(Constants.RESOURCE_STR);
        }

        sourceStrBuilder.append(Constants.SPACE_CHAR).append(Constants.FUNCTION_STR)
                .append(Constants.SPACE_CHAR).append(funcNode.getName())
                .append(Constants.SPACE_CHAR).append(Constants.PARENTHESES_START_CHAR);

        int paramCount = funcNode.getParameters().size();
        for (BLangSimpleVariable simpleVariable: funcNode.requiredParams) {
            simpleVariable.getTypeNode().accept(this);
            sourceStrBuilder.append(Constants.SPACE_CHAR).append(simpleVariable.getName());
            -- paramCount;
            if (paramCount > 0) {
                sourceStrBuilder.append(Constants.COMMA_CHAR).append(Constants.SPACE_CHAR);
            }
        }
        sourceStrBuilder.append(Constants.PARENTHESES_END_CHAR);
        funcNode.getBody().accept(this);
    }

    /**
     * Grammar:
     *  callableUnitBody
     *     :   LEFT_BRACE statement* (workerDeclaration+ statement*)? RIGHT_BRACE
     *     ;
     * @param blockNode
     */
    @Override
    public void visit(BLangBlockStmt blockNode) {
        sourceStrBuilder.append(Constants.LEFT_BRACE_CHAR).append(Constants.NEWLINE_CHAR);
        indentationStack.push(indentationStack.peek() + 1);

        blockNode.getStatements().forEach(bLangStatement -> bLangStatement.accept(this));

        indentationStack.pop();
        sourceStrBuilder.append(getIndentation()).append(Constants.RIGHT_BRACE_CHAR).append(Constants.NEWLINE_CHAR);
    }

    /**
     * Grammar:
     *  annotationAttachment
     *     :   AT nameReference recordLiteral?
     *     ;
     * @param annotationNode
     */
    @Override
    public void visit(BLangAnnotationAttachment annotationNode) {
        sourceStrBuilder.append(getIndentation())
                .append(Constants.ANNOTATION_AT_CHAR)
                .append(getNameReference(annotationNode.getPackageAlias(), annotationNode.getAnnotationName()))
                .append(Constants.SPACE_CHAR);
        annotationNode.expr.accept(this);
        sourceStrBuilder.append(Constants.NEWLINE_CHAR);
    }

    /**
     * Grammar:
     *  variableReference
     *     :   nameReference                                                           # simpleVariableReference
     *     |   functionInvocation                                                      # functionInvocationReference
     *     |   variableReference index                                                 # mapArrayVariableReference
     *     |   variableReference field                                                 # fieldVariableReference
     *     |   variableReference xmlAttrib                                             # xmlAttribVariableReference
     *     |   variableReference invocation                                            # invocationReference
     *     |   typeDescExpr invocation                                                 # typeDescExprInvocationReference
     *     |   QuotedStringLiteral invocation                                          # stringFunctionInvocationReference
     *     ;
     *
     * @param varRefExpr
     */
    @Override
    public void visit(BLangSimpleVarRef varRefExpr) {
        sourceStrBuilder.append(varRefExpr);
    }

    /**
     * Grammar:
     *  expressionStmt
     *     :   expression SEMICOLON
     *     ;
     * @param exprStmtNode
     */
    @Override
    public void visit(BLangExpressionStmt exprStmtNode) {
        sourceStrBuilder.append(getIndentation());
        exprStmtNode.getExpression().accept(this);
        sourceStrBuilder.append(Constants.STMTEND_STR).append(Constants.NEWLINE_CHAR);
    }

    /**
     * Grammar:
     *  namedArgs
     *     :   Identifier ASSIGN expression
     *     ;
     * @param bLangNamedArgsExpression
     */
    @Override
    public void visit(BLangNamedArgsExpression bLangNamedArgsExpression) {
        sourceStrBuilder.append(bLangNamedArgsExpression.getName())
                .append(Constants.SPACE_CHAR).append(Constants.EQUAL_STR)
                .append(Constants.SPACE_CHAR);
        ((BLangExpression)bLangNamedArgsExpression.getExpression()).accept(this);
    }

    /**
     * Grammar:
     *
     * @param conversionExpr
     */
    @Override
    public void visit(BLangTypeConversionExpr conversionExpr) {
        sourceStrBuilder.append(conversionExpr);
    }



    public String getSource() {
        return sourceStrBuilder.toString();
    }

    /**
     * Grammar:
     *  globalVariableDefinition
     *     :   PUBLIC? LISTENER typeName Identifier ASSIGN expression SEMICOLON
     *     |   FINAL? (typeName | VAR) Identifier ASSIGN expression SEMICOLON
     *     |   channelType Identifier ASSIGN expression SEMICOLON
     *     ;
     * @param bLangVariable
     */
    private void serializeGlobalVariable(BLangVariable bLangVariable) {
        sourceStrBuilder.append(getIndentation());
        //Serialize flags
        bLangVariable.flagSet.forEach(flag ->
                sourceStrBuilder.append(String.valueOf(flag).toLowerCase()).append(Constants.SPACE_STR));

        if (bLangVariable.getTypeNode() != null) {
            bLangVariable.getTypeNode().accept(this);
        } else {
            sourceStrBuilder.append(Constants.VAR_STR);
        }

        if (bLangVariable instanceof BLangSimpleVariable) {
            BLangSimpleVariable varNode = (BLangSimpleVariable) bLangVariable;
            sourceStrBuilder.append(Constants.SPACE_STR).append(varNode.getName());
        }

        sourceStrBuilder.append(Constants.SPACE_STR).append(Constants.EQUAL_STR).append(Constants.SPACE_STR);
        // Serialize expression
        bLangVariable.getInitialExpression().accept(this);
        sourceStrBuilder.append(Constants.STMTEND_STR).append(Constants.NEWLINE_CHAR);
    }

    private String getIndentation() {
        int indentCount = this.indentationStack.peek();
        if (indentCount > 0) {
            char[] indentCharArray = new char[indentCount * 4];
            Arrays.fill(indentCharArray, ' ');
            return new String(indentCharArray);
        }
        return "";
    }

    private String getNameReference(IdentifierNode pgkAlias, IdentifierNode identifier) {
        if (pgkAlias == null || pgkAlias.getValue().isEmpty()) {
            return identifier.getValue();
        }
        return pgkAlias.getValue() + Constants.COLON_STR + identifier.getValue();
    }
}
