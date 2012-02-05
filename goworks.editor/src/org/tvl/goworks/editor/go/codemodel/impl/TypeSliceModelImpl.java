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
import org.tvl.goworks.editor.go.codemodel.TypeKind;
import org.tvl.goworks.editor.go.codemodel.TypeSliceModel;

/**
 *
 * @author Sam Harwell
 */
public class TypeSliceModelImpl extends TypeWrapperModelImpl implements TypeSliceModel {

    public TypeSliceModelImpl(TypeModelImpl elementType, FileModelImpl fileModel) {
        super("[]" + elementType.getName(), elementType, fileModel);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.SLICE;
    }

    @Override
    public String getSimpleName() {
        return "[]" + getElementType().getSimpleName();
    }

    @Override
    public Collection<? extends AbstractCodeElementModel> getMembers() {
        return Collections.emptyList();
    }

}
