package hudson.plugins.m2extrasteps;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Hudson;
import hudson.model.InvisibleAction;
import hudson.model.Job;
import hudson.model.Run;
import hudson.slaves.NodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class M2ExtraStepsAction extends InvisibleAction implements EnvironmentContributingAction {
    public M2ExtraStepsAction() {

    }
    
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        for (NodeProperty nodeProperty: Hudson.getInstance().getGlobalNodeProperties()) {
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                env.overrideAll(((EnvironmentVariablesNodeProperty)nodeProperty).getEnvVars());
            }
        }
        for (NodeProperty nodeProperty: Computer.currentComputer().getNode().getNodeProperties()) {
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                env.overrideAll(((EnvironmentVariablesNodeProperty)nodeProperty).getEnvVars());
            }
        }
    }
}
