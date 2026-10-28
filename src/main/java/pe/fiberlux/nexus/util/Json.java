package pe.fiberlux.nexus.util;
import java.util.*;
import java.lang.reflect.Array;

/** Serializador JSON propio: sin dependencias para poder abrir el sistema sin internet. */
public final class Json {
 private Json(){}
 public static String stringify(Object x){StringBuilder b=new StringBuilder();add(b,x);return b.toString();}
 private static void add(StringBuilder b,Object x){
  if(x==null){b.append("null");return;}
  if(x instanceof String || x instanceof Character){b.append('"');for(char c:x.toString().toCharArray())switch(c){case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}} b.append('"');return;}
  if(x instanceof Number || x instanceof Boolean){b.append(x);return;}
  if(x instanceof Map<?,?> m){b.append('{');boolean first=true;for(var entry:m.entrySet()){if(!first)b.append(',');first=false;add(b,String.valueOf(entry.getKey()));b.append(':');add(b,entry.getValue());}b.append('}');return;}
  if(x instanceof Iterable<?> it){b.append('[');boolean first=true;for(Object v:it){if(!first)b.append(',');first=false;add(b,v);}b.append(']');return;}
  if(x.getClass().isArray()){b.append('[');for(int i=0;i<Array.getLength(x);i++){if(i>0)b.append(',');add(b,Array.get(x,i));}b.append(']');return;}
  add(b,x.toString());
 }
 public static Map<String,Object> object(String s){Object o=new Parser(s).value();if(!(o instanceof Map))throw new IllegalArgumentException("Se esperaba objeto JSON");return (Map<String,Object>)o;}
 public static String str(Map<String,Object> m,String k){Object x=m.get(k);return x==null?"":String.valueOf(x).trim();}
 public static int integer(Map<String,Object> m,String k){Object x=m.get(k);if(x==null||x.toString().isBlank())return 0;return Integer.parseInt(x.toString());}
 public static boolean bool(Map<String,Object> m,String k){return Boolean.parseBoolean(String.valueOf(m.getOrDefault(k,false)));}
 private static final class Parser {
  private final String s;private int i;
  Parser(String s){this.s=s;}
  void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
  Object value(){ws();if(i>=s.length())throw new IllegalArgumentException("JSON incompleto");char c=s.charAt(i);
   if(c=='{')return obj();if(c=='[')return arr();if(c=='"')return string();if(s.startsWith("true",i)){i+=4;return true;}if(s.startsWith("false",i)){i+=5;return false;}if(s.startsWith("null",i)){i+=4;return null;}return number();}
  Object obj(){Map<String,Object> o=new LinkedHashMap<>();i++;ws();if(i<s.length()&&s.charAt(i)=='}'){i++;return o;}while(true){ws();String k=string();ws();expect(':');o.put(k,value());ws();if(s.charAt(i)=='}'){i++;return o;}expect(',');}}
  Object arr(){List<Object> a=new ArrayList<>();i++;ws();if(s.charAt(i)==']'){i++;return a;}while(true){a.add(value());ws();if(s.charAt(i)==']'){i++;return a;}expect(',');}}
  String string(){expect('"');StringBuilder b=new StringBuilder();while(i<s.length()){char c=s.charAt(i++);if(c=='"')return b.toString();if(c=='\\'){if(i>=s.length())throw new IllegalArgumentException("Escape inválido");char e=s.charAt(i++);switch(e){case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'b'->b.append('\b');case 'f'->b.append('\f');case '"'->b.append('"');case '\\'->b.append('\\');case '/'->b.append('/');case 'u'->{if(i+4>s.length())throw new IllegalArgumentException("Unicode inválido");b.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;}default->throw new IllegalArgumentException("Escape inválido");}}else b.append(c);}throw new IllegalArgumentException("Texto JSON incompleto");}
  Number number(){int start=i;while(i<s.length()&&"-+.0123456789eE".indexOf(s.charAt(i))>=0)i++;if(start==i)throw new IllegalArgumentException("JSON inválido");String n=s.substring(start,i);if(n.contains(".")||n.contains("e")||n.contains("E"))return Double.valueOf(n);return Long.valueOf(n);}
  void expect(char c){ws();if(i>=s.length()||s.charAt(i++)!=c)throw new IllegalArgumentException("JSON: se esperaba "+c);}
 }
}
