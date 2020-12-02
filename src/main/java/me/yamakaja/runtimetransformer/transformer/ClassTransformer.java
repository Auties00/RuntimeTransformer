package me.yamakaja.runtimetransformer.transformer;

import me.yamakaja.runtimetransformer.agent.AgentJob;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Created by Yamakaja on 18.05.17.
 */
public class ClassTransformer implements ClassFileTransformer {
    private final List<AgentJob> agentJobs;
    private final List<Class<?>> classesToRedefine;

    public ClassTransformer(List<AgentJob> agentJobs) {
        this.agentJobs = agentJobs;
        this.classesToRedefine = agentJobs.stream().map(AgentJob::toTransform).collect(Collectors.toList());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!classesToRedefine.contains(classBeingRedefined)) {
            return classfileBuffer;
        }

        try {
            final var reader = new ClassReader(classfileBuffer);
            final var node = new ClassNode(Opcodes.ASM9);
            reader.accept(node, 0);

            agentJobs
              .stream()
              .filter(job -> job.toTransform().getName().replace('.', '/').equals(className))
              .forEach(job -> job.apply(node));

            var writer = new FixedClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Class<?>[] getClassesToTransform() {
        return agentJobs.stream().map(AgentJob::toTransform).toArray((IntFunction<Class<?>[]>) Class[]::new);
    }
}
