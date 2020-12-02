package me.yamakaja.runtimetransformer.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Yamakaja on 19.05.17.
 */
@UtilityClass
public class MethodUtils {
    public String getSignature(Method method) {
        var internalFirst = Arrays.stream(method.getParameters()).map(parameter -> translateTypeToInternal(parameter.getType())).collect(Collectors.joining(""));
        var internalSecond = translateTypeToInternal(method.getReturnType());
        return String.format("(%s)%s", internalFirst, internalSecond);
    }

    public String translateTypeToInternal(Class<?> type) {
        return type.isArray() ? type.getName() : switch (type.getName()) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "double" -> "D";
            case "float" -> "F";
            case "int" -> "I";
            case "long" -> "J";
            case "short" -> "S";
            case "void" -> "V";
            default -> "L" + type.getName().replace('.', '/') + ";";
        };
    }
}
