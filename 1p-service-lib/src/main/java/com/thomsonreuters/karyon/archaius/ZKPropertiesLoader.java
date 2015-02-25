package com.thomsonreuters.karyon.archaius;

import com.netflix.karyon.archaius.PropertiesLoader;

public interface ZKPropertiesLoader extends PropertiesLoader {
	public void load( String root, int connectTimeout, int sessionTimeout );
}
