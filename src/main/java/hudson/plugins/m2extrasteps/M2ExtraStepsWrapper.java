package hudson.plugins.m2extrasteps;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.slaves.NodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Wraps a build with pre and post build steps.  These steps can take
 * any action. This is based on the release plugin, but is specifically
 * intended to provide for additional actions to be run as part of an
 * m2 project build.
 *
 * @author Andrew Bayer
 * @since 1.0
 */
public class M2ExtraStepsWrapper extends BuildWrapper {
    private List<Builder> preBuildSteps = new ArrayList<Builder>();
    private List<Builder> postBuildSteps = new ArrayList<Builder>();
    private String runIfResult;
    
    @DataBoundConstructor
    public M2ExtraStepsWrapper(final String runIfResult) {
        this.runIfResult = runIfResult;
    }
    
    /**
     * @return Returns the preBuildSteps.
     */
    public List<Builder> getPreBuildSteps() {
        return preBuildSteps;
    }
    
    /**
     * @param preBuildSteps The preBuildSteps to set.
     */
    public void setPreBuildSteps(List<Builder> preBuildSteps) {
        this.preBuildSteps = preBuildSteps;
    }
    
    /**
     * @return Returns the postBuildSteps.
     */
    public List<Builder> getPostBuildSteps() {
        return postBuildSteps;
    }
    
    /**
     * @param postBuildSteps The postBuildSteps to set.
     */
    public void setPostBuildSteps(List<Builder> postBuildSteps) {
        this.postBuildSteps = postBuildSteps;
    }
    

    /**
     * @return Returns the runIfResult value.
     */
    public String getRunIfResult() {
        return runIfResult;
    }

    /**
     * @param runIfResult The runIfResult to set.
     */
    public void setRunIfResult(String runIfResult) {
        this.runIfResult = runIfResult;
    }

    private boolean shouldPostStepsRun(AbstractBuild build) {
        // If runIfResult is null, set it to "allCases".
        if (runIfResult == null) {
            setRunIfResult("allCases");
        }
        // If runIfResult is "allCases", we're running regardless.
        if (runIfResult.equals("allCases")) {
            return true;
        }
        else {
            // Otherwise, we're going to need to compare against the build result.
            Result buildResult = build.getResult();
            
            if (runIfResult.equals("success")) {
                return ((buildResult==null) || (buildResult.isBetterOrEqualTo(Result.SUCCESS)));
            }
            else if (runIfResult.equals("unstable")) {
                return ((buildResult==null) || (buildResult.isBetterOrEqualTo(Result.UNSTABLE)));
            }
        }

        // If we get down here, something weird's going on. Return false.
        return false;
    }
    
    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) throws IOException,
                                                                                                          InterruptedException {
        build.addAction(new M2ExtraStepsAction());
            
        if (!executeBuildSteps(preBuildSteps, build, launcher, listener)) {
            throw new IOException("Could not execute pre-build steps");
        }
        
        // return environment
        return new Environment() {
            
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException,
                                                                                        InterruptedException {
                if (shouldPostStepsRun(build)) {
                    return executeBuildSteps(postBuildSteps, build, launcher, listener);
                }
                else {
                    return true;
                }
            }
        };
    }
    
    private boolean executeBuildSteps(List<Builder> buildSteps, AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        boolean shouldContinue = true;
        
        // execute prebuild steps, stop processing if indicated
        for (BuildStep buildStep : buildSteps) {
            
            if (!shouldContinue) {
                break;
            }
            
            shouldContinue = buildStep.prebuild(build, listener);
        }
        
        // execute build step, stop processing if indicated
        for (BuildStep buildStep : buildSteps) {
            
            if (!shouldContinue) {
                break;
            }
            
            shouldContinue = buildStep.perform(build, launcher, listener);
        }
        
        return shouldContinue;
    }
    

    
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Configure M2 Extra Build Steps";
        }
        
        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            M2ExtraStepsWrapper instance = req.bindJSON(M2ExtraStepsWrapper.class, formData);
            instance.preBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "preBuildSteps", Builder.all());
            instance.postBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postBuildSteps", Builder.all());

            return instance;
        }
        
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return (item instanceof AbstractMavenProject);
        }
        
        @Override
        public String getHelpFile() {
            return "/plugin/m2-extra-steps/help-projectConfig.html";
        }
    }
    
    private static final Logger LOGGER = Logger.getLogger(M2ExtraStepsWrapper.class.getName());

}
