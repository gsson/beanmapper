package se.fnord.beanmapper;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class BeanMapperBuilder<A, B> {
    private final List<PropertyMapper> mappers;
    private final MapperGenerator mapperGenerator;

    private StackManipulation generateMapperBlock() {
        final StackManipulation[] mappingImpl = mappers.stream()
                .map(m -> m.createMapper(mapperGenerator))
                .toArray(StackManipulation[]::new);
        return new StackManipulation.Compound(mappingImpl);
    }

    private StackManipulation generateMapperMethod(StackManipulation mapperBlockImpl) {
        return new StackManipulation.Compound(
                mapperBlockImpl,
                mapperGenerator.returnTo()
        );
    }

    private ByteCodeAppender generateMapperAppender() {
        final StackManipulation mapperBlockImpl = generateMapperBlock();
        final StackManipulation mapperMethodImpl = generateMapperMethod(mapperBlockImpl);

        return (methodVisitor, implementationContext, instrumentedMethod) -> {
            StackManipulation.Size operandStackSize = mapperMethodImpl.apply(methodVisitor, implementationContext);
            return new ByteCodeAppender.Size(operandStackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        };
    }

    private Implementation generateMapperImplementation() {
        return new Implementation() {
            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public ByteCodeAppender appender(Target implementationTarget) {
                return generateMapperAppender();
            }
        };
    }

    public static class BeanMapperBuilderTo<_FROM> {
        private final Class<_FROM> fromClass;

        public BeanMapperBuilderTo(Class<_FROM> fromClass) {
            this.fromClass = fromClass;
        }

        public <_TO> BeanMapperBuilder<_FROM, _TO> into(Class<_TO> toClass) {
            return new BeanMapperBuilder<>(fromClass, toClass);
        }
    }

    private BeanMapperBuilder(Class<A> fromClass, Class<B> toClass) {
        this.mappers = new ArrayList<>();
        this.mapperGenerator = new MapperGenerator(fromClass, toClass, new Introspection());
    }

    public static <_FROM> BeanMapperBuilderTo<_FROM> mapFrom(Class<_FROM> fromClass) {
        return new BeanMapperBuilderTo<>(fromClass);
    }
    
    public BeanMapperBuilder<A, B> using(PropertyMapper... mappers) {
        Collections.addAll(this.mappers, mappers);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Class<BeanMapper<A, B>> createIn(ClassLoader targetClassLoader) {
        final Implementation mapperImplementation = generateMapperImplementation();
        return (Class<BeanMapper<A, B>>) new ByteBuddy()
                .subclass(BeanMapper.class)
                .method(named("map"))
                    .intercept(mapperImplementation)
                .make()
                .load(targetClassLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }
}
