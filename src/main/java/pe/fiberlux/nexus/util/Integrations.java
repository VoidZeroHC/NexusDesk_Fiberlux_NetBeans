package pe.fiberlux.nexus.util;
import java.util.logging.*;
/** Integraciones opcionales reales de Apache Commons, Guava y Logback mediante reflexión. */
public final class Integrations {
 private static final Logger JUL=Logger.getLogger("Fiberlux.NexusDesk");
 public static String clean(String input){String s=input==null?"":input.trim();
  try{Class<?> c=Class.forName("org.apache.commons.lang3.StringUtils");s=(String)c.getMethod("normalizeSpace",String.class).invoke(null,s);}catch(Exception ex){s=s.replaceAll("\\s+"," ");}
  try{Class<?> c=Class.forName("com.google.common.base.CharMatcher");Object matcher=c.getMethod("whitespace").invoke(null);s=(String)c.getMethod("trimAndCollapseFrom",CharSequence.class,char.class).invoke(matcher,s,' ');}catch(Exception ignored){}return s;
 }
 public static void log(String msg){try{Class<?> f=Class.forName("org.slf4j.LoggerFactory");Object logger=f.getMethod("getLogger",String.class).invoke(null,"NexusDesk");Class<?> l=Class.forName("org.slf4j.Logger");l.getMethod("info",String.class).invoke(logger,msg);}catch(Exception ex){JUL.info(msg);}}
}
