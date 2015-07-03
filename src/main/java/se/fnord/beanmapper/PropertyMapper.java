package se.fnord.beanmapper;

import net.bytebuddy.implementation.bytecode.StackManipulation;

public interface PropertyMapper {
    StackManipulation createMapper(MapperGenerator generator);

    static PropertyMapper copy(String fromPropertyName, String toPropertyName) {
        return (generator) -> generator.copyProperty(fromPropertyName, toPropertyName);
    }

    static PropertyMapper set(int value, String toPropertyName) {
        return (generator) -> generator.setProperty(value, toPropertyName);
    }

}
