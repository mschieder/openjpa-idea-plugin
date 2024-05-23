package org.openjpa.ide.idea;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * simple wrapper for a read file
 */
class RealVirtualFile extends VirtualFile {

    private final File file;

    public RealVirtualFile(@NotNull File file) {
        this.file = file;
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public @NotNull @NlsSafe String getName() {
        return file.getName();
    }

    @Override
    public @NotNull String getPath() {
        return file.getAbsolutePath();
    }
    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        return null;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return new VirtualFile[0];
    }

    @Override
    public @NotNull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        return null;
    }

    @Override
    public byte @NotNull [] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {

    }

    @Override
    public @NotNull InputStream getInputStream() throws IOException {
        return null;
    }


}
