package com.cuzz.bukkitmybatis.utils;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public abstract class URLClassLoaderAccess {

    private final URLClassLoader classLoader;

    static URLClassLoaderAccess create(final URLClassLoader classLoader) {
        if (Unsafe.isSupported()) {
            return new Unsafe(classLoader);
        }
        return Noop.INSTANCE;
    }

    protected URLClassLoaderAccess(final URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public abstract void addURL(final URL url);

    private static class Unsafe extends URLClassLoaderAccess {
        private static final sun.misc.Unsafe UNSAFE;
        private final Collection<URL> unopenedURLs;
        private final Collection<URL> pathURLs;

        private static boolean isSupported() {
            return Unsafe.UNSAFE != null;
        }

        Unsafe(final URLClassLoader classLoader) {
            super(classLoader);
            Collection<URL> unopenedURLs;
            Collection<URL> pathURLs;
            try {
                final Object ucp = fetchField(URLClassLoader.class, classLoader, "ucp");
                unopenedURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "unopenedUrls");
                pathURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "path");
            } catch (final Throwable e) {
                unopenedURLs = null;
                pathURLs = null;
            }
            this.unopenedURLs = unopenedURLs;
            this.pathURLs = pathURLs;
        }

        private static Object fetchField(final Class<?> clazz, final Object object, final String name) throws NoSuchFieldException {
            final Field field = clazz.getDeclaredField(name);
            final long offset = Unsafe.UNSAFE.objectFieldOffset(field);
            return Unsafe.UNSAFE.getObject(object, offset);
        }

        @Override
        public void addURL(final URL url) {
            if (this.unopenedURLs != null) {
                this.unopenedURLs.add(url);
            }
            if (this.pathURLs != null) {
                this.pathURLs.add(url);
            }
        }

        static {
            sun.misc.Unsafe unsafe;
            try {
                final Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            } catch (final Throwable t) {
                unsafe = null;
            }
            UNSAFE = unsafe;
        }
    }

    private static class Noop extends URLClassLoaderAccess {
        private static final Noop INSTANCE;

        private Noop() {
            super(null);
        }

        @Override
        public void addURL(final URL url) {
            throw new UnsupportedOperationException();
        }

        static {
            INSTANCE = new Noop();
        }
    }
}

