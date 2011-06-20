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
import org.nuxeo.build.maven.filter.GroupIdFilter;
import org.nuxeo.build.maven.filter.NotFilter;
import org.nuxeo.build.maven.filter.VersionFilter;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class NuxeoExpandTask extends ExpandTask {

    {
        setDepth("all");
        Filter nuxeoFilter = new Filter() {
            
            public boolean accept(Artifact artifact) {
                return true;
            }

            public boolean accept(Edge edge) {
               if (edge.isOptional) {
                    return false;
                }
                String scope = edge.scope;
                if (scope == null) {
                    scope = "compile";
                }
                if ( "compile".equals(scope)) {
                    return true;
                }
                if ( "runtime".equals(scope)) {
                    return true;
                }
                return false;

            }

            public boolean accept(Node node) {
                return true;
            }
        };
        filter.addFilter(nuxeoFilter);
        filter.addFilter(new NotFilter(new VersionFilter("[*)")));
        filter.addFilter(new NotFilter(new GroupIdFilter("org.nuxeo.build")));
    }

    protected boolean acceptNode(Node node) {
        return node.getArtifact().getGroupId().startsWith("org.nuxeo");
    }

}
