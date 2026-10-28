package pe.fiberlux.nexus.service;
import pe.fiberlux.nexus.dao.Store;
import pe.fiberlux.nexus.model.Database.*;
import pe.fiberlux.nexus.util.M;
import java.time.*;
import java.util.*;

public final class ReportService{
 private final Store dao;private final SlaService sla=new SlaService();
 public ReportService(Store dao){this.dao=dao;}
 public Map<String,Object> dashboard(User actor){return dao.read(db->{List<Ticket> list=db.tickets.values().stream().filter(t->actor.rol.equals("ADMINISTRADOR")||actor.rol.equals("CLIENTE")&&t.clienteId==actor.id||actor.rol.equals("TECNICO")&&t.tecnicoId==actor.id).toList();
  int total=list.size(),open=0,risk=0,solved=0,ontime=0,closed=0;Map<String,Integer> categories=new LinkedHashMap<>(),severity=new LinkedHashMap<>(),states=new LinkedHashMap<>();List<Object> urgent=new ArrayList<>();
  for(Ticket t:list){Map<String,Object> s=sla.status(t);String sl=(String)s.get("estado");categories.merge(t.categoria,1,Integer::sum);severity.merge(t.prioridad,1,Integer::sum);states.merge(t.estado,1,Integer::sum);if(!t.estado.equals("CERRADO")&&!t.estado.equals("RESUELTO")){open++;if(sl.equals("EN_RIESGO")||sl.equals("VENCIDO")){risk++;urgent.add(M.of("codigo",t.codigo,"titulo",t.titulo,"estado",t.estado,"prioridad",t.prioridad,"restanteMin",s.get("restanteMin"),"id",t.id));}}
   else {solved++;if(Boolean.TRUE.equals(s.get("cumplido")))ontime++;if(t.estado.equals("CERRADO"))closed++;}
  }
  urgent.sort(Comparator.comparingLong(x->((Number)((Map<?,?>)x).get("restanteMin")).longValue()));
  double pct=solved==0?100.0:Math.round(1000.0*ontime/solved)/10.0;
  return M.of("total",total,"abiertos",open,"riesgo",risk,"resueltos",solved,"cerrados",closed,"cumplidos",ontime,"slaPct",pct,"categorias",categories,"severidad",severity,"estados",states,"criticos",urgent);
 });}
 public Map<String,Object> slaReport(User actor,String technician,String range){AuthService.role(actor,"ADMINISTRADOR","TECNICO");return dao.read(db->{LocalDateTime cutoff=switch(range){case "semana"->LocalDateTime.now().minusDays(7);case "trimestre"->LocalDateTime.now().minusDays(90);case "todo"->LocalDateTime.MIN;default->LocalDateTime.now().minusDays(30);};
 List<Object> rows=new ArrayList<>();int total=0,closed=0,onTime=0,violations=0;Map<String,int[]> byTech=new LinkedHashMap<>(),byPriority=new LinkedHashMap<>();
 for(Ticket t:db.tickets.values()){
  if(t.creado.isBefore(cutoff))continue;
  if(actor.rol.equals("TECNICO")&&t.tecnicoId!=actor.id)continue;
  if(!technician.isBlank()&&!technician.equals("todos")&&!String.valueOf(t.tecnicoId).equals(technician))continue;
  total++;User u=db.usuarios.get(t.tecnicoId);String tech=u==null?"Sin asignar":u.nombre;
  Map<String,Object> status=sla.status(t);boolean done=t.resuelto!=null||t.cerrado!=null,ok=Boolean.TRUE.equals(status.get("cumplido"));if(done){closed++;if(ok)onTime++;else violations++;}
  int[] x=byTech.computeIfAbsent(tech,k->new int[3]);x[0]++;if(done){x[1]++;if(ok)x[2]++;}
  int[] pr=byPriority.computeIfAbsent(t.prioridad,k->new int[3]);pr[0]++;if(done){pr[1]++;if(ok)pr[2]++;}
  rows.add(M.of("codigo",t.codigo,"fecha",t.creado.toLocalDate().toString(),"cliente",db.usuarios.get(t.clienteId)==null?"":db.usuarios.get(t.clienteId).empresa,"categoria",t.categoria,"prioridad",t.prioridad,"tecnico",tech,"estado",t.estado,"sla",status.get("estado"),"limiteMin",status.get("limiteMin")));
 }
 List<Object> people=new ArrayList<>(),priorities=new ArrayList<>();for(var e:byTech.entrySet()){int[] x=e.getValue();people.add(M.of("nombre",e.getKey(),"asignados",x[0],"resueltos",x[1],"cumplimiento",x[1]==0?100:Math.round(1000.0*x[2]/x[1])/10.0));}for(var e:byPriority.entrySet()){int[] x=e.getValue();priorities.add(M.of("prioridad",e.getKey(),"total",x[0],"resueltos",x[1],"cumplimiento",x[1]==0?100:Math.round(1000.0*x[2]/x[1])/10.0));}
 return M.of("total",total,"resueltos",closed,"dentro",onTime,"incumplidos",violations,"porcentaje",closed==0?100:Math.round(1000.0*onTime/closed)/10.0,"tecnicos",people,"prioridades",priorities,"filas",rows);
 });}
}
