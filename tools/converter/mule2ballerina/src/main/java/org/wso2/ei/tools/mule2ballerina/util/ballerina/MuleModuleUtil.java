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

import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.tree.statements.StatementNode;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ei.tools.converter.common.builder.BuilderUtil;
import org.wso2.ei.tools.converter.common.generator.Constants;
import org.wso2.ei.tools.mule2ballerina.util.Constant;

import java.util.ArrayList;
import java.util.List;

public class MuleModuleUtil {

    /**
     * Util function to create ballerina statement to create context initialization
     *
     * Example: Context:MessageContext context = new(<@untained> request);
     * @return
     */
    public static BLangStatement createContextStmt() {
        BLangUserDefinedType userDefinedType = BuilderUtil.createUserDefinedType(
                ModuleConstants.CONTEXT_MODULE_NAME, ModuleConstants.CONTEXT_MODULE_TYPE_MESSAGECONTEXT);

        BLangAnnotationAttachment untainedAnnotation =
                BuilderUtil.createAnnotationAttachment(null, Constant.BLANG_KEYWORD_UNTAINED, null);
        BLangSimpleVarRef bLangSimpleVarRef =
                BuilderUtil.createSimpleVariableReference(null, ModuleConstants.BLANG_VAR_INCOMING_REQUEST);
        BLangTypeConversionExpr typeConversionExpr =
                BuilderUtil.createTypeConversionExpr(untainedAnnotation, bLangSimpleVarRef);

        List<BLangExpression> exprList = new ArrayList<>(1);
        exprList.add(typeConversionExpr);
        BLangTypeInit typeInitExpr = BuilderUtil.createTypeInitExpr(exprList);

        return BuilderUtil.createSimpleVariableDefWithType(userDefinedType,
                ModuleConstants.BLANG_VAR_CONTEXT, typeInitExpr);
    }

    /**
     * Function to create ballerina statement to invoke Mule:setPayload function
     *
     *  Example: Mule:setPayload(context,"Hello World !!");
     *
     * @param payload payload to set
     * @return
     */
    public static BLangStatement createSetPayloadStmt(String payload) {
        BLangSimpleVarRef contextVarRef = BuilderUtil.createSimpleVariableReference(null,
                ModuleConstants.BLANG_VAR_CONTEXT);
        String originalValue = Constants.QUOTE_STR + String.valueOf(payload) + Constants.QUOTE_STR;
        BLangLiteral payloadLiteral = BuilderUtil.createLiteralValue(TypeTags.STRING, payload, originalValue);

        List<BLangExpression> exprList = new ArrayList<>(2);
        exprList.add(contextVarRef);
        exprList.add(payloadLiteral);

        BLangInvocation funcInvocation = BuilderUtil.createFunctionInvocation(ModuleConstants.MULE_MODULE_NAME,
                ModuleConstants.MULE_MODULE_FUNC_SET_PAYLOAD, exprList, false);

        BLangExpressionStmt exprStmt = (BLangExpressionStmt) TreeBuilder.createExpressionStatementNode();
        exprStmt.expr = funcInvocation;
        return exprStmt;
    }

    public static BLangStatement createRespondStmt() {
        BLangSimpleVarRef contextVarRef = BuilderUtil.createSimpleVariableReference(null,
                ModuleConstants.BLANG_VAR_CONTEXT);
        BLangSimpleVarRef callerVarRef = BuilderUtil.createSimpleVariableReference(null,
                ModuleConstants.BLANG_VAR_INCOMING_CALLER);

        List<BLangExpression> exprList = new ArrayList<>(2);
        exprList.add(contextVarRef);
        exprList.add(callerVarRef);

        BLangInvocation funcInvocation = BuilderUtil.createFunctionInvocation(ModuleConstants.MULE_MODULE_NAME,
                ModuleConstants.MULE_MODULE_FUNC_RESPOND, exprList, false);

        return BuilderUtil.createSimpleVariableDef(ModuleConstants.BLANG_VAR_RESULT, funcInvocation);
    }
}
