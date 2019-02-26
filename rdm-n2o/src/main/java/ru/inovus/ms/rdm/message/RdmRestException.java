package ru.inovus.ms.rdm.message;

import net.n2oapp.framework.api.exception.N2oException;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Копия кода из класса net.n2oapp.platform.jaxrs.RestException в n2o-platform
 */
public class RdmRestException extends N2oException {
    private static final Pattern STACKTRACE_ELEMENT_PATTERN = Pattern.compile(".+\\(.+:[0-9]+\\)");

    private final List<String> stackTrace;

    RdmRestException(String message, List<String> stackTrace, Exception e) {
        super(message, e);
        this.stackTrace = stackTrace;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        if (stackTrace != null) {
            List<StackTraceElement> stackTraceElements = new ArrayList<>(Arrays.asList(super.getStackTrace()));
            stackTraceElements.addAll(stackTrace.stream().map(this::parseFrame)
                    .filter(Objects::nonNull).collect(Collectors.toList()));
            return stackTraceElements.toArray(new StackTraceElement[0]);
        } else {
            return super.getStackTrace();
        }
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        super.printStackTrace(writer);
        if (stackTrace != null) {
            writer.print("Caused by: ");
            stackTrace.forEach(writer::println);
        }
    }

    private StackTraceElement parseFrame(String stackTraceFrame) {
        String frame = stackTraceFrame.replace("\tat ", "").trim();
        if (STACKTRACE_ELEMENT_PATTERN.matcher(frame).matches()) {
            String classAndMethod = frame.substring(0, frame.indexOf('('));
            String fileAndLine = frame.substring(frame.indexOf('(') + 1, frame.length() - 1);
            String className = classAndMethod.substring(0, classAndMethod.lastIndexOf('.'));
            String methodName = classAndMethod.substring(classAndMethod.lastIndexOf('.') + 1);
            String fileName = fileAndLine.substring(0, fileAndLine.indexOf(':'));
            int lineNumber = Integer.parseInt(fileAndLine.substring(fileAndLine.indexOf(':') + 1));
            return new StackTraceElement(className, methodName, fileName, lineNumber);
        } else {
            return null;
        }
    }
}
