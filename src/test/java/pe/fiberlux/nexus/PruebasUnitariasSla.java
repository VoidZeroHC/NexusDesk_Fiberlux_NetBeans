package pe.fiberlux.nexus;
import org.junit.jupiter.api.Test;
import pe.fiberlux.nexus.service.SlaService;
import pe.fiberlux.nexus.model.Database.Ticket;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
public class PruebasUnitariasSla{
 @Test void tiemposPorPrioridad(){SlaService s=new SlaService();assertEquals(120,s.minutes("P1"));assertEquals(240,s.minutes("P2"));assertEquals(480,s.minutes("P3"));assertEquals(1440,s.minutes("P4"));}
 @Test void resueltoAntesDelVencimiento(){Ticket t=new Ticket();t.prioridad="P1";t.creado=LocalDateTime.now().minusMinutes(100);t.resuelto=t.creado.plusMinutes(70);assertEquals("CUMPLIDO",new SlaService().status(t).get("estado"));}
 @Test void resueltoFueraDeSla(){Ticket t=new Ticket();t.prioridad="P2";t.creado=LocalDateTime.now().minusHours(6);t.resuelto=t.creado.plusHours(5);assertEquals("INCUMPLIDO",new SlaService().status(t).get("estado"));}
}
