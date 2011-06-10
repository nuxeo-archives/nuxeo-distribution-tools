/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.build.ant.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class NuxeoExpandTask extends ExpandTask {

    {
        Filter scopeFilter = new Filter() {
            public boolean accept(Edge edge, Dependency dep) {
                final String depScope = dep.getScope();
                final String groupId = edge.dst.getArtifact().getGroupId();
                return "compile".equals(depScope)
                        || "runtime".equals(depScope)
                        || ("provided".equals(depScope) && groupId.startsWith(
                                "org.nuxeo"));
            }

            public boolean accept(Artifact artifact) {
                return true;
            }

            public boolean accept(Edge edge) {
                return true;
            }

            public boolean accept(Node node) {
                return true;
            }
        };
        filter.addFilter(scopeFilter);
        setDepth("all");
        setMavenExclusions(Boolean.TRUE);
    }

    protected boolean acceptNode(Node node) {
        return node.getArtifact().getGroupId().startsWith("org.nuxeo");
    }

}
