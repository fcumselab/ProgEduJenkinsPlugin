package org.jenkinsci.plugins;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.Symbol;
import org.json.JSONObject;
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

  // private final static String tempDir = System.getProperty("java.io.tmpdir");
  // private final static String tempDir = "/var/jenkins_home/tests";
  private final static String jenkinsHomeDir = "/var/jenkins_home";
  private final static String testDir = jenkinsHomeDir + "/tests";
  private final static String workspaceDir = jenkinsHomeDir + "/workspace";

  private String jobName = ""; // for workspace
  private String testFileName = ""; // tomcat test zip file path
  // ex. oop-hw1

  private String proDetailUrl = "";
  // http://140.134.26.71:10088/ProgEdu/webapi/project/checksum?proName=OOP-HW1

  private String testFileUrl = "";
  // http://140.134.26.71:10088/ProgEdu/webapi/jenkins/getTestFile?filePath=/usr/local/tomcat/temp/test/oop-hw1.zip

  private String testFileChecksum = "";
  // 12345678

  private String testFilePath = "";

  @DataBoundConstructor
  public AssessmentBuilder(String jobName, String testFileName, String proDetailUrl,
      String testFileUrl, String testFileChecksum) {
    this.jobName = jobName;
    this.testFileName = testFileName;
    this.proDetailUrl = proDetailUrl;
    this.testFileUrl = testFileUrl;
    this.testFileChecksum = testFileChecksum;
  }

  public String getJobName() {
    return jobName;
  }

  public String getTestFileName() {
    return testFileName;
  }

  public String getProDetailUrl() {
    return proDetailUrl;
  }

  public String getTestFileUrl() {
    return testFileUrl;
  }

  public String getTestFileChecksum() {
    return testFileChecksum;
  }

  public void cpFile() {
    File dataFile = new File(testFilePath);
    File targetFile = new File(workspaceDir + "/" + jobName);
    if (targetFile.isDirectory()) {// Check if it is the same file
      try {
        FileUtils.copyDirectory(dataFile, targetFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getProDetailJson(String proDetailUrl) {
    String json = null;
    HttpURLConnection conn = null;
    try {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }

      URL url = new URL(proDetailUrl);
      conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(10000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("GET");
      conn.connect();
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(conn.getInputStream(), "UTF-8"));
      String jsonString1 = reader.readLine();
      reader.close();

      json = jsonString1;

    } catch (Exception e) {
      System.out.print("Error : ");
      e.printStackTrace();
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return json;
  }

  private String getDatabaseChecksum(String strJson) {
    JSONObject json = new JSONObject(strJson);
    String currentChecksum = json.getString("testZipChecksum");
    return currentChecksum;
  }

  private String getTestZipUrl(String strJson) {
    JSONObject json = new JSONObject(strJson);
    String testZipUrl = json.getString("testZipUrl");
    return testZipUrl;
  }

  private String getChecksumFromZip(String zipFilePath) {
    String strChecksum = "";

    CheckedInputStream cis = null;
    try {
      cis = new CheckedInputStream(new FileInputStream(zipFilePath), new CRC32());
      byte[] buf = new byte[1024];
      // noinspection StatementWithEmptyBody
      while (cis.read(buf) >= 0) {
      }
      System.out.println(cis.getChecksum().getValue());
      strChecksum = String.valueOf(cis.getChecksum().getValue());
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (cis != null) {
        try {
          cis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return strChecksum;
  }

  private void downloadTestZipFromTomcat(String testZipUrl, TaskListener listener) {
    URL website;

    // destZipPath = test zip file path
    String destZipPath = testFilePath + ".zip";
    // /var/jenkins_home/tests/oop-hw1.zip
    FileOutputStream fos = null;
    try {
      website = new URL(testZipUrl);
      ReadableByteChannel rbc = Channels.newChannel(website.openStream());
      fos = new FileOutputStream(destZipPath);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    } catch (IOException e) {
      listener.getLogger().println("exception : " + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        fos.close();
      } catch (IOException e) {
        listener.getLogger().println("exception : " + e.getMessage());
      }
    }
  }

  private boolean checksumEquals(String currentChecksum) {
    boolean same = true;

    if (testFileChecksum.isEmpty()) {
      same = false;
    } else {
      if (currentChecksum.equals(testFileChecksum)) {
        same = true;
      } else {
        same = false;
      }
    }
    return same;
  }

  /**
   * Size of the buffer to read/write data
   */
  private static final int BUFFER_SIZE = 4096;

  /**
   * Extracts a zip file specified by the zipFilePath to a directory specified by destDirectory
   * (will be created if does not exists)
   * 
   * @param zipFilePath
   * @param destDirectory
   */
  private void unzip(String zipFilePath, String destDirectory, TaskListener listener) {
    File destDir = new File(destDirectory);
    if (!destDir.exists()) {
      destDir.mkdir();
    } else {
      destDir.delete();
    }

    execLinuxCommand("rm -rf " + destDirectory, listener);

    FileInputStream fileInputStream = null;
    ZipInputStream zipIn = null;
    try {
      fileInputStream = new FileInputStream(zipFilePath);
      listener.getLogger().println(" checksum in zip does not equals to checksum in setting");
      zipIn = new ZipInputStream(fileInputStream);
      ZipEntry entry = zipIn.getNextEntry();
      // iterates over entries in the zip file
      while (entry != null) {
        String filePath = destDirectory + File.separator + entry.getName();
        File newFile = new File(filePath);

        System.out.println("filePath : " + filePath);
        listener.getLogger().println("filePath : " + filePath);

        // create all non exists folders
        // else you will hit FileNotFoundException for compressed folder
        new File(newFile.getParent()).mkdirs();

        if (!entry.isDirectory()) {
          // if the entry is a file, extracts it
          extractFile(zipIn, filePath);
        } else {
          // if the entry is a directory, make the directory
          File dir = new File(filePath);
          dir.mkdir();
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();

      }
    } catch (IOException e) {
      listener.getLogger().println("exception : " + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        fileInputStream.close();
        zipIn.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Extracts a zip entry (file entry)
   * 
   * @param zipIn
   * @param filePath
   * @throws IOException
   */
  private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
    byte[] bytesIn = new byte[BUFFER_SIZE];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
  }

  private void execLinuxCommand(String command, TaskListener listener) {
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
        listener.getLogger().println("line : " + line);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {

    // open /var/jenkins_home/tests
    File testDirFile = new File(testDir);
    if (!testDirFile.exists()) {
      testDirFile.mkdirs();
    }

    String strJson = null;
    if (!proDetailUrl.equals("")) {
      strJson = getProDetailJson(proDetailUrl);
    }

    // /var/jenkins_home/tests/oop-hw1
    testFilePath = testDir + "/" + testFileName;

    String tempZipChecksum = null;
    String testZipUrl = getTestZipUrl(strJson);

    File zipTestFile = new File(testFilePath + ".zip");
    if (!zipTestFile.exists()) {
      // If test zip file does not exist, download it.

      // 1. Get the zip url from tomcat
      testZipUrl = getTestZipUrl(strJson);

      // 2. Download the zip to /var/jenkins_home/tests/OOP-HWX.zip
      downloadTestZipFromTomcat(testZipUrl, listener);

      // 3. Unzip the zip
      unzip(testFilePath + ".zip", testFilePath, listener);

      // 4. Get the checksum from temp zip
      tempZipChecksum = getChecksumFromZip(testFilePath + ".zip");

      // 5. Change the detail in Jenkins job
      testFileChecksum = tempZipChecksum;
      testFileUrl = testZipUrl;
    } else {
      // Zip file exists.

      // 1. Get the current temp zip checksum
      tempZipChecksum = getChecksumFromZip(testFilePath + ".zip");

      // 2. Get the current database checksum
      String databaseChecksum = getDatabaseChecksum(strJson);

      // 3. Check this tempZipChecksum equals to databaseChecksum
      if (tempZipChecksum.equals(databaseChecksum)) {
        // tempZipChecksum equals to databaseChecksum

        // Check if the zip has been already unzip
        File testFile = new File(testFilePath);
        if (testFile.exists()) {
          // testFile exists

          // copy to workspace
        } else {
          // testFile doesn't exist
          unzip(testFilePath + ".zip", testFilePath, listener);
        }
      } else {
        // tempZipChecksum doesn't equal to databaseChecksum
        testZipUrl = getTestZipUrl(strJson);
        downloadTestZipFromTomcat(testZipUrl, listener);
        unzip(testFilePath + ".zip", testFilePath, listener);
      }
    }

    // Final
    // Copy test file to workspace
    // Change the detail checksum with temp zip
    cpFile();
    testFileChecksum = tempZipChecksum;
    testFileUrl = testZipUrl;

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
