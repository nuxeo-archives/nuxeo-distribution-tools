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

/**
 * This is a sample of how to use NuxeoApp.
 * This sample is building a core server version 5.3.1-SNAPSHOT,
 * and then starts it.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    public static void main(String[] args) throws Exception {
        File home = null;
        String version = "5.3.1-SNAPSHOT";
        String profile = NuxeoApp.CORE_SERVER;
        String host = "localhost:8080";
        String h = "localhost";
        int port = 8080;
        String opt = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                opt = arg;
            } else if (opt != null) {
                if ("-v".equals(opt)) {
                    version = arg;
                } else if ("-p".equals(opt)) {
                    profile = arg;
                } else if ("-h".equals(opt)) {
                    host = arg;
                }
            } else { // the home directory
                home = arg.startsWith("/") ? new File(arg) : new File(".", arg);
                
            }
        }
        
        if (home == null) {
            System.err.println("Syntax error: You must specify a home directory to be used by the nuxeo server.");
            System.exit(1);
        }
        if (host != null) {            
            int p = host.indexOf(':');
            if (p != -1) {
                h = host.substring(0, p);
                p = Integer.parseInt(host.substring(p+1));
            }
        }

        home = home.getCanonicalFile();
        
        System.out.println("+---------------------------------------------------------");
        System.out.println("| Nuxeo Server Profile: "+profile+"; version: "+version);
        System.out.println("| Home Directory: "+home);
        System.out.println("| HTTP server at: "+host+":"+port);
        System.out.println("+---------------------------------------------------------\n");
        
        
        //FileUtils.deleteTree(home);
        final NuxeoApp app = new NuxeoApp(home);
        app.build(profile, version, true);
        NuxeoApp.setHttpServerAddress(h, port);
        Runtime.getRuntime().addShutdownHook(new Thread("Nuxeo Server Shutdown") {
            @Override
            public void run() {
                try {
                    app.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        app.start();
        
        //System.out.println("Hello!!");
        //app.shutdown();
    }
    

}
