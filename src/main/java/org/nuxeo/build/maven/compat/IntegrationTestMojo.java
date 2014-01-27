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
 *     mguillaume, jcarsique
 */
package org.nuxeo.build.maven.compat;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 *
 * Store a summary file in case of issue during execution and testFailureIgnore
 * is false.
 *
 * @see VerifyMojo
 *
 */
@Mojo(name = "integration-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, threadSafe = true, //
requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class IntegrationTestMojo extends
        org.nuxeo.build.maven.IntegrationTestMojo {

}
