package com.peony.engine.config.core.watch;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * 监听一个目录（不包含子目录）
 * 
 * @author jiangmin.wu
 *
 */
public interface FileListener {
	
	public WatchEvent.Kind<?>[] events();

	public void onEvent(String rootPath, WatchEvent<Path> pathEvent);
}