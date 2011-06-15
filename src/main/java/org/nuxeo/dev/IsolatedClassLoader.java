/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.dev;

import java.net.URL;
import java.net.URLClassLoader;

import org.nuxeo.osgi.application.MutableClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class IsolatedClassLoader extends URLClassLoader implements
        MutableClassLoader {

    public IsolatedClassLoader() {
        super(new URL[0], NuxeoApp.findContextClassLoader());
    }

    public IsolatedClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public IsolatedClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public ClassLoader getClassLoader() {
        return this;
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = preFind(name);
            } catch (ClassNotFoundException e) {
            }
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                }
                if (c == null) {
                    c = postFind(name);
                }
            }
        }
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    protected Class<?> loadFromParent(String name)
            throws ClassNotFoundException {
        if (getParent() != null) {
            return getParent().loadClass(name);
        }
        return null;
    }

    protected Class<?> preFind(String name) throws ClassNotFoundException {
        if (name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("org.w3c.")) {
            return loadFromParent(name);
        }
        return null;
    }

    protected Class<?> postFind(String name) throws ClassNotFoundException {
        return loadFromParent(name);
    }

    @SuppressWarnings("unused")
    private void printDebug(Class<?> c, String cname) {
        printDebug(c, cname, "### FOUND: ");
    }

    private void printDebug(Class<?> c, String cname, String msg) {
        if (c == null) {
            return;
        }
        if (c.getName().equals(cname)) {
            System.out.println(msg + cname + "; CL: " + c.getClassLoader()
                    + " : " + System.identityHashCode(c.getClassLoader())
                    + "; LOCATION: "
                    + c.getProtectionDomain().getCodeSource().getLocation());
        }
    }

}
