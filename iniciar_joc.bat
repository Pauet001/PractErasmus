@echo off
echo ========================================================
echo                 BREAKOUT PLATFORM
echo ========================================================
echo Iniciant el joc...
echo.
echo Nota: 
echo - La primera vegada que l'obris s'iniciara el servidor web 
echo   (accessible a http://localhost:8080).
echo - Si ja tens una partida oberta, aquesta nova finestra 
echo   s'obrira en un port aleatori per permetre jugar varies 
echo   partides al mateix temps sense conflictes.
echo ========================================================
echo.

rem Utilitza l'argfile que genera Eclipse per executar l'aplicació
set JAVA_EXE="C:\Users\Usuari\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.6.10-hotspot\bin\java.exe"
set ARGFILE="C:\Users\Usuari\AppData\Local\Temp\cp_6dtufzvjqsnsa7xrolotu4751.argfile"
set MAIN_CLASS=it.pissir.breakout.BreakoutApplication

if not exist %ARGFILE% (
    echo ERROR: No s'ha trobat el fitxer d'arguments d'Eclipse.
    echo Primer executa el projecte des d'Eclipse almenys una vegada.
    pause
    exit /b 1
)

%JAVA_EXE% @%ARGFILE% %MAIN_CLASS%

pause
