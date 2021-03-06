/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.antlr.works.editor.grammar.codemodel;

import java.util.Collection;
import org.netbeans.api.annotations.common.NonNull;

/**
 *
 * @author Sam Harwell
 */
public interface FileModel extends CodeElementModel {

    @NonNull
    Collection<? extends ImportDeclarationModel> getImportDeclarations();

    @NonNull
    Collection<? extends TokenVocabDeclarationModel> getTokenVocabDeclaration();

    @NonNull
    Collection<? extends ChannelModel> getChannels();

    @NonNull
    Collection<? extends ChannelModel> getChannels(String name);

    @NonNull
    Collection<? extends ModeModel> getModes();

    @NonNull
    Collection<? extends ModeModel> getModes(String name);

    @NonNull
    Collection<? extends RuleModel> getRules();

    @NonNull
    Collection<? extends RuleModel> getRules(String name);

    @NonNull
    TokenVocabModel getVocabulary();

}
