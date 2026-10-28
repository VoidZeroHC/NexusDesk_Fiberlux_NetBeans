package pe.fiberlux.nexus.dao;
import pe.fiberlux.nexus.model.Database;
import java.io.*;
import java.nio.file.*;
import java.util.function.Function;

/** DAO local persistente: escritura transaccional atómica y sincronizada. */
public final class FileStore implements Store {
 private Database db; private final Path path;
 public FileStore(Path p){path=p;try{Files.createDirectories(p.toAbsolutePath().getParent());if(Files.isRegularFile(p))try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(p))){db=(Database)in.readObject();} }catch(Exception e){throw new IllegalStateException("No se pudo leer la base local, revise data/",e);} if(db==null)db=new Database();}
 @Override public synchronized <T>T read(Function<Database,T> f){return f.apply(db);}
 @Override public synchronized <T>T write(Function<Database,T> f){T result=f.apply(db);save();return result;}
 private void save(){try{
  Path tmp=path.resolveSibling(path.getFileName()+".tmp");
  try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(tmp))){out.writeObject(db);}
  try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
 }catch(IOException e){throw new IllegalStateException("No fue posible guardar los cambios",e);}}
}
