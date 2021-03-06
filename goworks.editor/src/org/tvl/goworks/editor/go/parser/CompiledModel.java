/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.tvl.goworks.editor.go.parser;

import org.antlr.netbeans.editor.text.DocumentSnapshot;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.util.Parameters;

/**
 *
 * @author Sam Harwell
 */
public class CompiledModel {

    private final DocumentSnapshot snapshot;

    @NonNull
    private final CompiledFileModel result;

    public CompiledModel(DocumentSnapshot snapshot, CompiledFileModel result) {
        Parameters.notNull("result", result);

        this.snapshot = snapshot;
        this.result = result;
    }

    public DocumentSnapshot getSnapshot() {
        return snapshot;
    }

    @NonNull
    public CompiledFileModel getResult() {
        return result;
    }

}
