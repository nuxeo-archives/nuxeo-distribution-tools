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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.build.maven.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.nuxeo.build.maven.filter.DependencyFilter;
import org.nuxeo.build.maven.filter.Filter;

/**
 * TODO: use pom settings when resolving an artifact (use remote repos specified
 * in pom if any)
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Node {

    protected Graph graph;

    protected String id;

    protected Artifact artifact;

    protected List<Edge> edgesIn;

    protected List<Edge> edgesOut;

    protected boolean isExpanded;

    /**
     * Point to an artifact pom. When embedded in maven and using the current
     * project pom as the root this will be set by the maven loader mojo to
     * point to the current pom
     */
    protected MavenProject pom;

    private List<char[]> acceptedCategories;

    public List<char[]> getAcceptedCategories() {
        if (acceptedCategories == null) {
            acceptedCategories = new ArrayList<char[]>();
        }
        return acceptedCategories;
    }

    public static String createNodeId(Artifact artifact) {
        return new StringBuilder().append(artifact.getGroupId()).append(':').append(
                artifact.getArtifactId()).append(':').append(
                artifact.getVersion()).append(':').append(artifact.getType()).append(
                ':').toString();
    }

    public Node(Node node) {
        this.graph = node.graph;
        this.id = node.id;
        this.artifact = node.artifact;
        this.edgesIn = node.edgesIn;
        this.edgesOut = node.edgesOut;
        this.pom = node.pom;
        this.isExpanded = node.isExpanded;
    }

    public Node(Graph graph, MavenProject pom, Artifact artifact) {
        this(graph, pom, artifact, Node.createNodeId(artifact));
    }

    protected Node(Graph graph, MavenProject pom, Artifact artifact, String id) {
        this.graph = graph;
        this.id = id;
        this.artifact = artifact;
        this.pom = pom;
        edgesIn = new ArrayList<Edge>();
        edgesOut = new ArrayList<Edge>();
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public File getFile() {
        graph.getResolver().resolve(this);
        File file = artifact.getFile();
        if (file != null) {
            graph.file2artifacts.put(file.getName(), artifact);
        }
        return file;
    }

    public File getFile(String classifier) {
        graph.getResolver().resolve(this);
        Artifact ca = graph.maven.getArtifactFactory().createArtifactWithClassifier(
                artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getVersion(), artifact.getType(), classifier);
        try {
            graph.maven.resolve(ca);
            File file = ca.getFile();
            if (file != null) {
                graph.file2artifacts.put(file.getAbsolutePath(), ca);
            }
            return file;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public boolean isRoot() {
        return edgesIn.isEmpty();
    }

    public String getId() {
        return id;
    }

    public Collection<Edge> getEdgesOut() {
        return edgesOut;
    }

    
    protected static String dependencyId(Dependency dep) {
        final String groupId = StringUtils.defaultString(dep.getGroupId());
        final String artifactId = StringUtils.defaultString(dep.getArtifactId());
        final String version = StringUtils.defaultString(dep.getVersion());
        final String type = StringUtils.defaultString(dep.getType());
        final String classifier = StringUtils.defaultString(dep.getClassifier());
        return String.format("%s:%s:%s:%s:%s",groupId,artifactId,version,type, classifier);
    }
    
    public Collection<Edge> getEdgesIn() {
        return edgesIn;
    }
    
    protected void addEdgeIn(Node node, Edge edge) {
        edgesIn.add(edge);
    }

    protected void addEdgeOut(Node node, Edge edge) {
        edgesOut.add(edge);
    }

    public MavenProject getPom() {
        if (pom == null) {
            graph.getResolver().resolve(this);
        }
        return pom;
    }

    public MavenProject getPomIfAlreadyLoaded() {
        return pom;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void expand( int recurse, DependencyFilter filter) {
        if (isExpanded) {
            return;
        }
        Edge edge = new Edge( this);
        edge.expand(recurse, filter);
    }

    public void expand(DependencyFilter filter) {
        expand(0, filter);
    }

    public void expandAll(DependencyFilter filter) {
        expand(Integer.MAX_VALUE, filter);
    }

    public List<Node> getTrail() {
        if (edgesIn.isEmpty()) {
            ArrayList<Node> result = new ArrayList<Node>();
            result.add(this);
            return result;
        }
        Edge edge = edgesIn.get(0);
        List<Node> path = edge.src.getTrail();
        path.add(this);
        return path;
    }

    
    protected void unexpand() {
        isExpanded = false;
        for (Edge e:edgesOut) {
            final Node out = e.dst;
            out.isExpanded = false;
            out.unexpand();
        }
    }

    public void collectNodes(Collection<Node> nodes, Filter filter) {
        for (Edge edge : edgesOut) {
            if (filter.accept(edge)) {
                nodes.add(edge.dst);
            }
        }
    }

    public void collectNodes(Collection<Node> nodes) {
        for (Edge edge : edgesOut) {
            nodes.add(edge.dst);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Node) {
            return ((Node) obj).id.equals(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * @param pattern
     */
    public void setAcceptedCategory(char[] pattern) {
        getAcceptedCategories().add(pattern);
    }

    /**
     * @param patterns
     * @return true if at least one pattern has been accepted
     */
    public boolean isAcceptedCategory(List<char[]> patterns) {
        for (char[] pattern : patterns) {
            if (getAcceptedCategories().contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
