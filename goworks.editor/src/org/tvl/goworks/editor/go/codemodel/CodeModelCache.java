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
import org.netbeans.api.project.Project;

/**
 *
 * @author Sam Harwell
 */
public interface CodeModelCache {

    public Collection<? extends PackageModel> getPackages(Project project);

    public Collection<? extends PackageModel> getPackages(Project project, String path);

}
