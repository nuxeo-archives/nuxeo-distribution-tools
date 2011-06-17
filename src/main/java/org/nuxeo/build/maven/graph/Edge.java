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
package org.nuxeo.build.maven.graph;

import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.DependencyFilter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Edge {

    public final Graph graph;
    
    public final Edge from;
    
    public final Node src;

    public final Node dst;

    public final boolean isOptional;

    public final String scope;
    
    public final List<Exclusion> exclusions;

    protected final Dependency dep;
        
    public Edge(Node node) {
        this.graph = node.graph;
        this.from = null;
        this.src= node;
        this.dst = node;
        this.isOptional = false;
        this.scope = Artifact.SCOPE_COMPILE;
        this.exclusions = Collections.emptyList();
        this.dep = new Dependency();;
    }

    public Edge(Edge from, Node src, Node dst, Dependency d) {
        this.graph = from.graph;
        this.from = from;
        this.src= src;
        this.dst = dst;
        this.dep = d;
        this.scope = d.getScope();
        this.isOptional = d.isOptional();
        this.exclusions = Collections.unmodifiableList(d.getExclusions());
    }


  @SuppressWarnings("unchecked")
public void expand(int recurse,
            DependencyFilter filter) {
        if (recurse <= 0) {
            return;
        }
        if (dst.isExpanded) {
            return;
        }
        MavenProject pom = dst.getPom();
        if (pom == null) {
            return;
        }
        final String type = dst.getArtifact().getType();
        final boolean shouldLoadDependencyManagement = graph.shouldLoadDependencyManagement();
        if ("pom".equals(type) && shouldLoadDependencyManagement) {
            expand(recurse,
                    pom.getDependencyManagement().getDependencies(), filter);
        }
        expand(recurse, pom.getDependencies(), filter);
    }
    
    protected void expand(int recurse, 
            List<Dependency> deps, DependencyFilter filter) {
        dst.isExpanded = true;
        ArtifactFactory factory = graph.getMaven().getArtifactFactory();
        
        // add direct edges
        
        for (Dependency d : deps) {
            // Workaround to always ignore test scope dependencies
            // the last boolean parameter is redundant, but the version that
            // doesn't take this has a bug. See MNG-2524
            if ("test".equalsIgnoreCase(d.getScope())
                    || "system".equalsIgnoreCase(d.getScope())
                    || d.isOptional()
                    || (filter != null && !filter.accept(this, d))) {
                if (MavenClientFactory.getLog().isDebugEnabled()) {
                    MavenClientFactory.getLog().info(
                            "Filtering " + dst + "  - refused "
                                    + d.toString());
                }
                continue;
            }
            Artifact a = factory.createDependencyArtifact(d.getGroupId(),
                    d.getArtifactId(),
                    VersionRange.createFromVersion(d.getVersion()),
                    d.getType(), d.getClassifier(), d.getScope(), false);

            // beware of Maven bug! make sure artifact got the value inherited
            // from dependency
            assert a.getScope().equals(d.getScope());
            Node node = graph.getNode(dst, a);
            Edge newEdge = new Edge(this, dst, node, d);
            dst.addEdgeOut(newEdge);
            node.addEdgeIn(newEdge);
        }
        
        // recurse on edges 
        
        if (recurse <= 0) {
            return;
        }
        
        for (Edge e : dst.getEdgesOut()) {
            if (!e.dst.isExpanded) {
                e.expand(recurse - 1, filter);
            }
        }
    }

@Override
public String toString() {
    return "edge - " + src + " -> " + dst;
    }

    public static void print(Edge edge) {
        recursePrint(edge, 0);
    }

    protected static int recursePrint(Edge edge, int level) {
        if (edge == null) {
            return level;
        }
        if (level > 100) {
            System.out.println("Cutting, maximum depth");
            return level;
        }
        level = recursePrint(edge.from, level+1);
        System.out.println(String.format("%04d %s", level, edge));
        return level-1;
    }
}
