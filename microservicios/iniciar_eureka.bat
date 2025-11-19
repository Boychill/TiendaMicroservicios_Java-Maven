@echo off
TITLE Servidor Discovery (Eureka)
ECHO.
ECHO ======================================
ECHO    Iniciando Servidor Eureka (Discovery)
ECHO    (Java 21 + Maven)
ECHO ======================================
ECHO.
ECHO Ruta base: C:\Tienda\microservicios
ECHO.

REM Cambiamos a la unidad C: y a la carpeta correcta
C:
cd C:\Tienda\microservicios\eureka

ECHO Limpiando e instalando dependencias (mvn clean install)...
REM El flag -DskipTests salta los tests para arrancar mas rapido
call mvn clean install -DskipTests

ECHO.
ECHO Iniciando Eureka...
START "Servidor Discovery (Eureka: 8761)" cmd /c "mvn spring-boot:run"

ECHO.
ECHO El servidor Eureka se esta iniciando en una nueva ventana.
ECHO Esta ventana se cerrara en 10 segundos.
timeout /t 10