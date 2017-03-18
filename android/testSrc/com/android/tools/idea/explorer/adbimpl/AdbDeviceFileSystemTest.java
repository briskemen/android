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
package com.android.tools.idea.explorer.adbimpl;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.explorer.fs.DeviceFileEntry;
import com.android.tools.idea.explorer.fs.DeviceState;
import com.android.tools.idea.explorer.fs.FileTransferProgress;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.concurrency.BoundedTaskExecutor;
import org.hamcrest.core.IsInstanceOf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.PooledThreadExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;

public class AdbDeviceFileSystemTest {
  private static final long TIMEOUT_MILLISECONDS = 30_000;

  @Nullable private Disposable myParentDisposable;
  @Nullable private AdbDeviceFileSystem myFileSystem;
  @Nullable private MockDdmlibDevice myMockDevice;
  @Nullable private BoundedTaskExecutor myCallbackExecutor;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    myParentDisposable = Disposer.newDisposable();
    myCallbackExecutor = new BoundedTaskExecutor("EDT simulation thread",
                                                 PooledThreadExecutor.INSTANCE,
                                                 1,
                                                 myParentDisposable);
    ExecutorService taskExecutor = PooledThreadExecutor.INSTANCE;
    myMockDevice = new MockDdmlibDevice();
    TestDevices.addNexus7Api23Commands(myMockDevice.getShellCommands());
    Function<Void, File> adbRuntimeError = aVoid -> {
      throw new RuntimeException("No Adb for unit tests");
    };
    AdbDeviceFileSystemService service = new AdbDeviceFileSystemService(adbRuntimeError, myCallbackExecutor, taskExecutor);
    myFileSystem = new AdbDeviceFileSystem(service, myMockDevice.getIDevice());
  }

  @After
  public void cleanUp() {
    if (myParentDisposable != null) {
      Disposer.dispose(myParentDisposable);
    }
  }

  private static <V> V waitForFuture(@NotNull Future<V> future) throws Exception {
    assert !java.awt.EventQueue.isDispatchThread();
    return future.get(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
  }

  @Test
  public void test_FileSystem_Has_DeviceName() throws Exception {
    // Prepare
    assert myFileSystem != null;
    assert myMockDevice != null;

    // Act/Assert
    myMockDevice.setName("foo bar");
    assertThat(myFileSystem.getName()).isEqualTo(myMockDevice.getName());
  }

  @Test
  public void test_FileSystem_Is_Device() throws Exception {
    // Prepare
    assert myFileSystem != null;
    assert myMockDevice != null;

    // Act/Assert
    assertThat(myFileSystem.getDevice()).isEqualTo(myMockDevice.getIDevice());
    assertThat(myFileSystem.isDevice(myMockDevice.getIDevice())).isTrue();
  }

  @Test
  public void test_FileSystem_Exposes_DeviceState() throws Exception {
    // Prepare
    assert myFileSystem != null;
    assert myMockDevice != null;

    // Act/Assert
    myMockDevice.setState(IDevice.DeviceState.BOOTLOADER);
    assertThat(myFileSystem.getDeviceState()).isEqualTo(DeviceState.BOOTLOADER);

    myMockDevice.setState(IDevice.DeviceState.OFFLINE);
    assertThat(myFileSystem.getDeviceState()).isEqualTo(DeviceState.OFFLINE);

    myMockDevice.setState(IDevice.DeviceState.ONLINE);
    assertThat(myFileSystem.getDeviceState()).isEqualTo(DeviceState.ONLINE);

    myMockDevice.setState(IDevice.DeviceState.RECOVERY);
    assertThat(myFileSystem.getDeviceState()).isEqualTo(DeviceState.RECOVERY);

    myMockDevice.setState(IDevice.DeviceState.SIDELOAD);
    assertThat(myFileSystem.getDeviceState()).isEqualTo(DeviceState.SIDELOAD);

    myMockDevice.setState(IDevice.DeviceState.UNAUTHORIZED);
    assertThat(myFileSystem.getDeviceState()).isEqualTo(DeviceState.UNAUTHORIZED);

    myMockDevice.setState(IDevice.DeviceState.DISCONNECTED);
    assertThat(myFileSystem.getDeviceState()).isEqualTo(DeviceState.DISCONNECTED);
  }

  @Test
  public void test_FileSystem_Has_Root() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act
    DeviceFileEntry result = waitForFuture(myFileSystem.getRootDirectory());

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("");
  }

  @Test
  public void test_FileSystem_Has_DataTopLevelDirectory() throws Exception {
    // Prepare
    assert myFileSystem != null;
    DeviceFileEntry rootEntry = waitForFuture(myFileSystem.getRootDirectory());

    // Act
    List<DeviceFileEntry> result = waitForFuture(rootEntry.getEntries());

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.stream().anyMatch(x -> Objects.equals("data", x.getName()))).isTrue();
  }

  @Test
  public void test_FileSystem_GetEntry_Returns_Root_ForEmptyPath() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act
    DeviceFileEntry result = waitForFuture(myFileSystem.getEntry(""));

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("");
  }

  @Test
  public void test_FileSystem_GetEntry_Returns_Root() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act
    DeviceFileEntry result = waitForFuture(myFileSystem.getEntry("/"));

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("");
  }

  @Test
  public void test_FileSystem_GetEntry_Returns_LinkInfo() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act
    DeviceFileEntry result = waitForFuture(myFileSystem.getEntry("/charger"));

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("charger");
    assertThat(result.getSymbolicLinkTarget()).isEqualTo("/sbin/healthd");
    assertThat(result.getPermissions().getText()).isEqualTo("lrwxrwxrwx");
    assertThat(result.getSize()).isEqualTo(-1);
    assertThat(result.getLastModifiedDate().getText()).isEqualTo("1969-12-31 16:00");
  }

  @Test
  public void test_FileSystem_GetEntry_Returns_DataDirectory() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act
    DeviceFileEntry result = waitForFuture(myFileSystem.getEntry("/data"));

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("data");
  }

  @Test
  public void test_FileSystem_GetEntry_Returns_DataDataDirectory() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act
    DeviceFileEntry result = waitForFuture(myFileSystem.getEntry("/data/data"));

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("data");
  }

  @Test
  public void test_FileSystem_GetEntries_Returns_AppDirectories() throws Exception {
    // Prepare
    assert myFileSystem != null;
    DeviceFileEntry dataEntry = waitForFuture(myFileSystem.getEntry("/data/data"));

    // Act
    List<DeviceFileEntry> result = waitForFuture(dataEntry.getEntries());

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.stream().anyMatch(x -> Objects.equals(x.getName(), "com.example.rpaquay.myapplication"))).isTrue();
  }

  @Test
  public void test_FileSystem_GetEntry_Returns_DataLocalTempDirectory() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act
    DeviceFileEntry result = waitForFuture(myFileSystem.getEntry("/data/local/tmp"));

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("tmp");
  }

  @Test
  public void test_FileSystem_GetEntry_Fails_ForInvalidPath() throws Exception {
    // Prepare
    assert myFileSystem != null;

    // Act/Assert
    thrown.expect(ExecutionException.class);
    thrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
    /*DeviceFileEntry result = */
    waitForFuture(myFileSystem.getEntry("/data/invalid/path"));
  }

  @Test
  public void test_FileSystem_UploadLocalFile_Works() throws Exception {
    // Prepare
    assert myFileSystem != null;
    DeviceFileEntry dataEntry = waitForFuture(myFileSystem.getEntry("/data/local/tmp"));
    Path tempFile = FileUtil.createTempFile("localFile", "tmp").toPath();
    Files.write(tempFile, new byte[1024]);


    // Act
    AtomicReference<Long> totalBytesRef = new AtomicReference<>();
    Void result = waitForFuture(dataEntry.uploadFile(tempFile, new FileTransferProgress() {
      @Override
      public void progress(long currentBytes, long totalBytes) {
        totalBytesRef.set(totalBytes);
      }

      @Override
      public boolean isCancelled() {
        return false;
      }
    }));
    // Ensure all progress callbacks have been executed
    myCallbackExecutor.waitAllTasksExecuted(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

    // Assert
    assertThat(result).isNull();
    assertThat(totalBytesRef.get()).isEqualTo(1024);
  }

  @Test
  public void test_FileSystem_DownloadRemoteFile_Works() throws Exception {
    // Prepare
    assert myFileSystem != null;
    assert myMockDevice != null;
    assert myCallbackExecutor != null;
    DeviceFileEntry deviceEntry = waitForFuture(myFileSystem.getEntry("/default.prop"));
    myMockDevice.addRemoteFile(deviceEntry.getFullPath(), deviceEntry.getSize());
    Path tempFile = FileUtil.createTempFile("localFile", "tmp").toPath();

    // Act
    AtomicReference<Long> totalBytesRef = new AtomicReference<>();
    Void result = waitForFuture(deviceEntry.downloadFile(tempFile, new FileTransferProgress() {
      @Override
      public void progress(long currentBytes, long totalBytes) {
        totalBytesRef.set(totalBytes);
      }

      @Override
      public boolean isCancelled() {
        return false;
      }
    }));
    // Ensure all progress callbacks have been executed
    myCallbackExecutor.waitAllTasksExecuted(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

    // Assert
    assertThat(result).isNull();
    assertThat(totalBytesRef.get()).isEqualTo(deviceEntry.getSize());
    assertThat(Files.exists(tempFile)).isTrue();
    assertThat(tempFile.toFile().length()).isEqualTo(deviceEntry.getSize());
  }
}