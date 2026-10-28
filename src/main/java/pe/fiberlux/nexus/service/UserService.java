package pe.fiberlux.nexus.service;
import pe.fiberlux.nexus.dao.Store;
import pe.fiberlux.nexus.model.Database.User;
import pe.fiberlux.nexus.util.*;
import java.util.*;
import static pe.fiberlux.nexus.service.Errors.require;

public final class UserService {
 private final Store dao;
 public UserService(Store dao){this.dao=dao;}
 public List<Object> list(User actor){AuthService.role(actor,"ADMINISTRADOR");return dao.read(db->{List<Object> l=new ArrayList<>();for(User u:db.usuarios.values())l.add(AuthService.publicUser(u));return l;});}
 public Map<String,Object> create(User actor,Map<String,Object> form){AuthService.role(actor,"ADMINISTRADOR");String name=M.required(form,"nombre",150),mail=M.required(form,"correo",150).toLowerCase(Locale.ROOT),role=M.choice(Json.str(form,"rol"),"CLIENTE","TECNICO","ADMINISTRADOR"),pass=M.required(form,"password",100);require(mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),400,"Correo inválido");return dao.write(db->{require(db.usuarios.values().stream().noneMatch(u->u.correo.equals(mail)),409,"Este correo ya está registrado");User u=new User(db.nextUser++,name,mail,Security.hash(pass),role,Integrations.clean(Json.str(form,"empresa")),Integrations.clean(Json.str(form,"especialidad")));db.usuarios.put(u.id,u);Integrations.log("Cuenta creada id #"+u.id+" por #"+actor.id);return AuthService.publicUser(u);});}
 public Map<String,Object> update(User actor,int id,Map<String,Object> form){AuthService.role(actor,"ADMINISTRADOR");return dao.write(db->{User u=db.usuarios.get(id);require(u!=null,404,"Usuario no encontrado");if(form.containsKey("activo")){require(u.id!=actor.id||Json.bool(form,"activo"),400,"No puede desactivar su propia cuenta");u.activo=Json.bool(form,"activo");}if(form.containsKey("rol")){require(u.id!=actor.id||Json.str(form,"rol").equals("ADMINISTRADOR"),400,"No puede quitarse su propio rol");u.rol=M.choice(Json.str(form,"rol"),"CLIENTE","TECNICO","ADMINISTRADOR");}if(form.containsKey("nombre"))u.nombre=M.required(form,"nombre",150);if(form.containsKey("especialidad"))u.especialidad=Json.str(form,"especialidad");return AuthService.publicUser(u);});}
}
