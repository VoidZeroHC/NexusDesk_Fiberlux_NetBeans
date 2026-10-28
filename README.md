# NexusDesk — Sistema NOC / Helpdesk de Fiberlux SAC

**Proyecto Java para NetBeans 29 | JDK 21 o superior | Frontend + Backend funcional**

Aplicación con siete módulos: acceso con roles; dashboard NOC; nuevo ticket; detalle y seguimiento; tablero Kanban; usuarios y roles; reportes de acuerdos SLA. Incluye backend Java HTTP, servicios y DAOs, persistencia, auditoría, evidencias adjuntas, reportes PDF/Excel y esquema MySQL 8 opcional.

## 1. Inicio inmediato en Windows, sin configurar una base de datos

1. Descomprima el proyecto completo en una carpeta de Windows.
2. Confirme Java JDK 21 o superior (`java -version`). JDK 25 es compatible.
3. Ejecute **Iniciar_Fiberlux.bat**. También puede abrir directamente **NexusDesk_Fiberlux.jar** con `java -jar`.
4. Si no se abre el navegador automáticamente, visite **http://127.0.0.1:8090**.
5. La base de datos local se inicializa con registros demostrativos y los cambios se conservan al cerrar el programa.

### Accesos de demostración

| Perfil | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@fiberlux.test` | `Admin2026!` |
| Técnico NOC | `roberto@fiberlux.test` | `Tecnico2026!` |
| Cliente corporativo | `cliente@empresa.test` | `Cliente2026!` |

Otros técnicos y clientes demostrativos pueden consultarse desde **Usuarios y Roles**.

**Estas cuentas son solo para una demostración académica. Antes de implementar el proyecto en un servidor público, cree usuarios reales, suprima los datos de muestra y configure HTTPS, secretos, respaldos y monitoreo.**

## 2. Abrir y ejecutar en NetBeans 29

1. Abra **NetBeans 29** y seleccione **Archivo > Abrir proyecto**.
2. Escoja la carpeta del proyecto que contiene `pom.xml` (se reconoce como proyecto Maven).
3. En **Propiedades del proyecto**, utilice **JDK 21 o superior**.
4. Ejecute la clase principal **`pe.fiberlux.nexus.NexusDesk_Fiberlux_NetBeans`**. El nombre de proyecto, la clase principal y el artefacto coinciden.
5. Puede utilizar **Run Project** tras resolver las dependencias Maven. La primera resolución de dependencias Maven requiere Internet; si no hay Internet, ejecute el `.jar` incluido, que no lo necesita.
6. La ventana **Output** muestra la dirección del portal.

### Comandos Maven

```bash
mvn clean package
mvn exec:java
mvn test
```

### Iniciar desde consola (sin Maven)

```bash
java -jar NexusDesk_Fiberlux.jar
```

Si el puerto 8090 está ocupado, configure `PORT=8091` antes de ejecutar. Para no abrir el navegador automáticamente configure `NEXUSDESK_NO_BROWSER=true`.

## 3. Elegir persistencia

**Por defecto:** DAO persistente local bajo `data/`. Es el modo listo para ejecutar y no requiere servicios externos.

**MySQL 8 (opcional):** Importar `db/mysql_schema.sql` con una cuenta de instalación y definir:

```text
DB_URL=jdbc:mysql://localhost:3306/tickets_db?useUnicode=true&characterEncoding=UTF-8
DB_USER=app_fiberlux
DB_PASSWORD=SU_CLAVE_DE_DESARROLLO
```

Luego iniciar desde NetBeans con las dependencias Maven, que incluyen MySQL Connector/J. El DAO selecciona automáticamente MySQL si existe `DB_URL`; si no, utiliza el DAO persistente local. En MySQL se emplean transacciones y consultas parametrizadas. **La integración MySQL no pudo probarse sin una instancia MySQL disponible en el entorno de construcción.**

## 4. Funcionalidades

- Autenticación con cookies `HttpOnly`, validación de sesión y token CSRF; RBAC por cliente, técnico y administrador.
- Aislamiento de tickets por cliente, gestión de personal NOC y cuentas activas/inactivas.
- Apertura de tickets con código automático, categorías, prioridades P1–P4 y asignación al técnico disponible con menor carga.
- Estados: abierto, asignado, en proceso, escalado, resuelto y cerrado.
- Historial y auditoría de cambios, comentarios públicos y notas internas; adjuntos PDF/PNG/JPG/TXT hasta 2 MB.
- SLA automático: P1=2 h, P2=4 h, P3=8 h, P4=24 h. Riesgo de vencimiento cuando quedan 45 minutos o menos.
- Dashboard dinámico, Kanban, filtros, reportes por técnico y periodo; exportación PDF y XLSX.

## 5. Arquitectura del proyecto

- **Model:** `model.Database` y objetos de dominio.
- **View:** interfaz HTML/CSS/JavaScript responsive de 7 pantallas.
- **Controller (MVC):** `controller.WebServer` expone las rutas y valida acceso.
- **Service:** `AuthService`, `TicketService`, `SlaService`, `ReportService` y `UserService`.
- **DAO:** interfaz `Store` con `FileStore` o `MysqlStore`.
- **SOLID:** responsabilidad única por servicio, inyección de Store por constructor e inversión de dependencia mediante interfaz.
- **Logback:** registro de operaciones; integración opcional con SLF4J mediante Maven.
- **Apache Commons:** normalización de texto; **Guava:** procesamiento de espacios; **Apache POI:** generación de Excel; se habilitan automáticamente con las dependencias Maven y existen mecanismos compatibles sin Internet para la ejecución inmediata.
- **Seguridad:** hashed passwords (bcrypt con dependencia Maven, PBKDF2-SHA256 en ejecución Java autocontenida), consultas SQL parametrizadas, HttpOnly, CSRF, control de acceso, validación de adjuntos, datos locales ligados a `127.0.0.1`.

**Nota de implementación:** la solución funcional usa un servidor HTTP incluido en Java, no Spring Boot, para permitir el inicio offline inmediato. Conserva MVC y DAO. Para producción multiusuario distribuida corresponde migrar a un servidor Spring Boot con sesiones compartidas, pool JDBC, HTTPS y respaldos administrados.

## 6. Pruebas y capturas

La carpeta `evidencias/` incluye siete capturas de las interfaces renderizadas con datos obtenidos del backend Java y un reporte de **26 comprobaciones automáticas correctas**.

Para repetir las pruebas sin Maven, desde la carpeta raíz:

```bash
javac -encoding UTF-8 --release 21 -d build/classes src/main/java/pe/fiberlux/nexus/**/*.java
```

En Windows, es más sencillo compilar y ejecutar `PruebasIntegracion` desde NetBeans; su método `main` prueba servicios HTTP reales con un servidor temporal, controles de acceso, persistencia, reglas SLA y exportaciones. Las clases de pruebas están bajo `src/test/java`.

## 7. Control de versiones

Se incluye un repositorio local Git con commits por etapas y el historial también se registra en `docs/historial_git.txt`. Para publicarlo en su cuenta GitHub, cree un repositorio vacío y configure su URL:

```bash
git remote add origin URL_DE_SU_REPOSITORIO
git branch -M main
git push -u origin main
```

No se incluye una URL pública de GitHub porque la publicación requiere acceso a la cuenta del titular.

## 8. Limitaciones relevantes

Esta versión es una implementación funcional académica, no un despliegue productivo. Se validó en modo local con Java 21 y pruebas HTTP automatizadas; la conexión a un servidor MySQL, la descarga inicial de bibliotecas Maven y el despliegue con HTTPS no se probaron en este entorno. La herramienta de captura visual utilizó datos de ejecución provenientes de la API y renderizó cada pantalla, sin navegador conectado directamente al puerto local del servidor.
