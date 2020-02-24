package com.itderrickh.zipwatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

public abstract class DirectoryWatcher
{
    private WatchService watcher;
    private Map<WatchKey, Path> keys;
    private Map<Path, Long> fileTimeStamps;
    private boolean recursive;
    private boolean trace = true;
    public Path directory;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path directory) throws IOException
    {
        WatchKey watchKey = directory.register(watcher, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);

        addFileTimeStamps(directory);

        if (trace)
        {
            Path existingFilePath = keys.get(watchKey);
            if (existingFilePath == null)
            {
                System.out.format("register: %s\n", directory);
            } else
            {
                if (!directory.equals(existingFilePath))
                {
                    System.out.format("update: %s -> %s\n", existingFilePath, directory);
                }
            }
        }

        keys.put(watchKey, directory);
    }

    private void addFileTimeStamps(Path directory)
    {
        File[] files = directory.toFile().listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isFile())
                {
                    fileTimeStamps.put(file.toPath(), file.lastModified());
                }
            }
        }
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(Path directory) throws IOException
    {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path currentDirectory, BasicFileAttributes attrs)
                    throws IOException
            {
                register(currentDirectory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    DirectoryWatcher(Path directory, boolean recursive) throws IOException
    {
        this.directory = directory;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.fileTimeStamps = new HashMap<>();
        this.recursive = recursive;

        if (recursive)
        {
            System.out.format("Scanning %s ...\n", directory);
            registerAll(directory);
            System.out.println("Done.");
        } else
        {
            register(directory);
        }

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() throws InterruptedException, IOException
    {
        while (true)
        {
            WatchKey key = watcher.take();

            Path dir = keys.get(key);
            if (dir == null)
            {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents())
            {
                WatchEvent.Kind watchEventKind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (watchEventKind == OVERFLOW)
                {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> watchEvent = cast(event);
                Path fileName = watchEvent.context();
                Path filePath = dir.resolve(fileName);

                Long oldFileModifiedTimeStamp = this.fileTimeStamps.get(filePath);
                long newFileModifiedTimeStamp = filePath.toFile().lastModified();
                if (oldFileModifiedTimeStamp == null) {
                    this.fileTimeStamps.put(filePath, filePath.toFile().lastModified());
                    onCreateEvent(filePath.toString());
                } else {
                    if (newFileModifiedTimeStamp > oldFileModifiedTimeStamp)
                    {
                        fileTimeStamps.remove(filePath);
                        if (watchEventKind == ENTRY_MODIFY) {
                            onModifyEvent(filePath.toString());
                        }

                        if (watchEventKind == ENTRY_CREATE) {
                            onCreateEvent(filePath.toString());
                        }

                        onEventOccurred(filePath.toString());
                    }
                }

                if (recursive && watchEventKind == ENTRY_CREATE)
                {
                    if (Files.isDirectory(filePath, NOFOLLOW_LINKS))
                    {
                        registerAll(filePath);
                    }
                }

                break;
            }

            boolean valid = key.reset();

            if (!valid)
            {
                keys.remove(key);

                if (keys.isEmpty())
                {
                    break;
                }
            }
        }
    }

    public abstract void onModifyEvent(String path);
    public abstract void onCreateEvent(String path);
    public abstract void onEventOccurred(String path);
}