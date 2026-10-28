package pe.fiberlux.nexus;
import pe.fiberlux.nexus.controller.WebServer;
import pe.fiberlux.nexus.dao.FileStore;
import pe.fiberlux.nexus.dao.MysqlStore;
import pe.fiberlux.nexus.dao.Store;
import pe.fiberlux.nexus.service.DemoData;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.*;
public final class NexusDesk_Fiberlux_NetBeans{
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(System.getenv().getOrDefault("NEXUSDESK_DATA", "data")).toAbsolutePath();Files.createDirectories(dir);
  String url=System.getenv("DB_URL");Store store=(url!=null && !url.isBlank())?new MysqlStore(url,System.getenv().getOrDefault("DB_USER","app_fiberlux"),System.getenv().getOrDefault("DB_PASSWORD","")):new FileStore(dir.resolve("nexusdesk.db"));DemoData.load(store);
  int port=Integer.parseInt(System.getenv().getOrDefault("PORT","8090"));WebServer web=new WebServer(store,port,dir);web.start();
  System.out.println("\n==============================================\n NEXUSDESK - Fiberlux SAC / Java 21+\n http://127.0.0.1:"+web.port()+"\n ADMIN: admin@fiberlux.test / Admin2026!\n TECNICO: roberto@fiberlux.test / Tecnico2026!\n CLIENTE: cliente@empresa.test / Cliente2026!\n==============================================\n");
  if(!"true".equalsIgnoreCase(System.getenv("NEXUSDESK_NO_BROWSER")))try{if(Desktop.isDesktopSupported())Desktop.getDesktop().browse(new URI("http://127.0.0.1:"+web.port()));}catch(Exception ignored){}
 }
}
