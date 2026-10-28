package pe.fiberlux.nexus.dao;
import pe.fiberlux.nexus.model.Database;
import java.util.function.Function;
public interface Store {
 <T> T read(Function<Database,T> op);
 <T> T write(Function<Database,T> op);
}
