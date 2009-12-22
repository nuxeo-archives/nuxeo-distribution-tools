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
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.build.maven.filter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author jcarsique
 * 
 */
public class ManifestBundleCategoryFilter implements Filter {

    public static final String MANIFEST_BUNDLE_CATEGORY = "Bundle-Category";

    public static final String MANIFEST_BUNDLE_CATEGORY_TOKEN = ",";

    protected List<char[]> patterns = new ArrayList<char[]>();

    protected boolean isDependOnCategory;

    public ManifestBundleCategoryFilter(String patterns,
            boolean isDependsOnCategory) {
        this.isDependOnCategory = isDependsOnCategory;
        StringTokenizer st = new StringTokenizer(patterns,
                MANIFEST_BUNDLE_CATEGORY_TOKEN);
        while (st.hasMoreTokens()) {
            this.patterns.add(st.nextToken().toCharArray());
        }
    }

    protected List<String> getValuesToMatch(Artifact artifact) {
        List<String> valuesToMatch = new ArrayList<String>();
        File file = artifact.getFile();
        if (file == null) {
            if (artifact.isResolved()) {
                MavenClientFactory.getLog().warn(
                        "Artifact " + artifact + " doesn't contain a file");
            } else if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
                // ignore provided artifacts; raise a warning for non provided
                MavenClientFactory.getLog().warn(
                        "Artifact " + artifact + " unresolved");
            }
            return valuesToMatch;
        }
        // ignore pom files
        if (file.getName().endsWith(".pom")) {
            return valuesToMatch;
        }
        try {
            JarFile jarFile = new JarFile(file, true);
            Manifest mf = jarFile.getManifest();
            if (mf != null) {
                Attributes attributes = mf.getMainAttributes();
                if (attributes != null) {
                    String bundleCategories = attributes.getValue(MANIFEST_BUNDLE_CATEGORY);
                    if (bundleCategories != null) {
                        StringTokenizer st = new StringTokenizer(
                                bundleCategories,
                                MANIFEST_BUNDLE_CATEGORY_TOKEN);
                        while (st.hasMoreTokens()) {
                            valuesToMatch.add(st.nextToken());
                        }
                    }
                }
            } else {
                MavenClientFactory.getLog().warn(
                        "Artifact " + artifact + " doesn't contain a manifest");
            }
        } catch (IOException e) {
            MavenClientFactory.getLog().error(
                    "error while inspecting this jar manifest: "
                            + artifact.getFile(), e);
        }
        return valuesToMatch;
    }

    public boolean accept(Node node) {
        // Exclude non Nuxeo artifacts
        if (!node.getArtifact().getGroupId().startsWith("org.nuxeo")) {
            return false;
        }
        if (MavenClientFactory.getLog().isDebugEnabled()) {
            MavenClientFactory.getLog().debug(
                    getClass() + " filtering " + node.getArtifact());
        }
        // quick check of already accepted nodes
        boolean accept = node.isAcceptedCategory(patterns);
        // else check artifact's Manifest
        if (!accept) {
//            accept=accept(node.getArtifact());
            for (String valueToMatch : getValuesToMatch(node.getArtifact())) {
                for (char[] pattern : patterns) {
                    if (matchPattern(valueToMatch, pattern)) {
                        if (MavenClientFactory.getLog().isDebugEnabled()) {
                            MavenClientFactory.getLog().debug(
                                    "Match on " + String.valueOf(pattern));
                        }
                        accept =  true;
                        node.setAcceptedCategory(pattern);
                        break;
                    }
                }
            }
        }

        if (!accept && isDependOnCategory) {
            // check if there's an acceptable/accepted child
            List<Edge> children = node.getEdgesOut();
            // if (children!=null) {
            for (Edge child : children) {
                if (accept(child.dst)) {
                    accept = true;
                    break;
                }
            }
            // }
        }
        if (!accept) {
            // check if there's an acceptable/accepted parent
            List<Edge> parents = node.getEdgesIn();
            // if (parents!=null) {
            for (Edge ancestor : parents) {
                if (accept(ancestor.dst)) {
                    accept = true;
                    break;
                }
            }
            // }
        }
        if (MavenClientFactory.getLog().isDebugEnabled()) {
            MavenClientFactory.getLog().debug(
                    "Filtering result for " + node.getArtifact() + " : "
                            + accept);
        }
        return accept;
    }

    public boolean accept(Dependency dep) {
        throw new UnsupportedOperationException("Not supported");
    }

    public boolean accept(Edge edge) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * @deprecated prefer use of {@link #accept(Node)} as it remembers already parsed artifacts
     */
    public boolean accept(Artifact artifact) {
        boolean accept = matchPattern(getValuesToMatch(artifact));
        if (MavenClientFactory.getLog().isDebugEnabled()) {
            MavenClientFactory.getLog().debug(
                    (accept ? "Accepts " : "Rejects ") + artifact);
        }
        return accept;
    }

    private boolean matchPattern(List<String> valuesToMatch) {
        for (String valueToMatch : valuesToMatch) {
            for (char[] pattern : patterns) {
                if (matchPattern(valueToMatch, pattern)) {
                    if (MavenClientFactory.getLog().isDebugEnabled()) {
                        MavenClientFactory.getLog().debug(
                                "Match on " + String.valueOf(pattern));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matchPattern(String name, char[] pattern) {
        return matchPattern(name.toCharArray(), pattern);
    }

    public boolean matchPattern(char[] name, char[] pattern) {
        return matchPattern(name, 0, name.length, pattern);
    }

    public boolean matchPattern(char[] name, int offset, int len, char[] pattern) {
        int i = offset;
        boolean wildcard = false;
        for (char c : pattern) {
            switch (c) {
            case '*':
                wildcard = true;
                break;
            case '?':
                i++;
                break;
            default:
                if (wildcard) {
                    while (i < len) {
                        if (name[i++] == c) {
                            break;
                        }
                    }
                    if (i == len) {
                        return true;
                    }
                    wildcard = false;
                } else if (i >= len || name[i] != c) {
                    return false;
                } else {
                    i++;
                }
                break;
            }
        }
        return wildcard || i == len;
    }

}
