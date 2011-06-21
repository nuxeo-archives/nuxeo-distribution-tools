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
 *     bstefanescu
 */
package org.nuxeo.build.ant.artifact;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.resources.FileResource;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.VersionManagement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResolveFile extends FileResource {

    public String key;

    public String classifier;

    public void setKey(String pattern) {
        int p = pattern.lastIndexOf(';');
        if (p > -1) {
            key = pattern.substring(0, p);
            classifier = pattern.substring(p + 1);
        } else {
            key = pattern;
        }
    }

    /**
     * @deprecated since 1.8; put classifier in the key
     *             ("groupId:artifactId:version:type:classifier:scope")
     * @param classifier
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    protected File resolveFile() throws ArtifactNotFoundException {
        MavenClient maven = MavenClientFactory.getInstance();
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        // Sync classifier set from key or from setClassifier()
        if (ad.classifier != null) {
            classifier = ad.classifier;
        } else if (classifier != null) {
            ad.classifier = classifier;
        }
        if (ad.version == null) {
            VersionManagement versionManagement = maven.getGraph().getVersionManagement();
            ad.version = versionManagement.getVersion(ad);
            if (ad.version == null) {
                throw new BuildException(
                        "Version is required since not found in dependency management: "
                                + ad);
            }
        }
        Artifact artifact = maven.getArtifactFactory().createDependencyArtifact(
                ad.groupId, ad.artifactId,
                VersionRange.createFromVersion(ad.version), ad.type,
                ad.classifier, ad.scope);
        MavenClientFactory.getInstance().resolve(artifact);
        return artifact.getFile();
    }

    @Override
    public File getFile() {
        if (isReference()) {
            return ((FileResource) getCheckedRef()).getFile();
        }
        try {
            return resolveFile();
        } catch (ArtifactNotFoundException e) {
            throw new BuildException("Failed to resolve file: " + key
                    + "; classifier: " + classifier, e);
        }
    }

    @Override
    public File getBaseDir() {
        return isReference() ? ((FileResource) getCheckedRef()).getBaseDir()
                : getFile().getParentFile();
    }

}
