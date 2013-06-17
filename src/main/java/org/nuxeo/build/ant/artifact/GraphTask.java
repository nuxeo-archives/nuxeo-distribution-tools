/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, slacoin, jcarsique
 */
package org.nuxeo.build.ant.artifact;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GraphTask extends Task {

    protected List<ArtifactKey> resolves;

    protected String src;

    protected Expand expand;

    public void setResolve(String resolve) {
        if (resolves == null) {
            resolves = new ArrayList<ArtifactKey>();
        }
        resolves.add(new ArtifactKey(resolve));
    }

    public void setSrc(String file) {
        src = file;
    }

    public void addExpand(@SuppressWarnings("hiding") Expand expand) {
        this.expand = expand;
    }

    public void addResolve(ArtifactKey artifact) {
        if (resolves == null) {
            resolves = new ArrayList<ArtifactKey>();
        }
        resolves.add(artifact);
    }

    @Override
    public void execute() throws BuildException {
        MavenClient maven = MavenClientFactory.getInstance();
        if (src != null) {
            if (resolves == null) {
                resolves = new ArrayList<ArtifactKey>();
            }
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(src));
                String line = reader.readLine();
                while (line != null) {
                    line = getProject().replaceProperties(line.trim());
                    resolves.add(new ArtifactKey(line));
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new BuildException("Failed to import file: " + src, e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
        if (resolves != null) {
            for (ArtifactKey resolve : resolves) {
                ArtifactDescriptor ad = new ArtifactDescriptor(resolve.pattern);
                Artifact arti = ad.getBuildArtifact();
                final Graph graph = maven.getGraph();
                Node node = graph.getRootNode(arti);
                if (expand != null) {
                    graph.resolveDependencyTree(node,
                            CompositeFilter.compact(expand.filter),
                            expand.depth);
                }
            }
        }
    }
}
