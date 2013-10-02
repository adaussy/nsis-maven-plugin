/*
 * Copyright 2008 Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.nsis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generate a <code>project.nsh</code> containing NSIS variables that represent the common POM values.
 * <p>
 * The resulting <code>target/project.nsh</code> (configurable) can then be loaded into the main <code>setup.nsi</code>
 * script via the include command.
 * </p>
 * 
 * <pre>
 *   !include target\project.nsh
 * </pre>
 * 
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 */
@Mojo( name = "generate-headerfile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class GenerateHeaderfileMojo
    extends AbstractMojo
{

    /**
     * The name of the project script.
     */
    @Parameter( property = "nsis.headerfile", defaultValue = "${project.build.directory}/project.nsh", required = true )
    private File headerFile;

    /**
     * The Maven project itself.
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    private MavenProject project;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        FileWriter filewriter = null;
        MessageWriter writer = null;

        try
        {
            File parentDir = headerFile.getParentFile();

            if ( parentDir != null && !parentDir.exists() )
            {
                if ( !parentDir.mkdirs() )
                {
                    throw new MojoFailureException( "Unable to create parent directory " + parentDir.getAbsolutePath()
                        + " for header file " + headerFile.getAbsolutePath() );
                }
            }

            filewriter = new FileWriter( headerFile );
            writer = new MessageWriter( filewriter );

            writer.println( "; Template for project details" );
            writer.println( "; Generated by {0} from pom.xml version {1}", System.getProperty( "user.name" ),
                         project.getVersion() );
            writer.println( "; on date {0,date}, time {0,time}", new Date() );
            writer.println( "" );

            writer.println( "!define PROJECT_BASEDIR \"{0}\"", project.getBasedir() );
            writer.println( "!define PROJECT_BUILD_DIR \"{0}\"", project.getBuild().getDirectory() );
            writer.println( "!define PROJECT_FINAL_NAME \"{0}\"", project.getBuild().getFinalName() );

            // TODO: have variable for license?
            // TODO: how do we deal with multiple licenses?
            writer.println( "!define PROJECT_GROUP_ID \"{0}\"", project.getGroupId() );
            writer.println( "!define PROJECT_ARTIFACT_ID \"{0}\"", project.getArtifactId() );
            writer.println( "!define PROJECT_NAME \"{0}\"", project.getName() );
            writer.println( "!define PROJECT_VERSION \"{0}\"", project.getVersion() );

            if ( StringUtils.isNotEmpty( project.getUrl() ) )
            {
                writer.println( "!define PROJECT_URL \"{0}\"", project.getUrl() );
            }

            if ( project.getOrganization() != null )
            {
                writer.println( "!define PROJECT_ORGANIZATION_NAME \"{0}\"", project.getOrganization().getName() );
                writer.println( "!define PROJECT_ORGANIZATION_URL \"{0}\"", project.getOrganization().getUrl() );
                writer.println( "!define PROJECT_REG_KEY \"SOFTWARE\\{0}\\{1}\\{2}\"", new Object[] {
                    project.getOrganization().getName(), project.getName(), project.getVersion() } );
                writer.println( "!define PROJECT_REG_UNINSTALL_KEY "
                                 + "\"Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{0} {1}\"",
                             project.getName(),
                             project.getVersion() );
                writer.println( "!define PROJECT_STARTMENU_FOLDER \"{0}\\{1}\\{2} {3}\"", new Object[] { "$SMPROGRAMS",
                    project.getOrganization().getName(), project.getName(), project.getVersion() } );
            }
            else
            {
                writer.println( "; The project organization section is missing from your pom.xml" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to generate project script " + headerFile.getAbsolutePath(), e );
        }
        finally
        {
            IOUtil.close( writer );
            IOUtil.close( filewriter );
        }
    }

    class MessageWriter
        extends PrintWriter
    {
        public MessageWriter( Writer out )
        {
            super( out );
        }

        public void println( String pattern, Object arguments[] )
        {
            println( MessageFormat.format( pattern, arguments ) );
        }

        public void println( String pattern, Object arg1 )
        {
            println( pattern, new Object[] { arg1 } );
        }

        public void println( String pattern, Object arg1, Object arg2 )
        {
            println( pattern, new Object[] { arg1, arg2 } );
        }
    }

}
