/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.tvl.goworks.editor.go.semantics;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.netbeans.editor.text.VersionedDocument;
import org.antlr.netbeans.semantics.ObjectDecorator;
import org.antlr.netbeans.semantics.ObjectProperty;
import org.antlr.v4.runtime.Dependents;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleDependencies;
import org.antlr.v4.runtime.RuleDependency;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.works.editor.antlr4.parsing.ParseTrees;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Parameters;
import org.tvl.goworks.editor.go.codemodel.ChannelKind;
import org.tvl.goworks.editor.go.codemodel.CodeElementModel;
import org.tvl.goworks.editor.go.codemodel.ConstModel;
import org.tvl.goworks.editor.go.codemodel.FunctionModel;
import org.tvl.goworks.editor.go.codemodel.IntrinsicTypeModels;
import org.tvl.goworks.editor.go.codemodel.PackageModel;
import org.tvl.goworks.editor.go.codemodel.TypeKind;
import org.tvl.goworks.editor.go.codemodel.TypeModel;
import org.tvl.goworks.editor.go.codemodel.VarKind;
import org.tvl.goworks.editor.go.codemodel.VarModel;
import org.tvl.goworks.editor.go.codemodel.impl.CodeModelCacheImpl;
import org.tvl.goworks.editor.go.codemodel.impl.TypeArrayModelImpl;
import org.tvl.goworks.editor.go.codemodel.impl.TypeChannelModelImpl;
import org.tvl.goworks.editor.go.codemodel.impl.TypeMapModelImpl;
import org.tvl.goworks.editor.go.codemodel.impl.TypeModelImpl;
import org.tvl.goworks.editor.go.codemodel.impl.TypePointerModelImpl;
import org.tvl.goworks.editor.go.codemodel.impl.TypeSliceModelImpl;
import org.tvl.goworks.editor.go.highlighter.SemanticHighlighter;
import org.tvl.goworks.editor.go.parser.GoParser;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.AddAssignOpContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.AddExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.AndExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.AnonymousFieldContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ArgumentListContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ArrayLengthContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ArrayTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.AssignOpContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.AssignmentContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BaseTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BaseTypeNameContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BasicLiteralContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BlockContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BodyContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BreakStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BuiltinArgsContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BuiltinCallContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.BuiltinCallExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.CallExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ChannelContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ChannelTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.CommCaseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.CommClauseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.CompareExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.CompositeLiteralContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ConditionContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ConstDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ConstSpecContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ContinueStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ConversionContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ConversionOrCallExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.DeclarationContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.DeferStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ElementContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ElementListContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ElementNameOrIndexContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ElementTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.EmptyStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ExprCaseClauseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ExprSwitchCaseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ExprSwitchStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ExpressionContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ExpressionListContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ExpressionStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.FallthroughStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.FieldDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ForClauseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ForStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.FunctionDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.FunctionLiteralContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.FunctionTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.GoStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.GotoStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.IdentifierListContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.IfStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ImportDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ImportPathContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ImportSpecContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.IncDecStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.IndexExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.InitStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.InterfaceTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.InterfaceTypeNameContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.KeyContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.KeyTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.LabelContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.LabeledStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.LiteralContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.LiteralTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.LiteralValueContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.MapTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.MethodDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.MethodExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.MethodNameContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.MethodSpecContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.MulAssignOpContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.MultExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.OperandContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.OperandExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.OrExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.PackageClauseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.PackageNameContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ParameterDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ParameterListContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ParametersContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.PointerTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.PostStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.QualifiedIdentifierContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.RangeClauseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ReceiverContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ReceiverTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.RecvExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.RecvStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ResultContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ReturnStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SelectStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SelectorExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SendStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ShortVarDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SignatureContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SimpleStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SliceExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SliceTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SourceFileBodyContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SourceFileContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.StatementContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.StructTypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.SwitchStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TagContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TopLevelDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeAssertionExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeCaseClauseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeListContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeLiteralContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeNameContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeSpecContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeSwitchCaseContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeSwitchGuardContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.TypeSwitchStmtContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.UnaryExprContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.ValueContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.VarDeclContext;
import org.tvl.goworks.editor.go.parser.generated.AbstractGoParser.VarSpecContext;
import org.tvl.goworks.editor.go.parser.generated.GoParserBaseListener;
import org.tvl.goworks.editor.go.parser.generated.GoParserListener;
import org.tvl.goworks.project.GoProject;

/* TODO for QUALIFIED_EXPR and UNQUALIFIED_LINK
 *   +qualifiedIdentifier
 *   +typeName
 *   operand
 *   type
 *   literalType
 *   interfaceTypeName?
 *   receiverType?
 *   -anonymousField?
 */

/*
 * CODE_TYPE should be valid for the following types
 */

/**
 * Summary of required attributes.
 *
 * Need to specify {@link GoAnnotations#VAR_TYPE} for the following:
 *   - varSpec.idList
 *   - shortVarDecl.idList
 *   - parameterDecl.idList
 *
 * Need to specify {@link GoAnnotations#GLOBAL} for the following:
 *   - constSpec.idList
 *
 * Need to specify {@link GoAnnotations#BUILTIN} for the following:
 *   - function, var, and const references
 *
 * @author Sam Harwell
 */
public class SemanticAnalyzerListener implements GoParserListener {
    private static final Logger LOGGER = Logger.getLogger(SemanticAnalyzerListener.class.getName());

    @NonNull
    private final VersionedDocument document;
    @NonNull
    private final GoAnnotatedParseTree annotatedParseTree;
    @NonNull
    private final ObjectDecorator<Tree> treeDecorator;

    private final Deque<Map<String, TerminalNode>> visibleLocals = new ArrayDeque<>();
    private final Deque<Map<String, TerminalNode>> visibleConstants = new ArrayDeque<>();
    private final Deque<Map<String, TerminalNode>> visibleFunctions = new ArrayDeque<>();
    private final Deque<Map<String, TerminalNode>> visibleTypes = new ArrayDeque<>();

    private final Deque<Map<String, TerminalNode>> pendingVisibleLocals = new ArrayDeque<>();
    private final Deque<Map<String, TerminalNode>> pendingVisibleConstants = new ArrayDeque<>();

    private final List<TerminalNode> unresolvedIdentifiers = new ArrayList<>();
    private final List<TerminalNode> unresolvedQualifiedIdentifiers = new ArrayList<>();

    // label references are resolved at the end of a function
    private final Deque<Map<String, TerminalNode>> visibleLabels = new ArrayDeque<>();
    private final Deque<Collection<TerminalNode>> unresolvedLabels = new ArrayDeque<>();

    private final Map<String, List<TerminalNode>> importedPackages = new HashMap<>();

    private BigInteger _iota = BigInteger.ZERO;

    public SemanticAnalyzerListener(@NonNull VersionedDocument document, @NonNull GoAnnotatedParseTree annotatedParseTree) {
        Parameters.notNull("document", document);
        Parameters.notNull("annotatedParseTree", annotatedParseTree);
        this.document = document;
        this.annotatedParseTree = annotatedParseTree;
        this.treeDecorator = annotatedParseTree.getTreeDecorator();
        pushVarScope();
    }

    public void resolveReferences() {
        CodeModelCacheImpl codeModelCache = CodeModelCacheImpl.getInstance();
        Project project = FileOwnerQuery.getOwner(document.getFileObject());
        if (!(project instanceof GoProject)) {
            throw new UnsupportedOperationException("Unsupported project type.");
        }

        String currentPackagePath = getCurrentPackagePath((GoProject)project, document);
        PackageModel currentPackage = codeModelCache.getUniquePackage((GoProject)project, currentPackagePath);

        Map<String, Collection<PackageModel>> resolvedPackages = new HashMap<>();
        for (Map.Entry<String, List<TerminalNode>> entry : importedPackages.entrySet()) {
            Collection<PackageModel> packages = resolvedPackages.get(entry.getKey());
            if (packages == null) {
                packages = new ArrayList<>();
                resolvedPackages.put(entry.getKey(), packages);
            }

            for (TerminalNode importToken : entry.getValue()) {
                Collection<? extends CodeElementModel> resolved = treeDecorator.getProperty(importToken, GoAnnotations.MODELS);
                for (CodeElementModel model : resolved) {
                    if (!(model instanceof PackageModel)) {
                        continue;
                    }

                    packages.add((PackageModel)model);
                }
            }
        }

        for (TerminalNode node : unresolvedIdentifiers) {
            resolveIdentifier(node, currentPackage, resolvedPackages);
        }

        boolean updatedResolution;
        do {
            updatedResolution = false;
            for (TerminalNode node : unresolvedQualifiedIdentifiers) {
                try {
                    updatedResolution |= resolveQualifiedIdentifier(node, currentPackage, resolvedPackages);
                } catch (RuntimeException | Error ex) {
                    LOGGER.log(Level.FINE, String.format("An exception occurred while resolving a qualified identifier '%s'", node.getSymbol()), ex);
                }
            }
        } while (updatedResolution);
    }

    private static String getCurrentPackagePath(GoProject project, VersionedDocument document) {
        FileObject documentFileObject = document.getFileObject();
        FileObject packageFolder = documentFileObject != null ? documentFileObject.getParent() : null;
        FileObject sourceRoot = project != null ? project.getSourceRoot() : null;

        String packagePath;
        if (sourceRoot != null) {
            packagePath = FileUtil.getRelativePath(sourceRoot, packageFolder);
        } else {
            packagePath = packageFolder.getNameExt();
        }

        return packagePath;
    }

    private void resolveIdentifier(TerminalNode node,
                                   PackageModel currentPackage,
                                   Map<String, ? extends Collection<? extends PackageModel>> importedPackages) {
        // check again for a top-level definition in the current file
        Token token = node.getSymbol();
        TerminalNode target = getVisibleDeclaration(node);
        if (target != null) {
            boolean resolved = true;
            switch (treeDecorator.getProperty(target, GoAnnotations.NODE_TYPE)) {
            case CONST_DECL:
                treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                break;

            case VAR_DECL:
                treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, NodeType.VAR_REF);
                break;

            case FUNC_DECL:
                treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, NodeType.FUNC_REF);
                break;

            case TYPE_DECL:
                treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, NodeType.TYPE_REF);
                break;

            default:
                resolved = false;
                break;
            }

            treeDecorator.putProperty(node, GoAnnotations.LOCAL_TARGET, target);
            if (resolved) {
                treeDecorator.putProperty(node, GoAnnotations.RESOLVED, true);
            }

            VarKind varType = treeDecorator.getProperty(target, GoAnnotations.VAR_TYPE);
            if (varType != VarKind.UNDEFINED) {
                treeDecorator.putProperty(node, GoAnnotations.VAR_TYPE, varType);
            }

            TypeKind typeKind = treeDecorator.getProperty(target, GoAnnotations.TYPE_KIND);
            if (typeKind != TypeKind.UNDEFINED) {
                treeDecorator.putProperty(node, GoAnnotations.TYPE_KIND, typeKind);
            }

            if (treeDecorator.getProperty(target, GoAnnotations.GLOBAL)) {
                treeDecorator.putProperty(node, GoAnnotations.GLOBAL, true);
            }

            return;
        }

        // try to resolve the element in the current package
        if (currentPackage != null) {
            if (resolveIdentifier(node, currentPackage)) {
                return;
            }
        }

        // check packages with alias '.'
        Collection<? extends PackageModel> mergedPackages = importedPackages.get("");
        if (mergedPackages != null) {
            for (PackageModel model : mergedPackages) {
                if (resolveIdentifier(node, model)) {
                    return;
                }
            }
        }
    }

    private boolean resolveIdentifier(TerminalNode node, PackageModel packageModel) {
        Token token = node.getSymbol();
        Collection<? extends CodeElementModel> members = packageModel.getMembers(token.getText());
        for (CodeElementModel model : members) {
            NodeType nodeType = NodeType.UNDEFINED;
            VarKind varType = VarKind.UNDEFINED;
            TypeKind typeKind = TypeKind.UNDEFINED;
            boolean global = true;
            boolean resolved = true;
            if (model instanceof ConstModel) {
                nodeType = NodeType.CONST_REF;
            } else if (model instanceof VarModel) {
                nodeType = NodeType.VAR_REF;
                varType = VarKind.GLOBAL;
            } else if (model instanceof TypeModel) {
                nodeType = NodeType.TYPE_REF;
                typeKind = ((TypeModel)model).getKind();
            } else if (model instanceof FunctionModel) {
                nodeType = NodeType.FUNC_REF;
            } else {
                resolved = false;
            }

            if (nodeType != NodeType.UNDEFINED) {
                treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, nodeType);
            }

            if (resolved) {
                treeDecorator.putProperty(node, GoAnnotations.RESOLVED, true);
            }

            if (varType != VarKind.UNDEFINED) {
                treeDecorator.putProperty(node, GoAnnotations.VAR_TYPE, varType);
            }

            if (typeKind != TypeKind.UNDEFINED) {
                treeDecorator.putProperty(node, GoAnnotations.TYPE_KIND, typeKind);
            }

            treeDecorator.putProperty(node, GoAnnotations.GLOBAL, true);
            treeDecorator.putProperty(node, GoAnnotations.MODELS, members);
            return true;
        }

        return false;
    }

    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageName, version=0)
    private boolean resolveQualifiedIdentifier(TerminalNode node, PackageModel currentPackage, Map<String, Collection<PackageModel>> resolvedPackages) {
        Token token = node.getSymbol();
        if (treeDecorator.getProperty(node, GoAnnotations.NODE_TYPE) != NodeType.UNDEFINED) {
            // already resolved
            return false;
        }

        ParserRuleContext qualifier = treeDecorator.getProperty(node, GoAnnotations.QUALIFIER);
        if (qualifier == null) {
            // don't have the information necessary to resolve
            return false;
        }

        Collection<? extends CodeElementModel> resolvedQualifier = treeDecorator.getProperty(qualifier, GoAnnotations.MODELS);
        if (resolvedQualifier == null) {
            CodeElementReference qualifierCodeClass = treeDecorator.getProperty(qualifier, GoAnnotations.CODE_CLASS);
            assert qualifierCodeClass != null;
            if (qualifierCodeClass != CodeElementReference.MISSING) {
                resolvedQualifier = qualifierCodeClass.resolve(annotatedParseTree, currentPackage, resolvedPackages);
            }
        }

        if (resolvedQualifier == null) {
            CodeElementReference qualifierExprType = treeDecorator.getProperty(qualifier, GoAnnotations.EXPR_TYPE);
            assert qualifierExprType != null;
            if (qualifierExprType != CodeElementReference.MISSING) {
                resolvedQualifier = qualifierExprType.resolve(annotatedParseTree, currentPackage, resolvedPackages);
            }
        }

        if (resolvedQualifier == null) {
            NodeType qualifierNodeType = treeDecorator.getProperty(qualifier, GoAnnotations.NODE_TYPE);
            if (qualifierNodeType == NodeType.UNDEFINED) {
                if (treeDecorator.getProperty(qualifier, GoAnnotations.QUALIFIED_EXPR)) {
                    // haven't resolved the qualifier, which is itself a qualified expression
                    return false;
                }

                TerminalNode unqualifiedLink = treeDecorator.getProperty(qualifier, GoAnnotations.UNQUALIFIED_LINK);
                if (unqualifiedLink != null) {
                    Map<? extends ObjectProperty<?>, ?> properties = treeDecorator.getProperties(unqualifiedLink);
                    treeDecorator.putProperties(qualifier, properties);
                    qualifierNodeType = treeDecorator.getProperty(qualifier, GoAnnotations.NODE_TYPE);
                    if (qualifierNodeType == NodeType.UNDEFINED) {
                        treeDecorator.putProperty(qualifier, GoAnnotations.NODE_TYPE, NodeType.UNKNOWN);
                        qualifierNodeType = NodeType.UNKNOWN;
                    }
                } else {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, "Unable to resolve unqualified link from qualifier: {0}", qualifier.toString(Arrays.asList(GoParser.ruleNames)));
                    }

                    qualifierNodeType = NodeType.UNKNOWN;
                }
            }

            assert qualifierNodeType != NodeType.UNDEFINED;

            if (qualifierNodeType == NodeType.UNKNOWN) {
                // can't resolve a dereference if the qualifier couldn't be resolved
                treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, NodeType.UNKNOWN);
                return true;
            }

            if (qualifierNodeType == NodeType.TYPE_LITERAL) {
                return resolveQualifierType(qualifier, currentPackage, resolvedPackages);
            } else if (qualifierNodeType == NodeType.PACKAGE_REF) {
                assert qualifier instanceof PackageNameContext;
                String packageName = ((PackageNameContext)qualifier).IDENTIFIER().getSymbol().getText();
                resolvedQualifier = resolvedPackages.get(packageName);
                if (resolvedQualifier == null) {
                    resolvedQualifier = Collections.emptyList();
                }
            } else if (qualifierNodeType == NodeType.VAR_REF) {
                // must be referring to something within the current file since it's resolved internally
                TerminalNode target = treeDecorator.getProperty(qualifier, GoAnnotations.LOCAL_TARGET);
                assert target != null && treeDecorator.getProperty(target, GoAnnotations.NODE_TYPE) == NodeType.VAR_DECL;
                ParserRuleContext explicitType = treeDecorator.getProperty(qualifier, GoAnnotations.EXPLICIT_TYPE);
                if (explicitType == null && target != null) {
                    explicitType = treeDecorator.getProperty(target, GoAnnotations.EXPLICIT_TYPE);
                }

                if (explicitType != null) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, "Unable to resolve explicit type for qualifier: {0}", qualifier.toString(Arrays.asList(GoParser.ruleNames)));
                    }

                    resolvedQualifier = Collections.emptyList();
                } else {
                    ParserRuleContext implicitType = target != null ? treeDecorator.getProperty(target, GoAnnotations.IMPLICIT_TYPE) : null;
                    int implicitIndex = target != null ? treeDecorator.getProperty(target, GoAnnotations.IMPLICIT_INDEX) : -1;
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, "Unable to resolve implicit type for qualifier: {0}", qualifier.toString(Arrays.asList(GoParser.ruleNames)));
                    }

                    resolvedQualifier = Collections.emptyList();
                }
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Unable to resolve qualifier: {0}", qualifier.toString(Arrays.asList(GoParser.ruleNames)));
                }

                resolvedQualifier = Collections.emptyList();
            }
        }

        assert resolvedQualifier != null;
        if (resolvedQualifier == null) {
            LOGGER.log(Level.WARNING, "Should not have a null resolved qualifier at this point.");
            resolvedQualifier = Collections.emptyList();
        }

        String nameText = token.getText();
        List<CodeElementModel> qualifiedModels = new ArrayList<>();
        for (CodeElementModel model : resolvedQualifier) {
            qualifiedModels.addAll(SemanticAnalyzer.getSelectableMembers(model, nameText));
        }

        if (qualifiedModels.isEmpty()) {
            treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, NodeType.UNKNOWN);
            return true;
        }

        setNodeType(node, qualifiedModels.get(0));
        treeDecorator.putProperty(node, GoAnnotations.RESOLVED, true);
        treeDecorator.putProperty(node, GoAnnotations.MODELS, qualifiedModels);
        return true;
    }

    private void setNodeType(TerminalNode node, CodeElementModel model) {
        NodeType nodeType;
        VarKind varType = VarKind.UNDEFINED;
        TypeKind typeKind = TypeKind.UNDEFINED;

        if (model instanceof PackageModel) {
            nodeType = NodeType.PACKAGE_REF;
        } else if (model instanceof TypeModel) {
            nodeType = NodeType.TYPE_REF;
            typeKind = ((TypeModel)model).getKind();
        } else if (model instanceof VarModel) {
            nodeType = NodeType.VAR_REF;
            varType = ((VarModel)model).getVarKind();
        } else if (model instanceof ConstModel) {
            nodeType = NodeType.CONST_REF;
        } else if (model instanceof FunctionModel) {
            nodeType = ((FunctionModel)model).isMethod() ? NodeType.METHOD_REF : NodeType.FUNC_REF;
        } else {
            LOGGER.log(Level.WARNING, "Could not get a NodeType from model type: {0}", model.getClass());
            nodeType = NodeType.UNKNOWN;
        }

        treeDecorator.putProperty(node, GoAnnotations.NODE_TYPE, nodeType);
        if (typeKind != TypeKind.UNDEFINED) {
            treeDecorator.putProperty(node, GoAnnotations.TYPE_KIND, typeKind);
        }
        if (varType != VarKind.UNDEFINED) {
            treeDecorator.putProperty(node, GoAnnotations.VAR_TYPE, varType);
        }
    }

    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayType, version=0)
    private boolean resolveQualifierType(ParserRuleContext qualifier,
                                         PackageModel currentPackage,
                                         Map<String, Collection<PackageModel>> resolvedPackages) {

        if (qualifier instanceof ArrayTypeContext) {
            ArrayTypeContext ctx = (ArrayTypeContext)qualifier;
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class QualifierTypeResolver extends GoParserBaseListener {

        public boolean resolveType(ParserRuleContext qualifier) {
            ParseTreeWalker.DEFAULT.walk(this, qualifier);
            return treeDecorator.getProperty(qualifier, GoAnnotations.MODELS) != null;
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public void exitArrayType(ArrayTypeContext ctx) {
            Collection<? extends CodeElementModel> elementTypes = treeDecorator.getProperty(ctx.elementType(), GoAnnotations.MODELS);
            if (elementTypes != null) {
                if (elementTypes.isEmpty()) {
                    treeDecorator.putProperty(ctx, GoAnnotations.MODELS, Collections.<CodeElementModel>emptyList());
                    return;
                }

                List<TypeModelImpl> arrayTypes = new ArrayList<>();
                for (CodeElementModel model : elementTypes) {
                    if (!(model instanceof TypeModelImpl)) {
                        continue;
                    }

                    TypeModelImpl elementType = (TypeModelImpl)model;
                    TypeModelImpl arrayType = new TypeArrayModelImpl(elementType);
                    arrayTypes.add(arrayType);
                }

                treeDecorator.putProperty(ctx, GoAnnotations.MODELS, arrayTypes);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_pointerType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseType, version=0),
        })
        public void exitPointerType(PointerTypeContext ctx) {
            Collection<? extends CodeElementModel> elementTypes = treeDecorator.getProperty(ctx.baseType(), GoAnnotations.MODELS);
            if (elementTypes != null) {
                if (elementTypes.isEmpty()) {
                    treeDecorator.putProperty(ctx, GoAnnotations.MODELS, Collections.<CodeElementModel>emptyList());
                    return;
                }

                List<TypeModelImpl> pointerTypes = new ArrayList<>();
                for (CodeElementModel model : elementTypes) {
                    if (!(model instanceof TypeModelImpl)) {
                        continue;
                    }

                    TypeModelImpl elementType = (TypeModelImpl)model;
                    TypeModelImpl pointerType = new TypePointerModelImpl(elementType);
                    pointerTypes.add(pointerType);
                }

                treeDecorator.putProperty(ctx, GoAnnotations.MODELS, pointerTypes);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sliceType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public void exitSliceType(SliceTypeContext ctx) {
            Collection<? extends CodeElementModel> elementTypes = treeDecorator.getProperty(ctx.elementType(), GoAnnotations.MODELS);
            if (elementTypes != null) {
                if (elementTypes.isEmpty()) {
                    treeDecorator.putProperty(ctx, GoAnnotations.MODELS, Collections.<CodeElementModel>emptyList());
                    return;
                }

                List<TypeModelImpl> sliceTypes = new ArrayList<>();
                for (CodeElementModel model : elementTypes) {
                    if (!(model instanceof TypeModelImpl)) {
                        continue;
                    }

                    TypeModelImpl elementType = (TypeModelImpl)model;
                    TypeModelImpl sliceType = new TypeSliceModelImpl(elementType);
                    sliceTypes.add(sliceType);
                }

                treeDecorator.putProperty(ctx, GoAnnotations.MODELS, sliceTypes);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mapType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_keyType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public void exitMapType(MapTypeContext ctx) {
            Collection<? extends CodeElementModel> keyTypes = treeDecorator.getProperty(ctx.keyType(), GoAnnotations.MODELS);
            Collection<? extends CodeElementModel> elementTypes = treeDecorator.getProperty(ctx.elementType(), GoAnnotations.MODELS);
            if (keyTypes != null && elementTypes != null) {
                if (keyTypes == null || elementTypes.isEmpty()) {
                    treeDecorator.putProperty(ctx, GoAnnotations.MODELS, Collections.<CodeElementModel>emptyList());
                    return;
                }

                List<TypeModelImpl> mapTypes = new ArrayList<>();
                for (CodeElementModel keyModel : keyTypes) {
                    if (!(keyModel instanceof TypeModelImpl)) {
                        continue;
                    }

                    for (CodeElementModel model : elementTypes) {
                        if (!(model instanceof TypeModelImpl)) {
                            continue;
                        }

                        TypeModelImpl keyType = (TypeModelImpl)keyModel;
                        TypeModelImpl elementType = (TypeModelImpl)model;
                        TypeModelImpl mapType = new TypeMapModelImpl(keyType, elementType);
                        mapTypes.add(mapType);
                    }
                }

                treeDecorator.putProperty(ctx, GoAnnotations.MODELS, mapTypes);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channelType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public void exitChannelType(ChannelTypeContext ctx) {
            Collection<? extends CodeElementModel> elementTypes = treeDecorator.getProperty(ctx.elementType(), GoAnnotations.MODELS);
            if (elementTypes != null) {
                if (elementTypes.isEmpty()) {
                    treeDecorator.putProperty(ctx, GoAnnotations.MODELS, Collections.<CodeElementModel>emptyList());
                    return;
                }

                ChannelKind channelKind = ChannelKind.SendReceive;
                if (ctx.send != null) {
                    channelKind = ChannelKind.Send;
                } else if (ctx.recv != null) {
                    channelKind = ChannelKind.Receive;
                }

                List<TypeModelImpl> channelTypes = new ArrayList<>();
                for (CodeElementModel model : elementTypes) {
                    if (!(model instanceof TypeModelImpl)) {
                        continue;
                    }

                    TypeModelImpl elementType = (TypeModelImpl)model;
                    TypeModelImpl channelType = new TypeChannelModelImpl(elementType, channelKind);
                    channelTypes.add(channelType);
                }

                treeDecorator.putProperty(ctx, GoAnnotations.MODELS, channelTypes);
            }
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterMultExpr(MultExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitMultExpr(MultExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.MISSING;
        if (ctx.expression(0) != null && ctx.op != null && ctx.expression(1) != null) {
            CodeElementReference left = treeDecorator.getProperty(ctx.expression(0), GoAnnotations.EXPR_TYPE);
            CodeElementReference right = treeDecorator.getProperty(ctx.expression(1), GoAnnotations.EXPR_TYPE);
            exprType = new BinaryExpressionTypeReference(left, ctx.op, right);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channelType, version=0)
    public void enterChannelType(ChannelTypeContext ctx) {
       
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channelType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
    })
    public void exitChannelType(ChannelTypeContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.elementType() != null) {
            ChannelKind kind = ChannelKind.SendReceive;
            if (ctx.send != null) {
                kind = ChannelKind.Send;
            } else if (ctx.recv != null) {
                kind = ChannelKind.Receive;
            }

            codeClass = new ChannelTypeReference(treeDecorator.getProperty(ctx.elementType(), GoAnnotations.CODE_CLASS), kind);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.CHANNEL);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mulAssignOp, version=0)
    public void enterMulAssignOp(MulAssignOpContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mulAssignOp, version=0)
    public void exitMulAssignOp(MulAssignOpContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageName, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageClause, version=0),
    })
    public void enterPackageName(PackageNameContext ctx) {
        int invokingRule = ParseTrees.getInvokingRule(GoParser._ATN, ctx);
        NodeType nodeType = invokingRule == GoParser.RULE_packageClause ? NodeType.PACKAGE_DECL : NodeType.PACKAGE_REF;
        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, nodeType);
        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, nodeType);
            if (treeDecorator.getProperty(ctx, GoAnnotations.RESOLVED)) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
            }

            TerminalNode localTarget = treeDecorator.getProperty(ctx, GoAnnotations.LOCAL_TARGET);
            if (localTarget != null) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.LOCAL_TARGET, localTarget);
            }
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageName, version=0)
    public void exitPackageName(PackageNameContext ctx) {
        // not qualified!
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_receiver, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseTypeName, version=0),
    })
    public void enterReceiver(ReceiverContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.VAR_TYPE, VarKind.RECEIVER);
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.EXPLICIT_TYPE, ctx.baseTypeName());
            pendingVisibleLocals.peek().put(ctx.IDENTIFIER().getSymbol().getText(), ctx.IDENTIFIER());
        }

        if (ctx.ptr != null && ctx.baseTypeName() != null) {
            treeDecorator.putProperty(ctx.baseTypeName(), GoAnnotations.POINTER_RECEIVER, true);
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_receiver, version=0)
    public void exitReceiver(ReceiverContext ctx) {
        applyPendingVars();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayType, version=0)
    public void enterArrayType(ArrayTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
    })
    public void exitArrayType(ArrayTypeContext ctx) {
        CodeElementReference elemClass = CodeElementReference.UNKNOWN;
        if (ctx.elementType() != null) {
            elemClass = treeDecorator.getProperty(ctx.elementType(), GoAnnotations.CODE_CLASS);
        }

        CodeElementReference arrayClass = new ArrayTypeReference(elemClass);
        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, arrayClass);

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.ARRAY);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionList, version=0)
    public void enterExpressionList(ExpressionListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionList, version=0)
    public void exitExpressionList(ExpressionListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_tag, version=0)
    public void enterTag(TagContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_tag, version=0)
    public void exitTag(TagContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_fallthroughStmt, version=0)
    public void enterFallthroughStmt(FallthroughStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_fallthroughStmt, version=0)
    public void exitFallthroughStmt(FallthroughStmtContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterSelectorExpr(SelectorExprContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.QUALIFIED_EXPR, true);
            if (ctx.expression() != null) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.QUALIFIER, ctx.expression());
            }
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitSelectorExpr(SelectorExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        TerminalNode node = ctx.IDENTIFIER();
        if (node != null) {
            assert ctx.expression() != null;
            exprType = new SelectedElementReference(treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE), node.getSymbol());
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);

        if (node != null) {
            unresolvedQualifiedIdentifiers.add(node);
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameterList, version=0)
    public void enterParameterList(ParameterListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameterList, version=0)
    public void exitParameterList(ParameterListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_receiverType, version=0)
    public void enterReceiverType(ReceiverTypeContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_receiverType, version=0)
    public void exitReceiverType(ReceiverTypeContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_ifStmt, version=0)
    public void enterIfStmt(IfStmtContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_ifStmt, version=0)
    public void exitIfStmt(IfStmtContext ctx) {
        popVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodName, version=0)
    public void enterMethodName(MethodNameContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.METHOD_DECL);
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodName, version=0)
    public void exitMethodName(MethodNameContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_signature, version=0)
    public void enterSignature(SignatureContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_signature, version=0)
    public void exitSignature(SignatureContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mapType, version=0)
    public void enterMapType(MapTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mapType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_keyType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
    })
    public void exitMapType(MapTypeContext ctx) {
        CodeElementReference keyType = CodeElementReference.UNKNOWN;
        if (ctx.keyType() != null) {
            keyType = treeDecorator.getProperty(ctx.keyType(), GoAnnotations.CODE_CLASS);
        }

        CodeElementReference valueType = CodeElementReference.UNKNOWN;
        if (ctx.elementType() != null) {
            valueType = treeDecorator.getProperty(ctx.elementType(), GoAnnotations.CODE_CLASS);
        }

        CodeElementReference codeClass = new MapTypeReference(keyType, valueType);
        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.MAP);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_element, version=0)
    public void enterElement(ElementContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_element, version=0)
    public void exitElement(ElementContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterCallExpr(CallExprContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_argumentList, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionList, version=1),
    })
    public void exitCallExpr(CallExprContext ctx) {
        CodeElementReference returnType = CodeElementReference.UNKNOWN;
        if (ctx.expression() != null) {
            boolean builtin = false;
            if (ctx.expression().start != null && ctx.expression().start == ctx.expression().stop) {
                String methodName = ctx.expression().start.getText();
                builtin = SemanticHighlighter.PREDEFINED_FUNCTIONS.contains(methodName);
            }

            if (builtin) {
                CodeElementReference typeArgument = CodeElementReference.UNKNOWN;
                ArgumentListContext args = ctx.argumentList();
                if (args != null) {
                    ExpressionListContext exprs = args.expressionList();
                    if (exprs != null && !exprs.expression().isEmpty()) {
                        typeArgument = treeDecorator.getProperty(exprs.expression(0), GoAnnotations.CODE_CLASS);
                        if (typeArgument == CodeElementReference.MISSING) {
                            typeArgument = treeDecorator.getProperty(exprs.expression(0), GoAnnotations.EXPR_TYPE);
                        }
                    }
                }

                returnType = new BuiltinCallResultReference(ctx.expression().start, typeArgument);
            } else {
                returnType = new CallResultReference(treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE));
            }
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, returnType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeCaseClause, version=0)
    public void enterTypeCaseClause(TypeCaseClauseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeCaseClause, version=0)
    public void exitTypeCaseClause(TypeCaseClauseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_exprCaseClause, version=0)
    public void enterExprCaseClause(ExprCaseClauseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_exprCaseClause, version=0)
    public void exitExprCaseClause(ExprCaseClauseContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchGuard, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void enterTypeSwitchGuard(TypeSwitchGuardContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.VAR_TYPE, VarKind.LOCAL);
            if (ctx.expression() != null) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.EXPLICIT_TYPE, ctx.expression());
            }

            pendingVisibleLocals.peek().put(ctx.IDENTIFIER().getText(), ctx.IDENTIFIER());
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchGuard, version=0)
    public void exitTypeSwitchGuard(TypeSwitchGuardContext ctx) {
        applyPendingVars();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionLiteral, version=0)
    public void enterFunctionLiteral(FunctionLiteralContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionLiteral, version=0)
    public void exitFunctionLiteral(FunctionLiteralContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        LOGGER.log(Level.FINE, "Element references not implemented for context {0}.", ctx.getClass().getSimpleName());
        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);

        popVarScope();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterExpression(ExpressionContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitExpression(ExpressionContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterOrExpr(OrExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitOrExpr(OrExprContext ctx) {
        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, BuiltinTypeReference.BOOL);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_recvExpr, version=0)
    public void enterRecvExpr(RecvExprContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_recvExpr, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void exitRecvExpr(RecvExprContext ctx) {
        assert ctx.getChildCount() <= 1;
        assert ctx.getChildCount() == 0 || ctx.expression() != null;

        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.expression() != null) {
            exprType = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_topLevelDecl, version=0)
    public void enterTopLevelDecl(TopLevelDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_topLevelDecl, version=0)
    public void exitTopLevelDecl(TopLevelDeclContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodSpec, version=0)
    public void enterMethodSpec(MethodSpecContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodSpec, version=0)
    public void exitMethodSpec(MethodSpecContext ctx) {
        popVarScope();
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_constSpec, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_constDecl, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_declaration, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_topLevelDecl, version=0),
    })
    public void enterConstSpec(ConstSpecContext ctx) {
        if (ctx.identifierList() != null) {
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.NODE_TYPE, NodeType.CONST_DECL);
            boolean global = ParseTrees.isInContexts(ctx, false, GoParser.RULE_constSpec, GoParser.RULE_constDecl, GoParser.RULE_declaration, GoParser.RULE_topLevelDecl);
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.GLOBAL, global);
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_constSpec, version=0)
    public void exitConstSpec(ConstSpecContext ctx) {
        _iota = _iota.add(BigInteger.ONE);
        applyPendingVars();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_compositeLiteral, version=0)
    public void enterCompositeLiteral(CompositeLiteralContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_compositeLiteral, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalValue, version=0),
    })
    public void exitCompositeLiteral(CompositeLiteralContext ctx) {
        CodeElementReference exprType;
        if (ctx.literalType() != null) {
            exprType = treeDecorator.getProperty(ctx.literalType(), GoAnnotations.CODE_CLASS);
        } else {
            exprType = CodeElementReference.UNKNOWN;
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
        if (ctx.literalValue() != null) {
            treeDecorator.putProperty(ctx.literalValue(), GoAnnotations.EXPR_TYPE, exprType);
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_forClause, version=0)
    public void enterForClause(ForClauseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_forClause, version=0)
    public void exitForClause(ForClauseContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_shortVarDecl, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionList, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void enterShortVarDecl(ShortVarDeclContext ctx) {
        if (ctx.identifierList() != null) {
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.VAR_TYPE, VarKind.LOCAL);
            if (ctx.expressionList() != null) {
                int varCount = ctx.identifierList().IDENTIFIER().size();
                int exprCount = ctx.expressionList().expression().size();
                if (varCount > 1 && exprCount == 1) {
                    for (int i = 0; i < varCount; i++) {
                        treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_TYPE, ctx.expressionList().expression(0));
                        treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_INDEX, i);
                    }
                } else if (varCount == exprCount) {
                    for (int i = 0; i < varCount; i++) {
                        treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_TYPE, ctx.expressionList().expression(i));
                        if (varCount > 1) {
                            treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_INDEX, i);
                        }
                    }
                }
            }
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_shortVarDecl, version=1)
    public void exitShortVarDecl(ShortVarDeclContext ctx) {
        applyPendingVars();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_gotoStmt, version=0)
    public void enterGotoStmt(GotoStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_gotoStmt, version=0)
    public void exitGotoStmt(GotoStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayLength, version=0)
    public void enterArrayLength(ArrayLengthContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayLength, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void exitArrayLength(ArrayLengthContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.expression() != null) {
            exprType = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0)
    public void enterInterfaceType(InterfaceTypeContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0)
    public void exitInterfaceType(InterfaceTypeContext ctx) {
        CodeElementReference codeClass = new InterfaceTypeReference(((TypeModelImpl)IntrinsicTypeModels.INT).getFile());
        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.INTERFACE);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_conversion, version=0)
    public void enterConversion(ConversionContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_conversion, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void exitConversion(ConversionContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.type() != null) {
            exprType = treeDecorator.getProperty(ctx.type(), GoAnnotations.CODE_CLASS);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_block, version=0)
    public void enterBlock(BlockContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_block, version=0)
    public void exitBlock(BlockContext ctx) {
        popVarScope();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_breakStmt, version=0)
    public void enterBreakStmt(BreakStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_breakStmt, version=0)
    public void exitBreakStmt(BreakStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_emptyStmt, version=0)
    public void enterEmptyStmt(EmptyStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_emptyStmt, version=0)
    public void exitEmptyStmt(EmptyStmtContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionLiteral, version=0),
    })
    public void enterFunctionType(FunctionTypeContext ctx) {
        if (!ParseTrees.isInContexts(ctx, false, GoParser.RULE_functionType, GoParser.RULE_functionLiteral)) {
            pushVarScope();
        }
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionLiteral, version=0),
    })
    public void exitFunctionType(FunctionTypeContext ctx) {
        CodeElementReference codeClass = new FunctionTypeReference();
        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        if (!ParseTrees.isInContexts(ctx, false, GoParser.RULE_functionType, GoParser.RULE_functionLiteral)) {
            popVarScope();
        }

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.FUNCTION);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseType, version=0)
    public void enterBaseType(BaseTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void exitBaseType(BaseTypeContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.type() != null) {
            codeClass = treeDecorator.getProperty(ctx.type(), GoAnnotations.CODE_CLASS);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_fieldDecl, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void enterFieldDecl(FieldDeclContext ctx) {
        if (ctx.identifierList() != null) {
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.VAR_TYPE, VarKind.FIELD);
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.EXPLICIT_TYPE, ctx.type());
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_fieldDecl, version=0)
    public void exitFieldDecl(FieldDeclContext ctx) {
        applyPendingVars();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_exprSwitchStmt, version=0)
    public void enterExprSwitchStmt(ExprSwitchStmtContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_exprSwitchStmt, version=0)
    public void exitExprSwitchStmt(ExprSwitchStmtContext ctx) {
        popVarScope();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_goStmt, version=0)
    public void enterGoStmt(GoStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_goStmt, version=0)
    public void exitGoStmt(GoStmtContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameterDecl, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void enterParameterDecl(ParameterDeclContext ctx) {
        if (ctx.identifierList() != null) {
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
            if (ctx.ellip != null) {
                treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.VARIADIC, true);
            }

            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.EXPLICIT_TYPE, ctx.type());
            if (ParseTrees.isInContexts(ctx, false, GoParser.RULE_parameterDecl, GoParser.RULE_parameterList, GoParser.RULE_parameters, GoParser.RULE_result)) {
                treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.VAR_TYPE, VarKind.RETURN);
            } else {
                treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.VAR_TYPE, VarKind.PARAMETER);
            }

            for (TerminalNode id : ctx.identifierList().IDENTIFIER()) {
                pendingVisibleLocals.peek().put(id.getSymbol().getText(), id);
            }
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameterDecl, version=0)
    public void exitParameterDecl(ParameterDeclContext ctx) {
        applyPendingVars();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_basicLiteral, version=0)
    public void enterBasicLiteral(BasicLiteralContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_basicLiteral, version=0)
    public void exitBasicLiteral(BasicLiteralContext ctx) {
        assert ctx.stop == null || ctx.start == ctx.stop;
        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, new LiteralTypeReference(ctx.start));
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_exprSwitchCase, version=0)
    public void enterExprSwitchCase(ExprSwitchCaseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_exprSwitchCase, version=0)
    public void exitExprSwitchCase(ExprSwitchCaseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeLiteral, version=0)
    public void enterTypeLiteral(TypeLiteralContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeLiteral, version=0)
    public void exitTypeLiteral(TypeLiteralContext ctx) {
        assert ctx.getChildCount() <= 1;
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.getChildCount() == 1) {
            codeClass = treeDecorator.getProperty(ctx.getChild(0), GoAnnotations.CODE_CLASS);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_selectStmt, version=0)
    public void enterSelectStmt(SelectStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_selectStmt, version=0)
    public void exitSelectStmt(SelectStmtContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_importSpec, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_importPath, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageName, version=0),
    })
    public void enterImportSpec(ImportSpecContext ctx) {
        if (ctx.importPath() == null || ctx.importPath().StringLiteral() == null) {
            return;
        }

        String name = null;
        String path = null;
        TerminalNode target = null;
        if (ctx.importPath() != null && ctx.importPath().StringLiteral() != null) {
            target = ctx.importPath().StringLiteral();
            path = target.getText();
            if (path.startsWith("\"")) {
                path = path.substring(1);
            }
            if (path.endsWith("\"")) {
                path = path.substring(0, path.length() - 1);
            }
        }

        if (ctx.dot != null) {
            name = "";
            target = null;
        } else if (ctx.packageName != null) {
            if (ctx.packageName.IDENTIFIER() != null) {
                target = ctx.packageName.IDENTIFIER();
                name = target.getText();
            }
        } else {
            name = path.substring(path.lastIndexOf('/') + 1);
        }

        if (target != null) {
            Project currentProject = FileOwnerQuery.getOwner(document.getFileObject());
            if (!(currentProject instanceof GoProject)) {
                return;
            }

            String packagePath = getCurrentPackagePath((GoProject)currentProject, document);
            Collection<? extends PackageModel> packages = CodeModelCacheImpl.getInstance().resolvePackages((GoProject)currentProject, packagePath, path);

            List<PackageModel> visiblePackages = new ArrayList<>(packages);

            treeDecorator.putProperty(target, GoAnnotations.MODELS, visiblePackages);
            if (!visiblePackages.isEmpty()) {
                treeDecorator.putProperty(target, GoAnnotations.RESOLVED, true);
            }

            List<TerminalNode> packageList = importedPackages.get(name);
            if (packageList == null) {
                packageList = new ArrayList<>();
                importedPackages.put(name, packageList);
            }

            packageList.add(target);
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_importSpec, version=0)
    public void exitImportSpec(ImportSpecContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeName, version=0)
    public void enterTypeName(TypeNameContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeName, version=3, dependents=Dependents.PARENTS),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_qualifiedIdentifier, version=0),
    })
    public void exitTypeName(TypeNameContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.qualifiedIdentifier() != null) {
            codeClass = new QualifiedIdentifierElementReference(ctx.qualifiedIdentifier());
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        if (ctx.qualifiedIdentifier() == null) {
            return;
        }

        if (treeDecorator.getProperty(ctx.qualifiedIdentifier(), GoAnnotations.QUALIFIED_EXPR)) {
            treeDecorator.putProperty(ctx, GoAnnotations.QUALIFIED_EXPR, true);
        } else {
            treeDecorator.putProperty(ctx, GoAnnotations.UNQUALIFIED_LINK, treeDecorator.getProperty(ctx.qualifiedIdentifier(), GoAnnotations.UNQUALIFIED_LINK));
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalType, version=0)
    public void enterLiteralType(LiteralTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sliceType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mapType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeName, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
    })
    public void exitLiteralType(LiteralTypeContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.getChildCount() == 1) {
            ParseTree child = ctx.getChild(0);
            assert child instanceof StructTypeContext
                || child instanceof ArrayTypeContext
                || child instanceof SliceTypeContext
                || child instanceof MapTypeContext
                || child instanceof TypeNameContext;
            codeClass = treeDecorator.getProperty(child, GoAnnotations.CODE_CLASS);
        } else if (ctx.getChildCount() > 1) {
            ParseTree firstChild = ctx.getChild(0);
            assert firstChild instanceof TerminalNode
                && ((TerminalNode)firstChild).getSymbol() instanceof Token
                && ((TerminalNode)firstChild).getSymbol().getType() == GoParser.LeftBrack;

            ParseTree lastChild = ctx.getChild(ctx.getChildCount() - 1);
            assert lastChild instanceof ElementTypeContext;
            codeClass = new ArrayTypeReference(treeDecorator.getProperty(lastChild, GoAnnotations.CODE_CLASS));
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_assignment, version=0)
    public void enterAssignment(AssignmentContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_assignment, version=0)
    public void exitAssignment(AssignmentContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_assignOp, version=0)
    public void enterAssignOp(AssignOpContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_assignOp, version=0)
    public void exitAssignOp(AssignOpContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_recvStmt, version=0)
    public void enterRecvStmt(RecvStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_recvStmt, version=0)
    public void exitRecvStmt(RecvStmtContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSpec, version=0)
    public void enterTypeSpec(TypeSpecContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.TYPE_DECL);
            visibleTypes.peek().put(ctx.IDENTIFIER().getSymbol().getText(), ctx.IDENTIFIER());
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSpec, version=0)
    public void exitTypeSpec(TypeSpecContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageClause, version=0)
    public void enterPackageClause(PackageClauseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageClause, version=0)
    public void exitPackageClause(PackageClauseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalValue, version=0)
    public void enterLiteralValue(LiteralValueContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalValue, version=0)
    public void exitLiteralValue(LiteralValueContext ctx) {
        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, new LiteralValueElementReference());
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterIndexExpr(IndexExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitIndexExpr(IndexExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.MISSING;
        if (ctx.expression(0) != null) {
            CodeElementReference arrayType = treeDecorator.getProperty(ctx.expression(0), GoAnnotations.EXPR_TYPE);
            exprType = new ArrayElementTypeReference(arrayType);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_varSpec, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionList, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_varDecl, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_declaration, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_topLevelDecl, version=0),
    })
    public void enterVarSpec(VarSpecContext ctx) {
        if (ctx.identifierList() != null) {
            treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
            if (ParseTrees.isInContexts(ctx, false, GoParser.RULE_varSpec, GoParser.RULE_varDecl, GoParser.RULE_declaration, GoParser.RULE_topLevelDecl)) {
                treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.VAR_TYPE, VarKind.GLOBAL);
            } else {
                treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.VAR_TYPE, VarKind.LOCAL);
            }
            
            if (ctx.type() != null) {
                treeDecorator.putProperty(ctx.identifierList(), GoAnnotations.EXPLICIT_TYPE, ctx.type());
            } else if (ctx.expressionList() != null) {
                int varCount = ctx.identifierList().IDENTIFIER().size();
                int exprCount = ctx.expressionList().expression().size();
                if (varCount > 1 && exprCount == 1) {
                    for (int i = 0; i < varCount; i++) {
                        treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_TYPE, ctx.expressionList().expression(0));
                        treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_INDEX, i);
                    }
                } else if (varCount == exprCount) {
                    for (int i = 0; i < varCount; i++) {
                        treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_TYPE, ctx.expressionList().expression(i));
                        treeDecorator.putProperty(ctx.identifierList().IDENTIFIER(i), GoAnnotations.IMPLICIT_INDEX, i);
                    }
                }
            }
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_varSpec, version=0)
    public void exitVarSpec(VarSpecContext ctx) {
        applyPendingVars();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterOperandExpr(OperandExprContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_operand, version=0),
    })
    public void exitOperandExpr(OperandExprContext ctx) {
        // this isn't true in some parse error scenarios
        //assert ctx.getChildCount() == 0 || (ctx.getChildCount() == 1 && ctx.operand() != null);
        if (ctx.operand() != null) {
            treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, treeDecorator.getProperty(ctx.operand(), GoAnnotations.EXPR_TYPE));
        } else {
            LOGGER.log(Level.WARNING, "Unrecognized tree structure.");
            treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, CodeElementReference.UNKNOWN);
        }

        if (ctx.operand() != null) {
            treeDecorator.putProperties(ctx, treeDecorator.getProperties(ctx.operand()));
        } else {
            LOGGER.log(Level.FINER, "Expression resolution links are not supported for context: {0}", ctx.toString(Arrays.asList(GoParser.ruleNames)));
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterBuiltinCallExpr(BuiltinCallExprContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinCall, version=0),
    })
    public void exitBuiltinCallExpr(BuiltinCallExprContext ctx) {
        assert ctx.getChildCount() == 0 || (ctx.getChildCount() == 1 && ctx.builtinCall() != null);
        if (ctx.builtinCall() != null) {
            treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, treeDecorator.getProperty(ctx.builtinCall(), GoAnnotations.EXPR_TYPE));
        } else {
            LOGGER.log(Level.WARNING, "Unrecognized tree structure.");
            treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, CodeElementReference.UNKNOWN);
        }

        LOGGER.log(Level.FINER, "Expression resolution links are not supported for context: {0}", ctx.toString(Arrays.asList(GoParser.ruleNames)));
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_body, version=0)
    public void enterBody(BodyContext ctx) {
        pushVarScope();
        visibleLabels.push(new HashMap<String, TerminalNode>());
        unresolvedLabels.push(new ArrayList<TerminalNode>());
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_body, version=0)
    public void exitBody(BodyContext ctx) {
        for (TerminalNode labelReference : unresolvedLabels.peek()) {
            TerminalNode target = visibleLabels.peek().get(labelReference.getText());
            if (target == null) {
                continue;
            }

            treeDecorator.putProperty(labelReference, GoAnnotations.LOCAL_TARGET, target);
            treeDecorator.putProperty(labelReference, GoAnnotations.RESOLVED, true);
        }

        popVarScope();
        visibleLabels.pop();
        unresolvedLabels.pop();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_commClause, version=0)
    public void enterCommClause(CommClauseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_commClause, version=0)
    public void exitCommClause(CommClauseContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_qualifiedIdentifier, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageName, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchGuard, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchStmt, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeCaseClause, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchCase, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeList, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void enterQualifiedIdentifier(QualifiedIdentifierContext ctx) {
        if (ctx.packageName() != null) {
            if (ctx.IDENTIFIER() != null) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.QUALIFIED_EXPR, true);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.QUALIFIER, ctx.packageName());
                unresolvedQualifiedIdentifiers.add(ctx.IDENTIFIER());
            }

            // check known imports
            if (ctx.packageName().IDENTIFIER() != null) {
                List<? extends TerminalNode> imports = ParseTrees.emptyIfNull(importedPackages.get(ctx.packageName().IDENTIFIER().getSymbol().getText()));
                TerminalNode bestImport = null;
                boolean resolvedImport = false;
                for (TerminalNode importToken : imports) {
                    if (bestImport == null || (!resolvedImport && treeDecorator.getProperty(importToken, GoAnnotations.RESOLVED))) {
                        bestImport = importToken;
                        resolvedImport = treeDecorator.getProperty(importToken, GoAnnotations.RESOLVED);
                        break;
                    }
                }

                if (bestImport != null) {
                    treeDecorator.putProperty(ctx.packageName(), GoAnnotations.LOCAL_TARGET, bestImport);
                    if (resolvedImport) {
                        treeDecorator.putProperty(ctx.packageName(), GoAnnotations.RESOLVED, true);
                    }
                }
            }
        } else if (ctx.IDENTIFIER() != null) {
            assert ctx.packageName() == null;
            String text = ctx.IDENTIFIER().getText();
            switch (text) {
            case "true":
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.UNEVALUATED_CONSTANT, "true");
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.EVALUATED_CONSTANT, BigInteger.ONE);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                break;

            case "false":
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.UNEVALUATED_CONSTANT, "false");
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.EVALUATED_CONSTANT, BigInteger.ZERO);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                break;

            case "iota":
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.UNEVALUATED_CONSTANT, "iota");
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.EVALUATED_CONSTANT, _iota);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                break;
            }

            TerminalNode local = getVisibleLocal(ctx.IDENTIFIER());
            if (local != null) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.VAR_REF);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.LOCAL_TARGET, local);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
                VarKind varType = treeDecorator.getProperty(local, GoAnnotations.VAR_TYPE);
                if (varType != VarKind.UNDEFINED) {
                    treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.VAR_TYPE, varType);
                }

                if (local.getParent().getRuleContext() instanceof TypeSwitchGuardContext) {
                    TypeSwitchGuardContext typeSwitchGuardContext = (TypeSwitchGuardContext)local.getParent().getRuleContext();
                    TypeSwitchStmtContext typeSwitchStmtContext = (TypeSwitchStmtContext)typeSwitchGuardContext.getParent();
                    for (TypeCaseClauseContext typeCaseClauseContext : typeSwitchStmtContext.typeCaseClause()) {
                        if (ParseTrees.isAncestorOf(typeCaseClauseContext, ctx)) {
                            TypeListContext typeListContext = typeCaseClauseContext.typeSwitchCase() != null ? typeCaseClauseContext.typeSwitchCase().typeList() : null;
                            if (typeListContext != null && typeListContext.type().size() == 1) {
                                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.EXPLICIT_TYPE, typeListContext.type(0));
                            }

                            break;
                        }
                    }
                }

                return;
            }

            TerminalNode constant = getVisibleConstant(ctx.IDENTIFIER());
            if (constant != null) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.LOCAL_TARGET, constant);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
                return;
            }

            // check built-ins
            if (SemanticHighlighter.PREDEFINED_FUNCTIONS.contains(ctx.IDENTIFIER().getSymbol().getText())) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.FUNC_REF);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.BUILTIN, true);
            } else if (SemanticHighlighter.PREDEFINED_TYPES.contains(ctx.IDENTIFIER().getSymbol().getText())) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.TYPE_REF);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.TYPE_KIND, TypeKind.INTRINSIC);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.BUILTIN, true);
            } else if (SemanticHighlighter.PREDEFINED_CONSTANTS.contains(ctx.IDENTIFIER().getSymbol().getText())) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.BUILTIN, true);
            } else {
                unresolvedIdentifiers.add(ctx.IDENTIFIER());
            }
        }
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_qualifiedIdentifier, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_packageName, version=0),
    })
    public void exitQualifiedIdentifier(QualifiedIdentifierContext ctx) {
        if (ctx.packageName() != null) {
            treeDecorator.putProperty(ctx, GoAnnotations.QUALIFIED_EXPR, true);
        } else if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx, GoAnnotations.UNQUALIFIED_LINK, ctx.IDENTIFIER());
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_returnStmt, version=0)
    public void enterReturnStmt(ReturnStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_returnStmt, version=0)
    public void exitReturnStmt(ReturnStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_simpleStmt, version=0)
    public void enterSimpleStmt(SimpleStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_simpleStmt, version=0)
    public void exitSimpleStmt(SimpleStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterTypeAssertionExpr(TypeAssertionExprContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void exitTypeAssertionExpr(TypeAssertionExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.MISSING;
        if (ctx.type() != null) {
            exprType = treeDecorator.getProperty(ctx.type(), GoAnnotations.CODE_CLASS);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, new TypeAssertionResultReference(exprType));
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0)
    public void enterType(TypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=3, dependents=Dependents.PARENTS),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeName, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeLiteral, version=0),
    })
    public void exitType(TypeContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.typeName() != null) {
            codeClass = treeDecorator.getProperty(ctx.typeName(), GoAnnotations.CODE_CLASS);
        } else if (ctx.typeLiteral() != null) {
            codeClass = treeDecorator.getProperty(ctx.typeLiteral(), GoAnnotations.CODE_CLASS);
        } else if (ctx.type() != null) {
            codeClass = treeDecorator.getProperty(ctx.type(), GoAnnotations.CODE_CLASS);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceTypeName, version=0)
    public void enterInterfaceTypeName(InterfaceTypeNameContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceTypeName, version=0)
    public void exitInterfaceTypeName(InterfaceTypeNameContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_continueStmt, version=0)
    public void enterContinueStmt(ContinueStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_continueStmt, version=0)
    public void exitContinueStmt(ContinueStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_value, version=0)
    public void enterValue(ValueContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_value, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalValue, version=0),
    })
    public void exitValue(ValueContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.expression() != null) {
            exprType = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
        } else if (ctx.literalValue() != null) {
            exprType = treeDecorator.getProperty(ctx.literalValue(), GoAnnotations.EXPR_TYPE);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodDecl, version=0)
    public void enterMethodDecl(MethodDeclContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodDecl, version=0)
    public void exitMethodDecl(MethodDeclContext ctx) {
        popVarScope();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_labeledStmt, version=0)
    public void enterLabeledStmt(LabeledStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_labeledStmt, version=0)
    public void exitLabeledStmt(LabeledStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameters, version=0)
    public void enterParameters(ParametersContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameters, version=0)
    public void exitParameters(ParametersContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_deferStmt, version=0)
    public void enterDeferStmt(DeferStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_deferStmt, version=0)
    public void exitDeferStmt(DeferStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_key, version=0)
    public void enterKey(KeyContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_key, version=0)
    public void exitKey(KeyContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_declaration, version=0)
    public void enterDeclaration(DeclarationContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_declaration, version=0)
    public void exitDeclaration(DeclarationContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_commCase, version=0)
    public void enterCommCase(CommCaseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_commCase, version=0)
    public void exitCommCase(CommCaseContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinArgs, version=0)
    public void enterBuiltinArgs(BuiltinArgsContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinArgs, version=0)
    public void exitBuiltinArgs(BuiltinArgsContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_condition, version=0)
    public void enterCondition(ConditionContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_condition, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void exitCondition(ConditionContext ctx) {
        assert ctx.getChildCount() == 0 || (ctx.getChildCount() == 1 && ctx.expression() != null);

        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.expression() != null) {
            exprType = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterConversionOrCallExpr(ConversionOrCallExprContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_conversion, version=0),
    })
    public void exitConversionOrCallExpr(ConversionOrCallExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.MISSING;
        if (ctx.conversion() != null) {
            exprType = new ConversionOrCallResultReference(treeDecorator.getProperty(ctx.conversion(), GoAnnotations.EXPR_TYPE));
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_label, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_labeledStmt, version=0),
    })
    public void enterLabel(LabelContext ctx) {
        if (ctx.IDENTIFIER() == null) {
            return;
        }

        boolean definition = ParseTrees.isInContexts(ctx, false, GoParser.RULE_label, GoParser.RULE_labeledStmt);
        if (definition) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.LABEL_DECL);
            visibleLabels.peek().put(ctx.IDENTIFIER().getSymbol().getText(), ctx.IDENTIFIER());
        } else {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.LABEL_REF);
            unresolvedLabels.peek().add(ctx.IDENTIFIER());
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_label, version=0)
    public void exitLabel(LabelContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0)
    public void enterElementType(ElementTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void exitElementType(ElementTypeContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.type() != null) {
            codeClass = treeDecorator.getProperty(ctx.type(), GoAnnotations.CODE_CLASS);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionDecl, version=0)
    public void enterFunctionDecl(FunctionDeclContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.FUNC_DECL);
            visibleFunctions.peek().put(ctx.IDENTIFIER().getSymbol().getText(), ctx.IDENTIFIER());
        }

        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionDecl, version=0)
    public void exitFunctionDecl(FunctionDeclContext ctx) {
        popVarScope();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_statement, version=0)
    public void enterStatement(StatementContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_statement, version=0)
    public void exitStatement(StatementContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_pointerType, version=0)
    public void enterPointerType(PointerTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_pointerType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseType, version=0),
    })
    public void exitPointerType(PointerTypeContext ctx) {
        CodeElementReference elemClass = CodeElementReference.UNKNOWN;
        if (ctx.baseType() != null) {
            elemClass = treeDecorator.getProperty(ctx.baseType(), GoAnnotations.CODE_CLASS);
        }

        CodeElementReference codeClass = new PointerTypeReference(elemClass);
        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.POINTER);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_addAssignOp, version=0)
    public void enterAddAssignOp(AddAssignOpContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_addAssignOp, version=0)
    public void exitAddAssignOp(AddAssignOpContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sourceFileBody, version=0)
    public void enterSourceFileBody(SourceFileBodyContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sourceFileBody, version=0)
    public void exitSourceFileBody(SourceFileBodyContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sourceFile, version=1)
    public void enterSourceFile(SourceFileContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sourceFile, version=1)
    public void exitSourceFile(SourceFileContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterSliceExpr(SliceExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitSliceExpr(SliceExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.expression(0) != null) {
            exprType = new SliceExpressionTypeReference(treeDecorator.getProperty(ctx.expression(0), GoAnnotations.EXPR_TYPE));
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseTypeName, version=0)
    public void enterBaseTypeName(BaseTypeNameContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            unresolvedIdentifiers.add(ctx.IDENTIFIER());
        }
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseTypeName, version=0)
    public void exitBaseTypeName(BaseTypeNameContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.IDENTIFIER() != null) {
            codeClass = new ReceiverTypeReference(ctx.IDENTIFIER(), treeDecorator.getProperty(ctx, GoAnnotations.POINTER_RECEIVER));
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodExpr, version=0)
    public void enterMethodExpr(MethodExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodExpr, version=0)
    public void exitMethodExpr(MethodExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        LOGGER.log(Level.FINE, "Element references not implemented for context {0}.", ctx.getClass().getSimpleName());
        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementNameOrIndex, version=0)
    public void enterElementNameOrIndex(ElementNameOrIndexContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementNameOrIndex, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void exitElementNameOrIndex(ElementNameOrIndexContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.expression() != null) {
            exprType = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeList, version=0)
    public void enterTypeList(TypeListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeList, version=0)
    public void exitTypeList(TypeListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_incDecStmt, version=0)
    public void enterIncDecStmt(IncDecStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_incDecStmt, version=0)
    public void exitIncDecStmt(IncDecStmtContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinCall, version=0)
    public void enterBuiltinCall(BuiltinCallContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            if (SemanticHighlighter.PREDEFINED_FUNCTIONS.contains(ctx.IDENTIFIER().getSymbol().getText())) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.FUNC_REF);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.BUILTIN, true);
            } else {
                unresolvedIdentifiers.add(ctx.IDENTIFIER());
            }
        }
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinCall, version=0, dependents=Dependents.PARENTS),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinArgs, version=3, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinTypeArgs, version=3, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_argumentList, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionList, version=1, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchGuard, version=0, dependents=Dependents.PARENTS),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchStmt, version=0, dependents=Dependents.PARENTS),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeCaseClause, version=3, dependents={Dependents.SELF, Dependents.DESCENDANTS}),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchCase, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeList, version=0, dependents=Dependents.SELF),
    })
    public void exitBuiltinCall(BuiltinCallContext ctx) {
        BuiltinArgsContext args = ctx.builtinArgs();
        if (ctx.IDENTIFIER() != null
            && !SemanticHighlighter.PREDEFINED_FUNCTIONS.contains(ctx.IDENTIFIER().getText())
            && (args == null || args.type() == null))
        {
            // not a built in function, and no type was passed to it

            // HACK: copied from enterQualifiedIdentifier
            TerminalNode local = getVisibleLocal(ctx.IDENTIFIER());
            if (local != null) {
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.VAR_REF);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.LOCAL_TARGET, local);
                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
                VarKind varType = treeDecorator.getProperty(local, GoAnnotations.VAR_TYPE);
                if (varType != VarKind.UNDEFINED) {
                    treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.VAR_TYPE, varType);
                }

                if (local.getParent().getRuleContext() instanceof TypeSwitchGuardContext) {
                    TypeSwitchGuardContext typeSwitchGuardContext = (TypeSwitchGuardContext)local.getParent().getRuleContext();
                    TypeSwitchStmtContext typeSwitchStmtContext = (TypeSwitchStmtContext)typeSwitchGuardContext.getParent();
                    for (TypeCaseClauseContext typeCaseClauseContext : typeSwitchStmtContext.typeCaseClause()) {
                        if (ParseTrees.isAncestorOf(typeCaseClauseContext, ctx)) {
                            TypeListContext typeListContext = typeCaseClauseContext.typeSwitchCase() != null ? typeCaseClauseContext.typeSwitchCase().typeList() : null;
                            if (typeListContext != null && typeListContext.type().size() == 1) {
                                treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.EXPLICIT_TYPE, typeListContext.type(0));
                            }

                            break;
                        }
                    }
                }
            } else {
                TerminalNode constant = getVisibleConstant(ctx.IDENTIFIER());
                if (constant != null) {
                    treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                    treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.LOCAL_TARGET, constant);
                    treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.RESOLVED, true);
                } else {
                    // check built-ins
                    if (SemanticHighlighter.PREDEFINED_FUNCTIONS.contains(ctx.IDENTIFIER().getSymbol().getText())) {
                        treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.FUNC_REF);
                        treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.BUILTIN, true);
                    } else if (SemanticHighlighter.PREDEFINED_TYPES.contains(ctx.IDENTIFIER().getSymbol().getText())) {
                        treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.TYPE_REF);
                        treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.TYPE_KIND, TypeKind.INTRINSIC);
                        treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.BUILTIN, true);
                    } else if (SemanticHighlighter.PREDEFINED_CONSTANTS.contains(ctx.IDENTIFIER().getSymbol().getText())) {
                        treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.NODE_TYPE, NodeType.CONST_REF);
                        treeDecorator.putProperty(ctx.IDENTIFIER(), GoAnnotations.BUILTIN, true);
                    } else {
                        unresolvedIdentifiers.add(ctx.IDENTIFIER());
                    }
                }
            }

            treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, new CallResultReference(new UnqualifiedIdentifierElementReference(ctx.IDENTIFIER())));
            return;
        }

        CodeElementReference typeArgument = CodeElementReference.UNKNOWN;
        if (args != null) {
            if (args.type() != null) {
                typeArgument = treeDecorator.getProperty(args.type(), GoAnnotations.CODE_CLASS);
            } else {
                ArgumentListContext argumentList = args.argumentList();
                ExpressionListContext exprList = argumentList != null ? argumentList.expressionList() : null;
                if (exprList != null) {
                    List<? extends ExpressionContext> exprs = exprList.expression();
                    if (exprs != null && !exprs.isEmpty()) {
                        typeArgument = treeDecorator.getProperty(exprs.get(0), GoAnnotations.EXPR_TYPE);
                        if (typeArgument == CodeElementReference.MISSING) {
                            typeArgument = treeDecorator.getProperty(exprs.get(0), GoAnnotations.CODE_CLASS);
                        }
                    }
                }
            }
        }

        if (ctx.IDENTIFIER() != null) {
            treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, new BuiltinCallResultReference(ctx.IDENTIFIER().getSymbol(), typeArgument));
        } else {
            treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, CodeElementReference.UNKNOWN);
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_constDecl, version=0)
    public void enterConstDecl(ConstDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_constDecl, version=0)
    public void exitConstDecl(ConstDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_result, version=0)
    public void enterResult(ResultContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_result, version=0)
    public void exitResult(ResultContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterAndExpr(AndExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitAndExpr(AndExprContext ctx) {
        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, BuiltinTypeReference.BOOL);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0)
    public void enterStructType(StructTypeContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0)
    public void exitStructType(StructTypeContext ctx) {
        CodeElementReference codeClass = new StructTypeReference();
        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.STRUCT);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_varDecl, version=0)
    public void enterVarDecl(VarDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_varDecl, version=0)
    public void exitVarDecl(VarDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_initStmt, version=0)
    public void enterInitStmt(InitStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_initStmt, version=0)
    public void exitInitStmt(InitStmtContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0)
    public void enterIdentifierList(IdentifierListContext ctx) {
        NodeType nodeType = treeDecorator.getProperty(ctx, GoAnnotations.NODE_TYPE);
        VarKind varType = treeDecorator.getProperty(ctx, GoAnnotations.VAR_TYPE);
        boolean variadic = treeDecorator.getProperty(ctx, GoAnnotations.VARIADIC);
        ParserRuleContext explicitType = treeDecorator.getProperty(ctx, GoAnnotations.EXPLICIT_TYPE);
        boolean global =
            (varType != VarKind.LOCAL && varType != VarKind.RECEIVER && varType != VarKind.PARAMETER && varType != VarKind.RETURN)
            || treeDecorator.getProperty(ctx, GoAnnotations.GLOBAL);

        if (nodeType == NodeType.VAR_DECL) {
            if (varType == VarKind.UNDEFINED) {
                return;
            }
        } else if (nodeType == NodeType.CONST_DECL) {
            // nothing special to do here
        } else {
            return;
        }

        for (TerminalNode terminalNode : ctx.IDENTIFIER()) {
            Token token = terminalNode.getSymbol();
            if (nodeType == NodeType.VAR_DECL) {
                if (varType != VarKind.FIELD) {
                    pendingVisibleLocals.peek().put(token.getText(), terminalNode);
                }
            } else {
                assert nodeType == NodeType.CONST_DECL;
                pendingVisibleConstants.peek().put(token.getText(), terminalNode);
            }

            treeDecorator.putProperty(terminalNode, GoAnnotations.NODE_TYPE, nodeType);

            if (varType != null) {
                treeDecorator.putProperty(terminalNode, GoAnnotations.VAR_TYPE, varType);
            }

            if (variadic) {
                treeDecorator.putProperty(terminalNode, GoAnnotations.VARIADIC, variadic);
            }

            if (explicitType != null) {
                treeDecorator.putProperty(terminalNode, GoAnnotations.EXPLICIT_TYPE, explicitType);
            }

            if (global) {
                treeDecorator.putProperty(terminalNode, GoAnnotations.GLOBAL, global);
            }
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0)
    public void exitIdentifierList(IdentifierListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sliceType, version=0)
    public void enterSliceType(SliceTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sliceType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
    })
    public void exitSliceType(SliceTypeContext ctx) {
        CodeElementReference elemClass = CodeElementReference.UNKNOWN;
        if (ctx.elementType() != null) {
            elemClass = treeDecorator.getProperty(ctx.elementType(), GoAnnotations.CODE_CLASS);
        }

        CodeElementReference codeClass = new SliceTypeReference(elemClass);
        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);

        treeDecorator.putProperty(ctx, GoAnnotations.NODE_TYPE, NodeType.TYPE_LITERAL);
        treeDecorator.putProperty(ctx, GoAnnotations.TYPE_KIND, TypeKind.SLICE);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterCompareExpr(CompareExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitCompareExpr(CompareExprContext ctx) {
        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, BuiltinTypeReference.BOOL);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_importDecl, version=0)
    public void enterImportDecl(ImportDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_importDecl, version=0)
    public void exitImportDecl(ImportDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementList, version=0)
    public void enterElementList(ElementListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementList, version=0)
    public void exitElementList(ElementListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_keyType, version=0)
    public void enterKeyType(KeyTypeContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_keyType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
    })
    public void exitKeyType(KeyTypeContext ctx) {
        CodeElementReference codeClass = CodeElementReference.UNKNOWN;
        if (ctx.type() != null) {
            codeClass = treeDecorator.getProperty(ctx.type(), GoAnnotations.CODE_CLASS);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.CODE_CLASS, codeClass);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_importPath, version=0)
    public void enterImportPath(ImportPathContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_importPath, version=0)
    public void exitImportPath(ImportPathContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_anonymousField, version=0)
    public void enterAnonymousField(AnonymousFieldContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_anonymousField, version=0)
    public void exitAnonymousField(AnonymousFieldContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterAddExpr(AddExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitAddExpr(AddExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.MISSING;
        if (ctx.expression(0) != null && ctx.op != null && ctx.expression(1) != null) {
            CodeElementReference left = treeDecorator.getProperty(ctx.expression(0), GoAnnotations.EXPR_TYPE);
            CodeElementReference right = treeDecorator.getProperty(ctx.expression(1), GoAnnotations.EXPR_TYPE);
            exprType = new BinaryExpressionTypeReference(left, ctx.op, right);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionStmt, version=0)
    public void enterExpressionStmt(ExpressionStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expressionStmt, version=0)
    public void exitExpressionStmt(ExpressionStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sendStmt, version=0)
    public void enterSendStmt(SendStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sendStmt, version=0)
    public void exitSendStmt(SendStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_switchStmt, version=0)
    public void enterSwitchStmt(SwitchStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_switchStmt, version=0)
    public void exitSwitchStmt(SwitchStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_postStmt, version=0)
    public void enterPostStmt(PostStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_postStmt, version=0)
    public void exitPostStmt(PostStmtContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_forStmt, version=0)
    public void enterForStmt(ForStmtContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_forStmt, version=0)
    public void exitForStmt(ForStmtContext ctx) {
        popVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchCase, version=0)
    public void enterTypeSwitchCase(TypeSwitchCaseContext ctx) {
        pushVarScope();
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchCase, version=0)
    public void exitTypeSwitchCase(TypeSwitchCaseContext ctx) {
        popVarScope();
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_rangeClause, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void enterRangeClause(RangeClauseContext ctx) {
        if (ctx.defeq != null) {
            if (ctx.e1 != null && ctx.e1.start == ParseTrees.getStopSymbol(ctx.e1)) {
                Token token = ctx.e1.start;
                TerminalNode startNode = ParseTrees.getStartNode(ctx.e1);
                pendingVisibleLocals.peek().put(token.getText(), startNode);
                treeDecorator.putProperty(startNode, GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
                treeDecorator.putProperty(startNode, GoAnnotations.VAR_TYPE, VarKind.LOCAL);
                treeDecorator.putProperty(startNode, GoAnnotations.IMPLICIT_TYPE, ctx);
                treeDecorator.putProperty(startNode, GoAnnotations.IMPLICIT_INDEX, 0);
            }

            if (ctx.e2 != null && ctx.e2.start == ParseTrees.getStopSymbol(ctx.e2)) {
                Token token = ctx.e2.start;
                TerminalNode startNode = ParseTrees.getStartNode(ctx.e2);
                pendingVisibleLocals.peek().put(token.getText(), startNode);
                treeDecorator.putProperty(startNode, GoAnnotations.NODE_TYPE, NodeType.VAR_DECL);
                treeDecorator.putProperty(startNode, GoAnnotations.VAR_TYPE, VarKind.LOCAL);
                treeDecorator.putProperty(startNode, GoAnnotations.IMPLICIT_TYPE, ctx);
                treeDecorator.putProperty(startNode, GoAnnotations.IMPLICIT_INDEX, 1);
            }
        }
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_rangeClause, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void exitRangeClause(RangeClauseContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.e != null) {
            exprType = new RangeClauseResultReference(treeDecorator.getProperty(ctx.e, GoAnnotations.EXPR_TYPE));
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
        applyPendingVars();
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_operand, version=0)
    public void enterOperand(OperandContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_operand, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literal, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_qualifiedIdentifier, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodExpr, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void exitOperand(OperandContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.literal() != null) {
            exprType = treeDecorator.getProperty(ctx.literal(), GoAnnotations.EXPR_TYPE);
        } else if (ctx.qualifiedIdentifier() != null) {
            exprType = new QualifiedIdentifierElementReference(ctx.qualifiedIdentifier());
        } else if (ctx.methodExpr() != null) {
            exprType = treeDecorator.getProperty(ctx.methodExpr(), GoAnnotations.EXPR_TYPE);
        } else if (ctx.expression() != null) {
            exprType = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);

        if (ctx.qualifiedIdentifier() != null) {
            treeDecorator.putProperties(ctx, treeDecorator.getProperties(ctx.qualifiedIdentifier()));
        } else {
            LOGGER.log(Level.FINER, "Expression resolution links are not supported for context: {0}", ctx.toString(Arrays.asList(GoParser.ruleNames)));
        }
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_argumentList, version=0)
    public void enterArgumentList(ArgumentListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_argumentList, version=0)
    public void exitArgumentList(ArgumentListContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchStmt, version=0)
    public void enterTypeSwitchStmt(TypeSwitchStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSwitchStmt, version=0)
    public void exitTypeSwitchStmt(TypeSwitchStmtContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeDecl, version=0)
    public void enterTypeDecl(TypeDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeDecl, version=0)
    public void exitTypeDecl(TypeDeclContext ctx) {
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void enterUnaryExpr(UnaryExprContext ctx) {
    }

    @Override
    @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1)
    public void exitUnaryExpr(UnaryExprContext ctx) {
        CodeElementReference exprType = CodeElementReference.MISSING;
        if (ctx.expression() != null && ctx.op != null) {
            CodeElementReference e = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
            exprType = new UnaryExpressionTypeReference(e, ctx.op);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channel, version=0)
    public void enterChannel(ChannelContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channel, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
    })
    public void exitChannel(ChannelContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.expression() != null) {
            exprType = treeDecorator.getProperty(ctx.expression(), GoAnnotations.EXPR_TYPE);
            assert exprType != CodeElementReference.MISSING;
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    //@RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literal, version=0)
    public void enterLiteral(LiteralContext ctx) {
    }

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literal, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_basicLiteral, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_compositeLiteral, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionLiteral, version=0),
    })
    public void exitLiteral(LiteralContext ctx) {
        CodeElementReference exprType = CodeElementReference.UNKNOWN;
        if (ctx.basicLiteral() != null) {
            exprType = treeDecorator.getProperty(ctx.basicLiteral(), GoAnnotations.EXPR_TYPE);
        } else if (ctx.compositeLiteral() != null) {
            exprType = treeDecorator.getProperty(ctx.compositeLiteral(), GoAnnotations.EXPR_TYPE);
        } else if (ctx.functionLiteral() != null) {
            exprType = treeDecorator.getProperty(ctx.functionLiteral(), GoAnnotations.EXPR_TYPE);
        }

        treeDecorator.putProperty(ctx, GoAnnotations.EXPR_TYPE, exprType);
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        Token symbol = node.getSymbol();
        switch (symbol.getType()) {
        case GoParser.Const:
            _iota = BigInteger.ZERO;
            break;

        default:
            break;
        }
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
    }

    private static final Set<Class<? extends ParserRuleContext>> EXPR_TYPE_CONTEXTS =
        new HashSet<Class<? extends ParserRuleContext>>() {{
            add(ExpressionContext.class);
            add(ConversionOrCallExprContext.class);
            add(BuiltinCallContext.class);
            add(SelectorExprContext.class);
            add(IndexExprContext.class);
            add(SliceExprContext.class);
            add(TypeAssertionExprContext.class);
            add(CallExprContext.class);
            add(UnaryExprContext.class);
            add(MultExprContext.class);
            add(AddExprContext.class);
            add(CompareExprContext.class);
            add(AndExprContext.class);
            add(OrExprContext.class);
            add(OperandContext.class);
            add(LiteralContext.class);
            add(MethodExprContext.class);
            add(BasicLiteralContext.class);
            add(CompositeLiteralContext.class);
            add(FunctionLiteralContext.class);
            add(LiteralValueContext.class);
            add(ValueContext.class);
            add(ElementNameOrIndexContext.class);
            add(ConversionContext.class);
            add(ArrayLengthContext.class);
            add(ChannelContext.class);
            add(ConditionContext.class);
            add(RecvExprContext.class);
            add(RangeClauseContext.class);
        }};

    private static final Set<Class<? extends ParserRuleContext>> CODE_CLASS_CONTEXTS =
        new HashSet<Class<? extends ParserRuleContext>>() {{
            add(TypeContext.class);
            add(TypeNameContext.class);
            add(TypeLiteralContext.class);
            add(ArrayTypeContext.class);
            add(StructTypeContext.class);
            add(PointerTypeContext.class);
            add(FunctionTypeContext.class);
            add(InterfaceTypeContext.class);
            add(SliceTypeContext.class);
            add(MapTypeContext.class);
            add(ChannelTypeContext.class);
            add(ElementTypeContext.class);
            add(BaseTypeContext.class);
            add(KeyTypeContext.class);
            add(LiteralTypeContext.class);
        }};

    @Override
    @RuleDependencies({
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_expression, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_builtinCall, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_operand, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literal, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodExpr, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_basicLiteral, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_compositeLiteral, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionLiteral, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalValue, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_value, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementNameOrIndex, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_conversion, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayLength, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channel, version=1),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_condition, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_recvExpr, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_rangeClause, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeName, version=0, dependents=Dependents.SELF),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeLiteral, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_pointerType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sliceType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mapType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channelType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_keyType, version=0),
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_literalType, version=0),
    })
    @SuppressWarnings("element-type-mismatch")
    public void exitEveryRule(ParserRuleContext ctx) {
        if (EXPR_TYPE_CONTEXTS.contains(ctx.getClass())) {
            if (treeDecorator.getProperty(ctx, GoAnnotations.EXPR_TYPE) == CodeElementReference.MISSING) {
                LOGGER.log(Level.WARNING, "Expected EXPR_TYPE data for context {0}.", ctx.getClass().getSimpleName());
            }
        }

        if (CODE_CLASS_CONTEXTS.contains(ctx.getClass())) {
            if (treeDecorator.getProperty(ctx, GoAnnotations.CODE_CLASS) == CodeElementReference.MISSING) {
                LOGGER.log(Level.WARNING, "Expected CODE_TYPE data for context {0}.", ctx.getClass().getSimpleName());
            }
        }
    }

    private void pushVarScope() {
        visibleLocals.push(new HashMap<String, TerminalNode>());
        visibleConstants.push(new HashMap<String, TerminalNode>());
        visibleFunctions.push(new HashMap<String, TerminalNode>());
        visibleTypes.push(new HashMap<String, TerminalNode>());

        pendingVisibleLocals.push(new HashMap<String, TerminalNode>());
        pendingVisibleConstants.push(new HashMap<String, TerminalNode>());
    }

    private void popVarScope() {
        visibleLocals.pop();
        visibleConstants.pop();
        visibleFunctions.pop();
        visibleTypes.pop();

        assert pendingVisibleLocals.peek().isEmpty();
        assert pendingVisibleConstants.peek().isEmpty();
        pendingVisibleLocals.pop();
        pendingVisibleConstants.pop();
    }

    private void applyPendingVars() {
        visibleLocals.peek().putAll(pendingVisibleLocals.peek());
        pendingVisibleLocals.peek().clear();

        visibleConstants.peek().putAll(pendingVisibleConstants.peek());
        pendingVisibleConstants.peek().clear();
    }
    private TerminalNode getVisibleDeclaration(TerminalNode reference) {
        TerminalNode result = getVisibleLocal(reference);
        result = result != null ? result : getVisibleConstant(reference);
        result = result != null ? result : getVisibleFunction(reference);
        result = result != null ? result : getVisibleType(reference);
        return result;
    }

    private TerminalNode getVisibleLocal(TerminalNode reference) {
        return getVisibleElement(visibleLocals, reference);
    }

    private TerminalNode getVisibleConstant(TerminalNode reference) {
        return getVisibleElement(visibleConstants, reference);
    }

    private TerminalNode getVisibleFunction(TerminalNode reference) {
        return getVisibleElement(visibleFunctions, reference);
    }

    private TerminalNode getVisibleType(TerminalNode reference) {
        return getVisibleElement(visibleTypes, reference);
    }

    private TerminalNode getVisibleElement(Collection<Map<String, TerminalNode>> elements, TerminalNode reference) {
        for (Map<String, TerminalNode> localCollection : elements) {
            TerminalNode local = localCollection.get(reference.getText());
            if (local != null) {
                return local;
            }
        }

        return null;
    }

}
