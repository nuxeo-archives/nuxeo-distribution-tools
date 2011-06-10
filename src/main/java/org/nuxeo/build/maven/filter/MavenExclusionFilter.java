/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.build.maven.filter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author matic
 *
 */
public class MavenExclusionFilter implements Filter {
          
    protected boolean recurseAccept(Edge edge, Dependency dep) {
        if (edge == null) {
           return true;
        }
        if (MavenClientFactory.getLog().isDebugEnabled()) {
            MavenClientFactory.getLog().debug(
                     "Evaluating Exclusion Filter  - " + edge + " against " + dep.getArtifactId());
        }
        for (Exclusion e:edge.exclusions) { 
            String artifactId = e.getArtifactId();
            if (artifactId == null) {
                artifactId = dep.getArtifactId();
            }
            String groupId = e.getGroupId();
            if (groupId == null) {
                groupId = dep.getGroupId();
            }
            if (dep.getArtifactId().equals(artifactId) &&
                    dep.getGroupId().equals(groupId)) {
                return false;
            }
        }
        return recurseAccept(edge.from, dep);
    }

    public boolean accept(Edge edge, Dependency dep) {
        return recurseAccept(edge, dep);
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

    
}
