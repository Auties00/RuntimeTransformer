package me.yamakaja.runtimetransformer.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import me.yamakaja.runtimetransformer.annotation.CallParameters;

import java.lang.reflect.Method;

/**
 * Created by Yamakaja on 3/5/18.
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class SpecialInvocation {
  private final Method method;
  private final CallParameters callParameters;
}
