/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique, slacoin
 */
package org.nuxeo.build.maven;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DefaultArtifactCollector;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.ant.profile.AntProfileManager;
import org.nuxeo.build.maven.filter.TrueFilter;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 *
 * @goal build
 * @phase package
 *
 * @requiresDependencyResolution runtime
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AntBuildMojo extends AbstractMojo implements MavenClient {

    protected Graph graph;

    protected AntProfileManager antProfileManager;

    /**
     * Location of the build file, if unique
     *
     * @parameter expression="${buildFile}"
     */
    protected File buildFile;

    /**
     * Location of the build files.
     *
     * @parameter expression="${buildFiles}"
     */
    protected File[] buildFiles;

    /**
     * Ant target to call on build file(s).
     *
     * @parameter expression="${target}"
     */
    protected String target;

    /**
     * Ant targets to call on build file(s).
     *
     * @since 1.6
     * @parameter expression="${targets}"
     */
    protected String[] targets;

    /**
     * How many levels the graph must be expanded before running ant.
     *
     * @parameter expression="${expand}" default-value="0"
     */
    protected int expand;

    /**
     * Location of the file.
     *
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * Maven ProjectHelper
     *
     * @component
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected java.util.List<ArtifactRepository> remoteRepositories;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     * @required
     * @readonly
     */
    protected org.apache.maven.artifact.factory.ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     * @required
     * @readonly
     */
    protected org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     *
     * @component
     * @readonly
     */
    protected MavenProjectBuilder projectBuilder;

    /**
     * @component
     * @readonly
     */
    protected ArtifactMetadataSource metadataSource;

    /**
     * The character encoding scheme to be applied.
     *
     * @parameter expression="${encoding}"
     *            default-value="${project.reporting.outputEncoding}"
     */
    private String encoding;

    public String getEncoding() {
        if (StringUtils.isEmpty(encoding)) {
            getLog().warn(
                    "File encoding has not been set, using platform encoding "
                            + ReaderFactory.FILE_ENCODING
                            + ", i.e. build is platform dependent!");
            encoding = ReaderFactory.FILE_ENCODING;
        }
        return encoding;
    }

    private Logger logger;

    public void execute() throws MojoExecutionException, MojoFailureException {
        AntClient ant = new AntClient();
        MavenClientFactory.setInstance(this);
        logger = new Logger() {

            public void debug(String message) {
                getLog().debug(message);
            }

            public void debug(String message, Throwable error) {
                getLog().debug(message, error);
            }

            public void error(String message) {
                getLog().error(message);
            }

            public void error(String message, Throwable error) {
                getLog().error(message, error);
            }

            public void info(String message) {
                getLog().info(message);
            }

            public void info(String message, Throwable error) {
                getLog().info(message, error);
            }

            public void warn(String message) {
                getLog().warn(message);
            }

            public void warn(String message, Throwable error) {
                getLog().warn(message, error);
            }

            public boolean isDebugEnabled() {
                return getLog().isDebugEnabled();
            }

            @Override
            public void info(Throwable error) {
                getLog().info(error);
            }

            @Override
            public void warn(Throwable error) {
                getLog().warn(error);
            }

            @Override
            public void error(Throwable error) {
                getLog().error(error);
            }

            @Override
            public void debug(Throwable error) {
                getLog().debug(error);
            }
        };
        antProfileManager = new AntProfileManager();

        // add project properties
        HashMap<String, String> props = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : project.getProperties().entrySet()) {
            props.put(entry.getKey().toString(), entry.getValue().toString());
        }
        props.put("maven.basedir", project.getBasedir().getAbsolutePath());
        props.put("maven.project.name", project.getName());
        props.put("maven.project.artifactId", project.getArtifactId());
        props.put("maven.project.groupId", project.getGroupId());
        props.put("maven.project.version", project.getVersion());
        props.put("maven.project.packaging", project.getPackaging());
        props.put("maven.project.id", project.getId());
        props.put("maven.project.build.directory",
                project.getBuild().getDirectory());
        props.put("maven.project.build.outputDirectory",
                project.getBuild().getOutputDirectory());
        props.put("maven.project.build.finalName",
                project.getBuild().getFinalName());

        // add active maven profiles to ant
        List<Profile> profiles = getActiveProfiles();
        for (Profile profile : profiles) {
            antProfileManager.activateProfile(profile.getId(), true);
            // define a property for each activate profile (so you can use it in
            // ant conditional expression)
            props.put("maven.profile." + profile.getId(), "true");
        }

        ant.setGlobalProperties(props);

        if (buildFile != null && buildFiles != null && buildFiles.length > 0) {
            throw new MojoExecutionException(
                    "The configuration parameters 'buildFile' and 'buildFiles' cannot both be used.");
        }
        if (buildFiles == null || buildFiles.length == 0) {
            if (buildFile == null) {
                buildFile = new File("build.xml");
            }
            buildFiles = new File[] { buildFile };
        }

        if (target != null && targets != null && targets.length > 0) {
            throw new MojoExecutionException(
                    "The configuration parameters 'target' and 'targets' cannot both be used.");
        }
        if ((targets == null || targets.length == 0) && target != null) {
            targets = new String[] { target };
        }
        for (File file : buildFiles) {
            newGraph();
            if (expand > 0) {
                for (Node rootNode : graph.getRoots()) {
                    graph.resolveDependencyTree(rootNode, new TrueFilter(),
                            expand);
                }
            }

            try {
                if (targets != null && targets.length > 0) {
                    ant.run(file, Arrays.asList(targets));
                } else {
                    ant.run(file);
                }
            } catch (BuildException e) {
                throw new MojoExecutionException("Error occured while running "
                        + file + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * @since 1.10.2
     */
    public Graph newGraph() {
        graph = new Graph(this);
        graph.addRootNode(project);
        return graph;
    }

    @SuppressWarnings("unchecked")
    public List<Profile> getActiveProfiles() {
        return project.getActiveProfiles();
    }

    public MavenProject getProject() {
        return project;
    }

    public ArtifactFactory getArtifactFactory() {
        return factory;
    }

    public ArtifactResolver getArtifactResolver() {
        return resolver;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public MavenProjectBuilder getProjectBuilder() {
        return projectBuilder;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void resolve(Artifact artifact,
            List<ArtifactRepository> otherRemoteRepositories)
            throws ArtifactNotFoundException {
        try {
            resolver.resolve(artifact, otherRemoteRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        } catch (ArtifactNotFoundException e) {
            throw e;
        }
    }

    public void resolve(Artifact artifact) throws ArtifactNotFoundException {
        try {
            resolver.resolve(artifact, remoteRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        } catch (ArtifactNotFoundException e) {
            throw e;
        }
    }

    public void resolveDependencyTree(Artifact artifact, ArtifactFilter filter,
            ResolutionListener listener) throws ArtifactResolutionException,
            ProjectBuildingException {
        MavenProject mavenProject = projectBuilder.buildFromRepository(
                artifact, remoteRepositories, localRepository);
        ArtifactCollector collector = new DefaultArtifactCollector();
        collector.collect(mavenProject.getDependencyArtifacts(),
                mavenProject.getArtifact(),
                mavenProject.getManagedVersionMap(), localRepository,
                mavenProject.getRemoteArtifactRepositories(), metadataSource,
                filter, Collections.singletonList(listener));
    }

    public Graph getGraph() {
        return graph;
    }

    public AntProfileManager getAntProfileManager() {
        return antProfileManager;
    }

    public Logger getCommonLogger() {
        return logger;
    }

}
