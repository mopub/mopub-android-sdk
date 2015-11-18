package com.mopub.common.event;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ErrorEventTest {

    private ErrorEvent subject;

    @Before
    public void setUp() {
        subject = new ErrorEvent.Builder(BaseEvent.Name.AD_REQUEST, BaseEvent.Category.REQUESTS, 0.10000123)
                .withErrorExceptionClassName("error_exception_class_name")
                .withErrorMessage("error_message")
                .withErrorStackTrace("error_stack_trace")
                .withErrorFileName("error_file_name")
                .withErrorClassName("error_class_name")
                .withErrorMethodName("error_method_name")
                .withErrorLineNumber(123)
                .build();
    }

    @Test
    public void constructor_shouldCorrectlyAssignFieldsFromBuilder() throws Exception {
        assertThat(subject.getName()).isEqualTo(BaseEvent.Name.AD_REQUEST);
        assertThat(subject.getCategory()).isEqualTo(BaseEvent.Category.REQUESTS);
        assertThat(subject.getSamplingRate()).isEqualTo(0.10000123);
        assertThat(subject.getScribeCategory()).isEqualTo(BaseEvent.ScribeCategory.EXCHANGE_CLIENT_ERROR);
        assertThat(subject.getErrorExceptionClassName()).isEqualTo("error_exception_class_name");
        assertThat(subject.getErrorMessage()).isEqualTo("error_message");
        assertThat(subject.getErrorStackTrace()).isEqualTo("error_stack_trace");
        assertThat(subject.getErrorFileName()).isEqualTo("error_file_name");
        assertThat(subject.getErrorClassName()).isEqualTo("error_class_name");
        assertThat(subject.getErrorMethodName()).isEqualTo("error_method_name");
        assertThat(subject.getErrorLineNumber()).isEqualTo(123);
    }

    @Test
    public void builder_withException_shouldCorrectlyPopulateErrorFields() throws Exception {
        Exception exception;
        try {
            throw new ClassCastException("bad cast");
        } catch (Exception e)  {
            exception = e;
        }

        subject = new ErrorEvent.Builder(BaseEvent.Name.AD_REQUEST, BaseEvent.Category.REQUESTS, 0.10000123)
                .withException(exception)
                .build();

        assertThat(subject.getName()).isEqualTo(BaseEvent.Name.AD_REQUEST);
        assertThat(subject.getCategory()).isEqualTo(BaseEvent.Category.REQUESTS);
        assertThat(subject.getScribeCategory()).isEqualTo(BaseEvent.ScribeCategory.EXCHANGE_CLIENT_ERROR);
        assertThat(subject.getErrorExceptionClassName()).isEqualTo("java.lang.ClassCastException");
        assertThat(subject.getErrorMessage()).isEqualTo("bad cast");

        // We can't reliably check the stack trace since it changes from one run to another
//        assertThat(subject.getErrorStackTrace()).isEqualTo();

        assertThat(subject.getErrorFileName()).isEqualTo("ErrorEventTest.java");
        assertThat(subject.getErrorClassName()).isEqualTo("com.mopub.common.event.ErrorEventTest");
        assertThat(subject.getErrorMethodName()).isEqualTo("builder_withException_shouldCorrectlyPopulateErrorFields");

        // Ideally we check the actual line number here, but since this file is continuously
        // changing, it makes the test brittle to do so
        assertThat(subject.getErrorLineNumber()).isNotNull();
    }
}
