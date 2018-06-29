package org.jenkinsci.plugins;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class UpdatingDbPublisher extends Recorder {

  // http://140.134.26.72:8080/ProgEdu/webapi/commits/update
  private final String progeduDbUrl;
  private final String user;
  private final String proName;

  @DataBoundConstructor
  public UpdatingDbPublisher(String progeduDbUrl, String user, String proName) {
    this.progeduDbUrl = progeduDbUrl;
    this.user = user;
    this.proName = proName;
  }

  public String getProgeduDbUrl() {
    return progeduDbUrl;
  }

  public String getUser() {
    return user;
  }

  public String getProName() {
    return proName;
  }

  public void post(BuildListener listener) {
    String urlParameters = "user=" + user + "&proName=" + proName;
    byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
    int postDataLength = postData.length;
    String checkurl = progeduDbUrl;

    try {
      URL connectto = new URL(checkurl);
      HttpURLConnection conn = (HttpURLConnection) connectto.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setRequestProperty("charset", "utf-8");
      conn.setUseCaches(false);
      conn.setAllowUserInteraction(false);
      conn.setInstanceFollowRedirects(false);
      conn.setDoOutput(true);

      DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
      wr.writeBytes(urlParameters);
      wr.flush();
      wr.close();

      int responseCode = conn.getResponseCode();

      listener.getLogger().println("\nSending 'POST' request to URL : " + checkurl);
      listener.getLogger().println("Post parameters : " + urlParameters);
      listener.getLogger().println("Response Code : " + responseCode);

      BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;

      while ((line = br.readLine()) != null) {
        sb.append(line + "\n");
      }

      listener.getLogger().println("WEB return value is : " + sb);

      conn.disconnect();
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    listener.getLogger()
        .println("--------------------------UpdateDbPublisher--------------------------------");
    listener.getLogger().println("progeduDbUrl : " + progeduDbUrl);
    listener.getLogger().println("user : " + user);
    listener.getLogger().println("proName : " + proName);
    post(listener);
    listener.getLogger()
        .println("--------------------------UpdateDbPublisher--------------------------------");
    return true;
  }

  // Overridden for better type safety.
  // If your plugin doesn't really define any property on Descriptor,
  // you don't have to do this.
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    // TODO Auto-generated method stub
    return BuildStepMonitor.NONE;
  }

  @Symbol("greet")
  @Extension // This indicates to Jenkins that this is an implementation of an
             // extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    // public DescriptorImpl() {
    // load();
    // }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName() {
      return "ProgEdu Update Console to Database";
    }
  }

}
