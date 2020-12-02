package me.yamakaja.runtimetransformer.agent;

import me.yamakaja.runtimetransformer.annotation.InjectionType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Created by Yamakaja on 19.05.17.
 */
public class MethodJob {
    private final InjectionType type;
    private final MethodNode transformerNode;
    private final String transformationTarget;
    private final Class<?> transformationTargetClass;
    private final String transformer;
    private final String superClass;
    private final Map<String, SpecialInvocation> specialInvocations;
    private final Map<String, ClassNode> innerClasses;
    private MethodNode resultNode;

    public MethodJob(InjectionType type, String transformationTarget, Class<?> transformationTargetClass, String transformer, String superClass, MethodNode transformerNode,
                     Map<String, SpecialInvocation> specialInvocations, Map<String, ClassNode> innerClasses) {
        this.type = type;
        this.transformerNode = transformerNode;
        this.transformationTarget = transformationTarget;
        this.transformationTargetClass = transformationTargetClass;
        this.transformer = transformer;
        this.superClass = superClass;
        this.specialInvocations = specialInvocations;
        this.innerClasses = innerClasses;

        transformerNode.name = transformerNode.name.endsWith("_INJECTED")
                ? transformerNode.name.substring(0, transformerNode.name.length() - 9)
                : transformerNode.name;

        transformerNode.name = transformerNode.name.equals("_init_") ? "<init>" : transformerNode.name;
    }

    public void process() {
        switch (type) {
            case OVERRIDE -> override();
            case INSERT -> insert();
            case APPEND -> append();
        }

        transformInvocations();
    }

    private void transformInvocations() {
        var waitingForInit = false;
        var replacementClassName = "null";
        for (var entry : resultNode.instructions) {
            if (entry instanceof MethodInsnNode) {
                var methodInsnNode = (MethodInsnNode) entry;
                if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL && waitingForInit && methodInsnNode.owner.startsWith(transformer)) {
                    methodInsnNode.owner = replacementClassName;
                    waitingForInit = false;
                }

                var invocation = this.specialInvocations.get(methodInsnNode.name);
                if (invocation != null) {
                    var callParameters = invocation.callParameters();
                    methodInsnNode.setOpcode(callParameters.type().getOpcode());
                    methodInsnNode.name = callParameters.name().isEmpty() ? methodInsnNode.name : callParameters.name();
                    methodInsnNode.owner = callParameters.owner().isEmpty() ? methodInsnNode.owner : callParameters.owner();
                    methodInsnNode.desc = callParameters.desc().isEmpty() ? methodInsnNode.desc : callParameters.desc();
                    methodInsnNode.itf = callParameters.itf();
                }

                methodInsnNode.owner = methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL && methodInsnNode.owner.equals(this.transformationTarget) ? this.superClass : this.transformationTarget;
                if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL) {
                    methodInsnNode.desc = methodInsnNode.desc.replace("L" + this.transformer + ";", "L" + this.transformationTarget + ";");
                }

                if (methodInsnNode.name.endsWith("_INJECTED")) {
                    methodInsnNode.name = methodInsnNode.name.substring(0, methodInsnNode.name.length() - 9);
                }
            }

            if (entry.getOpcode() == Opcodes.NEW && entry instanceof TypeInsnNode) {
                var typeInsnNode = (TypeInsnNode) entry;
                var innerClassNode = this.innerClasses.get(typeInsnNode.desc);
                if (innerClassNode != null) {
                    var newClass = ClassFactory.generateAnonymousClassSubstitute(this.transformationTarget, innerClassNode, transformationTargetClass.getClassLoader());
                    typeInsnNode.desc = replacementClassName = newClass.getName().replace('.', '/');
                    waitingForInit = true;
                }
            }

            if (entry instanceof FieldInsnNode) {
                var fieldInsnNode  = (FieldInsnNode) entry;
                if(fieldInsnNode.owner.equals(transformer)) {
                    fieldInsnNode.owner = this.transformationTarget;
                }
            }
        }
    }

    private void append() {
        if (!this.resultNode.desc.endsWith("V")) {
            throw new RuntimeException("Can't append to non-void method!");
        }

        var list = resultNode.instructions;
        var node = list.getLast();
        if (node instanceof LabelNode) {
            node = node.getPrevious();
        }

        if (node.getOpcode() != Opcodes.RETURN) {
            throw new RuntimeException("Method " + this.resultNode.name + " in " + this.transformationTarget + " doesn't end with return opcode?!");
        }

        list.remove(node);
        list.add(transformerNode.instructions);
        resultNode.instructions.add(transformerNode.instructions);
    }

    private void insert() {
        var instructions = transformerNode.instructions;
        var node = instructions.getLast();

        while (true) {
            if (node == null) {
                break;
            }

            if (node instanceof LabelNode) {
                node = node.getPrevious();
                continue;
            }

            if (node.getOpcode() == Opcodes.RETURN) {
                instructions.remove(node);
            } else if (node.getOpcode() == Opcodes.ATHROW && node.getPrevious().getOpcode() == Opcodes.ACONST_NULL) {
                AbstractInsnNode prev = node.getPrevious();
                instructions.remove(node);
                instructions.remove(prev);
            }

            break;
        }

        resultNode.instructions.insert(instructions);
    }

    private void override() {
        resultNode = transformerNode;
    }

    public MethodNode getResultNode() {
        return resultNode;
    }

    public void apply(ClassNode node) {
        if(node.methods.isEmpty()){
            throw new RuntimeException("Target method node not found! Transformer: " + transformer);
        }

        for (var i = 0; i < node.methods.size(); i++) {
            if (!(transformerNode.name.equals(node.methods.get(i).name) && transformerNode.desc.equals(node.methods.get(i).desc))) {
                continue;
            }

            this.resultNode =  node.methods.get(i);
            process();

            node.methods.set(i, getResultNode());
            return;
        }
    }

    @Override
    public String toString() {
        return "MethodJob{" +
          "type=" + type +
          ", transformerNode=" + transformerNode +
          ", transformationTarget='" + transformationTarget + '\'' +
          ", transformationTargetClass=" + transformationTargetClass +
          ", transformer='" + transformer + '\'' +
          ", superClass='" + superClass + '\'' +
          ", specialInvocations=" + specialInvocations +
          ", innerClasses=" + innerClasses +
          ", resultNode=" + resultNode +
          '}';
    }
}
