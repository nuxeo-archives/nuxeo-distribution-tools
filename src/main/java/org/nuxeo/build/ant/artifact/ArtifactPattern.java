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
import org.nuxeo.build.maven.filter.DependsOnCategoryPatternFilter;
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

    private AndFilter filter = null;

    protected String category = null;

    protected String groupId = null;

    protected String artifactId = null;

    protected String version = null;

    protected String classifier = null;

    protected String type = null;

    protected String scope = null;

    protected boolean isOptional = false;

    protected String pattern = null;

    protected String ancestor = null;

    protected boolean isDependsOnCategory = true;

    public AndFilter getFilter() {
        if (filter == null) {
            filter = new AndFilter();
            filter.addFilter(GroupIdFilter.class, groupId);
            filter.addFilter(ArtifactIdFilter.class, artifactId);
            filter.addFilter(VersionFilter.class, version);
            filter.addFilter(ClassifierFilter.class, classifier);
            filter.addFilter(TypeFilter.class, type);
            filter.addFilter(ScopeFilter.class, scope);
            if (isOptional) {
                filter.addFilter(new IsOptionalFilter(isOptional));
            }
            filter.addFiltersFromPattern(pattern);
            filter.addFilter(AncestorFilter.class, ancestor);
            filter.addFilter(ManifestBundleCategoryPatternFilter.class,
                    category);
            if (isDependsOnCategory) {
                filter.addFilter(DependsOnCategoryPatternFilter.class, category);
            }
        }
        return filter;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setOptional(boolean isOptional) {
        this.isOptional = isOptional;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setAncestor(String ancestor) {
        this.ancestor = ancestor;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDependsOnCategory(boolean isDependsOnCategory) {
        this.isDependsOnCategory = isDependsOnCategory;
    }

}
