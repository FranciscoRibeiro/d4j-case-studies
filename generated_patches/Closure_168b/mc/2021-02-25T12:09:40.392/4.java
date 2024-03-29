package com.google.javascript.jscomp;

import static com.google.javascript.rhino.jstype.JSTypeNative.ARRAY_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.BOOLEAN_OBJECT_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.BOOLEAN_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.CHECKED_UNKNOWN_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.NULL_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.NUMBER_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.NUMBER_VALUE_OR_OBJECT_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.STRING_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.UNKNOWN_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.VOID_TYPE;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.javascript.jscomp.CodingConvention.AssertionFunctionSpec;
import com.google.javascript.jscomp.ControlFlowGraph.Branch;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.jscomp.graph.DiGraph.DiGraphEdge;
import com.google.javascript.jscomp.type.FlowScope;
import com.google.javascript.jscomp.type.ReverseAbstractInterpreter;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.BooleanLiteralSet;
import com.google.javascript.rhino.jstype.FunctionType;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.JSTypeNative;
import com.google.javascript.rhino.jstype.JSTypeRegistry;
import com.google.javascript.rhino.jstype.ModificationVisitor;
import com.google.javascript.rhino.jstype.ObjectType;
import com.google.javascript.rhino.jstype.ParameterizedType;
import com.google.javascript.rhino.jstype.StaticSlot;
import com.google.javascript.rhino.jstype.TemplateType;
import com.google.javascript.rhino.jstype.UnionType;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class TypeInference extends DataFlowAnalysis.BranchedForwardDataFlowAnalysis<Node, FlowScope> {

    static final DiagnosticType FUNCTION_LITERAL_UNDEFINED_THIS = DiagnosticType.warning("JSC_FUNCTION_LITERAL_UNDEFINED_THIS", "Function literal argument refers to undefined this argument");

    private final AbstractCompiler compiler;

    private final JSTypeRegistry registry;

    private final ReverseAbstractInterpreter reverseInterpreter;

    private final Scope syntacticScope;

    private final FlowScope functionScope;

    private final FlowScope bottomScope;

    private final Map<String, AssertionFunctionSpec> assertionFunctionsMap;

    TypeInference(AbstractCompiler compiler, ControlFlowGraph<Node> cfg, ReverseAbstractInterpreter reverseInterpreter, Scope functionScope, Map<String, AssertionFunctionSpec> assertionFunctionsMap) {
        super(cfg, new LinkedFlowScope.FlowScopeJoinOp());
        this.compiler = compiler;
        this.registry = compiler.getTypeRegistry();
        this.reverseInterpreter = reverseInterpreter;
        this.syntacticScope = functionScope;
        this.functionScope = LinkedFlowScope.createEntryLattice(functionScope);
        this.assertionFunctionsMap = assertionFunctionsMap;
        // type is VOID.
Iterator<Var> varIt = functionScope.getDeclarativelyUnboundVarsWithoutTypes();
        while (varIt.hasNext()) {
            Var var = varIt.next();
            if (isUnflowable(var)) {
                continue;
            }
            this.functionScope.inferSlotType(var.getName(), getNativeType(VOID_TYPE));
        }
        this.bottomScope = LinkedFlowScope.createEntryLattice(new Scope(functionScope.getRootNode(), functionScope.getTypeOfThis()));
    }

    @Override
FlowScope createInitialEstimateLattice() {
    return bottomScope;
}

    @Override
FlowScope createEntryLattice() {
    return functionScope;
}

    @Override
FlowScope flowThrough(Node n, FlowScope input) {
    if (input == bottomScope) {
        return input;
    }
    FlowScope output = input.createChildFlowScope();
    output = traverse(n, output);
    return output;
}

    @Override
@SuppressWarnings("fallthrough")
List<FlowScope> branchedFlowThrough(Node source, FlowScope input) {
    // worthwhile.
FlowScope output = flowThrough(source, input);
    Node condition = null;
    FlowScope conditionFlowScope = null;
    BooleanOutcomePair conditionOutcomes = null;
    List<DiGraphEdge<Node, Branch>> branchEdges = getCfg().getOutEdges(source);
    List<FlowScope> result = Lists.newArrayListWithCapacity(branchEdges.size());
    for (DiGraphEdge<Node, Branch> branchEdge : branchEdges) {
        Branch branch = branchEdge.getValue();
        FlowScope newScope = output;
        switch(branch) {
            case ON_TRUE:
                if (NodeUtil.isForIn(source)) {
                    // item is assigned a property name, so its type should be string.
Node item = source.getFirstChild();
                    Node obj = item.getNext();
                    FlowScope informed = traverse(obj, output.createChildFlowScope());
                    if (item.isVar()) {
                        item = item.getFirstChild();
                    }
                    if (item.isName()) {
                        JSType iterKeyType = getNativeType(STRING_TYPE);
                        ObjectType objType = getJSType(obj).dereference();
                        JSType objIndexType = objType == null ? null : objType.getIndexType();
                        if (objIndexType != null && !objIndexType.isUnknownType()) {
                            JSType narrowedKeyType = iterKeyType.getGreatestSubtype(objIndexType);
                            if (!narrowedKeyType.isEmptyType()) {
                                iterKeyType = narrowedKeyType;
                            }
                        }
                        redeclareSimpleVar(informed, item, iterKeyType);
                    }
                    newScope = informed;
                    break;
                }
            case ON_FALSE:
                if (condition == null) {
                    condition = NodeUtil.getConditionExpression(source);
                    if (condition == null && source.isCase()) {
                        condition = source;
                        if (conditionFlowScope == null) {
                            conditionFlowScope = traverse(condition.getFirstChild(), output.createChildFlowScope());
                        }
                    }
                }
                if (condition != null) {
                    if (condition.isAnd() || condition.isOr()) {
                        if (conditionOutcomes == null) {
                            conditionOutcomes = condition.isAnd() ? traverseAnd(condition, output.createChildFlowScope()) : traverseOr(condition, output.createChildFlowScope());
                        }
                        newScope = reverseInterpreter.getPreciserScopeKnowingConditionOutcome(condition, conditionOutcomes.getOutcomeFlowScope(condition.getType(), branch == Branch.ON_TRUE), branch == Branch.ON_TRUE);
                    } else {
                        if (conditionFlowScope == null) {
                            conditionFlowScope = traverse(condition, output.createChildFlowScope());
                        }
                        newScope = reverseInterpreter.getPreciserScopeKnowingConditionOutcome(condition, conditionFlowScope, branch == Branch.ON_TRUE);
                    }
                }
                break;
            }
        result.add(newScope.optimize());
    }
    return result;
}

    private FlowScope traverse(Node n, FlowScope scope) {
        switch(n.getType()) {
            case Token.ASSIGN:
                scope = traverseAssign(n, scope);
                break;
            case Token.NAME:
                scope = traverseName(n, scope);
                break;
            case Token.GETPROP:
                scope = traverseGetProp(n, scope);
                break;
            case Token.AND:
                scope = traverseAnd(n, scope).getJoinedFlowScope().createChildFlowScope();
                break;
            case Token.OR:
                scope = traverseOr(n, scope).getJoinedFlowScope().createChildFlowScope();
                break;
            case Token.HOOK:
                scope = traverseHook(n, scope);
                break;
            case Token.OBJECTLIT:
                scope = traverseObjectLiteral(n, scope);
                break;
            case Token.CALL:
                scope = traverseCall(n, scope);
                break;
            case Token.NEW:
                scope = traverseNew(n, scope);
                break;
            case Token.ASSIGN_ADD:
            case Token.ADD:
                scope = traverseAdd(n, scope);
                break;
            case Token.POS:
            case Token.NEG:
                // Find types.
scope = traverse(n.getFirstChild(), scope);
                n.setJSType(getNativeType(NUMBER_TYPE));
                break;
            case Token.ARRAYLIT:
                scope = traverseArrayLiteral(n, scope);
                break;
            case Token.THIS:
                n.setJSType(scope.getTypeOfThis());
                break;
            case Token.ASSIGN_LSH:
            case Token.ASSIGN_RSH:
            case Token.LSH:
            case Token.RSH:
            case Token.ASSIGN_URSH:
            case Token.URSH:
            case Token.ASSIGN_DIV:
            case Token.ASSIGN_MOD:
            case Token.ASSIGN_BITAND:
            case Token.ASSIGN_BITXOR:
            case Token.ASSIGN_BITOR:
            case Token.ASSIGN_MUL:
            case Token.ASSIGN_SUB:
            case Token.DIV:
            case Token.MOD:
            case Token.BITAND:
            case Token.BITXOR:
            case Token.BITOR:
            case Token.MUL:
            case Token.SUB:
            case Token.DEC:
            case Token.INC:
            case Token.BITNOT:
                scope = traverseChildren(n, scope);
                n.setJSType(getNativeType(NUMBER_TYPE));
                break;
            case Token.PARAM_LIST:
                scope = traverse(n.getFirstChild(), scope);
                n.setJSType(getJSType(n.getFirstChild()));
                break;
            case Token.COMMA:
                scope = traverseChildren(n, scope);
                n.setJSType(getJSType(n.getLastChild()));
                break;
            case Token.TYPEOF:
                scope = traverseChildren(n, scope);
                n.setJSType(getNativeType(STRING_TYPE));
                break;
            case Token.DELPROP:
            case Token.LT:
            case Token.LE:
            case Token.GT:
            case Token.GE:
            case Token.NOT:
            case Token.EQ:
            case Token.NE:
            case Token.SHEQ:
            case Token.SHNE:
            case Token.INSTANCEOF:
            case Token.IN:
                scope = traverseChildren(n, scope);
                n.setJSType(getNativeType(BOOLEAN_TYPE));
                break;
            case Token.GETELEM:
                scope = traverseGetElem(n, scope);
                break;
            case Token.EXPR_RESULT:
                scope = traverseChildren(n, scope);
                if (n.getFirstChild().isGetProp()) {
                    ensurePropertyDeclared(n.getFirstChild());
                }
                break;
            case Token.SWITCH:
                scope = traverse(n.getFirstChild(), scope);
                break;
            case Token.RETURN:
                scope = traverseReturn(n, scope);
                break;
            case Token.VAR:
            case Token.THROW:
                scope = traverseChildren(n, scope);
                break;
            case Token.CATCH:
                scope = traverseCatch(n, scope);
                break;
            }
        if (!n.isFunction()) {
            JSDocInfo info = n.getJSDocInfo();
            if (info != null && info.hasType()) {
                JSType castType = info.getType().evaluate(syntacticScope, registry);
                if (n.isQualifiedName() && n.getParent().isExprResult()) {
                    updateScopeForTypeChange(scope, n, n.getJSType(), castType);
                }
                n.setJSType(castType);
            }
        }
        return scope;
    }

    private FlowScope traverseReturn(Node n, FlowScope scope) {
        scope = traverseChildren(n, scope);
        Node retValue = n.getFirstChild();
        if (retValue != null) {
            JSType type = functionScope.getRootNode().getJSType();
            if (type != null) {
                FunctionType fnType = type.toMaybeFunctionType();
                if (fnType != null) {
                    inferPropertyTypesToMatchConstraint(retValue.getJSType(), fnType.getReturnType());
                }
            }
        }
        return scope;
    }

    private FlowScope traverseCatch(Node n, FlowScope scope) {
        Node name = n.getFirstChild();
        JSType type = getNativeType(JSTypeNative.UNKNOWN_TYPE);
        name.setJSType(type);
        redeclareSimpleVar(scope, name, type);
        return scope;
    }

    private FlowScope traverseAssign(Node n, FlowScope scope) {
        Node left = n.getFirstChild();
        Node right = n.getLastChild();
        scope = traverseChildren(n, scope);
        JSType leftType = left.getJSType();
        JSType rightType = getJSType(right);
        n.setJSType(rightType);
        updateScopeForTypeChange(scope, left, leftType, rightType);
        return scope;
    }

    private void updateScopeForTypeChange(FlowScope scope, Node left, JSType leftType, JSType resultType) {
        Preconditions.checkNotNull(resultType);
        switch(left.getType()) {
            case Token.NAME:
                String varName = left.getString();
                Var var = syntacticScope.getVar(varName);
                //    which is just wrong. This bug needs to be fixed eventually.
boolean isVarDeclaration = left.hasChildren();
                if (!isVarDeclaration || var == null || var.isTypeInferred()) {
                    redeclareSimpleVar(scope, left, resultType);
                }
                left.setJSType(isVarDeclaration || leftType == null ? resultType : null);
                if (var != null && var.isTypeInferred()) {
                    JSType oldType = var.getType();
                    var.setType(oldType == null ? resultType : oldType.getLeastSupertype(resultType));
                }
                break;
            case Token.GETPROP:
                String qualifiedName = left.getQualifiedName();
                if (qualifiedName != null) {
                    scope.inferQualifiedSlot(left, qualifiedName, leftType == null ? getNativeType(UNKNOWN_TYPE) : leftType, resultType);
                }
                left.setJSType(resultType);
                ensurePropertyDefined(left, resultType);
                break;
            }
    }

    private void ensurePropertyDefined(Node getprop, JSType rightType) {
        String propName = getprop.getLastChild().getString();
        JSType nodeType = getJSType(getprop.getFirstChild());
        ObjectType objectType = ObjectType.cast(nodeType.restrictByNotNullOrUndefined());
        if (objectType == null) {
            registry.registerPropertyOnType(propName, nodeType);
        } else {
            if (ensurePropertyDeclaredHelper(getprop, objectType)) {
                return;
            }
            if (!objectType.isPropertyTypeDeclared(propName)) {
                if (objectType.hasProperty(propName) || !objectType.isInstanceType()) {
                    if ("prototype".equals(propName)) {
                        objectType.defineDeclaredProperty(propName, rightType, getprop);
                    } else {
                        objectType.defineInferredProperty(propName, rightType, getprop);
                    }
                } else {
                    if (getprop.getFirstChild().isThis() && getJSType(syntacticScope.getRootNode()).isConstructor()) {
                        objectType.defineInferredProperty(propName, rightType, getprop);
                    } else {
                        registry.registerPropertyOnType(propName, objectType);
                    }
                }
            }
        }
    }

    private void ensurePropertyDeclared(Node getprop) {
        ObjectType ownerType = ObjectType.cast(getJSType(getprop.getFirstChild()).restrictByNotNullOrUndefined());
        if (ownerType != null) {
            ensurePropertyDeclaredHelper(getprop, ownerType);
        }
    }

    private boolean ensurePropertyDeclaredHelper(Node getprop, ObjectType objectType) {
        String propName = getprop.getLastChild().getString();
        String qName = getprop.getQualifiedName();
        if (qName != null) {
            Var var = syntacticScope.getVar(qName);
            if (var != null && !var.isTypeInferred()) {
                if (propName.equals("prototype") || (!objectType.hasOwnProperty(propName) && (!objectType.isInstanceType() || (var.isExtern() && !objectType.isNativeObjectType())))) {
                    return objectType.defineDeclaredProperty(propName, var.getType(), getprop);
                }
            }
        }
        return false;
    }

    private FlowScope traverseName(Node n, FlowScope scope) {
        String varName = n.getString();
        Node value = n.getNext();
        JSType type = n.getJSType();
        if (value != null) {
            scope = traverse(value, scope);
            updateScopeForTypeChange(scope, n, n.getJSType(), getJSType(value));
            return scope;
        } else {
            StaticSlot<JSType> var = scope.getSlot(varName);
            if (var != null) {
                // function f() { var x = 3; function g() { x = null } (x); }
boolean isInferred = var.isTypeInferred();
                boolean unflowable = isInferred && isUnflowable(syntacticScope.getVar(varName));
                // type {number}, even though it's undefined.
boolean nonLocalInferredSlot = false;
                if (isInferred && syntacticScope.isLocal()) {
                    Var maybeOuterVar = syntacticScope.getParent().getVar(varName);
                    if (var == maybeOuterVar && !maybeOuterVar.isMarkedAssignedExactlyOnce()) {
                        nonLocalInferredSlot = true;
                    }
                }
                if (!unflowable && !nonLocalInferredSlot) {
                    type = var.getType();
                    if (type == null) {
                        type = getNativeType(UNKNOWN_TYPE);
                    }
                }
            }
        }
        n.setJSType(type);
        return scope;
    }

    private FlowScope traverseArrayLiteral(Node n, FlowScope scope) {
        scope = traverseChildren(n, scope);
        n.setJSType(getNativeType(ARRAY_TYPE));
        return scope;
    }

    private FlowScope traverseObjectLiteral(Node n, FlowScope scope) {
        JSType type = n.getJSType();
        Preconditions.checkNotNull(type);
        for (Node name = n.getFirstChild(); name != null; name = name.getNext()) {
            scope = traverse(name.getFirstChild(), scope);
        }
        // we can check for here.
ObjectType objectType = ObjectType.cast(type);
        if (objectType == null) {
            return scope;
        }
        boolean hasLendsName = n.getJSDocInfo() != null && n.getJSDocInfo().getLendsName() != null;
        if (objectType.hasReferenceName() && !hasLendsName) {
            return scope;
        }
        String qObjName = NodeUtil.getBestLValueName(NodeUtil.getBestLValue(n));
        for (Node name = n.getFirstChild(); name != null; name = name.getNext()) {
            Node value = name.getFirstChild();
            String memberName = NodeUtil.getObjectLitKeyName(name);
            if (memberName != null) {
                JSType rawValueType = name.getFirstChild().getJSType();
                JSType valueType = NodeUtil.getObjectLitKeyTypeFromValueType(name, rawValueType);
                if (valueType == null) {
                    valueType = getNativeType(UNKNOWN_TYPE);
                }
                objectType.defineInferredProperty(memberName, valueType, name);
                if (qObjName != null && name.isStringKey()) {
                    String qKeyName = qObjName + "." + memberName;
                    Var var = syntacticScope.getVar(qKeyName);
                    JSType oldType = var == null ? null : var.getType();
                    if (var != null && var.isTypeInferred()) {
                        var.setType(oldType == null ? valueType : oldType.getLeastSupertype(oldType));
                    }
                    scope.inferQualifiedSlot(name, qKeyName, oldType == null ? getNativeType(UNKNOWN_TYPE) : oldType, valueType);
                }
            } else {
                n.setJSType(getNativeType(UNKNOWN_TYPE));
            }
        }
        return scope;
    }

    private FlowScope traverseAdd(Node n, FlowScope scope) {
        Node left = n.getFirstChild();
        Node right = left.getNext();
        scope = traverseChildren(n, scope);
        JSType leftType = left.getJSType();
        JSType rightType = right.getJSType();
        JSType type = getNativeType(UNKNOWN_TYPE);
        if (leftType != null && rightType != null) {
            boolean leftIsUnknown = leftType.isUnknownType();
            boolean rightIsUnknown = rightType.isUnknownType();
            if (leftIsUnknown && rightIsUnknown) {
                type = getNativeType(UNKNOWN_TYPE);
            } else if ((!leftIsUnknown && leftType.isString()) || (!rightIsUnknown && rightType.isString())) {
                type = getNativeType(STRING_TYPE);
            } else if (leftIsUnknown || rightIsUnknown) {
                type = getNativeType(UNKNOWN_TYPE);
            } else if (isAddedAsNumber(leftType) && isAddedAsNumber(rightType)) {
                type = getNativeType(NUMBER_TYPE);
            } else {
                type = registry.createUnionType(STRING_TYPE, NUMBER_TYPE);
            }
        }
        n.setJSType(type);
        if (n.isAssignAdd()) {
            updateScopeForTypeChange(scope, left, leftType, type);
        }
        return scope;
    }

    private boolean isAddedAsNumber(JSType type) {
        return type.isSubtype(registry.createUnionType(VOID_TYPE, NULL_TYPE, NUMBER_VALUE_OR_OBJECT_TYPE, BOOLEAN_TYPE, BOOLEAN_OBJECT_TYPE));
    }

    private FlowScope traverseHook(Node n, FlowScope scope) {
        Node condition = n.getFirstChild();
        Node trueNode = condition.getNext();
        Node falseNode = n.getLastChild();
        // verify the condition
scope = traverse(condition, scope);
        // reverse abstract interpret the condition to produce two new scopes
FlowScope trueScope = reverseInterpreter.getPreciserScopeKnowingConditionOutcome(condition, scope, true);
        FlowScope falseScope = reverseInterpreter.getPreciserScopeKnowingConditionOutcome(condition, scope, false);
        // traverse the true node with the trueScope
traverse(trueNode, trueScope.createChildFlowScope());
        // traverse the false node with the falseScope
traverse(falseNode, falseScope.createChildFlowScope());
        // meet true and false nodes' types and assign
JSType trueType = trueNode.getJSType();
        JSType falseType = falseNode.getJSType();
        if (trueType != null && falseType != null) {
            n.setJSType(trueType.getLeastSupertype(falseType));
        } else {
            n.setJSType(null);
        }
        return scope.createChildFlowScope();
    }

    private FlowScope traverseCall(Node n, FlowScope scope) {
        scope = traverseChildren(n, scope);
        Node left = n.getFirstChild();
        JSType functionType = getJSType(left).restrictByNotNullOrUndefined();
        if (functionType.isFunctionType()) {
            FunctionType fnType = functionType.toMaybeFunctionType();
            n.setJSType(fnType.getReturnType());
            backwardsInferenceFromCallSite(n, fnType);
        } else if (functionType.equals(getNativeType(CHECKED_UNKNOWN_TYPE))) {
            n.setJSType(getNativeType(CHECKED_UNKNOWN_TYPE));
        }
        scope = tightenTypesAfterAssertions(scope, n);
        return scope;
    }

    private FlowScope tightenTypesAfterAssertions(FlowScope scope, Node callNode) {
        Node left = callNode.getFirstChild();
        Node firstParam = left.getNext();
        AssertionFunctionSpec assertionFunctionSpec = assertionFunctionsMap.get(left.getQualifiedName());
        if (assertionFunctionSpec == null || firstParam == null) {
            return scope;
        }
        Node assertedNode = assertionFunctionSpec.getAssertedParam(firstParam);
        if (assertedNode == null) {
            return scope;
        }
        JSType assertedType = assertionFunctionSpec.getAssertedType(callNode, registry);
        String assertedNodeName = assertedNode.getQualifiedName();
        JSType narrowed;
        if (assertedType == null) {
            // Handle arbitrary expressions within the assert.
scope = reverseInterpreter.getPreciserScopeKnowingConditionOutcome(assertedNode, scope, true);
            // Build the result of the assertExpression
narrowed = getJSType(assertedNode).restrictByNotNullOrUndefined();
        } else {
            // Handle assertions that enforce expressions are of a certain type.
JSType type = getJSType(assertedNode);
            narrowed = type.getGreatestSubtype(assertedType);
            if (assertedNodeName != null && type.differsFrom(narrowed)) {
                scope = narrowScope(scope, assertedNode, narrowed);
            }
        }
        if (getJSType(callNode).differsFrom(narrowed)) {
            callNode.setJSType(narrowed);
        }
        return scope;
    }

    private FlowScope narrowScope(FlowScope scope, Node node, JSType narrowed) {
        if (node.isThis()) {
            return scope;
        }
        scope = scope.createChildFlowScope();
        if (node.isGetProp()) {
            scope.inferQualifiedSlot(node, node.getQualifiedName(), getJSType(node), narrowed);
        } else {
            redeclareSimpleVar(scope, node, narrowed);
        }
        return scope;
    }

    private void backwardsInferenceFromCallSite(Node n, FunctionType fnType) {
        boolean updatedFnType = inferTemplatedTypesForCall(n, fnType);
        if (updatedFnType) {
            fnType = n.getFirstChild().getJSType().toMaybeFunctionType();
        }
        updateTypeOfParameters(n, fnType);
        updateBind(n, fnType);
    }

    private void updateBind(Node n, FunctionType fnType) {
        CodingConvention.Bind bind = compiler.getCodingConvention().describeFunctionBind(n, true);
        if (bind == null) {
            return;
        }
        FunctionType callTargetFn = getJSType(bind.target).restrictByNotNullOrUndefined().toMaybeFunctionType();
        if (callTargetFn == null) {
            return;
        }
        n.setJSType(callTargetFn.getBindReturnType(bind.getBoundParameterCount() + 1));
    }

    private void updateTypeOfParameters(Node n, FunctionType fnType) {
        int i = 0;
        int childCount = n.getChildCount();
        for (Node iParameter : fnType.getParameters()) {
            if (i + 1 >= childCount) {
                return;
            }
            JSType iParameterType = getJSType(iParameter);
            Node iArgument = n.getChildAtIndex(i + 1);
            JSType iArgumentType = getJSType(iArgument);
            inferPropertyTypesToMatchConstraint(iArgumentType, iParameterType);
            // we only care about FUNCTION subtypes here.
JSType restrictedParameter = iParameterType.restrictByNotNullOrUndefined().toMaybeFunctionType();
            if (restrictedParameter != null) {
                if (iArgument.isFunction() && iArgumentType.isFunctionType() && iArgument.getJSDocInfo() == null) {
                    iArgument.setJSType(restrictedParameter);
                    iArgument.putBooleanProp(Node.INFERRED_FUNCTION, true);
                }
            }
            i++;
        }
    }

    private Map<TemplateType, JSType> inferTemplateTypesFromParameters(FunctionType fnType, Node call) {
        if (fnType.getTemplateTypeNames().isEmpty() || !call.hasMoreThanOneChild()) {
            return Collections.emptyMap();
        }
        Map<TemplateType, JSType> resolvedTypes = Maps.newIdentityHashMap();
        maybeResolveTemplateTypeFromNodes(fnType.getParameters(), call.getChildAtIndex(1).siblings(), resolvedTypes);
        return resolvedTypes;
    }

    private void maybeResolveTemplatedType(JSType paramType, JSType argType, Map<TemplateType, JSType> resolvedTypes) {
        if (paramType.isTemplateType()) {
            // @param {T}
resolvedTemplateType(resolvedTypes, paramType.toMaybeTemplateType(), argType);
        } else if (paramType.isUnionType()) {
            // @param {Array.<T>|NodeList|Arguments|{length:number}}
UnionType unionType = paramType.toMaybeUnionType();
            for (JSType alernative : unionType.getAlternates()) {
                maybeResolveTemplatedType(alernative, argType, resolvedTypes);
            }
        } else if (paramType.isFunctionType()) {
            FunctionType paramFunctionType = paramType.toMaybeFunctionType();
            FunctionType argFunctionType = argType.restrictByNotNullOrUndefined().collapseUnion().toMaybeFunctionType();
            if (argFunctionType != null && argFunctionType.isSubtype(paramType)) {
                // infer from return type of the function type
maybeResolveTemplatedType(paramFunctionType.getReturnType(), argFunctionType.getReturnType(), resolvedTypes);
                // infer from parameter types of the function type
maybeResolveTemplateTypeFromNodes(paramFunctionType.getParameters(), argFunctionType.getParameters(), resolvedTypes);
            }
        } else if (paramType.isParameterizedType()) {
            ParameterizedType paramObjectType = paramType.toMaybeParameterizedType();
            JSType typeParameter = paramObjectType.getParameterType();
            Preconditions.checkNotNull(typeParameter);
            if (typeParameter != null) {
                // @param {Array.<T>}
ObjectType argObjectType = argType.restrictByNotNullOrUndefined().collapseUnion().toMaybeParameterizedType();
                if (argObjectType != null && argObjectType.isSubtype(paramType)) {
                    JSType argTypeParameter = argObjectType.getParameterType();
                    Preconditions.checkNotNull(argTypeParameter);
                    maybeResolveTemplatedType(typeParameter, argTypeParameter, resolvedTypes);
                }
            }
        }
    }

    private void maybeResolveTemplateTypeFromNodes(Iterable<Node> declParams, Iterable<Node> callParams, Map<TemplateType, JSType> resolvedTypes) {
        maybeResolveTemplateTypeFromNodes(declParams.iterator(), callParams.iterator(), resolvedTypes);
    }

    private void maybeResolveTemplateTypeFromNodes(Iterator<Node> declParams, Iterator<Node> callParams, Map<TemplateType, JSType> resolvedTypes) {
        while (declParams.hasNext() && callParams.hasNext()) {
            maybeResolveTemplatedType(getJSType(declParams.next()), getJSType(callParams.next()), resolvedTypes);
        }
    }

    private void resolvedTemplateType(Map<TemplateType, JSType> map, TemplateType template, JSType resolved) {
        JSType previous = map.get(template);
        if (!resolved.isUnknownType()) {
            if (previous == null) {
                map.put(template, resolved);
            } else {
                JSType join = previous.getLeastSupertype(resolved);
                map.put(template, join);
            }
        }
    }

    private static class TemplateTypeReplacer extends ModificationVisitor {

        private final Map<TemplateType, JSType> replacements;

        private final JSTypeRegistry registry;

        TemplateTypeReplacer(JSTypeRegistry registry, Map<TemplateType, JSType> replacements) {
            super(registry);
            this.registry = registry;
            this.replacements = replacements;
        }

        @Override
public JSType caseTemplateType(TemplateType type) {
    JSType replacement = replacements.get(type);
    return replacement != null ? replacement : registry.getNativeType(UNKNOWN_TYPE);
}
    }

    private boolean inferTemplatedTypesForCall(Node n, FunctionType fnType) {
        if (fnType.getTemplateTypeNames().isEmpty()) {
            return false;
        }
        // Try to infer the template types
Map<TemplateType, JSType> inferred = inferTemplateTypesFromParameters(fnType, n);
        if (inferred.size() > 0) {
            // Something useful was found, try to replace it.
TemplateTypeReplacer replacer = new TemplateTypeReplacer(registry, inferred);
            Node callTarget = n.getFirstChild();
            FunctionType replacementFnType = fnType.visit(replacer).toMaybeFunctionType();
            Preconditions.checkNotNull(replacementFnType);
            callTarget.setJSType(replacementFnType);
            n.setJSType(replacementFnType.getReturnType());
            return true;
        }
        return false;
    }

    private FlowScope traverseNew(Node n, FlowScope scope) {
        scope = traverseChildren(n, scope);
        Node constructor = n.getFirstChild();
        JSType constructorType = constructor.getJSType();
        JSType type = null;
        if (constructorType != null) {
            constructorType = constructorType.restrictByNotNullOrUndefined();
            if (constructorType.isUnknownType()) {
                type = getNativeType(UNKNOWN_TYPE);
            } else {
                FunctionType ct = constructorType.toMaybeFunctionType();
                if (ct == null && constructorType instanceof FunctionType) {
                    // interface, precisely because it can validly construct objects.
ct = (FunctionType) constructorType;
                }
                if (ct != null && ct.isConstructor()) {
                    type = ct.getInstanceType();
                    backwardsInferenceFromCallSite(n, ct);
                }
            }
        }
        n.setJSType(type);
        return scope;
    }

    private BooleanOutcomePair traverseAnd(Node n, FlowScope scope) {
        return traverseShortCircuitingBinOp(n, scope, true);
    }

    private FlowScope traverseChildren(Node n, FlowScope scope) {
        for (Node el = n.getFirstChild(); el != null; el = el.getNext()) {
            scope = traverse(el, scope);
        }
        return scope;
    }

    private FlowScope traverseGetElem(Node n, FlowScope scope) {
        scope = traverseChildren(n, scope);
        ObjectType objType = ObjectType.cast(getJSType(n.getFirstChild()).restrictByNotNullOrUndefined());
        if (objType != null) {
            JSType type = objType.getParameterType();
            if (type != null) {
                n.setJSType(type);
            }
        }
        return dereferencePointer(n.getFirstChild(), scope);
    }

    private FlowScope traverseGetProp(Node n, FlowScope scope) {
        Node objNode = n.getFirstChild();
        Node property = n.getLastChild();
        scope = traverseChildren(n, scope);
        n.setJSType(getPropertyType(objNode.getJSType(), property.getString(), n, scope));
        return dereferencePointer(n.getFirstChild(), scope);
    }

    private void inferPropertyTypesToMatchConstraint(JSType type, JSType constraint) {
        if (type == null || constraint == null) {
            return;
        }
        type.matchConstraint(constraint);
    }

    private FlowScope dereferencePointer(Node n, FlowScope scope) {
        if (n.isQualifiedName()) {
            JSType type = getJSType(n);
            JSType narrowed = type.restrictByNotNullOrUndefined();
            if (type != narrowed) {
                scope = narrowScope(scope, n, narrowed);
            }
        }
        return scope;
    }

    private JSType getPropertyType(JSType objType, String propName, Node n, FlowScope scope) {
        // 4) A name in the type registry (as a last resort)
JSType unknownType = getNativeType(UNKNOWN_TYPE);
        JSType propertyType = null;
        boolean isLocallyInferred = false;
        // Scopes sometimes contain inferred type info about qualified names.
String qualifiedName = n.getQualifiedName();
        StaticSlot<JSType> var = scope.getSlot(qualifiedName);
        if (var != null) {
            JSType varType = var.getType();
            if (varType != null) {
                boolean isDeclared = !var.isTypeInferred();
                isLocallyInferred = (var != syntacticScope.getSlot(qualifiedName));
                if (isDeclared || isLocallyInferred) {
                    propertyType = varType;
                }
            }
        }
        if (propertyType == null && objType != null) {
            JSType foundType = objType.findPropertyType(propName);
            if (foundType != null) {
                propertyType = foundType;
            }
        }
        if ((propertyType == null || propertyType.isUnknownType()) && qualifiedName != null) {
            // If we find this node in the registry, then we can infer its type.
ObjectType regType = ObjectType.cast(registry.getType(qualifiedName));
            if (regType != null) {
                propertyType = regType.getConstructor();
            }
        }
        if (propertyType == null) {
            return getNativeType(UNKNOWN_TYPE);
        } else if (propertyType.equals(unknownType) && isLocallyInferred) {
            return getNativeType(CHECKED_UNKNOWN_TYPE);
        } else {
            return propertyType;
        }
    }

    private BooleanOutcomePair traverseOr(Node n, FlowScope scope) {
        return traverseShortCircuitingBinOp(n, scope, false);
    }

    private BooleanOutcomePair traverseShortCircuitingBinOp(Node n, FlowScope scope, boolean condition) {
        Node left = n.getFirstChild();
        Node right = n.getLastChild();
        // type the left node
BooleanOutcomePair leftLiterals = traverseWithinShortCircuitingBinOp(left, scope.createChildFlowScope());
        JSType leftType = left.getJSType();
        // scope in which to verify the right node
FlowScope rightScope = reverseInterpreter.getPreciserScopeKnowingConditionOutcome(left, leftLiterals.getOutcomeFlowScope(left.getType(), condition), condition);
        // type the right node
BooleanOutcomePair rightLiterals = traverseWithinShortCircuitingBinOp(right, rightScope.createChildFlowScope());
        JSType rightType = right.getJSType();
        JSType type;
        BooleanOutcomePair literals;
        if (leftType != null && rightType != null) {
            leftType = leftType.getRestrictedTypeGivenToBooleanOutcome(!condition);
            if (leftLiterals.toBooleanOutcomes == BooleanLiteralSet.get(!condition)) {
                // evaluated.
type = leftType;
                literals = leftLiterals;
            } else {
                // ToBoolean predicate and of the right type.
type = leftType.getLeastSupertype(rightType);
                literals = getBooleanOutcomePair(leftLiterals, rightLiterals, condition);
            }
            if (literals.booleanValues == BooleanLiteralSet.EMPTY && getNativeType(BOOLEAN_TYPE).isSubtype(type)) {
                if (type.isUnionType()) {
                    type = type.toMaybeUnionType().getRestrictedUnion(getNativeType(BOOLEAN_TYPE));
                }
            }
        } else {
            type = null;
            literals = new BooleanOutcomePair(BooleanLiteralSet.BOTH, BooleanLiteralSet.BOTH, leftLiterals.getJoinedFlowScope(), rightLiterals.getJoinedFlowScope());
        }
        n.setJSType(type);
        return literals;
    }

    private BooleanOutcomePair traverseWithinShortCircuitingBinOp(Node n, FlowScope scope) {
        switch(n.getType()) {
            case Token.AND:
                return traverseAnd(n, scope);
            case Token.OR:
                return traverseOr(n, scope);
            default:
                scope = traverse(n, scope);
                return newBooleanOutcomePair(n.getJSType(), scope);
            }
    }

    BooleanOutcomePair getBooleanOutcomePair(BooleanOutcomePair left, BooleanOutcomePair right, boolean condition) {
        return new BooleanOutcomePair(getBooleanOutcomes(left.toBooleanOutcomes, right.toBooleanOutcomes, condition), getBooleanOutcomes(left.booleanValues, right.booleanValues, condition), left.getJoinedFlowScope(), right.getJoinedFlowScope());
    }

    static BooleanLiteralSet getBooleanOutcomes(BooleanLiteralSet left, BooleanLiteralSet right, boolean condition) {
        return right.union(left.intersection(BooleanLiteralSet.get(!condition)));
    }

    private final class BooleanOutcomePair {

        final BooleanLiteralSet toBooleanOutcomes;

        final BooleanLiteralSet booleanValues;

        final FlowScope leftScope;

        final FlowScope rightScope;

        FlowScope joinedScope = null;

        BooleanOutcomePair(BooleanLiteralSet toBooleanOutcomes, BooleanLiteralSet booleanValues, FlowScope leftScope, FlowScope rightScope) {
            this.toBooleanOutcomes = toBooleanOutcomes;
            this.booleanValues = booleanValues;
            this.leftScope = leftScope;
            this.rightScope = rightScope;
        }

        FlowScope getJoinedFlowScope() {
            if (joinedScope == null) {
                if (leftScope == rightScope) {
                    joinedScope = rightScope;
                } else {
                    joinedScope = join(leftScope, rightScope);
                }
            }
            return joinedScope;
        }

        FlowScope getOutcomeFlowScope(int nodeType, boolean outcome) {
            if (nodeType == Token.AND && outcome || nodeType == Token.OR && !outcome) {
                return rightScope;
            } else {
                return getJoinedFlowScope();
            }
        }
    }

    private BooleanOutcomePair newBooleanOutcomePair(JSType jsType, FlowScope flowScope) {
        if (jsType == null) {
            return new BooleanOutcomePair(BooleanLiteralSet.BOTH, BooleanLiteralSet.BOTH, flowScope, flowScope);
        }
        return new BooleanOutcomePair(jsType.getPossibleToBooleanOutcomes(), registry.getNativeType(BOOLEAN_TYPE).isSubtype(jsType) ? BooleanLiteralSet.BOTH : BooleanLiteralSet.EMPTY, flowScope, flowScope);
    }

    private void redeclareSimpleVar(FlowScope scope, Node nameNode, JSType varType) {
        Preconditions.checkState(nameNode.isName());
        String varName = nameNode.getString();
        if (varType == null) {
            varType = getNativeType(JSTypeNative.UNKNOWN_TYPE);
        }
        if (isUnflowable(syntacticScope.getVar(varName))) {
            return;
        }
        scope.inferSlotType(varName, varType);
    }

    private boolean isUnflowable(Var v) {
        return v != null && v.isLocal() && v.isMarkedEscaped() && v.getScope() == syntacticScope;
    }

    private JSType getJSType(Node n) {
        JSType jsType = n.getJSType();
        if (jsType == null) {
            return getNativeType(UNKNOWN_TYPE);
        } else {
            return jsType;
        }
    }

    private JSType getNativeType(JSTypeNative typeId) {
        return registry.getNativeType(typeId);
    }
}
