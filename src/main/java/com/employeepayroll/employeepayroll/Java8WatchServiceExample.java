package com.employeepayroll.employeepayroll;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.*;

public class Java8WatchServiceExample {

	private final WatchService watcher;
	private final Map<WatchKey,Path> dirWatchers;

	/*Creates a WatchService and registers the given directory*/
	Java8WatchServiceExample(Path dir) throws IOException{
		this.watcher = FileSystems.getDefault().newWatchService();
		this.dirWatchers = new HashMap<WatchKey,Path>();
		scanAndRegisterDirectories(dir);	
	}

	/*register the given directory with the watchService*/
	private void registerDirWatchers(Path dir) throws IOException
	{
		WatchKey key = dir.register(watcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);
		dirWatchers.put(key, dir);
	}

	/*Register the given directory ,and all its sub-directories,with the watchservice*/
	private void scanAndRegisterDirectories(final Path start)throws IOException{
		//register directory and sub-directories
		Files.walkFileTree(start,new SimpleFileVisitor<Path>() {
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException{
				registerDirWatchers(dir);
				return FileVisitResult.CONTINUE;
			}	
		});
	}

	/*process all events for keys queued to the watcher*/
	@SuppressWarnings({"rawtypes","unchecked"})
	void processEvents() {
		while(true) {
			WatchKey Key;//wait for key to be signalled
			try {
				Key = watcher.take();
			}catch(InterruptedException x) {
				return;
			}
			Path dir = dirWatchers.get(Key);
			if(dir == null) continue;
			for(WatchEvent<?> event : Key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				Path name = ((WatchEvent<Path>)event).context();
				Path child = dir.resolve(name);
				System.out.format("%s: %s\n",event.kind().name(),child); //print out event

				//if directory is created ,then register it and its sub-directories
				if(kind == ENTRY_CREATE) {
					try {
						if(Files.isDirectory(child))scanAndRegisterDirectories(child);

					}catch(IOException x) {}
				}else if(kind.equals(ENTRY_DELETE)) {
					if(Files.isDirectory(child))dirWatchers.remove(Key);

				}	
			}
			//reset key and remove from set if directory no longer accessible
			boolean valid = Key.reset();
			if(!valid) {
				dirWatchers.remove(Key);
				if(dirWatchers.isEmpty())break;//all directories are inaccessible
			}
		}
	}
}