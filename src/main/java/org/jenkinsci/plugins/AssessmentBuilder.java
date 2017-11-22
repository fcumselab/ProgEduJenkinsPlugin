package org.jenkinsci.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class AssessmentBuilder extends Builder implements SimpleBuildStep {

  private final static String tempDir = System.getProperty("java.io.tmpdir");
  private String jobName;
  private String testFilePath;

  @DataBoundConstructor
  public AssessmentBuilder(String jobName, String testFilePath) {
    this.jobName = jobName;
    this.testFilePath = testFilePath;
  }

  public String getJobName() {
    return jobName;
  }

  public String getTestFilePath() {
    return testFilePath;
  }

  public String setUpCpCommand() {
    String cpCommand = "";
    cpCommand = "cp -r " + tempDir + "/progedu/" + testFilePath + " /var/jenkins_home/workspace/" + jobName + "/src";
    return cpCommand;
  }

  public void cpFile() {
    // cp -r
    // test file resource : /tmp/progedu/oop-hw1/src/test
    // workspace : /var/jenkins_home/workspace/oop-hw1/src

    // extract src/test file, delete the first folder name (ex. oop-hw1/src/test
    // -> src/test)

    // testFilePath = "C:/WINDOWS/TEMP/progedu/TestPlugin/src/test";
    // jobName = "A:/jenkins/workspace/TestPlugin/src";
    // String cpCommand = "cp -r " + testFilePath + " " + jobName;

    String cpCommand = setUpCpCommand();
    execLinuxCommand(cpCommand);
  }

  private void execLinuxCommand(String command) {
    Process process;

    try {
      process = Runtime.getRuntime().exec(command);
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while (true) {
        line = br.readLine();
        if (line == null) {
          break;
        }
        System.out.println(line);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {
    // cpFile();
    String cpCommand = setUpCpCommand();
    execLinuxCommand(cpCommand);
    listener.getLogger().println("cpCommand : " + cpCommand);
    listener.getLogger().println("jobName : " + jobName);
    listener.getLogger().println("testFilePath : " + testFilePath);
  }

  @Symbol("greet")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "ProgEdu Unit Test Assessment";
    }

  }

}
