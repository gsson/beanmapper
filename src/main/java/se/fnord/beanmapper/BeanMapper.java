package se.fnord.beanmapper;

public interface BeanMapper<T, U> {
    U map(T from, U to);
}
