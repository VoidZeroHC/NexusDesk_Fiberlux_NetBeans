package pe.fiberlux.nexus.util;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;
/** Usa BCrypt cuando su dependencia Maven está disponible; PBKDF2 SHA256 en ejecución sin librerías. */
public final class Security{
 private static final SecureRandom RAND=new SecureRandom();
 public static String hash(String password){
  if(password.length()<8)throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
  try{Class<?> c=Class.forName("org.mindrot.jbcrypt.BCrypt");String salt=(String)c.getMethod("gensalt",int.class).invoke(null,12);return (String)c.getMethod("hashpw",String.class,String.class).invoke(null,password,salt);}catch(ClassNotFoundException ex){return pbkdf2(password);}catch(Exception ex){throw new IllegalStateException("BCrypt no disponible",ex);}
 }
 private static String pbkdf2(String pw){byte[] salt=new byte[16];RAND.nextBytes(salt);return "$pbkdf2$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(pw,salt));}
 private static byte[] derive(String pw,byte[] salt){try{PBEKeySpec spec=new PBEKeySpec(pw.toCharArray(),salt,210_000,256);return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}catch(Exception e){throw new IllegalStateException(e);}}
 public static boolean verify(String pw,String stored){
  if(stored==null)return false;
  if(stored.startsWith("$2"))try{Class<?> c=Class.forName("org.mindrot.jbcrypt.BCrypt");return (boolean)c.getMethod("checkpw",String.class,String.class).invoke(null,pw,stored);}catch(Exception e){return false;}
  if(stored.startsWith("$pbkdf2$")){String[] p=stored.split("\\$");return p.length==4 && MessageDigest.isEqual(Base64.getDecoder().decode(p[3]),derive(pw,Base64.getDecoder().decode(p[2])));}return false;
 }
}
