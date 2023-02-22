package com.mybraintech.sdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ExampleParameterizedTest {

    @SuppressWarnings("DefaultAnnotationParam")
    @Parameterized.Parameter(value = 0)
    public int mTestInteger;

    @Parameterized.Parameter(value = 1)
    public String mTestString;

    @Parameterized.Parameters
    public static Collection<Object[]> initParameters() {
        return Arrays.asList(new Object[][] { { 0, "0" }, { 1, "1" } });
    }

    @Test
    public void sample_parseValue() {
        assertEquals(Integer.parseInt(mTestString), mTestInteger);
    }
}
