package org.eqasim.core.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.inject.internal.util.StackTraceElements;
import org.junit.Test;

/** Exercises the Guice diagnostic path that previously rejected Java 25 bytecode. */
public class TestGuiceJava25Compatibility {
    @Test
    public void guiceCanReadSourceLinesFromJava25Classes() throws Exception {
        var method = getClass().getDeclaredMethod("guiceCanReadSourceLinesFromJava25Classes");
        var location = (StackTraceElement) StackTraceElements.forMember(method);
        assertEquals("TestGuiceJava25Compatibility.java", location.getFileName());
        assertTrue("Guice must recover the source line, not silently discard it", location.getLineNumber() > 0);
    }
}
