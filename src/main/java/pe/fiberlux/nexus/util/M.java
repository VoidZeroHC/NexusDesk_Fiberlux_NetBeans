package pe.fiberlux.nexus.util;
import java.util.*;
public final class M {
 private M(){}
 public static Map<String,Object> of(Object... kv){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2)m.put(String.valueOf(kv[i]),kv[i+1]);return m;}
 public static String required(Map<String,Object> m,String k,int max){String s=Integrations.clean(Json.str(m,k));if(s.isEmpty())throw new IllegalArgumentException("Campo obligatorio: "+k);if(s.length()>max)throw new IllegalArgumentException("Longitud máxima superada: "+k);return s;}
 public static String choice(String value,String... options){for(String s:options)if(s.equals(value))return s;throw new IllegalArgumentException("Opción no válida: "+value);}
}
