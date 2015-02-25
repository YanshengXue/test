package com.thomsonreuters.karyon;

public interface BootstrapAware<T> {
	public Class<T> getBootstrap();
}
