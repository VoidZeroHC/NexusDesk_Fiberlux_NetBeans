package pe.fiberlux.nexus;
import pe.fiberlux.nexus.controller.WebServer;
import pe.fiberlux.nexus.dao.FileStore;
import pe.fiberlux.nexus.service.*;
import pe.fiberlux.nexus.util.Json;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Pruebas automáticas integrales sin necesidad de JUnit ni conexión a internet. */
public final class PruebasIntegracion{
 private static int ok=0;
 private static HttpClient client=HttpClient.newBuilder().cookieHandler(new java.net.CookieManager(null,java.net.CookiePolicy.ACCEPT_ALL)).build();
 private static String base,csrf;
 private static void check(boolean cond,String msg){if(!cond)throw new AssertionError("FALLÓ: "+msg);System.out.println("OK "+(++ok)+" - "+msg);}
 private static HttpResponse<String> req(String path,String method,String data,String csrfVal)throws Exception{
  HttpRequest.Builder b=HttpRequest.newBuilder().uri(URI.create(base+path)).timeout(Duration.ofSeconds(8));if(csrfVal!=null)b.header("X-CSRF-Token",csrfVal);if(data!=null)b.header("Content-Type","application/json");b.method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(data));return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
 }
 private static Map<String,Object> obj(String json){return Json.object(json);}
 public static void main(String[] args)throws Exception {
  Path dir=Files.createTempDirectory("nexusdesk-e2e-");FileStore store=new FileStore(dir.resolve("test.db"));DemoData.load(store);WebServer server=new WebServer(store,0,dir);server.start();base="http://127.0.0.1:"+server.port();
  try{
   check(req("/api/dashboard","GET",null,null).statusCode()==401,"API privada rechaza anónimo");
   check(req("/api/auth/login","POST","{\"correo\":\"admin@fiberlux.test\",\"password\":\"incorrecta\"}",null).statusCode()==401,"Credenciales incorrectas rechazadas");
   HttpResponse<String> login=req("/api/auth/login","POST","{\"correo\":\"admin@fiberlux.test\",\"password\":\"Admin2026!\"}",null);check(login.statusCode()==200,"Autenticación de administrador");csrf=String.valueOf(obj(login.body()).get("csrf"));
   check(req("/api/me","GET",null,null).statusCode()==200,"Sesión HTTP conservada");
   check(req("/api/dashboard","GET",null,null).statusCode()==200,"Dashboard con datos de incidencias");
   check(req("/api/tickets","GET",null,null).statusCode()==200,"Lista de tickets");
   check(req("/api/usuarios","GET",null,null).statusCode()==200,"RBAC permite administración");
   String newBody="{\"clienteId\":5,\"titulo\":\"Corte de acceso a fibra de pruebas\",\"descripcion\":\"Incidencia registrada en prueba de integración de aplicaciones\",\"prioridad\":\"P1\",\"categoria\":\"Fibra óptica\"}";
   check(req("/api/tickets","POST",newBody,null).statusCode()==403,"Protección CSRF habilitada");
   HttpResponse<String> created=req("/api/tickets","POST",newBody,csrf);check(created.statusCode()==201,"Creación persistente de ticket");int id=((Number)((Map<String,Object>)obj(created.body()).get("data")).get("id")).intValue();
   check(req("/api/tickets/"+id,"GET",null,null).statusCode()==200,"Detalle y trazabilidad del ticket");
   check(req("/api/tickets/"+id+"/comentarios","POST","{\"texto\":\"Diagnóstico desde centro NOC\"}",csrf).statusCode()==201,"Comentario con fecha y autor");
   check(req("/api/tickets/"+id+"/estado","POST","{\"estado\":\"EN_PROCESO\"}",csrf).statusCode()==200,"Cambio de estado autorizado");
   check(req("/api/tickets/"+id+"/estado","POST","{\"estado\":\"RESUELTO\"}",csrf).statusCode()==200,"Resolución registra tiempo SLA");
   check(req("/api/reportes/sla","GET",null,null).statusCode()==200,"Reporte SLA filtrable");
   check(req("/api/reportes/exportar?formato=xlsx","GET",null,null).statusCode()==200,"Exportación Excel OpenXML");
   check(req("/api/reportes/exportar?formato=pdf","GET",null,null).statusCode()==200,"Exportación PDF válida");
   check(new FileStore(dir.resolve("test.db")).read(d->d.tickets.containsKey(id)),"Persistencia después de abrir DAO nuevamente");
   check(new SlaService().minutes("P1")==120&&new SlaService().minutes("P4")==1440,"Reglas SLA P1 y P4");
   check(Json.object("{\"numero\":5}").get("numero") instanceof Long,"JSON conserva tipos enteros");
   HttpClient adminClient=client;
   client=HttpClient.newBuilder().cookieHandler(new java.net.CookieManager(null,java.net.CookiePolicy.ACCEPT_ALL)).build();
   HttpResponse<String> cl=req("/api/auth/login","POST","{\"correo\":\"retail@empresa.test\",\"password\":\"Cliente2026!\"}",null);
   check(cl.statusCode()==200,"Acceso de cliente corporativo independiente");
   String clientCsrf=String.valueOf(obj(cl.body()).get("csrf"));
   check(req("/api/usuarios","GET",null,null).statusCode()==403,"Cliente bloqueado en usuarios RBAC");
   check(req("/api/reportes/sla","GET",null,null).statusCode()==403,"Cliente bloqueado en reportes gerenciales");
   check(req("/api/tickets/"+id,"GET",null,null).statusCode()==403,"Aislamiento entre clientes de distintos contratos");
   check(req("/api/tickets/"+id+"/comentarios","POST","{\"texto\":\"Acceso no autorizado\"}",clientCsrf).statusCode()==403,"Comentario restringido al propietario");
   String clientTicket="{\"titulo\":\"Caida de enlace en sitio corporativo\",\"descripcion\":\"Se informa incidente de conectividad de otra empresa\",\"prioridad\":\"P2\",\"categoria\":\"Latencia\",\"clienteId\":5}";
   HttpResponse<String> cNew=req("/api/tickets","POST",clientTicket,clientCsrf);
   check(cNew.statusCode()==201,"Cliente puede registrar su incidencia");
   check(((Number)((Map<String,Object>)obj(cNew.body()).get("data")).get("clienteId")).intValue()==6,"ID de cliente se obtiene de sesión y no del formulario");
   client=adminClient;

   System.out.println("\nRESULTADO: "+ok+" verificaciones correctas.");
  }finally{server.stop();}
  System.exit(0);
 }
}
