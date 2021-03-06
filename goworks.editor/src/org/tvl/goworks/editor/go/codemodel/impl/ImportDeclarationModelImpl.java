/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.tvl.goworks.editor.go.codemodel.impl;

import java.util.Collection;
import java.util.Collections;
import org.antlr.netbeans.editor.text.OffsetRegion;
import org.antlr.v4.runtime.ParserRuleContext;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.util.Parameters;
import org.tvl.goworks.editor.go.codemodel.CodeElementPositionRegion;
import org.tvl.goworks.editor.go.codemodel.ImportDeclarationModel;

/**
 *
 * @author Sam Harwell
 */
public class ImportDeclarationModelImpl extends AbstractCodeElementModel implements ImportDeclarationModel {
    private final String path;
    private final String alias;
    private final boolean mergeWithLocal;
    private final OffsetRegion span;

    public ImportDeclarationModelImpl(@NonNull String path, @NullAllowed String alias, boolean mergeWithLocal, @NonNull FileModelImpl file, ParserRuleContext span) {
        super(getAlias(path, alias), file);
        this.path = path;
        this.alias = alias;
        this.mergeWithLocal = mergeWithLocal;
        this.span = getOffsetRegion(span);
    }

    @Override
    public CodeElementPositionRegion getSeek() {
        // super returns beginning position of span by default
        return super.getSeek();
    }

    @Override
    public CodeElementPositionRegion getSpan() {
        if (this.span == null) {
            return super.getSpan();
        }

        return new CodeElementPositionRegionImpl(this, span);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isMergeWithLocal() {
        return mergeWithLocal;
    }

    @Override
    public Collection<? extends AbstractCodeElementModel> getMembers() {
        return Collections.emptyList();
    }

    private static String getAlias(@NonNull String path, @NullAllowed String alias) {
        Parameters.notNull("path", path);

        if (alias == null || alias.isEmpty()) {
            alias = path.substring(path.lastIndexOf('/') + 1);
            int start = alias.startsWith("\"") ? 1 : 0;
            int end = alias.length() - start - (alias.endsWith("\"") ? 1 : 0);
            alias = alias.substring(start, end);
        }

        return alias;
    }
}
