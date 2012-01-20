/*
 * [The "BSD license"]
 *  Copyright (c) 2012 Sam Harwell
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.tvl.goworks.editor.go.completion;

import java.util.Locale;
import javax.swing.text.Document;
import org.antlr.netbeans.editor.text.DocumentSnapshot;
import org.antlr.netbeans.editor.text.SnapshotPositionRegion;
import org.antlr.netbeans.editor.text.TrackingPositionRegion;
import org.antlr.netbeans.editor.text.VersionedDocument;
import org.antlr.netbeans.editor.text.VersionedDocumentUtilities;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.util.Parameters;

/**
 *
 * @author Sam Harwell
 */
public class DeclarationCompletionItem extends GoCompletionItem {
    private final Document document;
    private final TrackingPositionRegion applicableTo;

    private String leftText;

    public DeclarationCompletionItem(@NonNull Document document, @NonNull TrackingPositionRegion applicableTo) {
        Parameters.notNull("document", document);
        Parameters.notNull("applicableTo", applicableTo);
        this.document = document;
        this.applicableTo = applicableTo;
    }

    public Document getDocument() {
        return document;
    }

    public TrackingPositionRegion getApplicableTo() {
        return applicableTo;
    }

    @Override
    public boolean allowInitialSelection() {
        return false;
    }

    @Override
    protected String getSortTextImpl() {
        return getInsertPrefix().toString().toLowerCase(Locale.getDefault());
    }

    @Override
    public int getSortPriority() {
        return DECLARATION_SORT_PRIORITY;
    }

    @Override
    public CharSequence getInsertPrefix() {
        VersionedDocument textBuffer = VersionedDocumentUtilities.getVersionedDocument(getDocument());
        DocumentSnapshot snapshot = textBuffer.getCurrentSnapshot();
        SnapshotPositionRegion applicableSpan = getApplicableTo().getRegion(snapshot);
        return applicableSpan.getText();
    }

    @Override
    protected String getLeftHtmlText() {
        if (leftText == null) {
            StringBuilder builder = new StringBuilder();

            builder.append(METHOD_COLOR);
            builder.append(getInsertPrefix());
            builder.append(COLOR_END);

            leftText = builder.toString();
        }

        return leftText;
    }

    @Override
    protected String getRightHtmlText() {
        return "Label";
    }
}
