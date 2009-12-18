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

import org.apache.tools.ant.types.DataType;
import org.nuxeo.build.maven.filter.AncestorFilter;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.ArtifactIdFilter;
import org.nuxeo.build.maven.filter.ClassifierFilter;
import org.nuxeo.build.maven.filter.GroupIdFilter;
import org.nuxeo.build.maven.filter.IsOptionalFilter;
import org.nuxeo.build.maven.filter.ManifestBundleCategoryPatternFilter;
import org.nuxeo.build.maven.filter.ScopeFilter;
import org.nuxeo.build.maven.filter.TypeFilter;
import org.nuxeo.build.maven.filter.VersionFilter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ArtifactPattern extends DataType {

    public AndFilter filter = new AndFilter();

    public void setGroupId(String groupId) {
        filter.addFilter(GroupIdFilter.class, groupId);
    }

    public void setArtifactId(String artifactId) {
        filter.addFilter(ArtifactIdFilter.class, artifactId);
    }

    public void setVersion(String version) {
        filter.addFilter(VersionFilter.class, version);
    }

    public void setClassifier(String classifier) {
        filter.addFilter(ClassifierFilter.class, classifier);
    }

    public void setType(String type) {
        filter.addFilter(TypeFilter.class, type);
    }

    public void setScope(String scope) {
        filter.addFilter(ScopeFilter.class, scope);
    }

    public void setOptional(boolean isOptional) {
        filter.addFilter(new IsOptionalFilter(isOptional));
    }

    public void setPattern(String pattern) {
        filter.addFiltersFromPattern(pattern);
    }

    public void setAncestor(String ancestor) {
        filter.addFilter(AncestorFilter.class, ancestor);
    }

    public void setCategory(String category) {
        filter.addFilter(ManifestBundleCategoryPatternFilter.class, category);
    }

}
