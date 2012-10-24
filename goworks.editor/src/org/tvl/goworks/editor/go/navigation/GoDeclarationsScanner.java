/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.tvl.goworks.editor.go.navigation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.netbeans.editor.navigation.Description;
import org.antlr.netbeans.editor.navigation.NavigatorPanelUI;
import org.antlr.netbeans.editor.text.DocumentSnapshot;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleDependencies;
import org.antlr.v4.runtime.RuleDependency;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Tuple;
import org.antlr.v4.runtime.misc.Tuple3;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.works.editor.antlr4.parsing.ParseTrees;
import org.tvl.goworks.editor.go.navigation.GoNode.DeclarationDescription;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ArrayTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.BaseTypeNameContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.BlockContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.BodyContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ChannelTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ConstSpecContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.FieldDeclContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.FunctionDeclContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.FunctionTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.IdentifierListContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.InterfaceTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.InterfaceTypeNameContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.MapTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.MethodDeclContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.MethodNameContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.MethodSpecContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ParameterDeclContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ParameterListContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ParametersContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.PointerTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ReceiverContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ResultContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.ShortVarDeclContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.SignatureContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.SliceTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.SourceFileContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.StructTypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.TypeContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.TypeLiteralContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.TypeNameContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.TypeSpecContext;
import org.tvl.goworks.editor.go.parser.AbstractGoParser.VarSpecContext;
import org.tvl.goworks.editor.go.parser.CompiledFileModel;
import org.tvl.goworks.editor.go.parser.CompiledModel;
import org.tvl.goworks.editor.go.parser.GoParser;
import org.tvl.goworks.editor.go.parser.GoParserBaseListener;
import org.tvl.goworks.editor.go.parser.GoParserBaseVisitor;

/**
 *
 * @author Sam Harwell
 */
public class GoDeclarationsScanner {
    // -J-Dorg.tvl.goworks.editor.go.navigation.GoDeclarationsScanner.level=FINE
    private static final Logger LOGGER = Logger.getLogger(GoDeclarationsScanner.class.getName());

    public Description scan(CompiledModel model) {
        GoDeclarationsPanel panel = GoDeclarationsPanel.getInstance();
        GoDeclarationsPanelUI ui = panel != null ? panel.getComponent() : null;
        if (ui == null) {
            return null;
        }

        GoNode.DeclarationDescription rootDescription = scan(ui, model);
        return rootDescription;
    }

    public GoNode.DeclarationDescription scan(NavigatorPanelUI ui, CompiledModel model) {
        try {
            // don't update if there were errors and a result is already displayed
            /*if (!result.getParser().getSyntaxErrors().isEmpty() && !ui.isShowingWaitNode()) {
                return;
            }*/

            GoNode.DeclarationDescription rootDescription = new GoNode.DeclarationDescription();
            rootDescription.setChildren(new ArrayList<Description>());
            rootDescription.setFileObject(model.getSnapshot().getVersionedDocument().getFileObject());

//            for (CompiledFileModel importedParseResult : model.getImportedGroupResults()) {
//                processParseResult(null, importedParseResult, ui, rootDescription);
//            }

            processParseResult(model.getSnapshot(), model.getResult(), ui, rootDescription);
            return rootDescription;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "An exception occurred while scanning for declarations.", ex);
            return null;
        }
    }

    private void processParseResult(DocumentSnapshot snapshot,
                                    CompiledFileModel result,
                                    NavigatorPanelUI ui,
                                    GoNode.DeclarationDescription rootDescription) {

        SourceFileContext parseResult = result.getResult();
        if (parseResult != null) {
            DeclarationsScannerListener listener = new DeclarationsScannerListener(snapshot, rootDescription);
            ParseTreeWalker.DEFAULT.walk(listener, parseResult);
        }
    }

    private static class DeclarationsScannerListener extends GoParserBaseListener {
        private final DocumentSnapshot snapshot;
        private final Deque<DeclarationDescription> descriptionStack = new ArrayDeque<DeclarationDescription>();
        private final Deque<String> typeNameStack = new ArrayDeque<String>();

        private final Map<String, Description> _typeDescriptions = new HashMap<String, Description>();
        /**
         * Name -> Parent Node -> Method Node
         */
        private final List<Tuple3<String, ? extends Description, ? extends Description>> _methodDescriptions =
            new ArrayList<Tuple3<String, ? extends Description, ? extends Description>>();

        private int resultLevel;
        private int blockLevel;

        public DeclarationsScannerListener(DocumentSnapshot snapshot, DeclarationDescription rootDescription) {
            this.snapshot = snapshot;
            this.descriptionStack.push(rootDescription);
        }

        public DeclarationDescription getCurrentParent() {
            return descriptionStack.peek();
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sourceFile, version=1)
        public void exitSourceFile(SourceFileContext ctx) {
            for (Tuple3<String, ? extends Description, ? extends Description> pair : _methodDescriptions) {
                Description typeDescription = _typeDescriptions.get(pair.getItem1());
                if (typeDescription == null) {
                    continue;
                }

                Description parent = pair.getItem2();
                parent.getChildren().remove(pair.getItem3());
                typeDescription.getChildren().add(pair.getItem3());
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_constSpec, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        })
        public void enterConstSpec(ConstSpecContext ctx) {
            if (!isTopLevel(ctx)) {
                return;
            }

            IdentifierListContext idListContext = ctx.identifierList();
            List<? extends TerminalNode<Token>> identifiers = idListContext.IDENTIFIER();
            for (TerminalNode<Token> identifier : identifiers) {
                Interval sourceInterval = new Interval(identifier.getSymbol().getStartIndex(), ParseTrees.getStopSymbol(ctx).getStopIndex());
                String signature = String.format("%s", identifier.getSymbol().getText());

                GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.CONSTANT);
                description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
                description.setHtmlHeader(String.format("%s", Description.htmlEscape(signature)));
                getCurrentParent().getChildren().add(description);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_varSpec, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        })
        public void enterVarSpec(VarSpecContext ctx) {
            // no locals in navigator
            if (blockLevel > 0) {
                return;
            }

            IdentifierListContext idListContext = ctx.identifierList();
            List<? extends TerminalNode<Token>> identifiers = idListContext.IDENTIFIER();
            String type = ctx.type() != null ? String.format(" : <font color='808080'>%s</font>", HtmlSignatureVisitor.UNCOLORED.visit(ctx.type())) : "";
            for (TerminalNode<Token> identifier : identifiers) {
                Interval sourceInterval = new Interval(identifier.getSymbol().getStartIndex(), ParseTrees.getStopSymbol(ctx).getStopIndex());
                String signature = identifier.getSymbol().getText() + type;

                GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.VARIABLE);
                description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
                description.setHtmlHeader(signature);
                getCurrentParent().getChildren().add(description);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_shortVarDecl, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        })
        public void enterShortVarDecl(ShortVarDeclContext ctx) {
            // no locals in navigator
            if (blockLevel > 0) {
                return;
            }

            IdentifierListContext idListContext = ctx.identifierList();
            List<? extends TerminalNode<Token>> identifiers = idListContext.IDENTIFIER();
            for (TerminalNode<Token> identifier : identifiers) {
                Interval sourceInterval = new Interval(identifier.getSymbol().getStartIndex(), ParseTrees.getStopSymbol(ctx).getStopIndex());
                String signature = String.format("%s", identifier.getSymbol().getText());

                GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.VARIABLE);
                description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
                description.setHtmlHeader(String.format("%s", Description.htmlEscape(signature)));
                getCurrentParent().getChildren().add(description);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_fieldDecl, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        })
        public void enterFieldDecl(FieldDeclContext ctx) {
            if (ctx.getParent() == null || isAnonymousType((StructTypeContext)ctx.getParent())) {
                return;
            }

            IdentifierListContext idListContext = ctx.identifierList();
            if (idListContext != null) {
                List<? extends TerminalNode<Token>> identifiers = idListContext.IDENTIFIER();
                String type = ctx.type() != null ? String.format(" : <font color='808080'>%s</font>", HtmlSignatureVisitor.UNCOLORED.visit(ctx.type())) : "";
                for (TerminalNode<Token> identifier : identifiers) {
                    Interval sourceInterval = new Interval(identifier.getSymbol().getStartIndex(), ParseTrees.getStopSymbol(ctx).getStopIndex());
                    String signature = identifier.getSymbol().getText() + type;

                    GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.FIELD);
                    description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
                    description.setHtmlHeader(signature);
                    getCurrentParent().getChildren().add(description);
                }
            }
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0)
        public void enterInterfaceType(InterfaceTypeContext ctx) {
            if (isAnonymousType(ctx)) {
                return;
            }

            Interval sourceInterval = ParseTrees.getSourceInterval(ctx);
            String signature = typeNameStack.isEmpty() ? "?interface?" : typeNameStack.peek();

            GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.INTERFACE);
            description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
            description.setHtmlHeader(String.format("%s", signature));
            getCurrentParent().getChildren().add(description);
            description.setChildren(new ArrayList<Description>());
            descriptionStack.push(description);
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0)
        public void exitInterfaceType(InterfaceTypeContext ctx) {
            if (isAnonymousType(ctx)) {
                return;
            }

            descriptionStack.pop();
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0)
        public void enterStructType(StructTypeContext ctx) {
            if (isAnonymousType(ctx)) {
                return;
            }

            Interval sourceInterval = ParseTrees.getSourceInterval(ctx);
            String signature = typeNameStack.isEmpty() ? "?struct?" : typeNameStack.peek();

            GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.STRUCT);
            description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
            description.setHtmlHeader(String.format("%s", signature));
            getCurrentParent().getChildren().add(description);
            description.setChildren(new ArrayList<Description>());
            descriptionStack.push(description);
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0)
        public void exitStructType(StructTypeContext ctx) {
            if (isAnonymousType(ctx)) {
                return;
            }

            if (isTopLevel(ctx)) {
                String name = descriptionStack.peek().getName();
                if (!_typeDescriptions.containsKey(name)) {
                    _typeDescriptions.put(name, descriptionStack.peek());
                }
            }

            descriptionStack.pop();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodSpec, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceTypeName, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeName, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodName, version=0),
        })
        public void enterMethodSpec(MethodSpecContext ctx) {
            if (ctx.getParent() == null || isAnonymousType((InterfaceTypeContext)ctx.getParent())) {
                return;
            }

            if (ctx.interfaceTypeName() != null) {
                InterfaceTypeNameContext interfaceTypeNameContext = ctx.interfaceTypeName();
                Interval sourceInterval = ParseTrees.getSourceInterval(ctx);
                String name = interfaceTypeNameContext.typeName() != null ? interfaceTypeNameContext.typeName().getText() : "?";
                String signature = name;

                GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.INTERFACE);
                description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
                description.setHtmlHeader(String.format("%s", Description.htmlEscape(signature)));
                getCurrentParent().getChildren().add(description);
                description.setChildren(new ArrayList<Description>());
                descriptionStack.push(description);
            } else if (ctx.methodName() != null) {
                MethodNameContext methodNameContext = ctx.methodName();
                Interval sourceInterval = ParseTrees.getSourceInterval(ctx);
                String name = methodNameContext.IDENTIFIER() != null ? methodNameContext.IDENTIFIER().getText() : "?";
                String signature = HtmlSignatureVisitor.COLORED.visit(ctx);

                GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.METHOD);
                description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
                description.setHtmlHeader(signature);
                getCurrentParent().getChildren().add(description);
                description.setChildren(new ArrayList<Description>());
                descriptionStack.push(description);
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodSpec, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceTypeName, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodName, version=0),
        })
        public void exitMethodSpec(MethodSpecContext ctx) {
            if (ctx.getParent() == null || isAnonymousType((InterfaceTypeContext)ctx.getParent())) {
                return;
            }

            if (ctx.interfaceTypeName() != null || ctx.methodName() != null) {
                descriptionStack.pop();
            }
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionDecl, version=0)
        public void enterFunctionDecl(FunctionDeclContext ctx) {
            Interval sourceInterval = ParseTrees.getSourceInterval(ctx);
            String name = ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : "?";
            String signature = HtmlSignatureVisitor.COLORED.visit(ctx);

            GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.FUNCTION);
            description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
            description.setHtmlHeader(signature);
            getCurrentParent().getChildren().add(description);
            description.setChildren(new ArrayList<Description>());
            descriptionStack.push(description);
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionDecl, version=0)
        public void exitFunctionDecl(FunctionDeclContext ctx) {
            descriptionStack.pop();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodDecl, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodName, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_receiver, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseTypeName, version=0),
        })
        public void enterMethodDecl(MethodDeclContext ctx) {
            Interval sourceInterval = ParseTrees.getSourceInterval(ctx);
            String name = ctx.methodName() != null && ctx.methodName().IDENTIFIER() != null ? ctx.methodName().IDENTIFIER().getSymbol().getText() : "?";
            String signature = HtmlSignatureVisitor.COLORED.visit(ctx);

            GoNode.DeclarationDescription description = new GoNode.DeclarationDescription(signature, DeclarationKind.METHOD);
            description.setOffset(snapshot, getCurrentParent().getFileObject(), sourceInterval.a);
            description.setHtmlHeader(signature);
            getCurrentParent().getChildren().add(description);
            description.setChildren(new ArrayList<Description>());

            ReceiverContext receiverContext = ctx.receiver();
            BaseTypeNameContext baseTypeNameContext = receiverContext != null ? receiverContext.baseTypeName() : null;
            if (baseTypeNameContext != null && baseTypeNameContext.IDENTIFIER() != null) {
                String receiverTypeName = baseTypeNameContext.IDENTIFIER().getText();
                _methodDescriptions.add(Tuple.create(receiverTypeName, getCurrentParent(), description));
            }

            descriptionStack.push(description);
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodDecl, version=0)
        public void exitMethodDecl(MethodDeclContext ctx) {
            descriptionStack.pop();
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSpec, version=0)
        public void enterTypeSpec(TypeSpecContext ctx) {
            if (ctx.IDENTIFIER() != null) {
                typeNameStack.push(ctx.IDENTIFIER().getSymbol().getText());
            } else {
                typeNameStack.push("?");
            }
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSpec, version=0)
        public void exitTypeSpec(TypeSpecContext ctx) {
            typeNameStack.pop();
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_result, version=0)
        public void enterResult(ResultContext ctx) {
            resultLevel++;
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_result, version=0)
        public void exitResult(ResultContext ctx) {
            resultLevel--;
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_block, version=0)
        public void enterBlock(BlockContext ctx) {
            blockLevel++;
        }

        @Override
        @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_block, version=0)
        public void exitBlock(BlockContext ctx) {
            blockLevel--;
        }

        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeLiteral, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeSpec, version=0),
        })
        private boolean isAnonymousType(ParserRuleContext<Token> context) {
            if (!(context instanceof InterfaceTypeContext) && !(context instanceof StructTypeContext)) {
                throw new IllegalArgumentException();
            }

            if (context.getParent() == null) {
                return true;
            }

            TypeLiteralContext typeLiteralContext = (TypeLiteralContext)context.getParent();
            if (typeLiteralContext.getParent() == null) {
                return true;
            }

            TypeContext typeContext = (TypeContext)typeLiteralContext.getParent();
            if (!(typeContext.getParent() instanceof TypeSpecContext)) {
                return true;
            }

            return false;
        }

        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_constSpec, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_body, version=0),
        })
        private boolean isTopLevel(ConstSpecContext context) {
            if (ParseTrees.findAncestor(context, BodyContext.class) != null) {
                return false;
            }

            return true;
        }

        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_body, version=0),
        })
        private boolean isTopLevel(StructTypeContext context) {
            if (isAnonymousType(context)) {
                return false;
            }

            if (ParseTrees.findAncestor(context, BodyContext.class) != null) {
                return false;
            }

            return true;
        }
    }

    public static class HtmlSignatureVisitor extends GoParserBaseVisitor<String> {
        public static final HtmlSignatureVisitor COLORED = new HtmlSignatureVisitor(true);
        public static final HtmlSignatureVisitor UNCOLORED = new HtmlSignatureVisitor(false);

        private final boolean _colored;

        public HtmlSignatureVisitor(boolean colored) {
            this._colored = colored;
        }

        @Override
        protected String defaultResult() {
            return "";
        }

        @Override
        protected String aggregateResult(String aggregate, String nextResult) {
            if (aggregate == null || aggregate.isEmpty()) {
                return nextResult;
            } else if (nextResult == null || nextResult.isEmpty()) {
                return aggregate;
            }

            return aggregate + ", " + nextResult;
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionDecl, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_signature, version=0),
        })
        public String visitFunctionDecl(FunctionDeclContext ctx) {
            // name(args) return
            StringBuilder result = new StringBuilder();
            if (ctx.IDENTIFIER() != null) {
                result.append(Description.htmlEscape(ctx.IDENTIFIER().getText()));
            }

            if (ctx.signature() != null) {
                result.append(visit(ctx.signature()));
            }

            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodDecl, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodName, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_signature, version=0),
        })
        public String visitMethodDecl(MethodDeclContext ctx) {
            // name(args) return
            StringBuilder result = new StringBuilder();
            if (ctx.methodName() != null) {
                result.append(Description.htmlEscape(ctx.methodName().getText()));
            }

            if (ctx.signature() != null) {
                result.append(visit(ctx.signature()));
            }

            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodSpec, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceTypeName, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodName, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_signature, version=0),
        })
        public String visitMethodSpec(MethodSpecContext ctx) {
            if (ctx.interfaceTypeName() != null) {
                return Description.htmlEscape(ctx.interfaceTypeName().getText());
            }

            StringBuilder result = new StringBuilder();
            if (ctx.methodName() != null) {
                result.append(Description.htmlEscape(ctx.methodName().getText()));
            }

            if (ctx.signature() != null) {
                result.append(visit(ctx.signature()));
            }

            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_signature, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameters, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_result, version=0),
        })
        public String visitSignature(SignatureContext ctx) {
            StringBuilder result = new StringBuilder();
            if (ctx.parameters() != null) {
                result.append(visit(ctx.parameters()));
            }

            result.append(' ');
            if (ctx.result() != null) {
                result.append(visit(ctx.result()));
            }

            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameters, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameterList, version=0),
        })
        public String visitParameters(ParametersContext ctx) {
            StringBuilder result = new StringBuilder();
            result.append('(');

            if (ctx.parameterList() != null) {
                result.append(visit(ctx.parameterList()));
            }

            result.append(')');
            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameterList, version=0),
        })
        public String visitParameterList(ParameterListContext ctx) {
            // default impl does the right thing
            return super.visitParameterList(ctx);
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameterDecl, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0),
        })
        public String visitParameterDecl(ParameterDeclContext ctx) {
            StringBuilder result = new StringBuilder();
            if (ctx.identifierList() != null) {
                result.append(visit(ctx.identifierList()));
            }

            result.append(' ');

            if (_colored) {
                result.append("<font color='808080'>");
            }

            if (ctx.ellip != null) {
                result.append("...");
            }

            if (ctx.type() != null) {
                result.append(UNCOLORED.visit(ctx.type()));
            }

            if (_colored) {
                result.append("</font>");
            }

            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_identifierList, version=0),
        })
        public String visitIdentifierList(IdentifierListContext ctx) {
            StringBuilder result = new StringBuilder();
            for (TerminalNode<Token> node : ctx.IDENTIFIER()) {
                if (result.length() > 0) {
                    result.append(", ");
                }

                result.append(Description.htmlEscape(node.getText()));
            }

            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0),
        })
        public String visitType(TypeContext ctx) {
            // default impl does the right thing
            return super.visitType(ctx);
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeName, version=0),
        })
        public String visitTypeName(TypeNameContext ctx) {
            return Description.htmlEscape(ctx.getText());
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_typeLiteral, version=0),
        })
        public String visitTypeLiteral(TypeLiteralContext ctx) {
            // default impl does the right thing
            return super.visitTypeLiteral(ctx);
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_arrayType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public String visitArrayType(ArrayTypeContext ctx) {
            if (ctx.elementType() == null) {
                return "[*]?";
            }

            return "[*]" + visit(ctx.elementType());
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_structType, version=0),
        })
        public String visitStructType(StructTypeContext ctx) {
            return "struct{?}";
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_pointerType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_baseType, version=0),
        })
        public String visitPointerType(PointerTypeContext ctx) {
            if (ctx.baseType() == null) {
                return "*?";
            }

            return "*" + visit(ctx.baseType());
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_functionType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_signature, version=0),
        })
        public String visitFunctionType(FunctionTypeContext ctx) {
            if (ctx.signature() != null) {
                return "func" + UNCOLORED.visit(ctx.signature());
            } else {
                return "func?";
            }
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_interfaceType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_methodSpec, version=0),
        })
        public String visitInterfaceType(InterfaceTypeContext ctx) {
            if (ctx.methodSpec().isEmpty()) {
                return "interface{}";
            }

            return "interface{?}";
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_sliceType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public String visitSliceType(SliceTypeContext ctx) {
            if (ctx.elementType() == null) {
                return "[]?";
            }

            return "[]" + visit(ctx.elementType());
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_mapType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_keyType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public String visitMapType(MapTypeContext ctx) {
            String keyType = ctx.keyType() != null ? visit(ctx.keyType()) : "?";
            String elementType = ctx.elementType() != null ? visit(ctx.elementType()) : "?";
            return String.format("map[%s]%s", keyType, elementType);
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_channelType, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_elementType, version=0),
        })
        public String visitChannelType(ChannelTypeContext ctx) {
            StringBuilder result = new StringBuilder();
            if (ctx.recv != null) {
                result.append("&lt;-");
            }

            result.append("chan");
            if (ctx.send != null) {
                result.append("&lt;-");
            }

            result.append(' ');
            if (ctx.elementType() != null) {
                result.append(visit(ctx.elementType()));
            }

            return result.toString();
        }

        @Override
        @RuleDependencies({
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_result, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_parameters, version=0),
            @RuleDependency(recognizer=GoParser.class, rule=GoParser.RULE_type, version=0),
        })
        public String visitResult(ResultContext ctx) {
            if (ctx.parameters() != null) {
                return visit(ctx.parameters());
            } else if (ctx.type() != null) {
                if (_colored) {
                    return String.format("<font color='808080'>%s</font>", UNCOLORED.visit(ctx.type()));
                } else {
                    return visit(ctx.type());
                }
            } else {
                return "";
            }
        }

    }
}
