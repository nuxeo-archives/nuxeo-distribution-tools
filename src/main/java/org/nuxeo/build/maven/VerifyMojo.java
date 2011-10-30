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
 *     mguillaume
 */
package org.nuxeo.build.maven;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.failsafe.model.FailsafeSummary;
import org.apache.maven.surefire.failsafe.model.io.xpp3.FailsafeSummaryXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 * @goal verify
 * @phase verify
 *
 * @requiresDependencyResolution runtime
 *
 * @author <a href="mailto:mg@nuxeo.com">Mathieu Guillaume</a>
 *
 */
public class VerifyMojo extends AntBuildMojo {

    /**
     * The summary file to write integration test results to.
     *
     * @parameter expression="${project.build.directory}/nxtools-reports/nxtools-summary.xml"
     * @required
     */
    private File summaryFile;

     public File getSummaryFile()
    {
        return summaryFile;
    }

    public void setSummaryFile(File summaryFile) {
        this.summaryFile = summaryFile;
    }

    /**
     * The character encoding scheme to be applied.
     *
     * @parameter expression="${encoding}" default-value="${project.reporting.outputEncoding}"
     */
    private String encoding;

    public void execute() throws MojoExecutionException, MojoFailureException {

        // Get integration-test summary
        int result;
        final FailsafeSummary summary;
        try {
            String encoding;
            if (StringUtils.isEmpty(this.encoding)) {
                getLog().warn(
                    "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                        + ", i.e. build is platform dependent!" );
                encoding = ReaderFactory.FILE_ENCODING;
            } else {
                encoding = this.encoding;
            }

            if (!summaryFile.isFile()) {
                summary = new FailsafeSummary();
            } else {
                summary = readSummary(encoding, summaryFile);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (XmlPullParserException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // Report cumulative results
        result = summary.getResult();
        if (result==0) {
            return;
        } else {
            throw new MojoFailureException("There are some test failures.");
        }

    }

     private FailsafeSummary readSummary(String encoding, File summaryFile)
            throws IOException, XmlPullParserException {

        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        Reader reader = null;
        try {
            fileInputStream = new FileInputStream(summaryFile);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            reader = new InputStreamReader(bufferedInputStream, encoding);
            FailsafeSummaryXpp3Reader xpp3Reader = new FailsafeSummaryXpp3Reader();
            return xpp3Reader.read(reader);
        } finally {
            IOUtil.close(reader);
            IOUtil.close(bufferedInputStream);
            IOUtil.close(fileInputStream);
        }
    }

}
