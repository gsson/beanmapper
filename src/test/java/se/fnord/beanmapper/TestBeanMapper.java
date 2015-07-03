package se.fnord.beanmapper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static se.fnord.beanmapper.PropertyMapper.copy;
import static se.fnord.beanmapper.PropertyMapper.set;


public class TestBeanMapper {

    public static class TestBean1 {
        private int value;

        public void setValue(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    public static class TestBean2 {
        private int value1;
        private int value2;

        public int getValue1() {
            return value1;
        }

        public void setValue1(int value1) {
            this.value1 = value1;
        }

        public int getValue2() {
            return value2;
        }

        public void setValue2(int value2) {
            this.value2 = value2;
        }
    }

    @Test
    public void testMapSame() throws IllegalAccessException, InstantiationException {

        final Class<BeanMapper<TestBean1, TestBean1>> mapperClass = BeanMapperBuilder
                .mapFrom(TestBean1.class)
                .into(TestBean1.class)
                .using(
                        copy("value", "value"))
                .createIn(getClass().getClassLoader());

        final TestBean1 source = new TestBean1();
        source.setValue(42);

        final BeanMapper<TestBean1, TestBean1> mapper = mapperClass.newInstance();
        final TestBean1 result = mapper.map(source, new TestBean1());

        assertThat(result.getValue(), is(42));
    }

    @Test
    public void testMapOther() throws IllegalAccessException, InstantiationException {
        final Class<BeanMapper<TestBean1, TestBean2>> mapperClass = BeanMapperBuilder
                .mapFrom(TestBean1.class)
                .into(TestBean2.class)
                .using(
                        copy("value", "value1"),
                        set(17, "value2"))
                .createIn(getClass().getClassLoader());

        final TestBean1 source = new TestBean1();
        source.setValue(42);

        final BeanMapper<TestBean1, TestBean2> mapper = mapperClass.newInstance();

        final TestBean2 result = mapper.map(source, new TestBean2());

        assertThat(result.getValue1(), is(42));
        assertThat(result.getValue2(), is(17));
    }
}
