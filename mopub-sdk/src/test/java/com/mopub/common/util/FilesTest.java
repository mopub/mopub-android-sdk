package com.mopub.common.util;

import android.app.Activity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(RobolectricTestRunner.class)
public class FilesTest {

    private Activity activity;
    private String expectedDirectoryPath;
    private File expectedDirectory;
    private String expectedFilePath;

    @Before
    public void setup() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        expectedDirectoryPath = activity.getFilesDir() + File.separator + "testDirectory";
        expectedFilePath = activity.getFilesDir() + File.separator + "test.txt";
    }

    @After
    public void tearDown() throws Exception {
        new File(expectedDirectoryPath).delete();
    }

    @Test
    public void createDirectory_shouldReturnNewDirectory() throws Exception {
        File directory = Files.createDirectory(expectedDirectoryPath);

        assertThat(directory.exists()).isTrue();
        assertThat(directory.isDirectory()).isTrue();
    }

    @Test
    public void createDirectory_whenDirectoryAlreadyExists_shouldReturnTheDirectory() throws Exception {
        expectedDirectory = new File(expectedDirectoryPath);
        expectedDirectory.mkdirs();

        assertThat(expectedDirectory.exists()).isTrue();
        assertThat(expectedDirectory.isDirectory()).isTrue();

        File directory = Files.createDirectory(expectedDirectoryPath);

        assertThat(directory.exists()).isTrue();
        assertThat(directory.isDirectory()).isTrue();
    }

    @Test
    public void createDirectory_whenFileAlreadyExistsButIsNotADirectory_shouldReturnNull() throws Exception {
        File file = new File(expectedFilePath);
        file.createNewFile();

        assertThat(file.exists()).isTrue();
        assertThat(file.isDirectory()).isFalse();

        File directory = Files.createDirectory(expectedFilePath);

        assertThat(directory).isNull();

        file.delete();
    }

    @Test
    public void createDirectory_whenAbsolutePathIsNull_shouldReturnNull() throws Exception {
        File directory = Files.createDirectory(null);

        assertThat(directory).isNull();
    }

    @Test
    public void createDirectory_withExternalStoragePath_withoutRelevantPermission_shouldReturnNullAndNotThrowException() throws Exception {
        String filePath = activity.getExternalCacheDir() + File.separator + "testFile.txt";
        File file = new File(filePath);

        Files.createDirectory(filePath);

        // pass

        file.delete();
    }

    @Test
    public void intLength_whenFileHasLengthLessThanMaxInt_shouldReturnThatLength() throws Exception {
        File file = mock(File.class);
        stub(file.length()).toReturn(1234L);

        int length = Files.intLength(file);

        assertThat(length).isEqualTo(1234);
    }

    @Test
    public void intLength_whenFileHasLengthGreaterThanMaxInt_shouldReturnMaxInt() throws Exception {
        File file = mock(File.class);
        stub(file.length()).toReturn(Integer.MAX_VALUE + 100L);

        int length = Files.intLength(file);

        assertThat(length).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void intLength_whenFileDoesNotExist_shouldReturnZero() throws Exception {
        File file = new File(expectedFilePath);

        assertThat(file.exists()).isFalse();

        int length = Files.intLength(file);

        assertThat(length).isEqualTo(0);
    }

    @Test
    public void intLength_whenFileIsNull_shouldReturnZero() throws Exception {
        int length = Files.intLength(null);

        assertThat(length).isEqualTo(0);
    }
}
