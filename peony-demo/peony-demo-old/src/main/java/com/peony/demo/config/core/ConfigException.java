package com.peony.demo.config.core;

/**
 * Created by jiangmin.wu on 17/7/20.
 */
@SuppressWarnings("serial")
public class ConfigException extends RuntimeException {

	public ConfigException(String cause, Exception e) {
        super(cause, e);
    }

    public ConfigException(String cause) {
        super(cause);
    }

    public ConfigException(Throwable cause) {
        super(cause);
    }

}
