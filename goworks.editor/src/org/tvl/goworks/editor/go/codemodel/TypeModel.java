/*
 *  Copyright (c) 2012 Sam Harwell, Tunnel Vision Laboratories LLC
 *  All rights reserved.
 *
 *  The source code of this document is proprietary work, and is not licensed for
 *  distribution. For information about licensing, contact Sam Harwell at:
 *      sam@tunnelvisionlabs.com
 */
package org.tvl.goworks.editor.go.codemodel;

import java.util.Collection;

/**
 *
 * @author Sam Harwell
 */
public interface TypeModel extends CodeElementModel {

    String getSimpleName();

    TypeKind getKind();

    Collection<? extends FieldModel> getFields();

    Collection<? extends FieldModel> getFields(String name);

    Collection<? extends FunctionModel> getMethods();

    Collection<? extends FunctionModel> getMethods(String name);

}
