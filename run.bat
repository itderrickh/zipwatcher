echo off
set arg1=%1
shift

java -cp "C:/Program Files/DLHSoft/zipwatcher-1.0-jar-with-dependencies.jar" com.itderrickh.zipwatcher.ZipWatcher %arg1%