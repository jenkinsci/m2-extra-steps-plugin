package hudson.plugins.m2extrasteps;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

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
    
    /**
     * @stapler-constructor
     */
    public M2ExtraStepsWrapper() {
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
    public void setPostBuildSteps(List<Builder> postSuccessBuildSteps) {
        this.postBuildSteps = postSuccessBuildSteps;
    }
    
    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) throws IOException,
                                                                                                          InterruptedException {
        
        if (!executeBuildSteps(preBuildSteps, build, launcher, listener)) {
            throw new IOException("Could not execute pre-build steps");
        }
        
        // return environment
        return new Environment() {
            
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException,
                                                                                        InterruptedException {
                
                // save build
                build.keepLog();
                
                return executeBuildSteps(postBuildSteps, build, launcher, listener);
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
            M2ExtraStepsWrapper instance = new M2ExtraStepsWrapper();
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
    
}
