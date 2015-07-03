package se.fnord.beanmapper;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.bytebuddy.matcher.ElementMatchers.named;

class Introspection {

    private static class TypeInfo {
        private final BeanInfo beanInfo;
        private final TypeDescription.ForLoadedType typeDescription;

        private TypeInfo(BeanInfo beanInfo, TypeDescription.ForLoadedType typeDescription) {
            this.beanInfo = beanInfo;
            this.typeDescription = typeDescription;
        }

        public BeanInfo getBeanInfo() {
            return beanInfo;
        }

        public TypeDescription.ForLoadedType getTypeDescription() {
            return typeDescription;
        }
    }

    private final Map<Class<?>, TypeInfo> typeInfoCache = new HashMap<>();

    private static TypeInfo createBeanInfo(Class<?> beanType) {
        try {
            return new TypeInfo(Introspector.getBeanInfo(beanType), new TypeDescription.ForLoadedType(beanType));
        } catch (IntrospectionException e) {
            return null;
        }
    }

    private Optional<TypeInfo> getTypeInfo(Class<?> beanType) {
        return Optional.ofNullable(typeInfoCache.computeIfAbsent(beanType, Introspection::createBeanInfo));
    }

    private Optional<PropertyDescriptor> findProperty(TypeInfo typeInfo, String propertyName) {
        for (final PropertyDescriptor propertyDescriptor : typeInfo.getBeanInfo().getPropertyDescriptors()) {
            if (propertyName.equalsIgnoreCase(propertyDescriptor.getName()))
                return Optional.of(propertyDescriptor);
        }
        return Optional.empty();
    }

    private Optional<PropertyDescriptor> findProperty(Class<?> beanType, String propertyName) {
        return getTypeInfo(beanType)
                .flatMap(info -> findProperty(info, propertyName));
    }

    public Optional<Method> findSetter(Class<?> beanType, String propertyName) {
        return findProperty(beanType, propertyName)
                .flatMap(property -> Optional.ofNullable(property.getWriteMethod()));
    }

    public Optional<Method> findGetter(Class<?> beanType, String propertyName) {
        return findProperty(beanType, propertyName)
                .flatMap(property -> Optional.ofNullable(property.getReadMethod()));
    }

    public Optional<TypeDescription.ForLoadedType> findPropertyType(Class<?> beanType, String propertyName) {
        return findProperty(beanType, propertyName)
                .map(PropertyDescriptor::getPropertyType)
                .flatMap(this::getTypeInfo)
                .map(TypeInfo::getTypeDescription);
    }

    public Optional<TypeDescription> describeType(Class<?> beanType) {
        return getTypeInfo(beanType).map(TypeInfo::getTypeDescription);
    }

    public Optional<MethodDescription> describeMethod(Class<?> beanType, String methodName) {
        return getTypeInfo(beanType)
                .map(TypeInfo::getTypeDescription)
                .map(typeDescription -> typeDescription
                        .getDeclaredMethods()
                        .filter(named(methodName))
                        .getOnly());
    }

    public ParameterDescription describeParameter(MethodDescription method, int parameterIndex) {
        return method.getParameters().get(parameterIndex);
    }
}
