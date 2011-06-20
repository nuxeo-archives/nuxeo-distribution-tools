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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.nuxeo.build.ant.artifact.GraphTask;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.Logger;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.filter.VersionManagement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Graph {

    protected MavenClient maven;

    protected Node root;

    protected final TreeMap<String, Node> nodes = new TreeMap<String, Node>();

    protected final LinkedList<Node> roots = new LinkedList<Node>();

    protected Resolver resolver = new Resolver(this);

    protected Map<String, Artifact> file2artifacts = new HashMap<String, Artifact>();

    // manage versions from dependency management -> lazy initialized when
    // required. (by calling artifact:resolveFile without a version)
    protected VersionManagement vmgr;

    protected boolean shouldLoadDependencyManagement = false;

    public Graph(MavenClient maven) {
        this.maven = maven;
        this.vmgr = new VersionManagement();
    }

    public VersionManagement getVersionManagement() {
        return vmgr;
    }

    public void setShouldLoadDependencyManagement(
            boolean shouldLoadDependencyManagement) {
        this.shouldLoadDependencyManagement = shouldLoadDependencyManagement;
    }

    public boolean shouldLoadDependencyManagement() {
        return shouldLoadDependencyManagement;
    }

    public MavenClient getMaven() {
        return maven;
    }

    public List<Node> getRoots() {
        return roots;
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Artifact getArtifactByFile(String fileName) {
        return file2artifacts.get(fileName);
    }

    public void collectNodes(Collection<Node> nodesToCollect) {
        for (Node node : roots) {
            node.collectNodes(nodesToCollect);
        }
    }

    public void collectNodes(Collection<Node> nodesToCollect, Filter filter) {
        for (Node node : roots) {
            node.collectNodes(nodesToCollect, filter);
        }
    }

    public Node[] getNodesArray() {
        return nodes.values().toArray(new Node[nodes.size()]);
    }

    public Node findFirst(String pattern) {
        return findFirst(pattern, false);
    }

    public Node findFirst(String pattern, boolean stopIfNotUnique) {
        SortedMap<String, Node> map = nodes.subMap(pattern + ':', pattern
                + ((char) (':' + 1)));
        int size = map.size();
        if (size == 0) {
            return null;
        }
        if (stopIfNotUnique && size > 1) {
            throw new BuildException(
                    "Pattern '"
                            + pattern
                            + "' cannot be resolved to a unique node. Matching nodes are: "
                            + map.values());
        }
        return map.get(map.firstKey());
    }

    public Collection<Node> find(String pattern) {
        SortedMap<String, Node> map = nodes.subMap(pattern + ':', pattern
                + ((char) (':' + 1)));
        return map.values();
    }

    /**
     * Add a root node given an artifact pom. This can be used by the embedder
     * maven mojo to initialize the graph with the current pom.
     */
    public Node addRootNode(MavenProject pom) {
        Artifact artifact = pom.getArtifact();
        return getRootNode(artifact);
    }

    public Node addRootNode(String key) {
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        Artifact artifact = GraphTask.readArtifact(ad);
        return getRootNode(artifact);
    }

    public Node getRootNode(Artifact artifact) {
        MavenProject pom = resolver.load(artifact);
        Node node = nodes.get(artifact);
        if (node == null) {
            node = new Node(Graph.this, artifact, pom);
            nodes.put(node.getId(), node);
            nodesByArtifact.put(artifact, node);
            roots.add(node);
        }
        return node;
    }

    public Resolver getResolver() {
        return resolver;
    }

    public Node lookup(String id) {
        return nodes.get(id);
    }

    public Node lookup(Artifact artifact) {
        return lookup(Node.createNodeId(artifact));
    }

    @SuppressWarnings("unchecked")
    public Node findNode(ArtifactDescriptor ad) {
        String key = ad.getNodeKeyPattern();
        Collection<Node> nodesToParse = null;
        if (key == null) {
            nodesToParse = getNodes();
        } else {
            nodesToParse = find(key);
        }
        Node returnNode = null;
        for (Node node : nodesToParse) {
            Artifact artifact = node.getArtifact();
            if (ad.artifactId != null
                    && !ad.artifactId.equals(artifact.getArtifactId())) {
                continue;
            }
            if (ad.groupId != null && !ad.groupId.equals(artifact.getGroupId())) {
                continue;
            }
            if (ad.version != null && !ad.version.equals(artifact.getVersion())) {
                continue;
            }
            if (ad.type != null && !ad.type.equals(artifact.getType())) {
                continue;
            }
            try {
                if (returnNode != null
                        && artifact.getSelectedVersion().compareTo(
                                returnNode.getArtifact().getSelectedVersion()) < 0) {
                    continue;
                }
            } catch (OverConstrainedVersionException e) {
                MavenClientFactory.getLog().error(
                        "Versions comparison failed on " + artifact, e);
            }
            returnNode = node;
        }
        return returnNode;
    }

    public MavenProject loadPom(Artifact artifact) {
        if ("system".equals(artifact.getScope()))
            return null;
        try {
            return maven.getProjectBuilder().buildFromRepository(
                    // this create another Artifact instance whose type is 'pom'
                    maven.getArtifactFactory().createProjectArtifact(
                            artifact.getGroupId(), artifact.getArtifactId(),
                            artifact.getVersion()),
                    maven.getRemoteRepositories(), maven.getLocalRepository());
        } catch (Exception e) {
            MavenClientFactory.getLog().error(e.getMessage(), e);
            return null;
        }
    }

    protected final IdentityHashMap<Artifact, Node> nodesByArtifact = new IdentityHashMap<Artifact, Node>();

    protected final HashSet<Node> filteredNodes = new HashSet<Node>();

    protected class NodesInjector implements
            org.apache.maven.artifact.resolver.ResolutionListener {

        protected final Stack<Node> parentNodes = new Stack<Node>();

        protected final Node rootNode;

        protected final Filter filter;

        protected final int maxDepth;

        protected Node currentNode;

        protected NodesInjector(Node node, Filter filter, int maxDepth) {
            this.currentNode = node;
            this.rootNode = node;
            this.filter = filter;
            this.maxDepth = maxDepth;
        }

        public void testArtifact(Artifact node) {
            log("testArtifact: artifact=" + node);
        }

        public void startProcessChildren(Artifact artifact) {
            log("startProcessChildren: artifact=" + artifact);

            if (!currentNode.getArtifact().equals(artifact)) {
                throw new IllegalStateException("Artifact was expected to be "
                        + currentNode.getArtifact() + " but was " + artifact);
            }

            parentNodes.push(currentNode);
        }

        public void endProcessChildren(Artifact artifact) {
            Node node = (Node) parentNodes.pop();

            log("endProcessChildren: artifact=" + artifact);

            if (node == null) {
                throw new IllegalStateException(
                        "Parent dependency node was null");
            }

            if (!node.getArtifact().equals(artifact)) {
                throw new IllegalStateException(
                        "Parent dependency node artifact was expected to be "
                                + node.getArtifact() + " but was " + artifact);
            }
        }

        public void includeArtifact(Artifact artifact) {
            log("includeArtifact: artifact=" + artifact);
            
            Node node = nodesByArtifact.get(artifact);

            /*
             * Ignore duplicate includeArtifact calls since omitForNearer can be
             * called prior to includeArtifact on the same artifact, and we
             * don't wish to include it twice.
             */
            if (node == null && isCurrentNodeIncluded()) {
                addNode(artifact);
            } else if (parentNodes.size() > 0) {  // add edge if not filtered
                Node parent = parentNodes.peek();
                
                if (parent.state == Node.FILTERED) {
                    return;
                }
                
                if (parentNodes.size() >= maxDepth) {
                    return;
                }
                
                Edge edge = new Edge(parent, node);
                if (!filter.accept(edge)) {
                    return;
                }
                
                node.addEdgeIn(edge);
                parent.addEdgeOut(edge);
                node.state = Node.INCLUDED;
            }
        }

        public void omitForNearer(Artifact omitted, Artifact kept) {
            log("omitForNearer: omitted=" + omitted + " kept=" + kept);

            if (!omitted.getDependencyConflictId().equals(
                    kept.getDependencyConflictId())) {
                throw new IllegalArgumentException(
                        "Omitted artifact dependency conflict id "
                                + omitted.getDependencyConflictId()
                                + " differs from kept artifact dependency conflict id "
                                + kept.getDependencyConflictId());
            }

            if (!isCurrentNodeIncluded()) {
                return;
            }

            // clear omitted node
            Node omittedNode = nodesByArtifact.get(omitted);

            if (omittedNode != null) {
                removeNode(omitted);
            } else {
                omittedNode = createNode(omitted);
                currentNode = omittedNode;
            }

            omittedNode.state = Node.OMITTED;
            if (filteredNodes.remove(omittedNode)) {
                log("Reset filtering " + omittedNode);
            }

            // refresh kept node
            Node keptNode = nodesByArtifact.get(kept);

            if (keptNode == null) {
                keptNode = addNode(kept);
            }

        }

        public void updateScope(Artifact artifact, String scope) {
            log("updateScope: artifact=" + artifact + ", scope=" + scope);
        }

        public void manageArtifact(Artifact artifact, Artifact replacement) {
            log("manageArtifact: artifact=" + artifact + ", replacement="
                    + replacement);
        }

        public void omitForCycle(Artifact artifact) {
            log("omitForCycle: artifact=" + artifact);

            if (isCurrentNodeIncluded()) {
                Node node = createNode(artifact);
                node.state = Node.OMITTED;
            }
        }

        public void updateScopeCurrentPom(Artifact artifact, String ignoredScope) {
            log("updateScopeCurrentPom: artifact=" + artifact
                    + ", scopeIgnored=" + ignoredScope);
        }

        public void selectVersionFromRange(Artifact artifact) {
            throw new UnsupportedOperationException(
                    "selectVersionFromRange: artifact=" + artifact);
        }

        public void restrictRange(Artifact artifact, Artifact replacement,
                VersionRange newRange) {
            throw new UnsupportedOperationException("restrictRange: artifact="
                    + artifact + ", replacement=" + replacement
                    + ", versionRange=" + newRange);
        }

        /**
         * The log to write debug messages to.
         */
        private final Logger logger = MavenClientFactory.getLog();

        /**
         * Writes the specified message to the log at debug level with
         * indentation for the current node's depth.
         * 
         * @param message the message to write to the log
         */
        private void log(String message) {

            if (logger.isDebugEnabled() == false) {
                return;
            }

            int depth = parentNodes.size();

            StringBuffer buffer = new StringBuffer();

            for (int i = 0; i < depth; i++) {
                buffer.append("  ");
            }

            buffer.append(message);

            logger.debug(buffer.toString());
        }

        private boolean isCurrentNodeIncluded() {

            for (Iterator<Node> iterator = parentNodes.iterator(); iterator.hasNext();) {
                Node node = (Node) iterator.next();

                if (node.state != Node.INCLUDED && node.state != Node.FILTERED) {
                    return false;
                }
            }

            return true;
        }

        protected Node addNode(Artifact artifact) {
            Node node = createNode(artifact);
            Node previousNode = (Node) nodesByArtifact.put(artifact, node);
            nodes.put(node.id, node);

            if (previousNode != null) {
                throw new IllegalStateException(
                        "Duplicate node registered for artifact: "
                                + node.getArtifact());
            }

            currentNode = node;

            return node;
        }

        protected void removeNode(Artifact artifact) {
            Node node = nodesByArtifact.remove(artifact);
            nodes.remove(node.id);
            filteredNodes.remove(node);
            for (Edge out : node.edgesOut) {
                out.out.edgesIn.remove(out);
            }

            if (!artifact.equals(node.getArtifact())) {
                throw new IllegalStateException(
                        "Removed dependency node artifact was expected to be "
                                + artifact + " but was " + node.getArtifact());
            }

        }

        protected Node createNode(Artifact artifact) {
            MavenProject pom = resolver.load(artifact);
            Node node = new Node(Graph.this, artifact, pom);

            if (parentNodes.isEmpty()) {
                return node;
            }

            // setup edge
            Node parent = (Node) parentNodes.peek();
            Edge edge = new Edge(parent, node);

            // is edge filtered ?
            if (parent.state == Node.FILTERED || parentNodes.size() >= maxDepth
                    || !filter.accept(edge)) {
                node.state = Node.FILTERED;
                filteredNodes.add(node);
                log("Filtering "  + node);
                return node;
            }

            // link nodes if not filtered only
            parent.addEdgeOut(edge);
            node.addEdgeIn(edge);

            return node;
        }

    }

    public void resolveDependencyTree(Node node, Filter filter, int depth) {
        try {
            maven.resolveDependencyTree(node.artifact, new ArtifactFilter() {

                public boolean include(Artifact artifact) {
                    return false;
                }

            }, new NodesInjector(node, filter, depth));
        } catch (Exception cause) {
            throw new Error("Cannot resolve dependency tree for " + node, cause);
        }
        // clear filtered nodes
        for (Node n : filteredNodes) {
            nodes.remove(n.id);
            nodesByArtifact.remove(n.artifact);
        }
        filteredNodes.clear();
    }

}
