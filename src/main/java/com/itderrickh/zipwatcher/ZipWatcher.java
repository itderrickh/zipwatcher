package com.itderrickh.zipwatcher;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

/**
 * Hello world!
 *
 */
public class ZipWatcher extends DirectoryWatcher {
    ZipWatcher(Path directory, boolean recursive) throws IOException {
        super(directory, recursive);
    }

    public void onModifyEvent(String path) {
        System.out.println("Modify");
    }

    public void onCreateEvent(String path) {
        if (path.contains(".zip")) {
            boolean unzipped = false;
            while (!unzipped) {
                try {
                    String fileZip = path;
                    Path destDir = Paths.get(path.replace(".zip", ""));
                    Files.createDirectories(destDir);
                    byte[] buffer = new byte[1024];
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
                    ZipEntry zipEntry = zis.getNextEntry();
                    while (zipEntry != null) {
                        // System.out.println(zipEntry.toString());
                        if (zipEntry.isDirectory()) {
                            File newFile = new File(destDir.toString(), zipEntry.getName());
                            if (!newFile.exists()) {
                                newFile.mkdir();
                            }
                            zipEntry = zis.getNextEntry();
                        } else {
                            File newFile = new File(destDir.toString(), zipEntry.getName());
                            newFile.getParentFile().mkdirs();
                            newFile.createNewFile();
                            FileOutputStream fos = new FileOutputStream(newFile);
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                            zipEntry = zis.getNextEntry();
                        }
                    }
                    zis.closeEntry();
                    zis.close();

                    File zipFileToDelete = new File(path);
                    if (zipFileToDelete.delete()) {
                        // System.out.println("Archive deleted");
                    }
                    unzipped = true;
                } catch (Exception ex) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception inner) {

                    }

                    ex.printStackTrace();
                }
            }

            System.out.println("Unzipped: " + path);
        }

        if (path.contains(".7z")) {
            boolean unzipped = false;
            while (!unzipped) {
                try {
                    String fileZip = path;
                    Path destDir = Paths.get(path.replace(".7z", ""));
                    Files.createDirectories(destDir);
                    byte[] buffer = new byte[1024];
                    SevenZFile zis = new SevenZFile(new File(fileZip));
                    SevenZArchiveEntry zipEntry = zis.getNextEntry();
                    while (zipEntry != null) {
                        // System.out.println(zipEntry.toString());
                        if (zipEntry.isDirectory()) {
                            File newFile = new File(destDir.toString(), zipEntry.getName());
                            if (!newFile.exists()) {
                                newFile.mkdir();
                            }
                            zipEntry = zis.getNextEntry();
                        } else {
                            File newFile = new File(destDir.toString(), zipEntry.getName());
                            newFile.getParentFile().mkdirs();
                            newFile.createNewFile();
                            FileOutputStream fos = new FileOutputStream(newFile);
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                            zipEntry = zis.getNextEntry();
                        }
                    }

                    zis.close();

                    File zipFileToDelete = new File(path);
                    if (zipFileToDelete.delete()) {
                        // System.out.println("Archive deleted");
                    }
                    unzipped = true;
                } catch (Exception ex) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception inner) {

                    }

                    ex.printStackTrace();
                }
            }

            System.out.println("7Z Unzipped: " + path);
        }
    }

    public void onEventOccurred(String path) {
        System.out.println(path);
    }

    public static void main(String[] args) throws IOException {
        try {
            ZipWatcher zipWatcher = new ZipWatcher(Paths.get(args[0]), false);

            zipWatcher.processEvents();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
