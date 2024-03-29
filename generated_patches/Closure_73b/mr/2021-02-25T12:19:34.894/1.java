package com.google.javascript.rhino;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.javascript.rhino.jstype.JSType;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class Node implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    public static final int LOCAL_BLOCK_PROP = -3, OBJECT_IDS_PROP = -2, CATCH_SCOPE_PROP = -1, LABEL_ID_PROP = 0, TARGET_PROP = 1, BREAK_PROP = 2, CONTINUE_PROP = 3, ENUM_PROP = 4, FUNCTION_PROP = 5, TEMP_PROP = 6, LOCAL_PROP = 7, CODEOFFSET_PROP = 8, FIXUPS_PROP = 9, VARS_PROP = 10, USES_PROP = 11, REGEXP_PROP = 12, CASES_PROP = 13, DEFAULT_PROP = 14, CASEARRAY_PROP = 15, SOURCENAME_PROP = 16, TYPE_PROP = 17, SPECIAL_PROP_PROP = 18, LABEL_PROP = 19, FINALLY_PROP = 20, LOCALCOUNT_PROP = 21, TARGETBLOCK_PROP = 22, VARIABLE_PROP = 23, LASTUSE_PROP = 24, ISNUMBER_PROP = 25, DIRECTCALL_PROP = 26, SPECIALCALL_PROP = 27, DEBUGSOURCE_PROP = 28, JSDOC_INFO_PROP = 29, VAR_ARGS_NAME = 29, SKIP_INDEXES_PROP = 30, INCRDECR_PROP = 31, MEMBER_TYPE_PROP = 32, NAME_PROP = 33, PARENTHESIZED_PROP = 34, QUOTED_PROP = 35, OPT_ARG_NAME = 36, SYNTHETIC_BLOCK_PROP = 37, EMPTY_BLOCK = 38, ORIGINALNAME_PROP = 39, BRACELESS_TYPE = 40, SIDE_EFFECT_FLAGS = 41, IS_CONSTANT_NAME = 42, IS_OPTIONAL_PARAM = 43, IS_VAR_ARGS_PARAM = 44, IS_NAMESPACE = 45, IS_DISPATCHER = 46, DIRECTIVES = 47, DIRECT_EVAL = 48, FREE_CALL = 1, LAST_PROP = 49;

    public static final int BOTH = 0, LEFT = 1, RIGHT = 2;

    public static final int NON_SPECIALCALL = 0, SPECIALCALL_EVAL = 1, SPECIALCALL_WITH = 2;

    public static final int DECR_FLAG = 0x1, POST_FLAG = 0x2;

    public static final int PROPERTY_FLAG = 0x1, ATTRIBUTE_FLAG = 0x2, DESCENDANTS_FLAG = 0x4;

    private static final String propToString(int propType) {
        switch(propType) {
            case LOCAL_BLOCK_PROP:
                return "local_block";
            case OBJECT_IDS_PROP:
                return "object_ids_prop";
            case CATCH_SCOPE_PROP:
                return "catch_scope_prop";
            case LABEL_ID_PROP:
                return "label_id_prop";
            case TARGET_PROP:
                return "target";
            case BREAK_PROP:
                return "break";
            case CONTINUE_PROP:
                return "continue";
            case ENUM_PROP:
                return "enum";
            case FUNCTION_PROP:
                return "function";
            case TEMP_PROP:
                return "temp";
            case LOCAL_PROP:
                return "local";
            case CODEOFFSET_PROP:
                return "codeoffset";
            case FIXUPS_PROP:
                return "fixups";
            case VARS_PROP:
                return "vars";
            case USES_PROP:
                return "uses";
            case REGEXP_PROP:
                return "regexp";
            case CASES_PROP:
                return "cases";
            case DEFAULT_PROP:
                return "default";
            case CASEARRAY_PROP:
                return "casearray";
            case SOURCENAME_PROP:
                return "sourcename";
            case TYPE_PROP:
                return "type";
            case SPECIAL_PROP_PROP:
                return "special_prop";
            case LABEL_PROP:
                return "label";
            case FINALLY_PROP:
                return "finally";
            case LOCALCOUNT_PROP:
                return "localcount";
            case TARGETBLOCK_PROP:
                return "targetblock";
            case VARIABLE_PROP:
                return "variable";
            case LASTUSE_PROP:
                return "lastuse";
            case ISNUMBER_PROP:
                return "isnumber";
            case DIRECTCALL_PROP:
                return "directcall";
            case SPECIALCALL_PROP:
                return "specialcall";
            case DEBUGSOURCE_PROP:
                return "debugsource";
            case JSDOC_INFO_PROP:
                return "jsdoc_info";
            case SKIP_INDEXES_PROP:
                return "skip_indexes";
            case INCRDECR_PROP:
                return "incrdecr";
            case MEMBER_TYPE_PROP:
                return "member_type";
            case NAME_PROP:
                return "name";
            case PARENTHESIZED_PROP:
                return "parenthesized";
            case QUOTED_PROP:
                return "quoted";
            case SYNTHETIC_BLOCK_PROP:
                return "synthetic";
            case EMPTY_BLOCK:
                return "empty_block";
            case ORIGINALNAME_PROP:
                return "originalname";
            case SIDE_EFFECT_FLAGS:
                return "side_effect_flags";
            case IS_CONSTANT_NAME:
                return "is_constant_name";
            case IS_OPTIONAL_PARAM:
                return "is_optional_param";
            case IS_VAR_ARGS_PARAM:
                return "is_var_args_param";
            case IS_NAMESPACE:
                return "is_namespace";
            case IS_DISPATCHER:
                return "is_dispatcher";
            case DIRECTIVES:
                return "directives";
            case DIRECT_EVAL:
                return "direct_eval";
            case FREE_CALL:
                return "free_call";
            default:
                Kit.codeBug();
            }
        return null;
    }

    private static class NumberNode extends Node {

        private static final long serialVersionUID = 1L;

        NumberNode(double number) {
            super(Token.NUMBER);
            this.number = number;
        }

        public NumberNode(double number, int lineno, int charno) {
            super(Token.NUMBER, lineno, charno);
            this.number = number;
        }

        @Override
public double getDouble() {
    return this.number;
}

        @Override
public void setDouble(double d) {
    this.number = d;
}

        @Override
boolean isEquivalentTo(Node node, boolean compareJsType, boolean recurse) {
    return (super.isEquivalentTo(node, compareJsType, recurse) && getDouble() == ((NumberNode) node).getDouble());
}

        private double number;
    }

    private static class StringNode extends Node {

        private static final long serialVersionUID = 1L;

        StringNode(int type, String str) {
            super(type);
            if (null == str) {
                throw new IllegalArgumentException("StringNode: str is null");
            }
            this.str = str;
        }

        StringNode(int type, String str, int lineno, int charno) {
            super(type, lineno, charno);
            if (null == str) {
                throw new IllegalArgumentException("StringNode: str is null");
            }
            this.str = str;
        }

        @Override
public String getString() {
    return this.str;
}

        @Override
public void setString(String str) {
    if (null == str) {
        throw new IllegalArgumentException("StringNode: str is null");
    }
    this.str = str;
}

        @Override
boolean isEquivalentTo(Node node, boolean compareJsType, boolean recurse) {
    return (super.isEquivalentTo(node, compareJsType, recurse) && this.str.equals(((StringNode) node).str));
}

        @Override
public boolean isQuotedString() {
    return getBooleanProp(QUOTED_PROP);
}

        @Override
public void setQuotedString() {
    putBooleanProp(QUOTED_PROP, true);
}

        private String str;
    }

    private static class PropListItem implements Serializable {

        private static final long serialVersionUID = 1L;

        final PropListItem next;

        final int type;

        final int intValue;

        final Object objectValue;

        PropListItem(int type, int intValue, PropListItem next) {
            this(type, intValue, null, next);
        }

        PropListItem(int type, Object objectValue, PropListItem next) {
            this(type, 0, objectValue, next);
        }

        PropListItem(int type, int intValue, Object objectValue, PropListItem next) {
            this.type = type;
            this.intValue = intValue;
            this.objectValue = objectValue;
            this.next = next;
        }
    }

    public Node(int nodeType) {
        type = nodeType;
        parent = null;
        sourcePosition = -1;
    }

    public Node(int nodeType, Node child) {
        Preconditions.checkArgument(child.parent == null, "new child has existing parent");
        Preconditions.checkArgument(child.next == null, "new child has existing sibling");
        type = nodeType;
        parent = null;
        first = last = child;
        child.next = null;
        child.parent = this;
        sourcePosition = -1;
    }

    public Node(int nodeType, Node left, Node right) {
        Preconditions.checkArgument(left.parent == null, "first new child has existing parent");
        Preconditions.checkArgument(left.next == null, "first new child has existing sibling");
        Preconditions.checkArgument(right.parent == null, "second new child has existing parent");
        Preconditions.checkArgument(right.next == null, "second new child has existing sibling");
        type = nodeType;
        parent = null;
        first = left;
        last = right;
        left.next = right;
        left.parent = this;
        right.next = null;
        right.parent = this;
        sourcePosition = -1;
    }

    public Node(int nodeType, Node left, Node mid, Node right) {
        Preconditions.checkArgument(left.parent == null);
        Preconditions.checkArgument(left.next == null);
        Preconditions.checkArgument(mid.parent == null);
        Preconditions.checkArgument(mid.next == null);
        Preconditions.checkArgument(right.parent == null);
        Preconditions.checkArgument(right.next == null);
        type = nodeType;
        parent = null;
        first = left;
        last = right;
        left.next = mid;
        left.parent = this;
        mid.next = right;
        mid.parent = this;
        right.next = null;
        right.parent = this;
        sourcePosition = -1;
    }

    public Node(int nodeType, Node left, Node mid, Node mid2, Node right) {
        Preconditions.checkArgument(left.parent == null);
        Preconditions.checkArgument(left.next == null);
        Preconditions.checkArgument(mid.parent == null);
        Preconditions.checkArgument(mid.next == null);
        Preconditions.checkArgument(mid2.parent == null);
        Preconditions.checkArgument(mid2.next == null);
        Preconditions.checkArgument(right.parent == null);
        Preconditions.checkArgument(right.next == null);
        type = nodeType;
        parent = null;
        first = left;
        last = right;
        left.next = mid;
        left.parent = this;
        mid.next = mid2;
        mid.parent = this;
        mid2.next = right;
        mid2.parent = this;
        right.next = null;
        right.parent = this;
        sourcePosition = -1;
    }

    public Node(int nodeType, int lineno, int charno) {
        type = nodeType;
        parent = null;
        sourcePosition = mergeLineCharNo(lineno, charno);
    }

    public Node(int nodeType, Node child, int lineno, int charno) {
        this(nodeType, child);
        sourcePosition = mergeLineCharNo(lineno, charno);
    }

    public Node(int nodeType, Node left, Node right, int lineno, int charno) {
        this(nodeType, left, right);
        sourcePosition = mergeLineCharNo(lineno, charno);
    }

    public Node(int nodeType, Node left, Node mid, Node right, int lineno, int charno) {
        this(nodeType, left, mid, right);
        sourcePosition = mergeLineCharNo(lineno, charno);
    }

    public Node(int nodeType, Node left, Node mid, Node mid2, Node right, int lineno, int charno) {
        this(nodeType, left, mid, mid2, right);
        sourcePosition = mergeLineCharNo(lineno, charno);
    }

    public Node(int nodeType, Node[] children, int lineno, int charno) {
        this(nodeType, children);
        sourcePosition = mergeLineCharNo(lineno, charno);
    }

    public Node(int nodeType, Node[] children) {
        this.type = nodeType;
        parent = null;
        if (children.length != 0) {
            this.first = children[0];
            this.last = children[children.length - 1];
            for (int i = 1; i < children.length; i++) {
                if (null != children[i - 1].next) {
                    throw new IllegalArgumentException("duplicate child");
                }
                children[i - 1].next = children[i];
                Preconditions.checkArgument(children[i - 1].parent == null);
                children[i - 1].parent = this;
            }
            Preconditions.checkArgument(children[children.length - 1].parent == null);
            children[children.length - 1].parent = this;
            if (null != this.last.next) {
                throw new IllegalArgumentException("duplicate child");
            }
        }
    }

    public static Node newNumber(double number) {
        return new NumberNode(number);
    }

    public static Node newNumber(double number, int lineno, int charno) {
        return new NumberNode(number, lineno, charno);
    }

    public static Node newString(String str) {
        return new StringNode(Token.STRING, str);
    }

    public static Node newString(int type, String str) {
        return new StringNode(type, str);
    }

    public static Node newString(String str, int lineno, int charno) {
        return new StringNode(Token.STRING, str, lineno, charno);
    }

    public static Node newString(int type, String str, int lineno, int charno) {
        return new StringNode(type, str, lineno, charno);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean hasChildren() {
        return first != null;
    }

    public Node getFirstChild() {
        return first;
    }

    public Node getLastChild() {
        return last;
    }

    public Node getNext() {
        return next;
    }

    public Node getChildBefore(Node child) {
        if (child == first) {
            return null;
        }
        Node n = first;
        while (n.next != child) {
            n = n.next;
            if (n == null) {
                throw new RuntimeException("node is not a child");
            }
        }
        return n;
    }

    public Node getChildAtIndex(int i) {
        Node n = first;
        while (i > 0) {
            n = n.next;
            i--;
        }
        return n;
    }

    public Node getLastSibling() {
        Node n = this;
        while (n.next != null) {
            n = n.next;
        }
        return n;
    }

    public void addChildToFront(Node child) {
        Preconditions.checkArgument(child.parent == null);
        Preconditions.checkArgument(child.next == null);
        child.parent = this;
        child.next = first;
        first = child;
        if (last == null) {
            last = child;
        }
    }

    public void addChildToBack(Node child) {
        Preconditions.checkArgument(child.parent == null);
        Preconditions.checkArgument(child.next == null);
        child.parent = this;
        child.next = null;
        if (last == null) {
            first = last = child;
            return;
        }
        last.next = child;
        last = child;
    }

    public void addChildrenToFront(Node children) {
        for (Node child = children; child != null; child = child.next) {
            Preconditions.checkArgument(child.parent == null);
            child.parent = this;
        }
        Node lastSib = children.getLastSibling();
        lastSib.next = first;
        first = children;
        if (last == null) {
            last = lastSib;
        }
    }

    public void addChildrenToBack(Node children) {
        for (Node child = children; child != null; child = child.next) {
            Preconditions.checkArgument(child.parent == null);
            child.parent = this;
        }
        if (last != null) {
            last.next = children;
        }
        last = children.getLastSibling();
        if (first == null) {
            first = children;
        }
    }

    public void addChildBefore(Node newChild, Node node) {
        Preconditions.checkArgument(node != null, "The existing child node of the parent should not be null.");
        Preconditions.checkArgument(newChild.next == null, "The new child node has siblings.");
        Preconditions.checkArgument(newChild.parent == null, "The new child node already has a parent.");
        if (first == node) {
            newChild.parent = this;
            newChild.next = first;
            first = newChild;
            return;
        }
        Node prev = getChildBefore(node);
        addChildAfter(newChild, prev);
    }

    public void addChildAfter(Node newChild, Node node) {
        Preconditions.checkArgument(newChild.next == null, "The new child node has siblings.");
        Preconditions.checkArgument(newChild.parent == null, "The new child node already has a parent.");
        newChild.parent = this;
        newChild.next = node.next;
        node.next = newChild;
        if (last == node) {
            last = newChild;
        }
    }

    public void removeChild(Node child) {
        Node prev = getChildBefore(child);
        if (prev == null)
            first = first.next;
        else
            prev.next = child.next;
        if (child == last)
            last = prev;
        child.next = null;
        child.parent = null;
    }

    public void replaceChild(Node child, Node newChild) {
        Preconditions.checkArgument(newChild.next == null, "The new child node has siblings.");
        Preconditions.checkArgument(newChild.parent == null, "The new child node already has a parent.");
        // Copy over important information.
newChild.copyInformationFrom(child);
        newChild.next = child.next;
        newChild.parent = this;
        if (child == first) {
            first = newChild;
        } else {
            Node prev = getChildBefore(child);
            prev.next = newChild;
        }
        if (child == last)
            last = newChild;
        child.next = null;
        child.parent = null;
    }

    public void replaceChildAfter(Node prevChild, Node newChild) {
        Preconditions.checkArgument(prevChild.parent == this, "prev is not a child of this node.");
        Preconditions.checkArgument(newChild.next == null, "The new child node has siblings.");
        Preconditions.checkArgument(newChild.parent == null, "The new child node already has a parent.");
        // Copy over important information.
newChild.copyInformationFrom(prevChild);
        Node child = prevChild.next;
        newChild.next = child.next;
        newChild.parent = this;
        prevChild.next = newChild;
        if (child == last)
            last = newChild;
        child.next = null;
        child.parent = null;
    }

    @VisibleForTesting
PropListItem lookupProperty(int propType) {
    PropListItem x = propListHead;
    while (x != null && propType != x.type) {
        x = x.next;
    }
    return x;
}

    public Node clonePropsFrom(Node other) {
        Preconditions.checkState(this.propListHead == null, "Node has existing properties.");
        this.propListHead = other.propListHead;
        return this;
    }

    public void removeProp(int propType) {
        PropListItem result = removeProp(propListHead, propType);
        if (result != propListHead) {
            propListHead = result;
        }
    }

    private PropListItem removeProp(PropListItem item, int propType) {
        if (item == null) {
            return null;
        } else if (item.type == propType) {
            return item.next;
        } else {
            PropListItem result = removeProp(item.next, propType);
            if (result != item.next) {
                return new PropListItem(item.type, item.intValue, item.objectValue, result);
            } else {
                return item;
            }
        }
    }

    public Object getProp(int propType) {
        PropListItem item = lookupProperty(propType);
        if (item == null) {
            return null;
        }
        return item.objectValue;
    }

    public boolean getBooleanProp(int propType) {
        return getIntProp(propType) != 0;
    }

    public int getIntProp(int propType) {
        PropListItem item = lookupProperty(propType);
        if (item == null) {
            return 0;
        }
        return item.intValue;
    }

    public int getExistingIntProp(int propType) {
        PropListItem item = lookupProperty(propType);
        if (item == null) {
            Kit.codeBug();
        }
        return item.intValue;
    }

    public void putProp(int propType, Object value) {
        removeProp(propType);
        if (value != null) {
            propListHead = new PropListItem(propType, value, propListHead);
        }
    }

    public void putBooleanProp(int propType, boolean value) {
        putIntProp(propType, value ? 1 : 0);
    }

    public void putIntProp(int propType, int value) {
        removeProp(propType);
        if (value != 0) {
            propListHead = new PropListItem(propType, value, propListHead);
        }
    }

    private int[] getSortedPropTypes() {
        int count = 0;
        for (PropListItem x = propListHead; x != null; x = x.next) {
            count++;
        }
        int[] keys = new int[count];
        for (PropListItem x = propListHead; x != null; x = x.next) {
            count--;
            keys[count] = x.type;
        }
        Arrays.sort(keys);
        return keys;
    }

    public int getLineno() {
        return extractLineno(sourcePosition);
    }

    public int getCharno() {
        return extractCharno(sourcePosition);
    }

    public int getSourcePosition() {
        return sourcePosition;
    }

    public double getDouble() throws UnsupportedOperationException {
        if (this.getType() == Token.NUMBER) {
            throw new IllegalStateException("Number node not created with Node.newNumber");
        } else {
            throw new UnsupportedOperationException(this + " is not a number node");
        }
    }

    public void setDouble(double s) throws UnsupportedOperationException {
        if (this.getType() == Token.NUMBER) {
            throw new IllegalStateException("Number node not created with Node.newNumber");
        } else {
            throw new UnsupportedOperationException(this + " is not a string node");
        }
    }

    public String getString() throws UnsupportedOperationException {
        if (this.getType() == Token.STRING) {
            throw new IllegalStateException("String node not created with Node.newString");
        } else {
            throw new UnsupportedOperationException(this + " is not a string node");
        }
    }

    public void setString(String s) throws UnsupportedOperationException {
        if (this.getType() == Token.STRING) {
            throw new IllegalStateException("String node not created with Node.newString");
        } else {
            throw new UnsupportedOperationException(this + " is not a string node");
        }
    }

    @Override
public String toString() {
    return toString(true, true, true);
}

    public String toString(boolean printSource, boolean printAnnotations, boolean printType) {
        if (Token.printTrees) {
            StringBuilder sb = new StringBuilder();
            toString(sb, printSource, printAnnotations, printType);
            return sb.toString();
        }
        return String.valueOf(type);
    }

    private void toString(StringBuilder sb, boolean printSource, boolean printAnnotations, boolean printType) {
        if (Token.printTrees) {
            sb.append(Token.name(type));
            if (this instanceof StringNode) {
                sb.append(' ');
                sb.append(getString());
            } else if (type == Token.FUNCTION) {
                sb.append(' ');
                if (first == null || first.getType() != Token.NAME) {
                    sb.append("<invalid>");
                } else {
                    sb.append(first.getString());
                }
            } else if (this instanceof ScriptOrFnNode) {
                ScriptOrFnNode sof = (ScriptOrFnNode) this;
                if (this instanceof FunctionNode) {
                    FunctionNode fn = (FunctionNode) this;
                    sb.append(' ');
                    sb.append(fn.getFunctionName());
                }
                if (printSource) {
                    sb.append(" [source name: ");
                    sb.append(sof.getSourceName());
                    sb.append("] [encoded source length: ");
                    sb.append(sof.getEncodedSourceEnd() - sof.getEncodedSourceStart());
                    sb.append("] [base line: ");
                    sb.append(sof.getBaseLineno());
                    sb.append("] [end line: ");
                    sb.append(sof.getEndLineno());
                    sb.append(']');
                }
            } else if (type == Token.NUMBER) {
                sb.append(' ');
                sb.append(getDouble());
            }
            if (printSource) {
                int lineno = getLineno();
                if (lineno != -1) {
                    sb.append(' ');
                    sb.append(lineno);
                }
            }
            if (printAnnotations) {
                int[] keys = getSortedPropTypes();
                for (int i = 0; i < keys.length; i++) {
                    int type = keys[i];
                    PropListItem x = lookupProperty(type);
                    sb.append(" [");
                    sb.append(propToString(type));
                    sb.append(": ");
                    String value;
                    switch(type) {
                        case TARGETBLOCK_PROP:
                            value = "target block property";
                            break;
                        case LOCAL_BLOCK_PROP:
                            value = "last local block";
                            break;
                        case ISNUMBER_PROP:
                            switch(x.intValue) {
                                case BOTH:
                                    value = "both";
                                    break;
                                case RIGHT:
                                    value = "right";
                                    break;
                                case LEFT:
                                    value = "left";
                                    break;
                                default:
                                    throw Kit.codeBug();
                                }
                            break;
                        case SPECIALCALL_PROP:
                            switch(x.intValue) {
                                case SPECIALCALL_EVAL:
                                    value = "eval";
                                    break;
                                case SPECIALCALL_WITH:
                                    value = "with";
                                    break;
                                default:
                                    throw Kit.codeBug();
                                }
                            break;
                        default:
                            Object obj = x.objectValue;
                            if (obj != null) {
                                value = obj.toString();
                            } else {
                                value = String.valueOf(x.intValue);
                            }
                            break;
                        }
                    sb.append(value);
                    sb.append(']');
                }
            }
            if (printType) {
                if (jsType != null) {
                    String jsTypeString = jsType.toString();
                    if (jsTypeString != null) {
                        sb.append(" : ");
                        sb.append(jsTypeString);
                    }
                }
            }
        }
    }

    public String toStringTree() {
        return toStringTreeImpl();
    }

    private String toStringTreeImpl() {
        try {
            StringBuilder s = new StringBuilder();
            appendStringTree(s);
            return s.toString();
        } catch (IOException e) {
            throw new RuntimeException("Should not happen\n" + e);
        }
    }

    public void appendStringTree(Appendable appendable) throws IOException {
        toStringTreeHelper(this, 0, appendable);
    }

    private static void toStringTreeHelper(Node n, int level, Appendable sb) throws IOException {
        if (Token.printTrees) {
            for (int i = 0; i != level; ++i) {
                sb.append("    ");
            }
            sb.append(n.toString());
            sb.append('\n');
            for (Node cursor = n.getFirstChild(); cursor != null; cursor = cursor.getNext()) {
                toStringTreeHelper(cursor, level + 1, sb);
            }
        }
    }

    int type;

    Node next;

    private Node first;

    private Node last;

    private PropListItem propListHead;

    public static final int COLUMN_BITS = 12;

    public static final int MAX_COLUMN_NUMBER = (1 << COLUMN_BITS) - 1;

    public static final int COLUMN_MASK = MAX_COLUMN_NUMBER;

    private int sourcePosition;

    private JSType jsType;

    private Node parent;

    public void setLineno(int lineno) {
        int charno = getCharno();
        if (charno == -1) {
            charno = 0;
        }
        sourcePosition = mergeLineCharNo(lineno, charno);
    }

    public void setCharno(int charno) {
        sourcePosition = mergeLineCharNo(getLineno(), charno);
    }

    public void setSourcePositionForTree(int sourcePosition) {
        this.sourcePosition = sourcePosition;
        for (Node child = getFirstChild(); child != null; child = child.getNext()) {
            child.setSourcePositionForTree(sourcePosition);
        }
    }

    protected static int mergeLineCharNo(int lineno, int charno) {
        if (lineno < 0 || charno < 0) {
            return -1;
        } else if ((charno & ~COLUMN_MASK) != 0) {
            return lineno << COLUMN_BITS | COLUMN_MASK;
        } else {
            return lineno << COLUMN_BITS | (charno & COLUMN_MASK);
        }
    }

    protected static int extractLineno(int lineCharNo) {
        if (lineCharNo == -1) {
            return -1;
        } else {
            return lineCharNo >>> COLUMN_BITS;
        }
    }

    protected static int extractCharno(int lineCharNo) {
        if (lineCharNo == -1) {
            return -1;
        } else {
            return lineCharNo & COLUMN_MASK;
        }
    }

    public Iterable<Node> children() {
        if (first == null) {
            return Collections.emptySet();
        } else {
            return new SiblingNodeIterable(first);
        }
    }

    public Iterable<Node> siblings() {
        return new SiblingNodeIterable(this);
    }

    private static final class SiblingNodeIterable implements Iterable<Node>, Iterator<Node> {

        private final Node start;

        private Node current;

        private boolean used;

        SiblingNodeIterable(Node start) {
            this.start = start;
            this.current = start;
            this.used = false;
        }

        public Iterator<Node> iterator() {
            if (!used) {
                used = true;
                return this;
            } else {
                return (new SiblingNodeIterable(start)).iterator();
            }
        }

        public boolean hasNext() {
            return current != null;
        }

        public Node next() {
            if (current == null) {
                throw new NoSuchElementException();
            }
            try {
                return current;
            } finally {
                current = current.getNext();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public Node getParent() {
        return parent;
    }

    public Node getAncestor(int level) {
        Preconditions.checkArgument(level >= 0);
        Node node = this;
        while (node != null && level-- > 0) {
            node = node.getParent();
        }
        return node;
    }

    public AncestorIterable getAncestors() {
        return new AncestorIterable(this.getParent());
    }

    public static class AncestorIterable implements Iterable<Node> {

        private Node cur;

        AncestorIterable(Node cur) {
            this.cur = cur;
        }

        public Iterator<Node> iterator() {
            return new Iterator<Node>() {
                
                public boolean hasNext() {
                    return cur != null;
                }

                public Node next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    Node n = cur;
                    cur = cur.getParent();
                    return n;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    public boolean hasOneChild() {
        return first != null && first == last;
    }

    public boolean hasMoreThanOneChild() {
        return first != null && first != last;
    }

    public int getChildCount() {
        int c = 0;
        for (Node n = first; n != null; n = n.next) c++;
        return c;
    }

    public boolean hasChild(Node child) {
        for (Node n = first; n != null; n = n.getNext()) {
            if (child == n) {
                return true;
            }
        }
        return false;
    }

    public String checkTreeEquals(Node node2) {
        NodeMismatch diff = checkTreeEqualsImpl(node2);
        if (diff != null) {
            return "Node tree inequality:" + "\nTree1:\n" + toStringTree() + "\n\nTree2:\n" + node2.toStringTree() + "\n\nSubtree1: " + diff.nodeA.toStringTree() + "\n\nSubtree2: " + diff.nodeB.toStringTree();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
static private Class getNodeClass(Node n) {
    Class c = n.getClass();
    if (c == FunctionNode.class || c == ScriptOrFnNode.class) {
        return Node.class;
    }
    return c;
}

    NodeMismatch checkTreeEqualsImpl(Node node2) {
        if (!isEquivalentTo(node2, false, false)) {
            return new NodeMismatch(this, node2);
        }
        NodeMismatch res = null;
        Node n, n2;
        for (n = first, n2 = node2.first; res == null && n != null; n = n.next, n2 = n2.next) {
            if (node2 == null) {
                throw new IllegalStateException();
            }
            res = n.checkTreeEqualsImpl(n2);
            if (res != null) {
                return res;
            }
        }
        return res;
    }

    NodeMismatch checkTreeTypeAwareEqualsImpl(Node node2) {
        if (!isEquivalentTo(node2, true, false)) {
            return new NodeMismatch(this, node2);
        }
        NodeMismatch res = null;
        Node n, n2;
        for (n = first, n2 = node2.first; res == null && n != null; n = n.next, n2 = n2.next) {
            res = n.checkTreeTypeAwareEqualsImpl(n2);
            if (res != null) {
                return res;
            }
        }
        return res;
    }

    public static String tokenToName(int token) {
        switch(token) {
            case Token.ERROR:
                return "error";
            case Token.EOF:
                return "eof";
            case Token.EOL:
                return "eol";
            case Token.ENTERWITH:
                return "enterwith";
            case Token.LEAVEWITH:
                return "leavewith";
            case Token.RETURN:
                return "return";
            case Token.GOTO:
                return "goto";
            case Token.IFEQ:
                return "ifeq";
            case Token.IFNE:
                return "ifne";
            case Token.SETNAME:
                return "setname";
            case Token.BITOR:
                return "bitor";
            case Token.BITXOR:
                return "bitxor";
            case Token.BITAND:
                return "bitand";
            case Token.EQ:
                return "eq";
            case Token.NE:
                return "ne";
            case Token.LT:
                return "lt";
            case Token.LE:
                return "le";
            case Token.GT:
                return "gt";
            case Token.GE:
                return "ge";
            case Token.LSH:
                return "lsh";
            case Token.RSH:
                return "rsh";
            case Token.URSH:
                return "ursh";
            case Token.ADD:
                return "add";
            case Token.SUB:
                return "sub";
            case Token.MUL:
                return "mul";
            case Token.DIV:
                return "div";
            case Token.MOD:
                return "mod";
            case Token.BITNOT:
                return "bitnot";
            case Token.NEG:
                return "neg";
            case Token.NEW:
                return "new";
            case Token.DELPROP:
                return "delprop";
            case Token.TYPEOF:
                return "typeof";
            case Token.GETPROP:
                return "getprop";
            case Token.SETPROP:
                return "setprop";
            case Token.GETELEM:
                return "getelem";
            case Token.SETELEM:
                return "setelem";
            case Token.CALL:
                return "call";
            case Token.NAME:
                return "name";
            case Token.NUMBER:
                return "number";
            case Token.STRING:
                return "string";
            case Token.NULL:
                return "null";
            case Token.THIS:
                return "this";
            case Token.FALSE:
                return "false";
            case Token.TRUE:
                return "true";
            case Token.SHEQ:
                return "sheq";
            case Token.SHNE:
                return "shne";
            case Token.REGEXP:
                return "regexp";
            case Token.POS:
                return "pos";
            case Token.BINDNAME:
                return "bindname";
            case Token.THROW:
                return "throw";
            case Token.IN:
                return "in";
            case Token.INSTANCEOF:
                return "instanceof";
            case Token.GETVAR:
                return "getvar";
            case Token.SETVAR:
                return "setvar";
            case Token.TRY:
                return "try";
            case Token.TYPEOFNAME:
                return "typeofname";
            case Token.THISFN:
                return "thisfn";
            case Token.SEMI:
                return "semi";
            case Token.LB:
                return "lb";
            case Token.RB:
                return "rb";
            case Token.LC:
                return "lc";
            case Token.RC:
                return "rc";
            case Token.LP:
                return "lp";
            case Token.RP:
                return "rp";
            case Token.COMMA:
                return "comma";
            case Token.ASSIGN:
                return "assign";
            case Token.ASSIGN_BITOR:
                return "assign_bitor";
            case Token.ASSIGN_BITXOR:
                return "assign_bitxor";
            case Token.ASSIGN_BITAND:
                return "assign_bitand";
            case Token.ASSIGN_LSH:
                return "assign_lsh";
            case Token.ASSIGN_RSH:
                return "assign_rsh";
            case Token.ASSIGN_URSH:
                return "assign_ursh";
            case Token.ASSIGN_ADD:
                return "assign_add";
            case Token.ASSIGN_SUB:
                return "assign_sub";
            case Token.ASSIGN_MUL:
                return "assign_mul";
            case Token.ASSIGN_DIV:
                return "assign_div";
            case Token.ASSIGN_MOD:
                return "assign_mod";
            case Token.HOOK:
                return "hook";
            case Token.COLON:
                return "colon";
            case Token.OR:
                return "or";
            case Token.AND:
                return "and";
            case Token.INC:
                return "inc";
            case Token.DEC:
                return "dec";
            case Token.DOT:
                return "dot";
            case Token.FUNCTION:
                return "function";
            case Token.EXPORT:
                return "export";
            case Token.IMPORT:
                return "import";
            case Token.IF:
                return "if";
            case Token.ELSE:
                return "else";
            case Token.SWITCH:
                return "switch";
            case Token.CASE:
                return "case";
            case Token.DEFAULT:
                return "default";
            case Token.WHILE:
                return "while";
            case Token.DO:
                return "do";
            case Token.FOR:
                return "for";
            case Token.BREAK:
                return "break";
            case Token.CONTINUE:
                return "continue";
            case Token.VAR:
                return "var";
            case Token.WITH:
                return "with";
            case Token.CATCH:
                return "catch";
            case Token.FINALLY:
                return "finally";
            case Token.RESERVED:
                return "reserved";
            case Token.NOT:
                return "not";
            case Token.VOID:
                return "void";
            case Token.BLOCK:
                return "block";
            case Token.ARRAYLIT:
                return "arraylit";
            case Token.OBJECTLIT:
                return "objectlit";
            case Token.LABEL:
                return "label";
            case Token.TARGET:
                return "target";
            case Token.LOOP:
                return "loop";
            case Token.EXPR_VOID:
                return "expr_void";
            case Token.EXPR_RESULT:
                return "expr_result";
            case Token.JSR:
                return "jsr";
            case Token.SCRIPT:
                return "script";
            case Token.EMPTY:
                return "empty";
            case Token.GET_REF:
                return "get_ref";
            case Token.REF_SPECIAL:
                return "ref_special";
            }
        return "<unknown=" + token + ">";
    }

    public boolean isEquivalentTo(Node node) {
        return isEquivalentTo(node, false, true);
    }

    public boolean isEquivalentToTyped(Node node) {
        return isEquivalentTo(node, true, true);
    }

    boolean isEquivalentTo(Node node, boolean compareJsType, boolean recurse) {
        if (type != node.getType() || getChildCount() != node.getChildCount() || getNodeClass(this) != getNodeClass(node)) {
            return false;
        }
        if (compareJsType && !JSType.isEquivalent(jsType, node.getJSType())) {
            return false;
        }
        if (type == Token.ARRAYLIT) {
            try {
                int[] indices1 = (int[]) getProp(Node.SKIP_INDEXES_PROP);
                int[] indices2 = (int[]) node.getProp(Node.SKIP_INDEXES_PROP);
                if (indices1 == null) {
                    if (indices2 != null) {
                        return false;
                    }
                } else if (indices2 == null) {
                    return false;
                } else if (indices1.length != indices2.length) {
                    return false;
                } else {
                    for (int i = 0; i < indices1.length; i++) {
                        if (indices1[i] != indices2[i]) {
                            return false;
                        }
                    }
                }
            } catch (Exception e) {
                return false;
            }
        } else if (type == Token.INC || type == Token.DEC) {
            int post1 = this.getIntProp(INCRDECR_PROP);
            int post2 = node.getIntProp(INCRDECR_PROP);
            if (post1 != post2) {
                return false;
            }
        } else if (type == Token.STRING) {
            int quoted1 = this.getIntProp(QUOTED_PROP);
            int quoted2 = node.getIntProp(QUOTED_PROP);
            if (quoted1 != quoted2) {
                return false;
            }
        }
        if (recurse) {
            Node n, n2;
            for (n = first, n2 = node.first; n != null; n = n.next, n2 = n2.next) {
                if (!n.isEquivalentTo(n2, compareJsType, true)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasSideEffects() {
        switch(type) {
            case Token.EXPR_VOID:
            case Token.COMMA:
                if (last != null)
                    return last.hasSideEffects();
                else
                    return true;
            case Token.HOOK:
                if (first == null || first.next == null || first.next.next == null) {
                    Kit.codeBug();
                }
                return first.next.hasSideEffects() && first.next.next.hasSideEffects();
            case Token.ERROR:
            case Token.EXPR_RESULT:
            case Token.ASSIGN:
            case Token.ASSIGN_ADD:
            case Token.ASSIGN_SUB:
            case Token.ASSIGN_MUL:
            case Token.ASSIGN_DIV:
            case Token.ASSIGN_MOD:
            case Token.ASSIGN_BITOR:
            case Token.ASSIGN_BITXOR:
            case Token.ASSIGN_BITAND:
            case Token.ASSIGN_LSH:
            case Token.ASSIGN_RSH:
            case Token.ASSIGN_URSH:
            case Token.ENTERWITH:
            case Token.LEAVEWITH:
            case Token.RETURN:
            case Token.GOTO:
            case Token.IFEQ:
            case Token.IFNE:
            case Token.NEW:
            case Token.DELPROP:
            case Token.SETNAME:
            case Token.SETPROP:
            case Token.SETELEM:
            case Token.CALL:
            case Token.THROW:
            case Token.RETHROW:
            case Token.SETVAR:
            case Token.CATCH_SCOPE:
            case Token.RETURN_RESULT:
            case Token.SET_REF:
            case Token.DEL_REF:
            case Token.REF_CALL:
            case Token.TRY:
            case Token.SEMI:
            case Token.INC:
            case Token.DEC:
            case Token.EXPORT:
            case Token.IMPORT:
            case Token.IF:
            case Token.ELSE:
            case Token.SWITCH:
            case Token.WHILE:
            case Token.DO:
            case Token.FOR:
            case Token.BREAK:
            case Token.CONTINUE:
            case Token.VAR:
            case Token.CONST:
            case Token.WITH:
            case Token.CATCH:
            case Token.FINALLY:
            case Token.BLOCK:
            case Token.LABEL:
            case Token.TARGET:
            case Token.LOOP:
            case Token.JSR:
            case Token.SETPROP_OP:
            case Token.SETELEM_OP:
            case Token.LOCAL_BLOCK:
            case Token.SET_REF_OP:
                return true;
            default:
                return false;
            }
    }

    public String getQualifiedName() {
        if (type == Token.NAME) {
            return getString();
        } else if (type == Token.GETPROP) {
            String left = getFirstChild().getQualifiedName();
            if (left == null) {
                return null;
            }
            return left + "." + getLastChild().getString();
        } else if (type == Token.THIS) {
            return "this";
        } else {
            return null;
        }
    }

    public boolean isQualifiedName() {
        switch(getType()) {
            case Token.NAME:
            case Token.THIS:
                return true;
            case Token.GETPROP:
                return getFirstChild().isQualifiedName();
            default:
                return false;
            }
    }

    public boolean isUnscopedQualifiedName() {
        switch(getType()) {
            case Token.NAME:
                return true;
            case Token.GETPROP:
                return getFirstChild().isUnscopedQualifiedName();
            default:
                return false;
            }
    }

    public Node detachFromParent() {
        Preconditions.checkState(parent != null);
        parent.removeChild(this);
        return this;
    }

    public Node removeFirstChild() {
        Node child = first;
        if (child != null) {
            removeChild(child);
        }
        return child;
    }

    public Node removeChildren() {
        Node children = first;
        for (Node child = first; child != null; child = child.getNext()) {
            child.parent = null;
        }
        first = null;
        last = null;
        return children;
    }

    public void detachChildren() {
        for (Node child = first; child != null; ) {
            Node nextChild = child.getNext();
            child.parent = null;
            child.next = null;
            child = nextChild;
        }
        first = null;
        last = null;
    }

    public Node removeChildAfter(Node prev) {
        Preconditions.checkArgument(prev.parent == this, "prev is not a child of this node.");
        Preconditions.checkArgument(prev.next != null, "no next sibling.");
        Node child = prev.next;
        prev.next = child.next;
        if (child == last)
            last = prev;
        child.next = null;
        child.parent = null;
        return child;
    }

    public Node cloneNode() {
        Node result;
        try {
            result = (Node) super.clone();
            // need to clone them here.
result.next = null;
            result.first = null;
            result.last = null;
            result.parent = null;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    public Node cloneTree() {
        Node result = cloneNode();
        for (Node n2 = getFirstChild(); n2 != null; n2 = n2.getNext()) {
            Node n2clone = n2.cloneTree();
            n2clone.parent = result;
            if (result.last != null) {
                result.last.next = n2clone;
            }
            if (result.first == null) {
                result.first = n2clone;
            }
            result.last = n2clone;
        }
        return result;
    }

    public Node copyInformationFrom(Node other) {
        if (getProp(ORIGINALNAME_PROP) == null) {
            putProp(ORIGINALNAME_PROP, other.getProp(ORIGINALNAME_PROP));
        }
        if (getProp(SOURCENAME_PROP) == null) {
            putProp(SOURCENAME_PROP, other.getProp(SOURCENAME_PROP));
            sourcePosition = other.sourcePosition;
        }
        return this;
    }

    public Node copyInformationFromForTree(Node other) {
        copyInformationFrom(other);
        for (Node child = getFirstChild(); child != null; child = child.getNext()) {
            child.copyInformationFromForTree(other);
        }
        return this;
    }

    public JSType getJSType() {
        return jsType;
    }

    public void setJSType(JSType jsType) {
        this.jsType = jsType;
    }

    public FileLevelJsDocBuilder getJsDocBuilderForNode() {
        return new FileLevelJsDocBuilder();
    }

    public class FileLevelJsDocBuilder {

        public void append(String fileLevelComment) {
            JSDocInfo jsDocInfo = getJSDocInfo();
            if (jsDocInfo == null) {
                // parse the JsDoc documentation from here?
jsDocInfo = new JSDocInfo(false);
            }
            String license = jsDocInfo.getLicense();
            if (license == null) {
                license = "";
            }
            jsDocInfo.setLicense(license + fileLevelComment);
            setJSDocInfo(jsDocInfo);
        }
    }

    public JSDocInfo getJSDocInfo() {
        return (JSDocInfo) getProp(JSDOC_INFO_PROP);
    }

    public void setJSDocInfo(JSDocInfo info) {
        putProp(JSDOC_INFO_PROP, info);
    }

    public void setVarArgs(boolean varArgs) {
        putBooleanProp(VAR_ARGS_NAME, varArgs);
    }

    public boolean isVarArgs() {
        return getBooleanProp(VAR_ARGS_NAME);
    }

    public void setOptionalArg(boolean optionalArg) {
        putBooleanProp(OPT_ARG_NAME, optionalArg);
    }

    public boolean isOptionalArg() {
        return getBooleanProp(OPT_ARG_NAME);
    }

    public void setIsSyntheticBlock(boolean val) {
        putBooleanProp(SYNTHETIC_BLOCK_PROP, val);
    }

    public boolean isSyntheticBlock() {
        return getBooleanProp(SYNTHETIC_BLOCK_PROP);
    }

    public void setDirectives(Set<String> val) {
        putProp(DIRECTIVES, val);
    }

    @SuppressWarnings("unchecked")
public Set<String> getDirectives() {
    return (Set<String>) getProp(DIRECTIVES);
}

    public void addSuppression(String warning) {
        if (getJSDocInfo() == null) {
            setJSDocInfo(new JSDocInfo(false));
        }
        getJSDocInfo().addSuppression(warning);
    }

    public void setWasEmptyNode(boolean val) {
        putBooleanProp(EMPTY_BLOCK, val);
    }

    public boolean wasEmptyNode() {
        return getBooleanProp(EMPTY_BLOCK);
    }

    final public static int FLAG_GLOBAL_STATE_UNMODIFIED = 1;

    final public static int FLAG_THIS_UNMODIFIED = 2;

    final public static int FLAG_ARGUMENTS_UNMODIFIED = 4;

    final public static int FLAG_NO_THROWS = 8;

    final public static int FLAG_LOCAL_RESULTS = 16;

    final public static int SIDE_EFFECTS_FLAGS_MASK = 31;

    final public static int SIDE_EFFECTS_ALL = 0;

    final public static int NO_SIDE_EFFECTS = FLAG_GLOBAL_STATE_UNMODIFIED | FLAG_THIS_UNMODIFIED | FLAG_ARGUMENTS_UNMODIFIED | FLAG_NO_THROWS;

    public void setSideEffectFlags(int flags) {
        Preconditions.checkArgument(getType() == Token.CALL || getType() == Token.NEW, "setIsNoSideEffectsCall only supports CALL and NEW nodes, got " + Token.name(getType()));
        putIntProp(SIDE_EFFECT_FLAGS, flags);
    }

    public void setSideEffectFlags(SideEffectFlags flags) {
        setSideEffectFlags(flags.valueOf());
    }

    public int getSideEffectFlags() {
        return getIntProp(SIDE_EFFECT_FLAGS);
    }

    public static class SideEffectFlags {

        private int value = Node.SIDE_EFFECTS_ALL;

        public SideEffectFlags() {
        }

        public SideEffectFlags(int value) {
            this.value = value;
        }

        public int valueOf() {
            return value;
        }

        public void setAllFlags() {
            value = Node.SIDE_EFFECTS_ALL;
        }

        public void clearAllFlags() {
            value = Node.NO_SIDE_EFFECTS | Node.FLAG_LOCAL_RESULTS;
        }

        public boolean areAllFlagsSet() {
            return value == Node.SIDE_EFFECTS_ALL;
        }

        public void clearSideEffectFlags() {
            value |= Node.NO_SIDE_EFFECTS;
        }

        public void setMutatesGlobalState() {
            // Modify global means everything must be assumed to be modified.
removeFlag(Node.FLAG_GLOBAL_STATE_UNMODIFIED);
            removeFlag(Node.FLAG_ARGUMENTS_UNMODIFIED);
            removeFlag(Node.FLAG_THIS_UNMODIFIED);
        }

        public void setThrows() {
            removeFlag(Node.FLAG_NO_THROWS);
        }

        public void setMutatesThis() {
            removeFlag(Node.FLAG_THIS_UNMODIFIED);
        }

        public void setMutatesArguments() {
            removeFlag(Node.FLAG_ARGUMENTS_UNMODIFIED);
        }

        public void setReturnsTainted() {
            removeFlag(Node.FLAG_LOCAL_RESULTS);
        }

        private void removeFlag(int flag) {
            value &= ~flag;
        }
    }

    public boolean isOnlyModifiesThisCall() {
        return areBitFlagsSet(getSideEffectFlags() & Node.NO_SIDE_EFFECTS, Node.FLAG_GLOBAL_STATE_UNMODIFIED | Node.FLAG_ARGUMENTS_UNMODIFIED | Node.FLAG_NO_THROWS);
    }

    public boolean isNoSideEffectsCall() {
        return areBitFlagsSet(getSideEffectFlags(), NO_SIDE_EFFECTS);
    }

    public boolean isLocalResultCall() {
        return areBitFlagsSet(getSideEffectFlags(), FLAG_LOCAL_RESULTS);
    }

    private boolean areBitFlagsSet(int value, int flags) {
        return (value & flags) == flags;
    }

    public boolean isQuotedString() {
        return false;
    }

    public void setQuotedString() {
        Kit.codeBug();
    }

    static class NodeMismatch {

        final Node nodeA;

        final Node nodeB;

        NodeMismatch(Node nodeA, Node nodeB) {
            this.nodeA = nodeA;
            this.nodeB = nodeB;
        }

        @Override
public boolean equals(Object object) {
    if (object instanceof NodeMismatch) {
        NodeMismatch that = (NodeMismatch) object;
        return that.nodeA.equals(this.nodeA) && that.nodeB.equals(this.nodeB);
    }
    return false;
}

        @Override
public int hashCode() {
    return Objects.hashCode(nodeA, nodeB);
}
    }
}
