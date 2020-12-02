package me.yamakaja.runtimetransformer.agent;

import lombok.Getter;
import lombok.experimental.Accessors;
import me.yamakaja.runtimetransformer.transformer.ClassTransformer;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Yamakaja on 19.05.17.
 */
@Accessors(fluent = true)
public class Agent {
    private final Instrumentation instrumentation;
    private Agent(Instrumentation inst){
        this.instrumentation = inst;
    }

    @Getter
    private static Agent instance;
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        instance = new Agent(instrumentation);
    }

    public void process(Class<?> ... transformerClasses) throws UnmodifiableClassException {
        var agentJobs = Arrays.stream(transformerClasses).map(AgentJob::new).collect(Collectors.toList());

        var classTransformer = new ClassTransformer(agentJobs);
        instrumentation.addTransformer(classTransformer, true);

        instrumentation.retransformClasses(classTransformer.getClassesToTransform());
    }
}
