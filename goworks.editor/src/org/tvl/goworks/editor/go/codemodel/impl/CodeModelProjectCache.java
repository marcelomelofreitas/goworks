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
package org.tvl.goworks.editor.go.codemodel.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;

/**
 *
 * @author Sam Harwell
 */
public class CodeModelProjectCache {
    @NullAllowed
    private final Project project;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, FileModelImpl> files =
        new HashMap<String, FileModelImpl>();
    private final Set<PackageModelImpl> packages =
        new HashSet<PackageModelImpl>();
    private final Map<String, PackageModelImpl> packagesByPath =
        new HashMap<String, PackageModelImpl>();
    private final Map<String, Collection<PackageModelImpl>> packagesByName =
        new HashMap<String, Collection<PackageModelImpl>>();

    public CodeModelProjectCache(@NullAllowed Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    @NonNull
    public Collection<PackageModelImpl> getPackages() {
        return lockedRead(new Callable<Collection<PackageModelImpl>>() {

            @Override
            public Collection<PackageModelImpl> call() throws Exception {
                return new ArrayList<PackageModelImpl>(packages);
            }

        });
    }

    @NonNull
    public Collection<PackageModelImpl> getPackages(final String name) {
        return lockedRead(new Callable<Collection<PackageModelImpl>>() {

            @Override
            public Collection<PackageModelImpl> call() throws Exception {
                return new ArrayList<PackageModelImpl>(packagesByName.get(name));
            }

        });
    }

    @CheckForNull
    public PackageModelImpl getUniquePackage(final String path) {
        return lockedRead(new Callable<PackageModelImpl>() {

            @Override
            public PackageModelImpl call() throws Exception {
                return packagesByPath.get(path);
            }

        });
    }

    public void updateFile(@NonNull final FileModelImpl fileModel) {
        assert fileModel.isFrozen();
        assert fileModel.getProject() == getProject();

        lockedWrite(new Runnable() {

            @Override
            public void run() {
                files.put(fileModel.getName(), fileModel);

                String packagePath = fileModel.getPackagePath();
                PackageModelImpl packageModel = getUniquePackage(packagePath);
                if (packageModel == null) {
                    String packageName = packagePath.substring(packagePath.lastIndexOf('/') + 1);
                    packageModel = new PackageModelImpl(packageName, project, packagePath);
                    packages.add(packageModel);
                    packagesByPath.put(packagePath, packageModel);

                    Collection<PackageModelImpl> set = packagesByName.get(packageName);
                    if (set == null) {
                        set = new HashSet<PackageModelImpl>();
                        packagesByName.put(packageName, set);
                    }

                    set.add(packageModel);
                }

                packageModel.updateFile(fileModel);
            }
            
        });
    }

    protected <T> T lockedRead(Callable<T> runnable) {
        Lock readLock = lock.readLock();
        boolean locked = false;
        try {
            readLock.lock();
            locked = true;
            return runnable.call();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            throw new RuntimeException(ex);
        } finally {
            if (locked) {
                readLock.unlock();
            }
        }
    }

    protected void lockedWrite(Runnable runnable) {
        Lock writeLock = lock.writeLock();
        boolean locked = false;
        try {
            writeLock.lock();
            locked = true;
            runnable.run();
        } finally {
            if (locked) {
                writeLock.unlock();
            }
        }
    }

}
