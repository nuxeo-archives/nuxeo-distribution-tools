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
package org.nuxeo.dev;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.embedder.MavenEmbedderLogger;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.dev.ConfigurationReader.SectionReader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ConfigurationLoader {

    protected final Map<String,String> config = new HashMap<String,String>();

    protected final Set<String> bundles = new LinkedHashSet<String>();

    protected final Set<String> libs = new LinkedHashSet<String>();

    protected final Set<String> poms = new LinkedHashSet<String>();

    protected final Map<String, String> props = new HashMap<String,String>();

    protected final ConfigurationReader reader = new ConfigurationReader();

    protected ArtifactDescriptor templateArtifact;

    protected String templatePrefix;


    public ConfigurationLoader() {
        reader.addReader("configuration", new PropertiesReader(config));
        reader.addReader("bundles", new ArtifactReader(bundles));
        reader.addReader("libs", new ArtifactReader(libs));
        reader.addReader("properties", new PropertiesReader(props));
        reader.addReader("template", new TemplateReader());
        reader.addReader("poms", new ArtifactReader(poms));
    }

    public Map<String, String> getProperties() {
        return props;
    }

    public Set<String> getPoms() {
        return poms;
    }

    public ConfigurationReader getReader() {
        return reader;
    }

    public Set<String> getBundles() {
        return bundles;
    }

    public Set<String> getLibs() {
        return libs;
    }

    public boolean isOffline(boolean defaultValue) {
        if (!config.containsKey("offline")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(config.get("offline"));
    }

    public int logThreshold(int defaultValue) {
        if (!config.containsKey("logThreshold")) {
            return defaultValue;
        }
        return Integer.parseInt(config.get("logThreshold"));
    }

    public ArtifactDescriptor getTemplateArtifact() {
        return templateArtifact;
    }

    public String getTemplatePrefix() {
        return templatePrefix;
    }

    class ArtifactReader implements SectionReader {
        protected Set<String> result;

        ArtifactReader(Set<String> result) {
            this.result = result;
        }

        public void readLine(String section, String line) throws IOException {
            result.add(expandVars(line, props));
        }
    }

    class PropertiesReader implements SectionReader {
        final Map<String,String> props;
        public PropertiesReader(Map<String,String> map) {
           props = map;
        }
        public void readLine(String section, String line) throws IOException {
            int p = line.indexOf('=');
            if (p == -1) {
                throw new IOException("Invalid properties line: " + line);
            }
            String key = line.substring(0, p).trim();
            String value = line.substring(p + 1).trim();
            props.put(key, value);
        }
    }

    class TemplateReader implements SectionReader {
        public void readLine(String section, String line) throws IOException {
            int p = line.indexOf('=');
            if (p == -1) {
                throw new IOException("Invalid configuration line: " + line);
            }
            String key = line.substring(0, p).trim();
            String value = line.substring(p + 1).trim();
            if ("path".equals(key)) {
                templatePrefix = expandVars(value, props);
            } else if ("artifact".equals(key)) {
                templateArtifact = new ArtifactDescriptor(expandVars(value,
                        props));
            } else {
                throw new IOException("Unknown configuration property: " + key);
            }
        }
    }

    /**
     * Expands any variable found in the given expression with the values in the
     * given map.
     * <p>
     * The variable format is ${property_key}.
     *
     * @param expression the expression to expand
     * @param properties a map containing variables
     * @return expanded variable
     */
    public static String expandVars(String expression, Map<?, ?> properties) {
        int p = expression.indexOf("${");
        if (p == -1) {
            return expression; // do not expand if not needed
        }

        char[] buf = expression.toCharArray();
        StringBuilder result = new StringBuilder(buf.length);
        if (p > 0) {
            result.append(expression.substring(0, p));
        }
        StringBuilder varBuf = new StringBuilder();
        boolean dollar = false;
        boolean var = false;
        for (int i = p; i < buf.length; i++) {
            char c = buf[i];
            switch (c) {
            case '$':
                dollar = true;
                break;
            case '{':
                if (dollar) {
                    dollar = false;
                    var = true;
                } else {
                    result.append(c);
                }
                break;
            case '}':
                if (var) {
                    var = false;
                    String varName = varBuf.toString();
                    varBuf.setLength(0);
                    // get the variable value
                    Object varValue = properties.get(varName);
                    if (varValue != null) {
                        result.append(varValue.toString());
                    } else { // let the variable as is
                        result.append("${").append(varName).append('}');
                    }
                } else {
                    result.append(c);
                }
                break;
            default:
                if (var) {
                    varBuf.append(c);
                } else {
                    result.append(c);
                }
                break;
            }
        }
        return result.toString();
    }

}
