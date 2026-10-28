package pe.fiberlux.nexus.service;
import pe.fiberlux.nexus.dao.Store;
import pe.fiberlux.nexus.model.Database;
import pe.fiberlux.nexus.model.Database.*;
import pe.fiberlux.nexus.util.Security;
import java.time.*;

public final class DemoData {
 private DemoData(){}
 public static void load(Store store){store.write(db->{if(!db.usuarios.isEmpty())return null;
   add(db,"Administrador NOC","admin@fiberlux.test","Admin2026!","ADMINISTRADOR","Fiberlux SAC","");
   add(db,"Ing. Roberto Quispe","roberto@fiberlux.test","Tecnico2026!","TECNICO","Fiberlux SAC","Fibra óptica");
   add(db,"Ing. Diana Torres","diana@fiberlux.test","Tecnico2026!","TECNICO","Fiberlux SAC","Redes y BGP");
   add(db,"Ing. Marcos Vega","marcos@fiberlux.test","Tecnico2026!","TECNICO","Fiberlux SAC","Conectividad");
   add(db,"Cliente Corporativo Demo","cliente@empresa.test","Cliente2026!","CLIENTE","Empresa Corporativa Demo","");
   add(db,"Cliente Retail Demo","retail@empresa.test","Cliente2026!","CLIENTE","Retail Nacional Demo","");
   add(db,"Cliente Salud Demo","salud@empresa.test","Cliente2026!","CLIENTE","Clínica Demo","");
   String[][] t={
    {"Corte de fibra óptica en enlace troncal","Pérdida total de conectividad de la sede principal. Alarma LOS.","Fibra óptica","P1","EN_PROCESO","5","2","68","San Isidro"},
    {"Degradación de latencia en circuito empresarial","Latencia anómala y paquetes perdidos durante horario de atención.","Latencia","P2","ASIGNADO","6","3","110","Chorrillos"},
    {"Intermitencia en servicio de internet","Microcortes reportados en el enlace dedicado.","CPE / Switch","P2","ABIERTO","7","0","45","Callao"},
    {"Solicitud de monitoreo de enlace","Se requiere habilitar supervisión SNMP.","Monitoreo","P4","ABIERTO","5","0","120","Lima Centro"},
    {"Revisión de configuración BGP","Cambios de enrutamiento posteriores a mantenimiento.","BGP / Enrutamiento","P3","EN_PROCESO","6","3","100","Miraflores"},
    {"Recuperación de circuito corporativo","Se restableció el servicio de conectividad tras reemplazar CPE.","CPE / Switch","P2","RESUELTO","7","2","170","San Borja"},
    {"Soporte de enlace dedicado","Solicitud resuelta dentro del acuerdo de servicio.","Fibra óptica","P3","CERRADO","5","2","240","Surco"},
    {"Falla de transceptor SFP 10G","Transceptor sin enlace y portadora intermitente.","CPE / Switch","P1","ASIGNADO","6","2","80","La Molina"},
    {"Configuración de nodo de monitoreo","Se solicita incluir el equipo en el inventario de telemetría.","Monitoreo","P4","RESUELTO","5","3","300","Lima"},
    {"Corte parcial de segmento MPLS","Falla en acceso WAN hacia la central logística.","BGP / Enrutamiento","P1","ESCALADO","7","2","155","Callao"}
   };
   for(String[] x:t){Ticket z=new Ticket();z.id=db.nextTicket++;z.codigo="FLX-"+z.id;z.titulo=x[0];z.descripcion=x[1];z.categoria=x[2];z.prioridad=x[3];z.estado=x[4];z.clienteId=Integer.parseInt(x[5]);z.tecnicoId=Integer.parseInt(x[6]);z.creado=LocalDateTime.now().minusMinutes(Integer.parseInt(x[7]));z.actualizado=z.creado.plusMinutes(4);z.sede=x[8];z.servicio="Internet Dedicado / Fibra";z.enlace="FLX-LNK-"+z.id;
   if(z.estado.equals("RESUELTO")||z.estado.equals("CERRADO"))z.resuelto=z.creado.plusMinutes(35);if(z.estado.equals("CERRADO")){z.cerrado=z.creado.plusMinutes(55);z.conformidad=true;}
   db.tickets.put(z.id,z);}
   return null;});}
 private static void add(Database db,String name,String mail,String pass,String role,String company,String specialty){int id=db.nextUser++;db.usuarios.put(id,new User(id,name,mail,Security.hash(pass),role,company,specialty));}
}
