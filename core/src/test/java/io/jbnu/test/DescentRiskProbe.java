package io.jbnu.test;
/** Executable RED/GREEN contract probe, intentionally independent of JUnit. */
public final class DescentRiskProbe {
 public static void main(String[] args) throws Exception {
  java.lang.reflect.Method risk;
  try { risk=GameRules.class.getMethod("survival",int.class); }
  catch(NoSuchMethodException e){throw new AssertionError("Missing depth-based landing risk API",e);}
  double p=(Double)risk.invoke(null,3);
  if(Math.abs(p-.531441)>1e-12)throw new AssertionError("three-floor survival: "+p);
  System.out.println("PASS depth-based risk probe");
 }
}
