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
package org.nuxeo.dev;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a sample of how to use NuxeoApp. This sample is building a core
 * server version 5.3.1-SNAPSHOT, and then starts it.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Main {

    public static void main(String[] args) throws Exception {

        File home = null;
        String profile = NuxeoApp.DEFAULT;
        String host = "localhost";
        int port = 8080;
        String config = null;
        String mainType = null;
        String updatePolicy = "daily";
        boolean offline = false;
        String opt = null;
        boolean noCache = false;
        String[] extBundles = null;
        String[] extLibs = null;
        boolean sharedcl = false;
        for (String arg : args) {
            if (arg.equals("-o")) {
                offline = true;
            } else if (arg.equals("--nocache")) {
                noCache = true;
            } else if (arg.equals("-shared")) {
                sharedcl = true;
            } else if (arg.startsWith("-")) {
                opt = arg;
            } else if (opt != null) {
                if ("-p".equals(opt)) {
                    profile = arg;
                } else if ("-u".equals(opt)) {
                    updatePolicy = arg;
                } else if ("-h".equals(opt)) {
                    int p = arg.indexOf(':');
                    if (p != -1) {
                        host = arg.substring(0, p);
                        port = Integer.parseInt(arg.substring(p + 1));
                    } else {
                        host = arg;
                    }
                } else if ("-c".equals(opt)) {
                    config = arg;
                } else if ("-e".equals(opt)) {
                    mainType = arg;
                } else if ("-bundles".equals(opt)) {
                    extBundles = splitCp(arg);
                } else if ("-libs".equals(opt)) {
                    extLibs = splitCp(arg);
                }
                opt = null;
            } else { // the home directory
                home = arg.startsWith("/") ? new File(arg) : new File(".", arg);
                opt = null;
            }
        }

        if (home == null) {
            System.err.println("Syntax error: You must specify a home directory to be used by the nuxeo server.");
            System.exit(1);
        }

        home = home.getCanonicalFile();
        Method mainMethod = null;
        if (mainType != null) {
            mainMethod = Class.forName(mainType).getMethod("main",
                    String[].class);
        }

        System.out.println("+---------------------------------------------------------");
        System.out.println("| Nuxeo Server Profile: "
                + (profile == null ? "custom" : profile));
        System.out.println("| Home Directory: " + home);
        System.out.println("| HTTP server at: " + host + ":" + port);
        System.out.println("| Use cache: " + !noCache
                + "; Snapshot update policy: " + updatePolicy + "; offline: "
                + offline);
        System.out.println("+---------------------------------------------------------\n");

        // FileUtils.deleteTree(home);
        final NuxeoApp app = new NuxeoApp(home, null, !sharedcl);
        Thread.currentThread().setContextClassLoader(
                app.bootstrap.getClassLoader());
        if (extBundles != null) {
            app.setExternalBundles(extBundles);
        }
        if (extLibs != null) {
            app.setExternalLibs(extLibs);
        }
        app.setVerbose(true);
        app.setOffline(offline);
        app.setUpdatePolicy(updatePolicy);
        if (config != null) {
            app.build(makeUrl(config), !noCache);
        } else {
            app.build(profile, !noCache);
        }
        NuxeoApp.setHttpServerAddress(host, port);

        app.start();

        if (mainMethod == null) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread("Nuxeo Server Shutdown") {
                        @Override
                        public void run() {
                            try {
                                app.shutdown();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } else {
            try {
                mainMethod.invoke(null, new Object[] { args });
            } finally {
                app.shutdown();
            }
        }
    }

    protected static URL makeUrl(String spec) {
        try {
            if (spec.indexOf(':') > -1) {
                if (spec.startsWith("java:")) {
                    spec = spec.substring(5);
                    ClassLoader cl = getContextClassLoader();
                    URL url = cl.getResource(spec);
                    if (url == null) {
                        fail("Canot found java resource: " + spec);
                    }
                    return url;
                } else {
                    return new URL(spec);
                }
            } else {
                return new File(spec).toURI().toURL();
            }
        } catch (Exception e) {
            fail("Invalid config file soecification. Not a valid URL or file: "
                    + spec);
            return null;
        }
    }

    protected static void fail(String msg) {
        System.err.println(msg);
        System.exit(2);
    }

    protected static ClassLoader getContextClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl == null ? Main.class.getClassLoader() : cl;
    }

    protected static String[] splitCp(String expr) {
        expr = expr.trim();
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            expr = expr.substring(1, expr.length() - 1).trim();
        }
        return split(expr, ':', true);
    }

    public static String[] split(String str, char delimiter, boolean trim) {
        int s = 0;
        int e = str.indexOf(delimiter, s);
        if (e == -1) {
            if (trim) {
                str = str.trim();
            }
            return new String[] { str };
        }
        List<String> ar = new ArrayList<String>();
        do {
            String segment = str.substring(s, e);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
            s = e + 1;
            e = str.indexOf(delimiter, s);
        } while (e != -1);

        int len = str.length();
        if (s < len) {
            String segment = str.substring(s);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
        } else {
            ar.add("");
        }

        return ar.toArray(new String[ar.size()]);
    }

}
