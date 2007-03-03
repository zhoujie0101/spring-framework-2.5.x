/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internal class that caches JavaBeans {@link java.beans.PropertyDescriptor}
 * information for a Java class. Not intended for direct use by application code.
 *
 * <p>Necessary as {@link java.beans.Introspector#getBeanInfo()} in JDK 1.3 will
 * return a new deep copy of the BeanInfo every time we ask for it. We take the
 * opportunity to cache property descriptors by method name for fast lookup.
 * Furthermore, we do our own caching of descriptors here, rather than rely on
 * the JDK's system-wide BeanInfo cache (to avoid leaks on ClassLoader shutdown).
 *
 * <p>Information is cached statically, so we don't need to create new
 * objects of this class for every JavaBean we manipulate. Hence, this class
 * implements the factory design pattern, using a private constructor and
 * a static {@link #forClass(Class)} factory method to obtain instances.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 05 May 2001
 * @see #acceptClassLoader(ClassLoader)
 * @see #clearClassLoader(ClassLoader)
 * @see #forClass(Class)
 */
public class CachedIntrospectionResults {

	private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);

	/**
	 * Set of ClassLoaders that this CachedIntrospectionResults class will always
	 * accept classes from, even if the classes do not qualify as cache-safe.
	 */
	static final Set acceptedClassLoaders = Collections.synchronizedSet(new HashSet());

	/**
	 * Map keyed by class containing CachedIntrospectionResults.
	 * Needs to be a WeakHashMap with WeakReferences as values to allow
	 * for proper garbage collection in case of multiple class loaders.
	 */
	static final Map classCache = Collections.synchronizedMap(new WeakHashMap());


	/**
	 * Accept the given ClassLoader as cache-safe, even if its classes would
	 * not qualify as cache-safe in this CachedIntrospectionResults class.
	 * <p>This configuration method is only relevant in scenarios where the Spring
	 * classes reside in a 'common' ClassLoader (e.g. the system ClassLoader)
	 * whose lifecycle is not coupled to the application. In such a scenario,
	 * CachedIntrospectionResults would by default not cache any of the application's
	 * classes, since they would create a leak in the common ClassLoader.
	 * <p>Any <code>acceptClassLoader</code> call at application startup should
	 * be paired with a {@link #clearClassLoader} call at application shutdown.
	 * @param classLoader the ClassLoader to accept
	 */
	public static void acceptClassLoader(ClassLoader classLoader) {
		if (classLoader != null) {
			acceptedClassLoaders.add(classLoader);
		}
	}

	/**
	 * Clear the introspection cache for the given ClassLoader, removing the
	 * introspection results for all classes underneath that ClassLoader,
	 * and deregistering the ClassLoader (and any of its children) from the
	 * acceptance list.
	 * @param classLoader the ClassLoader to clear the cache for
	 */
	public static void clearClassLoader(ClassLoader classLoader) {
		if (classLoader == null) {
			return;
		}
		synchronized (classCache) {
			for (Iterator it = classCache.keySet().iterator(); it.hasNext();) {
				Class beanClass = (Class) it.next();
				if (isUnderneathClassLoader(beanClass.getClassLoader(), classLoader)) {
					it.remove();
				}
			}
		}
		synchronized (acceptedClassLoaders) {
			for (Iterator it = acceptedClassLoaders.iterator(); it.hasNext();) {
				ClassLoader registeredLoader = (ClassLoader) it.next();
				if (isUnderneathClassLoader(registeredLoader, classLoader)) {
					it.remove();
				}
			}
		}
	}

	/**
	 * Create CachedIntrospectionResults for the given bean class.
	 * <P>We don't want to use synchronization here. Object references are atomic,
	 * so we can live with doing the occasional unnecessary lookup at startup only.
	 * @param beanClass the bean class to analyze
	 * @return the corresponding CachedIntrospectionResults
	 * @throws BeansException in case of introspection failure
	 */
	static CachedIntrospectionResults forClass(Class beanClass) throws BeansException {
		CachedIntrospectionResults results = null;
		Object value = classCache.get(beanClass);
		if (value instanceof Reference) {
			Reference ref = (Reference) value;
			results = (CachedIntrospectionResults) ref.get();
		}
		else {
			results = (CachedIntrospectionResults) value;
		}
		if (results == null) {
			// can throw BeansException
			results = new CachedIntrospectionResults(beanClass);
			if (isCacheSafe(beanClass) || isClassLoaderAccepted(beanClass.getClassLoader())) {
				classCache.put(beanClass, results);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Not strongly caching class [" + beanClass.getName() + "] because it is not cache-safe");
				}
				classCache.put(beanClass, new WeakReference(results));
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Using cached introspection results for class [" + beanClass.getName() + "]");
			}
		}
		return results;
	}

	/**
	 * Check whether this CachedIntrospectionResults class is configured
	 * to accept the given ClassLoader.
	 * @param classLoader the ClassLoader to check
	 * @return whether the given ClassLoader is accepted
	 * @see #acceptClassLoader
	 */
	private static boolean isClassLoaderAccepted(ClassLoader classLoader) {
		// Iterate over array copy in order to avoid synchronization for the entire
		// ClassLoader check (avoiding a synchronized acceptedClassLoaders Iterator).
		Object[] acceptedLoaderArray = acceptedClassLoaders.toArray();
		for (int i = 0; i < acceptedLoaderArray.length; i++) {
			ClassLoader registeredLoader = (ClassLoader) acceptedLoaderArray[i];
			if (isUnderneathClassLoader(classLoader, registeredLoader)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given class is cache-safe,
	 * i.e. whether it is loaded by the same class loader as the
	 * CachedIntrospectionResults class or a parent of it.
	 * <p>Many thanks to Guillaume Poirier for pointing out the
	 * garbage collection issues and for suggesting this solution.
	 * @param clazz the class to analyze
	 */
	private static boolean isCacheSafe(Class clazz) {
		ClassLoader target = clazz.getClassLoader();
		if (target == null) {
			return false;
		}
		ClassLoader cur = CachedIntrospectionResults.class.getClassLoader();
		if (cur == target) {
			return true;
		}
		while (cur != null) {
			cur = cur.getParent();
			if (cur == target) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given ClassLoader is underneath the given parent,
	 * that is, whether the parent is within the candidate's hierarchy.
	 * @param candidate the candidate ClassLoader to check
	 * @param parent the parent ClassLoader to check for
	 */
	private static boolean isUnderneathClassLoader(ClassLoader candidate, ClassLoader parent) {
		if (candidate == null) {
			return false;
		}
		if (candidate == parent) {
			return true;
		}
		ClassLoader classLoaderToCheck = candidate;
		while (classLoaderToCheck != null) {
			classLoaderToCheck = classLoaderToCheck.getParent();
			if (classLoaderToCheck == parent) {
				return true;
			}
		}
		return false;
	}


	/** The BeanInfo object for the introspected bean class */
	private final BeanInfo beanInfo;

	/** PropertyDescriptor objects keyed by property name String */
	private final Map propertyDescriptorCache;


	/**
	 * Create a new CachedIntrospectionResults instance for the given class.
	 * @param beanClass the bean class to analyze
	 * @throws BeansException in case of introspection failure
	 */
	private CachedIntrospectionResults(Class beanClass) throws BeansException {
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Getting BeanInfo for class [" + beanClass.getName() + "]");
			}
			this.beanInfo = Introspector.getBeanInfo(beanClass);

			// Immediately remove class from Introspector cache, to allow for proper
			// garbage collection on class loader shutdown - we cache it here anyway,
			// in a GC-friendly manner. In contrast to CachedIntrospectionResults,
			// Introspector does not use WeakReferences as values of its WeakHashMap!
			Class classToFlush = beanClass;
			do {
				Introspector.flushFromCaches(classToFlush);
				classToFlush = classToFlush.getSuperclass();
			}
			while (classToFlush != null);

			if (logger.isTraceEnabled()) {
				logger.trace("Caching PropertyDescriptors for class [" + beanClass.getName() + "]");
			}
			this.propertyDescriptorCache = new HashMap();

			// This call is slow so we do it once.
			PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
			for (int i = 0; i < pds.length; i++) {
				PropertyDescriptor pd = pds[i];
				if (logger.isTraceEnabled()) {
					logger.trace("Found bean property '" + pd.getName() + "'" +
							(pd.getPropertyType() != null ?
							" of type [" + pd.getPropertyType().getName() + "]" : "") +
							(pd.getPropertyEditorClass() != null ?
							"; editor [" + pd.getPropertyEditorClass().getName() + "]" : ""));
				}
				this.propertyDescriptorCache.put(pd.getName(), pd);
			}
		}
		catch (IntrospectionException ex) {
			throw new FatalBeanException("Cannot get BeanInfo for object of class [" + beanClass.getName() + "]", ex);
		}
	}

	BeanInfo getBeanInfo() {
		return this.beanInfo;
	}

	Class getBeanClass() {
		return this.beanInfo.getBeanDescriptor().getBeanClass();
	}

	PropertyDescriptor getPropertyDescriptor(String propertyName) {
		return (PropertyDescriptor) this.propertyDescriptorCache.get(propertyName);
	}

}
