/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.build.ant.artifact;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * Resolve multiple files from a properties list
 *
 * @since 1.10.2
 */
public class ResolveFiles extends DataType implements ResourceCollection {

    private Properties source;

    private String classifier;

    private List<FileResource> artifacts;

    /**
     * Set source of artifacts to resolve
     *
     * @param source Properties files with artifacts list
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void setSource(String source) throws FileNotFoundException,
            IOException {
        this.source = new Properties();
        this.source.load(new FileInputStream(source));
    }

    /**
     * Change classifier of all artifacts to resolve
     *
     * @param classifier Maven classifier
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    @Override
    public boolean isFilesystemOnly() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<FileResource> iterator() {
        if (isReference()) {
            return ((ResourceCollection) getCheckedRef()).iterator();
        }

        if (artifacts == null) {
            artifacts = new ArrayList<FileResource>();
            for (Iterator<?> it = source.values().iterator(); it.hasNext();) {
                String artifactKey = (String) it.next();
                try {
                    artifacts.add(resolveFile(artifactKey));
                } catch (ArtifactNotFoundException e) {
                    MavenClientFactory.getLog().warn(e.getMessage());
                }
            }
        }
        return artifacts.iterator();
    }

    /**
     * @param artifactKey
     * @return
     * @throws ArtifactNotFoundException
     */
    private FileResource resolveFile(String artifactKey)
            throws ArtifactNotFoundException {
        ArtifactDescriptor ad = new ArtifactDescriptor(artifactKey);
        if (classifier != null) {
            ad.classifier = classifier;
        }
        Artifact artifact = ad.getArtifact();
        MavenClientFactory.getInstance().resolve(artifact);
        FileResource fr = new FileResource(artifact.getFile());
        fr.setBaseDir(artifact.getFile().getParentFile());
        return fr;
    }

    @Override
    public int size() {
        return artifacts.size();
    }

}
