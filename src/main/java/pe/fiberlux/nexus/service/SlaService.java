package pe.fiberlux.nexus.service;
import pe.fiberlux.nexus.model.Database.Ticket;
import pe.fiberlux.nexus.util.M;
import java.time.*;
import java.util.*;
public final class SlaService {
 public int minutes(String p){return switch(p){case "P1"->120;case "P2"->240;case "P3"->480;default->1440;};}
 public Map<String,Object> status(Ticket t){LocalDateTime deadline=t.creado.plusMinutes(minutes(t.prioridad));LocalDateTime now=t.resuelto!=null?t.resuelto:LocalDateTime.now();long remaining=Duration.between(now,deadline).toMinutes();long elapsed=Duration.between(t.creado,now).toMinutes();boolean finished=t.resuelto!=null||t.cerrado!=null;boolean ok=finished?remaining>=0:false;
  String state=finished?(ok?"CUMPLIDO":"INCUMPLIDO"):(remaining<0?"VENCIDO":remaining<=45?"EN_RIESGO":"DENTRO_DE_SLA");
  return M.of("limiteMin",minutes(t.prioridad),"restanteMin",remaining,"transcurridoMin",elapsed,"consumoPct",Math.min(100,Math.max(0,Math.round(100.0*elapsed/minutes(t.prioridad)))),"estado",state,"cumplido",ok,"fechaLimite",deadline.toString());
 }
}
