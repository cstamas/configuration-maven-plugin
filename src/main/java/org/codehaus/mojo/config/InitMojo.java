package org.codehaus.mojo.config;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Simple initialization mojo that verifies that extensions are enabled for this plugin.
 */
@Mojo(name = "init", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class InitMojo
    extends AbstractMojo
{
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!ConfigLifecycleParticipant.ACTIVATED) {
      throw new MojoExecutionException(
          "Plugin mis-configured: configuration-maven-plugin must have extensions enabled"
      );
    }
  }
}
