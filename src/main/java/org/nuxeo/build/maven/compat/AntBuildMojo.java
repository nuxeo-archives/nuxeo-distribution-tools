/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu, jcarsique, slacoin
 */

package org.nuxeo.build.maven.compat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * @deprecated Since 2.0. Use ant-assembly-maven-plugin instead.
 * @see org.nuxeo.build.maven.AntBuildMojo
 */
@Mojo(name = "build", threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE, //
requiresDependencyCollection = ResolutionScope.TEST, //
requiresDependencyResolution = ResolutionScope.TEST)
@Deprecated
public class AntBuildMojo extends org.nuxeo.build.maven.AntBuildMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().warn(
                "The nuxeo-distribution-tools plugin is DEPRECATED, please use ant-assembly-maven-plugin instead.");
        super.execute();
    }
}
