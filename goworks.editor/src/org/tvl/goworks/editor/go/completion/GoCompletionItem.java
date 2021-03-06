/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.tvl.goworks.editor.go.completion;

import org.antlr.works.editor.antlr4.completion.AbstractCompletionItem;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.tvl.goworks.editor.go.codemodel.CodeElementModel;

/**
 *
 * @author Sam Harwell
 */
public abstract class GoCompletionItem extends AbstractCompletionItem {

    public static final int KEYWORD_SORT_PRIORITY = 100;
    public static final int PACKAGE_SORT_PRIORITY = 100;
    public static final int TYPE_SORT_PRIORITY = 100;
    public static final int LABEL_SORT_PRIORITY = 100;
    public static final int RULE_SORT_PRIORITY = 100;
    public static final int ELEMENT_REFERENCE_SORT_PRIORITY = 100;
    public static final int PROPERTY_SORT_PRIORITY = 100;
    public static final int MEMBER_SORT_PRIORITY = 100;
    public static final int DECLARATION_SORT_PRIORITY = -100;

    public static final String KEYWORD_COLOR = "<font color=#000099>"; //NOI18N
    public static final String FIELD_COLOR = "<font color=#008618>"; //NOI18N
    public static final String METHOD_COLOR = "<font color=#000000>"; //NOI18N
    public static final String PARAMETER_NAME_COLOR = "<font color=#a06001>"; //NOI18N
    public static final String PARAMETER_TYPE_COLOR = "<font color=#000000>"; //NOI18N
    public static final String REFERENCE_COLOR = "<font color=#a06001>"; //NOI18N
    public static final String PACKAGE_COLOR = "<font color=#a06001>"; //NOI18N
    public static final String TYPE_COLOR = "<font color=#2B91AF>"; //NOI18N
    public static final String LABEL_COLOR = "<font color=#000000>"; //NOI18N

    @CheckForNull
    public CodeElementModel getCodeElementModel() {
        return null;
    }

    @NonNull
    public abstract String getToolTipText();

}
