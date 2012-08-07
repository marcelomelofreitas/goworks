/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.tvl.goworks.editor.go.parser;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.misc.RuleDependencyChecker;
import org.antlr.works.editor.antlr4.parsing.DescriptiveErrorListener;
import org.tvl.goworks.editor.go.completion.GoCompletionQuery;
import org.tvl.goworks.editor.go.fold.DeclarationFoldScanner;
import org.tvl.goworks.editor.go.navigation.GoDeclarationsScanner;
import org.tvl.goworks.editor.go.semantics.QualifiedIdentifierElementReference;
import org.tvl.goworks.editor.go.semantics.SemanticAnalyzerListener;
import org.tvl.goworks.editor.go.semantics.SemanticAnalyzerParseTreeWalker;
import org.tvl.goworks.editor.go.semantics.SemanticCorrections;

/**
 *
 * @author Sam Harwell
 */
public class GoParserCache extends AbstractGoParserCache {
    public static final GoParserCache DEFAULT = new GoParserCache();

    private static boolean dependenciesChecked;

    @Override
    protected GoParser createParser(TokenStream<? extends Token> input) {
        if (!dependenciesChecked) {
            checkDependencies();
        }

        GoParser parser = new GoParserWrapper(input);
        return parser;
    }

    @Override
    public GoParser getParser(TokenStream<? extends Token> input) {
        GoParser result = super.getParser(input);
        result.setBuildParseTree(false);
        result.setErrorHandler(new DefaultErrorStrategy<Token>());
        result.getInterpreter().disable_global_context = true;
        result.getInterpreter().always_try_local_context = true;
        result.getInterpreter().force_global_context = false;

        result.removeErrorListeners();
        result.addErrorListener(DescriptiveErrorListener.INSTANCE);

        return result;
    }

    private final class GoParserWrapper extends GoParser {

        public GoParserWrapper(TokenStream<? extends Token> input) {
            super(input);
            _interp = new GoParserATNSimulator(this, _ATN);
        }

    }

    private static final class GoParserATNSimulator extends AbstractGoParserATNSimulator {
        public GoParserATNSimulator(Parser<Token> parser, ATN atn) {
            super(parser, atn);
        }

        @Override
        public int adaptivePredict(TokenStream<? extends Token> input, int decision, ParserRuleContext<Token> outerContext) {
            assert disable_global_context : "Should use " + GoFullContextParserCache.class.getName() + " for full-context parsing.";
            return super.adaptivePredict(input, decision, outerContext);
        }
    }

    private static void checkDependencies() {
        RuleDependencyChecker.checkDependencies(CodeModelBuilderListener.class);
        RuleDependencyChecker.checkDependencies(CurrentDeclarationContextData.class);
        RuleDependencyChecker.checkDependencies(CurrentMemberContextParserTask.class);
        RuleDependencyChecker.checkDependencies(DeclarationFoldScanner.class);
        RuleDependencyChecker.checkDependencies(GoCompletionQuery.class);
        RuleDependencyChecker.checkDependencies(GoDeclarationsScanner.class);
        RuleDependencyChecker.checkDependencies(GoParserAnchorListener.class);
        RuleDependencyChecker.checkDependencies(SemanticAnalyzerListener.class);
        RuleDependencyChecker.checkDependencies(QualifiedIdentifierElementReference.class);
        RuleDependencyChecker.checkDependencies(SemanticAnalyzerParseTreeWalker.class);
        RuleDependencyChecker.checkDependencies(SemanticCorrections.class);
        dependenciesChecked = true;
    }
}
