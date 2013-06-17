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
 *     bstefanescu
 */
package org.nuxeo.build.ant;


/**
 * @deprecated Since 1.14. Use org.nuxeo.connect.update.Version instead
 */
@Deprecated
public class Version extends org.nuxeo.connect.update.Version {

    public Version(String version) {
        super(version);
    }

    public Version(int major) {
        super(major);
    }

    public Version(int major, int minor) {
        super(major, minor);
    }

    public Version(int major, int minor, int patch) {
        super(major, minor, patch);
    }

    public Version(int major, int minor, int patch, String classifier) {
        super(major, minor, patch, classifier);
    }

}
