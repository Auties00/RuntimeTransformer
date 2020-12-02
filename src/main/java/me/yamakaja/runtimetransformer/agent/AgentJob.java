package me.yamakaja.runtimetransformer.agent;

import lombok.Data;
import lombok.experimental.Accessors;
import me.yamakaja.runtimetransformer.annotation.*;
import me.yamakaja.runtimetransformer.util.MethodUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Yamakaja on 19.05.17.
 */
@Data
@Accessors(fluent = true)
public class AgentJob {
    private final List<MethodJob> methodJobs;
    private final Class<?> transformer;
    private Class<?> toTransform;
    private final Map<String, SpecialInvocation> specialInvocations;

    public AgentJob(Class<?> transformer) {
        this.transformer = transformer;
        this.specialInvocations = new HashMap<>();
        this.readTransformationTarget(transformer);

        try {
            var transformerNode = new ClassNode(Opcodes.ASM9);
            var transformerReader = new ClassReader(transformer.getResource(transformer.getSimpleName() + ".class").openStream());
            transformerReader.accept(transformerNode, 0);

            var innerClasses = readInnerClasses(transformerNode);
            var methods = transformer.getDeclaredMethods();

            this.findSpecialInvocations(methods);
            this.methodJobs = new ArrayList<>(methods.length);

            Arrays
              .stream(methods)
              .filter(method -> method.isAnnotationPresent(Inject.class))
              .forEach(method -> {
                  var type = method.getAnnotation(Inject.class).value();
                  var transformerMethodNode = transformerNode.methods
                    .stream()
                    .filter(node -> node != null && method.getName().equals(node.name) && MethodUtils.getSignature(method).equals(node.desc))
                    .findAny();

                  if (transformerMethodNode.isEmpty()) {
                      throw new RuntimeException("Transformer method node not found! (WTF?)");
                  }

                  var methodJob = new MethodJob(type, this.toTransform.getName().replace('.', '/'), this.toTransform, transformer.getName().replace('.', '/'), this.toTransform.getSuperclass().getName().replace('.', '/'),  transformerMethodNode.get(), this.specialInvocations, innerClasses);
                  System.out.println(methodJob);
                  methodJobs.add(methodJob);
              });
        } catch (IOException e) {
            throw new RuntimeException("Failed to load class file of " + this.toTransform.getSimpleName(), e);
        }
    }

    private Map<String, ClassNode> readInnerClasses(ClassNode classNode) {
        var ret = new HashMap<String, ClassNode>();
        classNode.innerClasses
          .stream()
          .filter(node -> node.name.matches(".*\\$[0-9]+"))
          .filter(node -> node.innerName == null && node.outerName == null)
          .map(node -> {
              try (var inputStream = this.transformer.getResourceAsStream(node.name.substring(node.name.lastIndexOf('/') + 1) + ".class")) {
                  var innerClassNode = new ClassNode(Opcodes.ASM9);
                  var reader = new ClassReader(inputStream);
                  reader.accept(innerClassNode, 0);
                  return innerClassNode;
              } catch (IOException e) {
                  throw new RuntimeException(e);
              }
          })
          .forEach(node -> ret.put(node.name, node));

        return ret;
    }

    private void findSpecialInvocations(Method[] methods) {
        Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(CallParameters.class))
                .forEach(method -> this.specialInvocations.put(method.getName().replace('.', '/'), new SpecialInvocation(method, method.getAnnotation(CallParameters.class))));
    }

    private void readTransformationTarget(Class<?> transformer) {
        if (transformer.isAnnotationPresent(Transform.class)) {
            this.toTransform = transformer.getAnnotation(Transform.class).value();
            return;
        }

        if (!transformer.isAnnotationPresent(TransformByName.class)) {
            throw new RuntimeException("No transformation annotation present on transformer: " + transformer.getName());
        }


        try {
            this.toTransform = Class.forName(transformer.getAnnotation(TransformByName.class).value(), true, transformer.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to transform class: ", e);
        }
    }

    public void apply(ClassNode node) {
        methodJobs.forEach(methodJob -> methodJob.apply(node));
    }
}
