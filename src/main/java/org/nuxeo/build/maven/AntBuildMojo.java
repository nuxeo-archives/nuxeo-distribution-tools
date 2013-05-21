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
import org.apache.maven.artifact.manager.WagonManager;
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
import org.apache.maven.settings.MavenSettingsBuilder;
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

    /**
     * @component
     * @readonly
     * @since 1.12
     */
    protected WagonManager wagonManager;

    /**
     * @component
     * @readonly
     * @since 1.12
     */
    protected MavenSettingsBuilder settingsBuilder;

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

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void execute() throws MojoExecutionException, MojoFailureException {
        AntClient ant = new AntClient();
        MavenClientFactory.setInstance(this);
        wagonManager.setInteractive(false);

        logger = new Logger() {

            @Override
            public void debug(String message) {
                getLog().debug(message);
            }

            @Override
            public void debug(String message, Throwable error) {
                getLog().debug(message, error);
            }

            @Override
            public void error(String message) {
                getLog().error(message);
            }

            @Override
            public void error(String message, Throwable error) {
                getLog().error(message, error);
            }

            @Override
            public void info(String message) {
                getLog().info(message);
            }

            @Override
            public void info(String message, Throwable error) {
                getLog().info(message, error);
            }

            @Override
            public void warn(String message) {
                getLog().warn(message);
            }

            @Override
            public void warn(String message, Throwable error) {
                getLog().warn(message, error);
            }

            @Override
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

        HashMap<String, String> props = new HashMap<String, String>();
        // add project properties
        for (String key : project.getProperties().stringPropertyNames()) {
            props.put(key, project.getProperties().getProperty(key));
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
        props.put("maven.offline", wagonManager.isOnline() ? "" : "-o");

        // add active Maven profiles to Ant
        List<Profile> profiles = getActiveProfiles();
        for (Profile profile : profiles) {
            antProfileManager.activateProfile(profile.getId(), true);
            // define a property for each activated profile
            props.put("maven.profile." + profile.getId(), "true");
            // add profile properties (overriding project ones)
            for (String key : profile.getProperties().stringPropertyNames()) {
                props.put(key, profile.getProperties().getProperty(key));
            }
        }
        // Finally add System properties (overriding project and profile ones)
        props.putAll((Map) System.getProperties());

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
                throw new MojoExecutionException(
                        "Error occurred while running " + file + ": "
                                + e.getMessage(), e);
            }
        }
    }

    /**
     * @since 1.10.2
     */
    @Override
    public Graph newGraph() {
        graph = new Graph(this);
        graph.addRootNode(project);
        return graph;
    }

    @Override
    public List<Profile> getActiveProfiles() {
        return project.getActiveProfiles();
    }

    public MavenProject getProject() {
        return project;
    }

    @Override
    public ArtifactFactory getArtifactFactory() {
        return factory;
    }

    @Override
    public ArtifactResolver getArtifactResolver() {
        return resolver;
    }

    @Override
    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    @Override
    public MavenProjectBuilder getProjectBuilder() {
        return projectBuilder;
    }

    @Override
    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    @Override
    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    @Override
    public void resolve(Artifact artifact,
            List<ArtifactRepository> otherRemoteRepositories)
            throws ArtifactNotFoundException {
        try {
            resolver.resolve(artifact, otherRemoteRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        } catch (ArtifactNotFoundException e) {
            tryResolutionOnLocalBaseVersion(artifact, e);
        }
    }

    @Override
    public void resolve(Artifact artifact) throws ArtifactNotFoundException {
        try {
            resolver.resolve(artifact, remoteRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        } catch (ArtifactNotFoundException e) {
            tryResolutionOnLocalBaseVersion(artifact, e);
        }
    }

    /**
     * Try to locally resolve an artifact with its "unique" version.
     *
     * @since 1.11.1
     * @param artifact Artifact to resolve with its unique version
     * @param e ArtifactNotFoundException originally thrown
     * @throws ArtifactNotFoundException If alternate resolution failed.
     * @see Artifact#getBaseVersion()
     */
    protected void tryResolutionOnLocalBaseVersion(Artifact artifact,
            ArtifactNotFoundException e) throws ArtifactNotFoundException {
        String resolvedVersion = artifact.getVersion();
        artifact.updateVersion(artifact.getBaseVersion(), localRepository);
        File localFile = new File(localRepository.getBasedir(),
                localRepository.pathOf(artifact));
        if (localFile.exists()) {
            getLog().warn(
                    String.format(
                            "Couldn't resolve %s, fallback on local install of unique version %s.",
                            resolvedVersion, artifact.getBaseVersion()));
            artifact.setResolved(true);
        } else {
            // No success, set back the previous version and raise an error
            artifact.updateVersion(resolvedVersion, localRepository);
            throw e;
        }
    }

    @Override
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

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public AntProfileManager getAntProfileManager() {
        return antProfileManager;
    }

    @Override
    public Logger getCommonLogger() {
        return logger;
    }

}
