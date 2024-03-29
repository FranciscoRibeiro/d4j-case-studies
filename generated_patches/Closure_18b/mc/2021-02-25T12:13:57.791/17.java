package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.deps.DependencyInfo;
import com.google.javascript.jscomp.deps.JsFileParser;
import com.google.javascript.rhino.InputId;
import com.google.javascript.rhino.Node;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CompilerInput implements SourceAst, DependencyInfo {

    private static final long serialVersionUID = 1L;

    private JSModule module;

    final private InputId id;

    private final SourceAst ast;

    private final Set<String> provides = Sets.newHashSet();

    private final Set<String> requires = Sets.newHashSet();

    private boolean generatedDependencyInfoFromSource = false;

    private transient AbstractCompiler compiler;

    public CompilerInput(SourceAst ast) {
        this(ast, ast.getSourceFile().getName(), false);
    }

    public CompilerInput(SourceAst ast, boolean isExtern) {
        this(ast, ast.getInputId(), isExtern);
    }

    public CompilerInput(SourceAst ast, String inputId, boolean isExtern) {
        this(ast, new InputId(inputId), isExtern);
    }

    public CompilerInput(SourceAst ast, InputId inputId, boolean isExtern) {
        this.ast = ast;
        this.id = inputId;
        if (ast != null && ast.getSourceFile() != null) {
            ast.getSourceFile().setIsExtern(isExtern);
        }
    }

    public CompilerInput(SourceFile file) {
        this(file, false);
    }

    public CompilerInput(SourceFile file, boolean isExtern) {
        this(new JsAst(file), isExtern);
    }

    @Override
public InputId getInputId() {
    return id;
}

    @Override
public String getName() {
    return id.getIdName();
}

    public SourceAst getAst() {
        return ast;
    }

    @Override
public String getPathRelativeToClosureBase() {
    throw new UnsupportedOperationException();
}

    @Override
public Node getAstRoot(AbstractCompiler compiler) {
    Node root = ast.getAstRoot(compiler);
    if (root != null) {
        Preconditions.checkState(root.isScript());
        Preconditions.checkNotNull(root.getInputId());
    }
    return root;
}

    @Override
public void clearAst() {
    ast.clearAst();
}

    @Override
public SourceFile getSourceFile() {
    return ast.getSourceFile();
}

    @Override
public void setSourceFile(SourceFile file) {
    ast.setSourceFile(file);
}

    public SourceAst getSourceAst() {
        return ast;
    }

    public void setCompiler(AbstractCompiler compiler) {
        this.compiler = compiler;
    }

    private void checkErrorManager() {
        Preconditions.checkNotNull(compiler, "Expected setCompiler to be called first: " + this);
        Preconditions.checkNotNull(compiler.getErrorManager(), "Expected compiler to call an error manager: " + this);
    }

    @Override
public Collection<String> getRequires() {
    checkErrorManager();
    try {
        regenerateDependencyInfoIfNecessary();
        return Collections.<String>unmodifiableSet(AbstractCompiler);
    } catch (IOException e) {
        compiler.getErrorManager().report(CheckLevel.ERROR, JSError.make(AbstractCompiler.READ_ERROR, getName()));
        return ImmutableList.<String>of();
    }
}

    @Override
public Collection<String> getProvides() {
    checkErrorManager();
    try {
        regenerateDependencyInfoIfNecessary();
        return Collections.<String>unmodifiableSet(provides);
    } catch (IOException e) {
        compiler.getErrorManager().report(CheckLevel.ERROR, JSError.make(AbstractCompiler.READ_ERROR, getName()));
        return ImmutableList.<String>of();
    }
}

    void addProvide(String provide) {
        getProvides();
        provides.add(provide);
    }

    void addRequire(String require) {
        getRequires();
        requires.add(require);
    }

    public void removeRequire(String require) {
        getRequires();
        requires.remove(require);
    }

    private void regenerateDependencyInfoIfNecessary() throws IOException {
        if (!(ast instanceof JsAst)) {
            Preconditions.checkNotNull(compiler, "Expected setCompiler to be called first");
            DepsFinder finder = new DepsFinder();
            Node root = getAstRoot(compiler);
            if (root == null) {
                return;
            }
            finder.visitTree(getAstRoot(compiler));
            provides.addAll(finder.provides);
            requires.addAll(finder.requires);
        } else {
            if (!generatedDependencyInfoFromSource) {
                // symbol dependencies.)
DependencyInfo info = (new JsFileParser(compiler.getErrorManager())).setIncludeGoogBase(true).parseFile(getName(), getName(), getCode());
                provides.addAll(info.getProvides());
                requires.addAll(info.getRequires());
                generatedDependencyInfoFromSource = true;
            }
        }
    }

    private static class DepsFinder {

        private final List<String> provides = Lists.newArrayList();

        private final List<String> requires = Lists.newArrayList();

        private final CodingConvention codingConvention = new ClosureCodingConvention();

        void visitTree(Node n) {
            visitSubtree(n, null);
        }

        void visitSubtree(Node n, Node parent) {
            if (n.isCall()) {
                String require = codingConvention.extractClassNameIfRequire(n, parent);
                if (require != null) {
                    requires.add(require);
                }
                String provide = codingConvention.extractClassNameIfProvide(n, parent);
                if (provide != null) {
                    provides.add(provide);
                }
                return;
            } else if (parent != null && !parent.isExprResult() && !parent.isScript()) {
                return;
            }
            for (Node child = n.getFirstChild(); child != null; child = child.getNext()) {
                visitSubtree(child, n);
            }
        }
    }

    public String getLine(int lineNumber) {
        return getSourceFile().getLine(lineNumber);
    }

    public Region getRegion(int lineNumber) {
        return getSourceFile().getRegion(lineNumber);
    }

    public String getCode() throws IOException {
        return getSourceFile().getCode();
    }

    public JSModule getModule() {
        return module;
    }

    public void setModule(JSModule module) {
        // An input may only belong to one module.
Preconditions.checkArgument(module == null || this.module == null || this.module == module);
        this.module = module;
    }

    void overrideModule(JSModule module) {
        this.module = module;
    }

    public boolean isExtern() {
        if (ast == null || ast.getSourceFile() == null) {
            return false;
        }
        return ast.getSourceFile().isExtern();
    }

    void setIsExtern(boolean isExtern) {
        if (ast == null || ast.getSourceFile() == null) {
            return;
        }
        ast.getSourceFile().setIsExtern(isExtern);
    }

    public int getLineOffset(int lineno) {
        return ast.getSourceFile().getLineOffset(lineno);
    }

    public int getNumLines() {
        return ast.getSourceFile().getNumLines();
    }

    @Override
public String toString() {
    return getName();
}
}
