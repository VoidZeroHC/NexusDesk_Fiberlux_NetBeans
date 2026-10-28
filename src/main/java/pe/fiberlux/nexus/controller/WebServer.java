package pe.fiberlux.nexus.controller;
import com.sun.net.httpserver.*;
import pe.fiberlux.nexus.dao.Store;
import pe.fiberlux.nexus.service.*;
import pe.fiberlux.nexus.model.Database.User;
import pe.fiberlux.nexus.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Controller HTTP MVC: valida autenticación y delega toda regla de negocio a Service. */
public final class WebServer implements HttpHandler{
 private final HttpServer server;private final AuthService auth;private final TicketService tickets;private final UserService users;private final ReportService reports;
 private final Map<String,Deque<Long>> attempts=new ConcurrentHashMap<>();
 public WebServer(Store db,int port,java.nio.file.Path dataDir)throws IOException{
   auth=new AuthService(db);tickets=new TicketService(db,dataDir);users=new UserService(db);reports=new ReportService(db);
   server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);server.createContext("/",this);server.setExecutor(Executors.newFixedThreadPool(16));
 }
 public void start(){server.start();Integrations.log("NexusDesk listo en http://127.0.0.1:"+server.getAddress().getPort());}
 public void stop(){server.stop(0);}
 public int port(){return server.getAddress().getPort();}
 @Override public void handle(HttpExchange ex)throws IOException{
  try {String path=ex.getRequestURI().getPath(),method=ex.getRequestMethod();
   if(!path.startsWith("/api/")){staticFile(ex,path);return;}
   String origin=ex.getRequestHeaders().getFirst("Origin");if(origin!=null&&!origin.equals("http://"+ex.getRequestHeaders().getFirst("Host"))&&!origin.equals("https://"+ex.getRequestHeaders().getFirst("Host")))throw new Errors.HttpError(403,"Origen no autorizado");
   if(method.equals("OPTIONS"))throw new Errors.HttpError(405,"Método no habilitado");
   String token=cookie(ex,"NEXUS_SESSION");AuthService.Session session=auth.session(token);Map<String,Object> body=Map.of();
   if(method.equals("POST")||method.equals("PATCH")||method.equals("PUT")){
    if(!path.equals("/api/auth/login")){if(session==null)throw new Errors.HttpError(401,"Inicie sesión");String csrf=ex.getRequestHeaders().getFirst("X-CSRF-Token");if(csrf==null||!csrf.equals(session.csrf()))throw new Errors.HttpError(403,"Token CSRF ausente o inválido");}
    byte[] raw=ex.getRequestBody().readNBytes(3_000_001);if(raw.length>3_000_000)throw new Errors.HttpError(413,"Solicitud demasiado grande");String rawText=new String(raw,StandardCharsets.UTF_8);if(!rawText.isBlank())body=Json.object(rawText);
   }
   if(method.equals("POST")&&path.equals("/api/auth/login")){limitLogin(ex);AuthService.Session s=auth.login(Json.str(body,"correo"),Json.str(body,"password"));ex.getResponseHeaders().add("Set-Cookie","NEXUS_SESSION="+s.token()+"; Path=/; HttpOnly; SameSite=Lax"+("true".equalsIgnoreCase(System.getenv("NEXUSDESK_SECURE_COOKIE"))?"; Secure":""));json(ex,200,M.of("data",AuthService.publicUser(auth.user(s)),"csrf",s.csrf()));return;}
   User actor=auth.user(session);
   if(method.equals("POST")&&path.equals("/api/auth/logout")){auth.logout(token);ex.getResponseHeaders().add("Set-Cookie","NEXUS_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");json(ex,200,M.of("ok",true));return;}
   if(method.equals("GET")&&path.equals("/api/me")){json(ex,200,M.of("data",AuthService.publicUser(actor),"csrf",session.csrf()));return;}
   if(method.equals("GET")&&path.equals("/api/dashboard")){json(ex,200,M.of("data",reports.dashboard(actor)));return;}
   if(method.equals("GET")&&path.equals("/api/tecnicos")){json(ex,200,M.of("data",tickets.technicians()));return;}
   if(method.equals("GET")&&path.equals("/api/clientes")){AuthService.role(actor,"ADMINISTRADOR");json(ex,200,M.of("data",tickets.clients()));return;}
   if(path.equals("/api/tickets")){
    if(method.equals("GET")){Map<String,String> q=query(ex.getRequestURI().getRawQuery());json(ex,200,M.of("data",tickets.list(actor,q.getOrDefault("q",""),q.getOrDefault("estado",""),q.getOrDefault("prioridad",""),q.getOrDefault("categoria",""))));return;}
    if(method.equals("POST")){json(ex,201,M.of("data",tickets.create(actor,body)));return;}
   }
   if(path.equals("/api/usuarios")){
    if(method.equals("GET")){json(ex,200,M.of("data",users.list(actor)));return;}
    if(method.equals("POST")){json(ex,201,M.of("data",users.create(actor,body)));return;}
   }
   if(path.startsWith("/api/usuarios/")&&method.equals("PATCH")){int id=idPart(path,3);json(ex,200,M.of("data",users.update(actor,id,body)));return;}
   if(method.equals("GET")&&path.equals("/api/reportes/sla")){Map<String,String> q=query(ex.getRequestURI().getRawQuery());json(ex,200,M.of("data",reports.slaReport(actor,q.getOrDefault("tecnico","todos"),q.getOrDefault("periodo","mes"))));return;}
   if(method.equals("GET")&&path.equals("/api/reportes/exportar")){Map<String,String> q=query(ex.getRequestURI().getRawQuery());String type=q.getOrDefault("formato","pdf");Map<String,Object> r=reports.slaReport(actor,q.getOrDefault("tecnico","todos"),q.getOrDefault("periodo","mes"));List<List<String>> rows=new ArrayList<>();rows.add(List.of("Código","Fecha","Cliente","Categoría","Prioridad","Técnico","Estado","SLA"));List<String> lines=new ArrayList<>();lines.add("Tickets: "+r.get("total")+" | Resueltos: "+r.get("resueltos")+" | Cumplimiento: "+r.get("porcentaje")+"%");lines.add("Codigo       Categoria       Prioridad     Estado        SLA");for(Object obj:(List<?>)r.get("filas")){Map<?,?> t=(Map<?,?>)obj;List<String> row=new ArrayList<>();for(String key:List.of("codigo","fecha","cliente","categoria","prioridad","tecnico","estado","sla"))row.add(String.valueOf(t.get(key)));rows.add(row);lines.add(row.get(0)+"   "+row.get(3)+"   "+row.get(4)+"   "+row.get(6)+"   "+row.get(7));}
    switch(type){case "xlsx"->binary(ex,200,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","reporte_sla.xlsx",Exports.xlsx(rows));case "csv"->binary(ex,200,"text/csv; charset=utf-8","reporte_sla.csv",Exports.csv(rows));case "pdf"->binary(ex,200,"application/pdf","reporte_sla.pdf",Exports.pdf("REPORTE DE CUMPLIMIENTO SLA",lines));default->throw new Errors.HttpError(400,"Formato no permitido");}return;}
   if(path.startsWith("/api/adjuntos/")&&method.equals("GET")){int id=idPart(path,3);binary(ex,200,tickets.mimeOf(id),"evidencia_"+id,tickets.attachment(actor,id));return;}
   if(path.startsWith("/api/tickets/")){
    String[] seg=path.split("/");int id=idPart(path,3);
    if(seg.length==4&&method.equals("GET")){json(ex,200,M.of("data",tickets.detail(actor,id)));return;}
    if(seg.length==5){String op=seg[4];
      if(op.equals("estado")&&(method.equals("PATCH")||method.equals("POST"))){json(ex,200,M.of("data",tickets.updateState(actor,id,Json.str(body,"estado"))));return;}
      if(op.equals("asignar")&&method.equals("POST")){json(ex,200,M.of("data",tickets.assign(actor,id,Json.integer(body,"tecnicoId"))));return;}
      if(op.equals("comentarios")&&method.equals("POST")){json(ex,201,M.of("data",tickets.comment(actor,id,body)));return;}
      if(op.equals("conformidad")&&method.equals("POST")){json(ex,200,M.of("data",tickets.conformity(actor,id)));return;}
      if(op.equals("adjuntos")&&method.equals("POST")){json(ex,201,M.of("data",tickets.upload(actor,id,body)));return;}
    }
   }
   throw new Errors.HttpError(404,"Ruta no encontrada");
  }catch(Errors.HttpError e){json(ex,e.status,M.of("error",e.getMessage()));}catch(IllegalArgumentException e){json(ex,400,M.of("error",e.getMessage()));}catch(Exception e){Integrations.log("Error interno: "+e.getClass().getSimpleName()+": "+e.getMessage());json(ex,500,M.of("error","Error interno; consulte registro del servidor"));}
 }
 private int idPart(String path,int pos){try{int id=Integer.parseInt(path.split("/")[pos]);if(id<=0)throw new IllegalArgumentException();return id;}catch(Exception e){throw new Errors.HttpError(400,"Identificador inválido");}}
 private Map<String,String> query(String s){Map<String,String> m=new HashMap<>();if(s!=null)for(String kv:s.split("&")){int p=kv.indexOf('=');if(p>0)m.put(decode(kv.substring(0,p)),decode(kv.substring(p+1)));}return m;}
 private String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
 private void limitLogin(HttpExchange ex){String ip=ex.getRemoteAddress().getAddress().getHostAddress();Deque<Long> q=attempts.computeIfAbsent(ip,k->new ArrayDeque<>());synchronized(q){long now=System.currentTimeMillis();while(!q.isEmpty()&&q.peekFirst()<now-60_000)q.removeFirst();if(q.size()>=20)throw new Errors.HttpError(429,"Demasiados intentos. Reintente en un minuto");q.addLast(now);}}
 private String cookie(HttpExchange ex,String name){String s=ex.getRequestHeaders().getFirst("Cookie");if(s==null)return null;for(String p:s.split(";")){String[] kv=p.trim().split("=",2);if(kv.length==2&&kv[0].equals(name))return kv[1];}return null;}
 private void staticFile(HttpExchange ex,String p)throws IOException{if(!ex.getRequestMethod().equals("GET")){json(ex,405,M.of("error","Método inválido"));return;}String f=p.equals("/")?"/web/index.html":p;if(!List.of("/web/index.html","/app.js","/styles.css","/favicon.svg").contains(f)){json(ex,404,M.of("error","No encontrado"));return;}String resource=f.startsWith("/web/")?f:"/web"+f;try(InputStream in=getClass().getResourceAsStream(resource)){if(in==null){json(ex,404,M.of("error","Recurso no encontrado"));return;}byte[] b=in.readAllBytes();String mime=f.endsWith(".css")?"text/css; charset=utf-8":f.endsWith(".js")?"application/javascript; charset=utf-8":f.endsWith(".svg")?"image/svg+xml":"text/html; charset=utf-8";ex.getResponseHeaders().set("Content-Type",mime);ex.getResponseHeaders().set("Cache-Control","no-store");ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");ex.getResponseHeaders().set("X-Frame-Options","DENY");ex.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'");ex.sendResponseHeaders(200,b.length);try(OutputStream out=ex.getResponseBody()){out.write(b);}}}
 private void json(HttpExchange ex,int status,Object obj)throws IOException{byte[] b=Json.stringify(obj).getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.getResponseHeaders().set("Cache-Control","no-store");ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");ex.sendResponseHeaders(status,b.length);try(OutputStream out=ex.getResponseBody()){out.write(b);}}
 private void binary(HttpExchange ex,int status,String mime,String name,byte[] bytes)throws IOException{ex.getResponseHeaders().set("Content-Type",mime);ex.getResponseHeaders().set("Content-Disposition","attachment; filename=\""+name+"\"");ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");ex.sendResponseHeaders(status,bytes.length);try(OutputStream out=ex.getResponseBody()){out.write(bytes);}}
}
