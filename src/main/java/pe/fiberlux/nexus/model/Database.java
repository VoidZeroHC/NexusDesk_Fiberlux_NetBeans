package pe.fiberlux.nexus.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Database implements Serializable {
  private static final long serialVersionUID=1L;
  public final Map<Integer,User> usuarios=new LinkedHashMap<>();
  public final Map<Integer,Ticket> tickets=new LinkedHashMap<>();
  public final Map<Integer,Comment> comentarios=new LinkedHashMap<>();
  public final Map<Integer,Audit> auditorias=new LinkedHashMap<>();
  public final Map<Integer,Attachment> adjuntos=new LinkedHashMap<>();
  public int nextUser=1,nextTicket=8921,nextComment=1,nextAudit=1,nextAttachment=1;
  public static class User implements Serializable {
    private static final long serialVersionUID=1L;
    public int id; public String nombre,correo,passwordHash,rol,empresa,especialidad; public boolean activo=true;
    public User(){}
    public User(int i,String n,String e,String h,String r,String emp,String esp){id=i;nombre=n;correo=e;passwordHash=h;rol=r;empresa=emp;especialidad=esp;}
  }
  public static class Ticket implements Serializable {
    private static final long serialVersionUID=1L;
    public int id,clienteId,tecnicoId; public String codigo,titulo,descripcion,categoria,prioridad,estado="ABIERTO",enlace="",servicio="",sede="";
    public LocalDateTime creado,actualizado,resuelto,cerrado; public boolean conformidad=false;
  }
  public static class Comment implements Serializable {
    private static final long serialVersionUID=1L;
    public int id,ticketId,usuarioId;public String texto; public boolean interno; public LocalDateTime creado;
  }
  public static class Audit implements Serializable {
    private static final long serialVersionUID=1L;
    public int id,ticketId,usuarioId; public String accion,detalle; public LocalDateTime creado;
  }
  public static class Attachment implements Serializable {
    private static final long serialVersionUID=1L;
    public int id,ticketId,usuarioId; public String nombre,mime,storedName;public long tamano;public LocalDateTime creado;
  }
}
