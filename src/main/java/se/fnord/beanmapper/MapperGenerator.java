package se.fnord.beanmapper;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

import java.lang.reflect.Method;

class MapperGenerator {
    private final TypeDescription fromType;
    private final TypeDescription toType;

    private final MethodDescription mapMethod;

    private final ParameterDescription fromParameter;
    private final ParameterDescription toParameter;
    private final Class<?> fromClass;
    private final Class<?> toClass;
    private final Introspection introspection;

    public MapperGenerator(Class<?> fromClass, Class<?> toClass, Introspection introspection) {
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.introspection = introspection;
        this.fromType = describeType(fromClass);
        this.toType = describeType(toClass);
        this.mapMethod = introspection.describeMethod(BeanMapper.class, "map").orElseThrow(NoSuchMethodError::new);
        this.fromParameter = introspection.describeParameter(mapMethod, 0);
        this.toParameter = introspection.describeParameter(mapMethod, 1);
    }

    private TypeDescription describeType(Class<?> beanType) {
        return introspection.describeType(beanType).orElseThrow(IllegalArgumentException::new);
    }

    private TypeDescription fromPropertyType(String name) {
        return introspection.findPropertyType(fromClass, name).orElseThrow(() -> new NoSuchMethodError(name));
    }

    private TypeDescription toPropertyType(String name) {
        return introspection.findPropertyType(toClass, name).orElseThrow(() -> new NoSuchMethodError(name));
    }

    public StackManipulation loadFrom() {
        return loadParameter(fromParameter, fromType);
    }

    public StackManipulation loadTo() {
        return loadParameter(toParameter, toType);
    }

    public StackManipulation loadParameter(ParameterDescription parameterDescription, TypeDescription cast) {
        return new StackManipulation.Compound(
                MethodVariableAccess.forType(parameterDescription.getTypeDescription()).loadOffset(parameterDescription.getOffset()),
                TypeCasting.to(cast)
        );
    }

    public StackManipulation loadProperty(String fromName) {
        final Method getterMethod = introspection.findGetter(fromClass, fromName).orElseThrow(NoSuchMethodError::new);
        return invokeMethod(getterMethod);
    }

    public StackManipulation storeProperty(String toName) {
        final Method setterMethod = introspection.findSetter(toClass, toName).orElseThrow(NoSuchMethodError::new);
        return invokeMethod(setterMethod);
    }

    public StackManipulation invokeMethod(Method method) {
        final MethodDescription methodDescription = new MethodDescription.ForLoadedMethod(method);
        return MethodInvocation.invoke(methodDescription);
    }

    public StackManipulation copyProperty(String fromName, String toName) {
        final TypeDescription fromType = fromPropertyType(fromName);
        final TypeDescription toType = toPropertyType(toName);
        if (!toType.isAssignableFrom(fromType))
            throw new IllegalArgumentException(fromName + " -> " + toName);
        return new StackManipulation.Compound(
                loadTo(),
                loadFrom(),
                loadProperty(fromName),
                storeProperty(toName)
        );
    }

    public StackManipulation returnTo() {
        return new StackManipulation.Compound(
                loadTo(),
                MethodReturn.returning(toType)
        );
    }

    public StackManipulation setProperty(int value, String toPropertyName) {
        final TypeDescription toType = toPropertyType(toPropertyName);
        if (!toType.represents(Integer.TYPE))
            throw new IllegalArgumentException(toPropertyName + " is not a primitive int");
        return new StackManipulation.Compound(
                loadTo(),
                IntegerConstant.forValue(value),
                storeProperty(toPropertyName)
        );
    }
}
