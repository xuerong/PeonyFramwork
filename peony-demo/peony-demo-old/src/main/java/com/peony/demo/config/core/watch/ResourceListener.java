package com.peony.demo.config.core.watch;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 资源监听
 * @author jiangmin.wu
 */
public class ResourceListener {
	static Logger logger = LoggerFactory.getLogger(ResourceListener.class);
	private WatchService watchService;

	private ResourceListener(String path, FileListener listener) {
		try {
			watchService = FileSystems.getDefault().newWatchService();
			new Thread(new Watcher(watchService, path, listener)).start();
		} catch (IOException e) {
			logger.warn("ResourceListener:", e);
		}
	}

	/**
	 * 监听一个目录（不包含子目录）
	 * 
	 * @param path
	 * @param listener
	 * @throws IOException
	 */
	public static void addListener(String path, FileListener listener) throws IOException {
		ResourceListener resourceListener = new ResourceListener(path, listener);
		Path p = Paths.get(path);
		p.register(resourceListener.watchService, listener.events());
		logger.info("res lintener {} {} {}", p, path, Arrays.toString(listener.events()));
	}

	public static void main(String[] args) throws IOException {
		FileListener listener = new FileListener() {
			@Override
			public void onEvent(String rootPath, WatchEvent<Path> event) {
				Kind<Path> kind = event.kind();
				String fileName = event.context().getFileName().toString();
				if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					System.err.println(rootPath +"/"+fileName +"   "+ event.kind().name());
				}
			}

			@Override
			public Kind<?>[] events() {
				return new Kind<?>[] { StandardWatchEventKinds.ENTRY_MODIFY };
			}
		};

		ResourceListener.addListener("E:/jPersist2", listener);
		ResourceListener.addListener("E:/MyDownloads/Download", listener);
	}
}

class Watcher implements Runnable {
	private WatchService service;
	private String rootPath;
	private FileListener listener;

	public Watcher(WatchService service, String rootPath, FileListener listener) {
		this.service = service;
		this.rootPath = rootPath;
		this.listener = listener;
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {
			while (true) {
				WatchKey watchKey = service.take();
				List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
				for (WatchEvent<?> event : watchEvents) {
					Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW) {
	                    continue;
	                }
					WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
					listener.onEvent(rootPath, pathEvent);
					Path name = pathEvent.context();
					
					ResourceListener.logger.info("{}/{} {}", rootPath, name.getFileName().toString(), pathEvent.kind().name());
				}
				watchKey.reset();
			}
		} catch (InterruptedException e) {
			ResourceListener.logger.warn("ResourceListener:", e);
		} finally {
			try {
				service.close();
			} catch (IOException e) {
				ResourceListener.logger.warn("ResourceListener:", e);
			}
		}
	}
}