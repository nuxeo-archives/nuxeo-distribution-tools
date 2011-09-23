/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique, slacoin
 */
package org.nuxeo.build.ant.artifact;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.EmbeddedMavenClient;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PrintGraphTask extends Task {

    /**
     * @since 1.10.2
     */
    public static final String MODE_TREE = "tree";

    /**
     * @since 1.10.2
     */
    public static final String MODE_FLAT = "flat";

    private OutputStream output = System.out;

    private String mode = MODE_TREE;

    private int format = Node.FORMAT_GAV;

    private boolean append = false;

    private String source;

    @Override
    public void execute() throws BuildException {
        Graph graph;
        Node rootNode = null;
        MavenClient originalMavenClient = MavenClientFactory.getInstance();
        if (source != null) {
            // Build a new graph with "source" as root node
            EmbeddedMavenClient mavenClient = new EmbeddedMavenClient();
            MavenClientFactory.setInstance(mavenClient);
            try {
                mavenClient.start();
            } catch (MavenEmbedderException e) {
                throw new BuildException(e);
            }
            graph = MavenClientFactory.getInstance().newGraph();
            ArtifactDescriptor ad = new ArtifactDescriptor(source);
            rootNode = graph.getRootNode(ad.getArtifact());
            ExpandTask expandTask = new NuxeoExpandTask();
            expandTask.setDepth("all");
            expandTask.execute(graph);
        } else {
            graph = MavenClientFactory.getInstance().getGraph();
        }
        for (Node node : graph.getRoots()) {
            try {
                HashSet<Node> collectedNodes = new HashSet<Node>();
                if (MODE_TREE.equalsIgnoreCase(mode)) {
                    printTree("", node, collectedNodes);
                } else if (MODE_FLAT.equalsIgnoreCase(mode)) {
                    HashSet<String> printedArtifacts = new HashSet<String>();
                    printFlat(node, collectedNodes, printedArtifacts);
                } else {
                    throw new BuildException("Unknown mode: " + mode);
                }
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
        if (source != null) {
            MavenClientFactory.setInstance(originalMavenClient);
        }
    }

    /**
     * Print graph content as a flat listing. Do not write twice a given node.
     *
     * @param node current node
     * @param collectedNodes already walked through nodes
     * @param printedArtifacts already printed artifacts
     * @throws IOException
     */
    private void printFlat(Node node, HashSet<Node> collectedNodes,
            HashSet<String> printedArtifacts) throws IOException {
        if (!printedArtifacts.contains(node.getId())) {
            print(node.toString(format) + System.getProperty("line.separator"));
            printedArtifacts.add(node.getId());
        }
        if (collectedNodes.contains(node)) {
            return;
        }
        collectedNodes.add(node);
        for (Edge edge : node.getEdgesOut()) {
            printFlat(edge.out, collectedNodes, printedArtifacts);
        }
    }

    /**
     * Print graph as a tree. Do not develop twice a given node.
     *
     * @param tabs incremented tabulation
     * @param node current node
     * @param collectedNodes already walked through nodes
     * @throws IOException
     */
    private void printTree(String tabs, Node node, Set<Node> collectedNodes)
            throws IOException {
        print(tabs + node.toString(format)
                + System.getProperty("line.separator"));
        if (collectedNodes.contains(node)) {
            return;
        }
        collectedNodes.add(node);
        for (Edge edge : node.getEdgesOut()) {
            printTree(tabs + " |-- ", edge.out, collectedNodes);
        }
    }

    public void setOutput(String output) throws FileNotFoundException {
        this.output = new FileOutputStream(output, append);
    }

    private void print(String message) throws IOException {
        output.write(message.getBytes());
    }

    /**
     * @param mode print as tree if true, else print flat
     * @since 1.10.2
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Defines output format
     *
     * @param format
     * @see Node#FORMAT_GAV
     * @see Node#FORMAT_KV_F_GAV
     * @since 1.10.2
     */
    public void setFormat(int format) {
        this.format = format;
    }

    /**
     * Output append mode
     *
     * @param append
     * @since 1.10.2
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * If set, print another graph than the current one
     *
     * @param source GAV key of root node to expand as a graph
     * @since 1.10.2
     */
    public void setSource(String source) {
        this.source = source;
    }

}
