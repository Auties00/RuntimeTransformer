module ExtensionMethods {
  requires java.compiler;
  requires lombok;
  requires java.instrument;
  requires java.management;
  requires jdk.attach;
  requires org.objectweb.asm;
  requires org.objectweb.asm.tree;

  exports me.yamakaja.runtimetransformer.agent;
  exports me.yamakaja.runtimetransformer.annotation;
  exports me.yamakaja.runtimetransformer.transformer;
  exports me.yamakaja.runtimetransformer.util;
}