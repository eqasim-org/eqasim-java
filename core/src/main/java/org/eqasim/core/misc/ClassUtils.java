package org.eqasim.core.misc;

import java.lang.reflect.InvocationTargetException;

public class ClassUtils {
    public static <T> T getInstanceOfClassExtendingOtherClass(String className, Class<T> otherClass) {
        try {
            Class<?> classDescription = Class.forName(className);
            Object instance = null;
            try {
                instance = classDescription.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("The class %s does not have a constructor that does not require arguments", classDescription.getCanonicalName()), e);
            }

            if (otherClass.isInstance(instance)) {
                return (T) instance;
            } else {
                throw new IllegalStateException(String.format("Class %s does not extend %s", classDescription.getCanonicalName(), otherClass.getCanonicalName()));
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
