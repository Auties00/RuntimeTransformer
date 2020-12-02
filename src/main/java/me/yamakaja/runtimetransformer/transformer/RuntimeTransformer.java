package me.yamakaja.runtimetransformer.transformer;

import me.yamakaja.runtimetransformer.util.TransformerUtils;

/**
 * Created by Yamakaja on 19.05.17.
 */
public class RuntimeTransformer {
    public RuntimeTransformer(Class<?>... transformers) {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
        TransformerUtils.attachAgent(TransformerUtils.saveAgentJar(), transformers);
    }
}
