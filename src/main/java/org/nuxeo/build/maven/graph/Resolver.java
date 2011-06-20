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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Resolver {

    protected Graph graph;

    public Resolver(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    public void resolve(Artifact artifact) {
        if (artifact.isResolved()) {
            return;
        }
        // TODO remote repos from pom
        try {
            graph.maven.resolve(artifact);
        } catch (ArtifactNotFoundException e) {
            MavenClientFactory.getLog().warn("Cannot resolve " + artifact, e);
        }
    }

    public MavenProject load(Artifact artifact) {
        if ("system".equals(artifact.getScope()))
            return null;
        try {
            return graph.maven.getProjectBuilder().buildFromRepository(
                    // this create another Artifact instance whose type is 'pom'
                    graph.maven.getArtifactFactory().createProjectArtifact(
                            artifact.getGroupId(), artifact.getArtifactId(),
                            artifact.getVersion()),
                    graph.maven.getRemoteRepositories(),
                    graph.maven.getLocalRepository());
        } catch (ProjectBuildingException e) {
            MavenClientFactory.getLog().error(
                    "Error loading POM of " + artifact, e);
            return null;
        }
    }

}
