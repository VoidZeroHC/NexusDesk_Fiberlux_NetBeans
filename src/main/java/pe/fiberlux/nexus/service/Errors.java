package pe.fiberlux.nexus.service;
public final class Errors {
 private Errors(){}
 public static class HttpError extends RuntimeException {public final int status;public HttpError(int code,String message){super(message);status=code;}}
 public static void require(boolean ok,int status,String msg){if(!ok)throw new HttpError(status,msg);}
}
