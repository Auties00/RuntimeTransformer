package me.yamakaja.runtimetransformer.util;

import com.sun.tools.attach.VirtualMachine;
import lombok.experimental.UtilityClass;
import me.yamakaja.runtimetransformer.agent.Agent;
import me.yamakaja.runtimetransformer.transformer.RuntimeTransformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Created by Yamakaja on 9/2/17.
 */
@UtilityClass
public class TransformerUtils {
    public void attachAgent(File agentFile, Class<?>[] transformers) {
        try {
            var pid = ManagementFactory.getRuntimeMXBean().getName();
            var vm = VirtualMachine.attach(pid.substring(0, pid.indexOf('@')));
            vm.loadAgent(agentFile.getAbsolutePath());
            vm.detach();

            Agent.instance().process(transformers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File saveAgentJar() {
        try (var is = new FileInputStream(new File("C:\\Users\\alaut\\ExtensionMethods\\target\\ExtensionMethods-1.0-SNAPSHOT.jar"))) {
            var agentFile = File.createTempFile("agent", ".jar");
            agentFile.deleteOnExit();

            Files.copy(is, agentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return agentFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
