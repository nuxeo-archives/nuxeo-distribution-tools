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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.failsafe.model.FailsafeSummary;
import org.apache.maven.surefire.failsafe.model.io.xpp3.FailsafeSummaryXpp3Writer;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @goal integration-test
 * @phase integration-test
 *
 * @requiresDependencyResolution runtime
 *
 * @author <a href="mailto:mg@nuxeo.com">Mathieu Guillaume</a>
 *
 */
public class IntegrationTestMojo extends AntBuildMojo {

    /**
     * Set this to "false" to fail on errors during integration testing.
     *
     * @parameter default-value="true" expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    public boolean isTestFailureIgnore()
    {
        return testFailureIgnore;
    }

    public void setTestFailureIgnore( boolean testFailureIgnore )
    {
        this.testFailureIgnore = testFailureIgnore;
    }

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

    public void setSummaryFile( File summaryFile )
    {
        this.summaryFile = summaryFile;
    }

    /**
     * The character encoding scheme to be applied.
     *
     * @parameter expression="${encoding}" default-value="${project.reporting.outputEncoding}"
     */
    private String encoding;

    public void execute() throws MojoExecutionException, MojoFailureException {

        FailsafeSummary result = new FailsafeSummary();

        try {
            super.execute();
        } catch (MojoExecutionException e) {
            if (!isTestFailureIgnore()) {
                throw e;
            } else {
                getLog().error(e.getMessage());
                result.setResult(ProviderConfiguration.TESTS_FAILED_EXIT_CODE);
                result.setException(e.getMessage());
                writeSummary(result);
            }
        }

    }

     private void writeSummary(FailsafeSummary summary) throws MojoExecutionException {

        File summaryFile = getSummaryFile();
        if (!summaryFile.getParentFile().isDirectory()) {
            summaryFile.getParentFile().mkdirs();
        }
        try {
            String encoding;
            if ( StringUtils.isEmpty( this.encoding ) ) {
                getLog().warn(
                    "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                        + ", i.e. build is platform dependent!" );
                encoding = ReaderFactory.FILE_ENCODING;
            } else {
                encoding = this.encoding;
            }

            FileOutputStream fileOutputStream = new FileOutputStream(summaryFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            Writer writer = new OutputStreamWriter(bufferedOutputStream, encoding);
            FailsafeSummaryXpp3Writer xpp3Writer = new FailsafeSummaryXpp3Writer();
            xpp3Writer.write(writer, summary);
            writer.close();
            bufferedOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
