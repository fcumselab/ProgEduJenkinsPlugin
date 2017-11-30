package org.jenkinsci.plugins;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class TestFileUtils {

  public static void main(String[] args) {
    // TODO Auto-generated method stub
    File dataFile = new File("C:/Users/GJen/Desktop/test/oop-hw1");
    File targetFile = new File("C:/Users/GJen/Desktop/test/oop-hw123");
    if (!targetFile.exists()) {
      targetFile.mkdir();
    }
    if (targetFile.isDirectory()) {// 判断是否是一个目录
      try {
        FileUtils.copyDirectory(dataFile, targetFile);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

}
