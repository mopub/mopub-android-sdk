package com.mopub.common.util;

import android.app.Activity;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.mopub.common.util.Reflection.MethodBuilder;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;


@RunWith(RobolectricTestRunner.class)
public class ReflectionTest {
    private Activity activity;
    private View view;
    private String string;
    private MethodBuilder methodBuilder;

    @Before
    public void setup(){
        activity = Robolectric.buildActivity(Activity.class).create().get();
        view = new View(activity);
        string = "goat";
    }

    @Test
    public void execute_withCorrectVoidMethodThatHasNoParameters_shouldPass() throws Exception {
        methodBuilder = new MethodBuilder(activity, "finish");

        methodBuilder.execute();

        // pass
    }

    @Test
    public void execute_withCorrectNonVoidMethodThatHasNoParameters_shouldPass() throws Exception {
        methodBuilder = new MethodBuilder(string, "length");

        int result = (Integer)methodBuilder.execute();

        assertThat(result).isEqualTo(4);
    }

    @Test
    public void execute_withCorrectVoidMethodThatHasParameters_shouldPass() throws Exception {
        methodBuilder = new MethodBuilder(view, "buildDrawingCache");
        methodBuilder.addParam(boolean.class, true);

        methodBuilder.execute();

        // pass
    }

    @Test
    public void execute_withCorrectNonVoidMethodThatHasParameters_shouldPass() throws Exception {
        methodBuilder = new MethodBuilder(string, "charAt");
        methodBuilder.addParam(int.class, 2);

        Object result = methodBuilder.execute();

        assertThat(result).isEqualTo('a');
    }

    @Test
    public void execute_withNoSuchMethod_shouldThrowException() throws Exception {
        methodBuilder = new MethodBuilder(activity, "noSuchMethod");

        try {
            methodBuilder.execute();
            fail("Should fail because method did not exist");
        } catch (Exception e) {
            // pass
        }
    }

    @Test
    public void execute_withCorrectVoidMethodThatHasParameters_withMissingParameters_shouldThrowException() throws Exception {
        methodBuilder = new MethodBuilder(activity, "finishActivity");
        // forget to add int requestCode parameter

        try {
            methodBuilder.execute();
            fail("Should fail because we did not supply all the parameters");
        } catch (Exception e) {
            // pass
        }
    }

    @Test
    public void execute_withExistingMethodButIncorrectParameterTypes_shouldThrowException() throws Exception {
        methodBuilder = new MethodBuilder(string, "concat");
        methodBuilder.addParam(Object.class, "other");

        try {
            methodBuilder.execute();
            fail("Should fail because there is no string.concat(Object) method");
        } catch (Exception e) {
            // pass
        }
    }

    @Test
    public void execute_withExistingMethodButSubclassedParameter_shouldPass() throws Exception {
        methodBuilder = new MethodBuilder(string, "equals");
        methodBuilder.addParam(Object.class, "cheese");

        boolean result = (Boolean) methodBuilder.execute();

        assertThat(result).isFalse();
    }

    @Test
    public void execute_withCorrectMethodThatHasParameters_withIncorrectOrderingOfParameters_shouldThrowException() throws Exception {
        methodBuilder = new MethodBuilder(string, "indexOf");
        methodBuilder.addParam(int.class, 2);
        methodBuilder.addParam(String.class, "g");

        try {
            methodBuilder.execute();
            fail("Should fail because we expected string.indexOf(String, int) instead of string.indexOf(int, String)");
        } catch (Exception e) {
            // pass
        }
    }

    @Test
    public void execute_withNullInstanceOnInstanceMethod_shouldThrowException() throws Exception {
        methodBuilder = new MethodBuilder(null, "length");

        try {
            methodBuilder.execute();
            fail("Should fail because we are giving a null instance");
        } catch (Exception e) {
            // pass
        }
    }

    @Test
    public void execute_withStaticMethod_shouldPass() throws Exception {
        methodBuilder = new MethodBuilder(null, "valueOf").setStatic(String.class).addParam(int.class, 20);

        assertThat(methodBuilder.execute()).isEqualTo("20");
    }

    @Test
    public void execute_withAccessibility_shouldRunPrivateMethods() throws Exception {
        methodBuilder = new MethodBuilder(string, "indexOfSupplementary");
        methodBuilder.addParam(int.class, (int)'a');
        methodBuilder.addParam(int.class, 0);
        methodBuilder.setAccessible();

        int result = (Integer) methodBuilder.execute();

        assertThat(result).isEqualTo(-1);
    }

    @Test
    public void instantiateClassWithConstructor_withCorrectParameters_shouldInstiantiateObject() throws Exception {
        Class[] classes = {int.class};
        Object[] parameters = {30};
        Number integer = Reflection.instantiateClassWithConstructor("java.lang.Integer",
                Number.class, classes, parameters);

        assertThat(integer).isEqualTo(new Integer(30));
    }

    @Test
    public void instantiateClassWithConstructor_withCorrectMultipleParameters_shouldInstiantiateObject() throws Exception {
        Class[] classes = {BigInteger.class, int.class};
        Object[] parameters = {new BigInteger("10"), 5};
        Number bigDecimal = Reflection.instantiateClassWithConstructor("java.math.BigDecimal",
                Number.class, classes, parameters);

        assertThat(bigDecimal).isEqualTo(new BigDecimal(new BigInteger("10"), 5));
    }

    @Test(expected = ClassCastException.class)
    public void instantiateClassWithConstructor_withInvalidSuperClass_shouldThrowException() throws Exception {
        Class[] classes = {int.class};
        Object[] parameters = {30};

        Reflection.instantiateClassWithConstructor("java.lang.Integer", String.class, classes, parameters);
    }

    @Test(expected = ClassNotFoundException.class)
    public void instantiateClassWithConstructor_withClassNotFound_shouldThrowException() throws Exception {
        Class[] classes = {int.class};
        Object[] parameters = {30};

        Reflection.instantiateClassWithConstructor("java.lang.FakeClass123", Integer.class, classes, parameters);
    }

    @Test(expected = Exception.class)
    public void instantiateClassWithConstructor_withMismatchingClassParameters_shouldThrowException() throws Exception {
        Class[] classes = {boolean.class};
        Object[] parameters = {30};

        Reflection.instantiateClassWithConstructor("java.lang.Integer", Number.class, classes, parameters);
    }
}
