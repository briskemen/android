/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.tests;

import com.android.repository.io.FileOpUtils;
import com.android.repository.testframework.FakeProgressIndicator;
import com.android.repository.util.InstallerUtil;
import com.android.testutils.TestUtils;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.android.testutils.TestUtils.getWorkspaceFile;


public class IdeaTestSuiteBase {
  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

  static {
    VfsRootAccess.allowRootAccess("/");  // Bazel tests are sandboxed so we disable VfsRoot checks.
    setProperties();
  }

  private static void setProperties() {
    System.setProperty("idea.home", createTmpDir("tools/idea").toString());
    System.setProperty("gradle.user.home", createTmpDir("home").toString());
    // See AndroidLocation.java for more information on this system property.
    System.setProperty("ANDROID_SDK_HOME", createTmpDir(".android").toString());
    System.setProperty("layoutlib.thread.timeout", "60000");
    setRealJdkPathForGradle();
  }

  protected static Path createTmpDir(String p) {
    Path path = Paths.get(TMP_DIR, p);
    try {
      Files.createDirectories(path);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return path;
  }

  /**
   * Gradle cannot handle a JDK set up with symlinks. It gets confused
   * and in two consecutive executions it thinks that we are calling it
   * with two different JDKs. See
   * https://discuss.gradle.org/t/gradle-daemon-different-context/2146/3
   */
  private static void setRealJdkPathForGradle() {
    try {
      File jdk = new File(TestUtils.getWorkspaceRoot(), "prebuilts/studio/jdk");
      if (jdk.exists()) {
        File file = new File(jdk, "BUILD").toPath().toRealPath().toFile();
        System.setProperty("studio.dev.jdk", file.getParentFile().getAbsolutePath());
      }
    }
    catch (IOException e) {
      // Ignore if we cannot resolve symlinks.
    }
  }

  /**
   * An idea test is run in a temp writable directory. Idea home
   * is set to $TMP/tools/idea. This method creates symlinks from
   * the readonly runfiles to the home directory tree. These
   * directories must first exist as test data for the test.
   */
  protected static void symlinkToIdeaHome(String... targets) {
    try {
      for (String target : targets) {
        Path targetPath = TestUtils.getWorkspaceFile(target).toPath();
        Path linkName = Paths.get(TMP_DIR, target);
        Files.createDirectories(linkName.getParent());
        Files.createSymbolicLink(linkName, targetPath);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void setUpOfflineRepo(@NotNull String repoZip, @NotNull String outputPath) {
    File offlineRepoZip = getWorkspaceFile(repoZip);
    try {
      InstallerUtil.unzip(
        offlineRepoZip,
        createTmpDir(outputPath).toFile(),
        FileOpUtils.create(),
        offlineRepoZip.length(),
        new FakeProgressIndicator());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
