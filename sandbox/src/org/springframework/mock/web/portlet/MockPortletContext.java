/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web.portlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.util.WebUtils;

/**
 * Mock implementation of the PortletContext interface.
 *
 * @author John Lewis
 */
public class MockPortletContext implements PortletContext {

	private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";

	private final Log logger = LogFactory.getLog(getClass());

	private final String resourceBasePath;
	
	private final ResourceLoader resourceLoader;

	private final Properties initParameters = new Properties();

	private final Hashtable attributes = new Hashtable();

	
	/**
	 * Create a new MockPortletContext with no base path and a 
	 * DefaultResourceLoader (i.e. the classpath root as WAR root).
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public MockPortletContext() {
		this("");
	}

	/**
	 * Create a new MockPortletContext using a DefaultResourceLoader.
	 * @param resourceBasePath the WAR root directory (should not end with a /)
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public MockPortletContext(String resourceBasePath) {
		this(resourceBasePath, new DefaultResourceLoader());
	}

	/**
	 * Create a new MockPortletContext.
	 * @param resourceBasePath the WAR root directory (should not end with a /)
	 * @param resourceLoader the ResourceLoader to use
	 */
	public MockPortletContext(String resourceBasePath, ResourceLoader resourceLoader) {
	    this.resourceBasePath = resourceBasePath;
		this.resourceLoader = resourceLoader;

		// use JVM temp dir as PortletContext temp dir
		String tempDir = System.getProperty(TEMP_DIR_SYSTEM_PROPERTY);
		if (tempDir != null) {
			this.attributes.put(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File(tempDir));
		}
	}

	
	//---------------------------------------------------------------------
	// PortletContext methods
	//---------------------------------------------------------------------
	
	public String getServerInfo() {
		return "MockPortal/1.0";
	}

	public PortletRequestDispatcher getRequestDispatcher(String path) {
		if (!path.startsWith("/"))
			throw new IllegalArgumentException("PortletRequestDispatcher path at PortletContext level must start with '/'");
		return new MockPortletRequestDispatcher(path);
	}

	public PortletRequestDispatcher getNamedDispatcher(String path) {
		return null;
	}

	public InputStream getResourceAsStream(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getInputStream();
		}
		catch (IOException ex) {
			logger.info("Couldn't open InputStream for " + resource, ex);
			return null;
		}
	}

	public int getMajorVersion() {
		return 1;
	}

	public int getMinorVersion() {
		return 0;
	}
	
	public String getMimeType(String filePath) {
		return null;
	}

	public String getRealPath(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getFile().getAbsolutePath();
		}
		catch (IOException ex) {
			logger.info("Couldn't determine real path of resource " + resource, ex);
			return null;
		}
	}

	public Set getResourcePaths(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			File file = resource.getFile();
			String[] fileList = file.list();
			String prefix = (path.endsWith("/") ? path : path + "/");
			Set resourcePaths = new HashSet(fileList.length);
			for (int i = 0; i < fileList.length; i++) {
				resourcePaths.add(prefix + fileList[i]);
			}
			return resourcePaths;
		}
		catch (IOException ex) {
			logger.info("Couldn't get resource paths for " + resource, ex);
			return null;
		}
	}

	public URL getResource(String path) throws MalformedURLException {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getURL();
		}
		catch (IOException ex) {
			logger.info("Couldn't get URL for " + resource, ex);
			return null;
		}
	}

	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	public Enumeration getAttributeNames() {
		return this.attributes.keys();
	}

	public void setAttribute(String name, Object value) {
		if (value != null)
			this.attributes.put(name, value);
		else
			this.attributes.remove(name);
	}

	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	public String getInitParameter(String name) {
		return this.initParameters.getProperty(name);
	}

	public Enumeration getInitParameterNames() {
		return this.initParameters.keys();
	}

	public void log(String message) {
		logger.info(message);
	}

	public void log(String message, Throwable t) {
		logger.info(message, t);
	}

	public String getPortletContextName() {
        return "MockPortletContext";
    }

	
	//---------------------------------------------------------------------
	// MockPortletContext methods
	//---------------------------------------------------------------------
	
	public void addInitParameter(String name, String value) {
		this.initParameters.put(name, value);
	}

	/**
	 * Build a full resource location for the given path,
	 * prepending the resource base path of this MockServletContext.
	 * @param path the path as specified
	 * @return the full resource path
	 */
	protected String getResourceLocation(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return this.resourceBasePath + path;
	}

}
