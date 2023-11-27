package org.eqasim.core.misc;

import java.lang.reflect.InvocationTargetException;

public class ClassUtils {
    public static <T> Class<? extends T> getClassImplementingInterface(String className, Class<T> interfaceDescription) {
        try {
            Class<?> classDescription = Class.forName(className);
            for(Class<?> classInterfaceDescription: classDescription.getInterfaces()) {
                if(classInterfaceDescription.equals(interfaceDescription)) {
                    return (Class<T>) classDescription;
                }
            }
            throw new IllegalStateException(String.format("The provided Class %s does not implement the %s interface", className, interfaceDescription.getName()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Class %s not found", className), e);
        }
    }

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

    public static <T> T getInstanceImplementingInterface(String className, Class<T> interfaceDescription) {
        Class<? extends T> classDescription = getClassImplementingInterface(className, interfaceDescription);
        try {
            return classDescription.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(String.format("The class %s does not have a constructor that does not require arguments", classDescription.getCanonicalName()), e);
        }
    }
}
