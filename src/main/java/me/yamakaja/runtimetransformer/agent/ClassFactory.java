package me.yamakaja.runtimetransformer.agent;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Yamakaja on 3/5/18.
 */
@UtilityClass
public class ClassFactory {
    private final String CLASS_PREFIX = "me/runtimetransformer/generated/Anonymous$";
    private final AtomicLong CLASS_COUNTER = new AtomicLong(0);
    private Method classLoaderDefineClass;

    static {
        try {
            classLoaderDefineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            classLoaderDefineClass.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public Class<?> generateAnonymousClassSubstitute(String newOuterClass, ClassNode paramNode, ClassLoader targetClassLoader) {
        var node = new ClassNode(Opcodes.ASM9);
        paramNode.accept(node);

        String originalClassName = node.name;
        String originalContainingClass = node.name.substring(0, node.name.lastIndexOf('$'));

        node.name = CLASS_PREFIX + CLASS_COUNTER.getAndIncrement();
        node.access = Modifier.PUBLIC;


        node.methods.forEach(method -> {
            method.access = (method.access | Modifier.PUBLIC) & ~(Modifier.PRIVATE | Modifier.PROTECTED);
            for (var instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode) {
                    var fieldInsnNode = (FieldInsnNode) instruction;
                    if (method.name.equals("<init>")) {
                        fieldInsnNode.owner = node.name;
                        if (fieldInsnNode.desc.equals("L" + originalContainingClass + ";")) {
                            fieldInsnNode.desc = "L" + newOuterClass + ";";
                        }
                        continue;
                    }

                    if (fieldInsnNode.owner.equals(originalClassName)) {
                        fieldInsnNode.owner = node.name;
                    }
                }

                if (instruction instanceof MethodInsnNode) {
                    var methodInsnNode = (MethodInsnNode) instruction;
                    methodInsnNode.owner = methodInsnNode.owner.equals(originalClassName) ? node.name : newOuterClass;
                }
            }
            if (method.name.equals("<init>")) {
                method.desc = method.desc.replace(originalContainingClass, newOuterClass);
            }
        });

        node.fields.forEach(field -> {
            field.access = (field.access | Modifier.PUBLIC) & ~(Modifier.PRIVATE | Modifier.PROTECTED);
            if (!field.desc.equals("L" + originalContainingClass + ";")) {
                return;
            }

            field.desc = "L" + newOuterClass + ";";
        });

        node.outerClass = null;
        try {
            var writer = new ClassWriter(Opcodes.ASM9);
            node.accept(writer);
            var data = writer.toByteArray();
            return (Class<?>) classLoaderDefineClass.invoke(targetClassLoader, node.name.replace('/', '.'), data, 0, data.length);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
