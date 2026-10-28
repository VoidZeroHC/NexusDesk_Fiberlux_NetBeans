package pe.fiberlux.nexus.service;
import pe.fiberlux.nexus.dao.Store;
import pe.fiberlux.nexus.model.Database.User;
import pe.fiberlux.nexus.util.*;
import java.util.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.concurrent.*;

public final class AuthService{
 private final Store dao; private final SecureRandom rnd=new SecureRandom();private final Map<String,Session> sessions=new ConcurrentHashMap<>();
 public record Session(int userId,String token,String csrf,Instant expires){}
 public AuthService(Store store){dao=store;}
 public Session login(String email,String pass){String e=email.trim().toLowerCase(Locale.ROOT);User u=dao.read(db->db.usuarios.values().stream().filter(v->v.correo.equalsIgnoreCase(e)).findFirst().orElse(null));
  if(u==null||!u.activo||!Security.verify(pass,u.passwordHash))throw new Errors.HttpError(401,"Credenciales incorrectas");
  byte[] b=new byte[32];rnd.nextBytes(b);String tok=Base64.getUrlEncoder().withoutPadding().encodeToString(b);rnd.nextBytes(b);String csrf=Base64.getUrlEncoder().withoutPadding().encodeToString(b);Session s=new Session(u.id,tok,csrf,Instant.now().plus(Duration.ofHours(8)));sessions.put(tok,s);Integrations.log("Acceso autenticado usuario #"+u.id);return s;
 }
 public Session session(String token){if(token==null)return null;Session s=sessions.get(token);if(s==null)return null;if(Instant.now().isAfter(s.expires)){sessions.remove(token);return null;}return s;}
 public void logout(String token){if(token!=null)sessions.remove(token);}
 public User user(Session session){if(session==null)throw new Errors.HttpError(401,"Inicie sesión");User u=dao.read(db->db.usuarios.get(session.userId));if(u==null||!u.activo)throw new Errors.HttpError(401,"Cuenta inactiva");return u;}
 public static Map<String,Object> publicUser(User u){return M.of("id",u.id,"nombre",u.nombre,"correo",u.correo,"rol",u.rol,"empresa",u.empresa,"especialidad",u.especialidad,"activo",u.activo);}
 public static void role(User u,String... roles){for(String r:roles)if(r.equals(u.rol))return;throw new Errors.HttpError(403,"Permiso insuficiente para esta operación");}
}
