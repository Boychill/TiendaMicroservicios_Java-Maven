@echo off
TITLE Lanzador de Microservicios (Java + Maven)
ECHO.
ECHO ======================================
ECHO    Lanzador de Microservicios
ECHO ======================================
ECHO.
ECHO IMPORTANTE: Asegurate de que 'iniciar_eureka.bat' ya se ejecuto
ECHO y que Eureka (puerto 8761) esta listo.
ECHO.
ECHO Servicios a iniciar:
ECHO  1. Auth (Puerto 8081)
ECHO  2. Gateway (Puerto 8080)
ECHO  3. Catalogo (Puerto 8082)
ECHO  4. Pedidos (Puerto 8083)
ECHO.
PAUSE

C:

:: --- 1. SERVICIO AUTH ---
ECHO.
ECHO [1/4] Iniciando 'auth' (Puerto 8081)...
cd C:\Tienda\microservicios\auth
START "Auth Service (8081)" cmd /c "mvn spring-boot:run "-Dmaven.test.skip=true""

:: Pausa breve para escalonar el inicio
timeout /t 5 /nobreak > nul

:: --- 2. SERVICIO GATEWAY ---
ECHO.
ECHO [2/4] Iniciando 'gateway' (Puerto 8080)...
cd C:\Tienda\microservicios\gateway
START "Gateway Service (8080)" cmd /c "mvn spring-boot:run"

timeout /t 5 /nobreak > nul

:: --- 3. SERVICIO CATALOGO ---
ECHO.
ECHO [3/4] Iniciando 'catalogo' (Puerto 8082)...
cd C:\Tienda\microservicios\catalogo
START "Catalogo Service (8082)" cmd /c "mvn spring-boot:run"

timeout /t 5 /nobreak > nul

:: --- 4. SERVICIO PEDIDOS ---
ECHO.
ECHO [4/4] Iniciando 'pedidos' (Puerto 8083)...
cd C:\Tienda\microservicios\pedidos
START "Pedidos Service (8083)" cmd /c "mvn spring-boot:run"

ECHO.
ECHO ======================================
ECHO   TODOS LOS SERVICIOS LANZADOS
ECHO ======================================
ECHO.
ECHO Veras 4 nuevas ventanas de terminal.
ECHO Esta ventana se cerrara en 10 segundos.
ECHO.
timeout /t 10