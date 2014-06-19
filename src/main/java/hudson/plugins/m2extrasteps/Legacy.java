package hudson.plugins.m2extrasteps;

import hudson.PluginWrapper;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.lifecycle.RestartNotSupportedException;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class Legacy {

    @Initializer(after= InitMilestone.JOB_LOADED, before=InitMilestone.COMPLETED)
    public static void migrate() throws IOException, RestartNotSupportedException {

        // If maven plugin 2.0+ is installed, migrate data and uninstall plugin
        PluginWrapper maven = Jenkins.getInstance().getPluginManager().getPlugin("maven-plugin");
        if (maven != null && maven.getVersionNumber().compareTo(new VersionNumber("2.0")) >= 0) {
            LOGGER.info("Migrating legacy m2-extra-step into (new) maven-plugin extra-step support");
            boolean migreated = false;
            for (MavenModuleSet job : Jenkins.getInstance().getAllItems(MavenModuleSet.class)) {
                for (BuildWrapper bw : job.getBuildWrappersList()) {
                    if (bw instanceof M2ExtraStepsWrapper) {
                        M2ExtraStepsWrapper m2w = (M2ExtraStepsWrapper) bw;
                        job.getPrebuilders().addAll(m2w.getPreBuildSteps());
                        job.getPostbuilders().addAll(m2w.getPostBuildSteps());
                        job.setRunPostStepsIfResult(toResult(m2w.getRunIfResult()));
                        job.getBuildWrappersList().remove(M2ExtraStepsWrapper.class);
                        migreated = true;
                    }
                }
            }
            Jenkins.getInstance().getPluginManager().getPlugin("m2-extra-steps").doDoUninstall();
            if (migreated) Jenkins.getInstance().restart();
        }

    }

    private static Result toResult(String runIfResult) {
        if (runIfResult.equals("success")) return Result.SUCCESS;
        if (runIfResult.equals("unstable")) return Result.UNSTABLE;
        return Result.FAILURE;
    }

    private static final Logger LOGGER = Logger.getLogger(Legacy.class.getName());
}
