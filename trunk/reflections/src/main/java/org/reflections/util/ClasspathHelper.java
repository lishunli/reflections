package org.reflections.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import org.reflections.ReflectionsException;

import org.reflections.vfs.Vfs;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Some classpath convenient methods
 *
 */
public abstract class ClasspathHelper {

    /**
     * returns a set of urls that contain resources with prefix as the given parameter, that is exist in
     * the equivalent directory within the urls of current classpath
     */
    public static List<URL> getUrlsForPackagePrefix(String packagePrefix) {
        try {
            Enumeration<URL> urlEnumeration = Utils.getContextClassLoader().getResources(
                    packagePrefix.replace(".", "/"));
            List<URL> urls = Lists.newArrayList(Iterators.forEnumeration(urlEnumeration));

            return getBaseUrls(urls, getUrlsForCurrentClasspath());

        } catch (IOException e) {
            throw new ReflectionsException("Can't resolve URL for package prefix " + packagePrefix, e);
        }
    }

    public static List<URL> getUrlsForCurrentClasspath() {
        List<URL> urls = Lists.newArrayList();

        //is URLClassLoader?
        ClassLoader loader = Utils.getContextClassLoader();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                Collections.addAll(urls, ((URLClassLoader) loader).getURLs());
            }
            loader = loader.getParent();
        }

        if (urls.isEmpty()) {
            //get from java.class.path
            String javaClassPath = System.getProperty("java.class.path");
            if (javaClassPath != null) {

                for (String path : javaClassPath.split(File.pathSeparator)) {
                    try {
                        urls.add(new File(path).toURI().toURL());
                    } catch (Exception e) {
                        throw new ReflectionsException("could not create url from " + path, e);
                    }
                }
            }
        }

        return ImmutableList.copyOf(urls);
    }

    /**
     * the url that contains the given class.
     */
    public static URL getUrlForClass(Class<?> aClass) {
        String resourceName = aClass.getName().replace(".", "/") + ".class";
        URL packageUrl = Utils.getContextClassLoader().getResource(resourceName);
        if (packageUrl != null) {
            return getBaseUrl(packageUrl, getUrlsForCurrentClasspath());
        } else {
            return null;
        }
    }

    /** get's the base url from the given urls */
    public static URL getBaseUrl(final URL url, final Collection<URL> baseUrls) {
        if (url != null) {
            String path1 = Vfs.normalizePath(url);

            //try to return the base url
            for (URL baseUrl : baseUrls) {
                String path2 = Vfs.normalizePath(baseUrl);
                if (path1.startsWith(path2)) {
                    return baseUrl;
                }
            }
        }

        return url;
    }

    /** get's the base url from urls in current classpath */
    public static URL getBaseUrl(final URL url) {
        return getBaseUrl(url, getUrlsForCurrentClasspath());
    }

    /** get's the base urls from the given urls */
    public static List<URL> getBaseUrls(final List<URL> urls, final Collection<URL> baseUrls) {
        List<URL> result = Lists.newArrayList();
        for (URL url : urls) {
            result.add(getBaseUrl(url, baseUrls));
        }
        return result;
    }
}

