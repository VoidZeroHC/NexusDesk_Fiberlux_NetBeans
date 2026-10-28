package pe.fiberlux.nexus.dao;
import pe.fiberlux.nexus.model.Database;
import pe.fiberlux.nexus.model.Database.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/** DAO relacional opcional: JDBC con consultas parametrizadas y transacciones MySQL 8. */
public final class MysqlStore implements Store {
 private final String url,user,password;
 private final Database db=new Database();
 private final Map<String,Map<Integer,Integer>> previous=new HashMap<>();
 public MysqlStore(String url,String user,String password){this.url=url;this.user=user;this.password=password;try(Connection c=connection()){load(c);}catch(SQLException ex){throw new IllegalStateException("Conexión MySQL fallida. Importe db/mysql_schema.sql y verifique DB_URL/DB_USER/DB_PASSWORD",ex);}}
 private Connection connection()throws SQLException{return DriverManager.getConnection(url,user,password);}
 private LocalDateTime date(ResultSet r,String col)throws SQLException{Timestamp t=r.getTimestamp(col);return t==null?null:t.toLocalDateTime();}
 private void load(Connection c)throws SQLException {
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM usuario")){while(r.next()){User u=new User(r.getInt("id_usuario"),r.getString("nombre"),r.getString("correo"),r.getString("password_hash"),r.getString("rol"),r.getString("empresa"),r.getString("especialidad"));u.activo=r.getBoolean("activo");db.usuarios.put(u.id,u);}}
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM ticket")){while(r.next()){Ticket t=new Ticket();t.id=r.getInt("id_ticket");t.codigo=r.getString("codigo");t.clienteId=r.getInt("id_cliente");t.tecnicoId=r.getInt("id_tecnico");t.categoria=r.getString("categoria");t.titulo=r.getString("titulo");t.descripcion=r.getString("descripcion");t.prioridad=r.getString("prioridad");t.estado=r.getString("estado");t.enlace=r.getString("enlace");t.servicio=r.getString("servicio");t.sede=r.getString("sede");t.creado=date(r,"fecha_creacion");t.actualizado=date(r,"fecha_actualizacion");t.resuelto=date(r,"fecha_resolucion");t.cerrado=date(r,"fecha_cierre");t.conformidad=r.getBoolean("conformidad");db.tickets.put(t.id,t);}}
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM comentario")){while(r.next()){Comment x=new Comment();x.id=r.getInt("id_comentario");x.ticketId=r.getInt("id_ticket");x.usuarioId=r.getInt("id_usuario");x.texto=r.getString("texto");x.interno=r.getBoolean("interno");x.creado=date(r,"fecha");db.comentarios.put(x.id,x);}}
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM auditoria")){while(r.next()){Audit x=new Audit();x.id=r.getInt("id_auditoria");x.ticketId=r.getInt("id_ticket");x.usuarioId=r.getInt("id_usuario");x.accion=r.getString("accion");x.detalle=r.getString("detalle");x.creado=date(r,"fecha");db.auditorias.put(x.id,x);}}
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM adjunto")){while(r.next()){Attachment a=new Attachment();a.id=r.getInt("id_adjunto");a.ticketId=r.getInt("id_ticket");a.usuarioId=r.getInt("id_usuario");a.nombre=r.getString("nombre");a.mime=r.getString("mime");a.storedName=r.getString("archivo");a.tamano=r.getLong("tamano");a.creado=date(r,"fecha");db.adjuntos.put(a.id,a);}}
  db.nextUser=db.usuarios.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)+1;
  db.nextTicket=Math.max(8921,db.tickets.keySet().stream().mapToInt(Integer::intValue).max().orElse(8920)+1);
  db.nextComment=db.comentarios.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)+1;
  db.nextAudit=db.auditorias.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)+1;
  db.nextAttachment=db.adjuntos.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)+1;
  for(var e:db.usuarios.entrySet())signature("usuario").put(e.getKey(),sigUser(e.getValue()));
  for(var e:db.tickets.entrySet())signature("ticket").put(e.getKey(),sigTicket(e.getValue()));
  for(var e:db.comentarios.entrySet())signature("comentario").put(e.getKey(),sigComment(e.getValue()));
  for(var e:db.auditorias.entrySet())signature("auditoria").put(e.getKey(),sigAudit(e.getValue()));
  for(var e:db.adjuntos.entrySet())signature("adjunto").put(e.getKey(),sigAttachment(e.getValue()));
 }
 @Override public synchronized <T>T read(Function<Database,T> op){return op.apply(db);}
 @Override public synchronized <T>T write(Function<Database,T> op){T result=op.apply(db);persist();return result;}
 private Map<Integer,Integer> signature(String key){return previous.computeIfAbsent(key,k->new HashMap<>());}
 private boolean changed(String table,int id,int hash){return !Objects.equals(signature(table).get(id),hash);}
 private void done(String name,int id,int hash){signature(name).put(id,hash);}
 private static int sigUser(User u){return Objects.hash(u.nombre,u.correo,u.passwordHash,u.rol,u.activo,u.empresa,u.especialidad);}
 private static int sigTicket(Ticket t){return Objects.hash(t.codigo,t.titulo,t.descripcion,t.categoria,t.prioridad,t.estado,t.clienteId,t.tecnicoId,t.enlace,t.servicio,t.sede,t.creado,t.actualizado,t.resuelto,t.cerrado,t.conformidad);}
 private static int sigComment(Comment x){return Objects.hash(x.ticketId,x.usuarioId,x.texto,x.interno,x.creado);}
 private static int sigAudit(Audit x){return Objects.hash(x.ticketId,x.usuarioId,x.accion,x.detalle,x.creado);}
 private static int sigAttachment(Attachment x){return Objects.hash(x.ticketId,x.usuarioId,x.nombre,x.mime,x.storedName,x.tamano,x.creado);}
 private static Timestamp ts(LocalDateTime x){return x==null?null:Timestamp.valueOf(x);}
 private void persist(){List<Runnable> updates=new ArrayList<>();try(Connection c=connection()){
  c.setAutoCommit(false);Map<String,Map<Integer,Integer>> next=new HashMap<>();
  try {
   String uSql="INSERT INTO usuario(id_usuario,nombre,correo,password_hash,rol,empresa,especialidad,activo) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),correo=VALUES(correo),password_hash=VALUES(password_hash),rol=VALUES(rol),empresa=VALUES(empresa),especialidad=VALUES(especialidad),activo=VALUES(activo)";
   try(PreparedStatement p=c.prepareStatement(uSql)){for(User u:db.usuarios.values()){int sig=sigUser(u);if(!changed("usuario",u.id,sig))continue;p.setInt(1,u.id);p.setString(2,u.nombre);p.setString(3,u.correo);p.setString(4,u.passwordHash);p.setString(5,u.rol);p.setString(6,u.empresa);p.setString(7,u.especialidad);p.setBoolean(8,u.activo);p.addBatch();next.computeIfAbsent("usuario",k->new HashMap<>()).put(u.id,sig);}p.executeBatch();}
   String tSql="INSERT INTO ticket(id_ticket,codigo,id_cliente,id_tecnico,categoria,titulo,descripcion,prioridad,estado,enlace,servicio,sede,fecha_creacion,fecha_actualizacion,fecha_resolucion,fecha_cierre,conformidad) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id_tecnico=VALUES(id_tecnico),categoria=VALUES(categoria),titulo=VALUES(titulo),descripcion=VALUES(descripcion),prioridad=VALUES(prioridad),estado=VALUES(estado),enlace=VALUES(enlace),servicio=VALUES(servicio),sede=VALUES(sede),fecha_actualizacion=VALUES(fecha_actualizacion),fecha_resolucion=VALUES(fecha_resolucion),fecha_cierre=VALUES(fecha_cierre),conformidad=VALUES(conformidad)";
   try(PreparedStatement p=c.prepareStatement(tSql)){for(Ticket t:db.tickets.values()){int sig=sigTicket(t);if(!changed("ticket",t.id,sig))continue;p.setInt(1,t.id);p.setString(2,t.codigo);p.setInt(3,t.clienteId);if(t.tecnicoId==0)p.setNull(4,Types.INTEGER);else p.setInt(4,t.tecnicoId);p.setString(5,t.categoria);p.setString(6,t.titulo);p.setString(7,t.descripcion);p.setString(8,t.prioridad);p.setString(9,t.estado);p.setString(10,t.enlace);p.setString(11,t.servicio);p.setString(12,t.sede);p.setTimestamp(13,ts(t.creado));p.setTimestamp(14,ts(t.actualizado));p.setTimestamp(15,ts(t.resuelto));p.setTimestamp(16,ts(t.cerrado));p.setBoolean(17,t.conformidad);p.addBatch();next.computeIfAbsent("ticket",k->new HashMap<>()).put(t.id,sig);}p.executeBatch();}
   try(PreparedStatement p=c.prepareStatement("INSERT INTO comentario(id_comentario,id_ticket,id_usuario,texto,interno,fecha) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE texto=VALUES(texto),interno=VALUES(interno)")){for(Comment x:db.comentarios.values()){int sig=sigComment(x);if(!changed("comentario",x.id,sig))continue;p.setInt(1,x.id);p.setInt(2,x.ticketId);p.setInt(3,x.usuarioId);p.setString(4,x.texto);p.setBoolean(5,x.interno);p.setTimestamp(6,ts(x.creado));p.addBatch();next.computeIfAbsent("comentario",k->new HashMap<>()).put(x.id,sig);}p.executeBatch();}
   try(PreparedStatement p=c.prepareStatement("INSERT INTO auditoria(id_auditoria,id_ticket,id_usuario,accion,detalle,fecha) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE detalle=VALUES(detalle)")){for(Audit x:db.auditorias.values()){int sig=sigAudit(x);if(!changed("auditoria",x.id,sig))continue;p.setInt(1,x.id);p.setInt(2,x.ticketId);p.setInt(3,x.usuarioId);p.setString(4,x.accion);p.setString(5,x.detalle);p.setTimestamp(6,ts(x.creado));p.addBatch();next.computeIfAbsent("auditoria",k->new HashMap<>()).put(x.id,sig);}p.executeBatch();}
   try(PreparedStatement p=c.prepareStatement("INSERT INTO adjunto(id_adjunto,id_ticket,id_usuario,nombre,mime,archivo,tamano,fecha) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),mime=VALUES(mime),archivo=VALUES(archivo),tamano=VALUES(tamano)")){for(Attachment a:db.adjuntos.values()){int sig=sigAttachment(a);if(!changed("adjunto",a.id,sig))continue;p.setInt(1,a.id);p.setInt(2,a.ticketId);p.setInt(3,a.usuarioId);p.setString(4,a.nombre);p.setString(5,a.mime);p.setString(6,a.storedName);p.setLong(7,a.tamano);p.setTimestamp(8,ts(a.creado));p.addBatch();next.computeIfAbsent("adjunto",k->new HashMap<>()).put(a.id,sig);}p.executeBatch();}
   c.commit();next.forEach((k,m)->signature(k).putAll(m));
  }catch(Exception e){c.rollback();throw e;}
 }catch(Exception e){throw new IllegalStateException("Transacción MySQL rechazada; revise acceso y esquema",e);}}
}
