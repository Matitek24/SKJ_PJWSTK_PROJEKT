@echo off
setlocal EnableDelayedExpansion

:: === KONFIGURACJA ===
color 07
cls

echo ==========================================================
echo       SKJ PROJEKT - PELNY TEST (500 PKT) [WINDOWS]
echo ==========================================================
echo.

:: 1. PRZYGOTOWANIE
echo [1/4] Sprzatanie i Kompilacja...

taskkill /F /IM java.exe >nul 2>&1

if exist out rmdir /s /q out
if exist logs rmdir /s /q logs
mkdir out
mkdir logs

echo - Kompilacja...
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo [BLAD] Kompilacja nie powiodla sie!
    del sources.txt
    pause
    exit /b 1
)
del sources.txt
echo [OK] Kompilacja udana.
echo.

:: 2. START SERWEROW (5 SZTUK)
echo [2/4] Uruchamianie serwerow (3xTCP, 2xUDP)...
echo    1. TCP : 9011 (T_Lalka = 25)
echo    2. TCP : 9012 (T_Mis = 1013)
echo    3. TCP : 9013 (T_Klocki = 500)
echo    4. UDP : 9014 (U_Auto = 15)
echo    5. UDP : 9015 (U_Baton = 65)

start /B cmd /c "java -cp out TCPServer -port 9011 -key T_Lalka -value 25 > logs\t1.log 2>&1"
start /B cmd /c "java -cp out TCPServer -port 9012 -key T_Mis -value 1013 > logs\t2.log 2>&1"
start /B cmd /c "java -cp out TCPServer -port 9013 -key T_Klocki -value 500 > logs\t3.log 2>&1"
start /B cmd /c "java -cp out UDPServer -port 9014 -key U_Auto -value 15 > logs\u1.log 2>&1"
start /B cmd /c "java -cp out UDPServer -port 9015 -key U_Baton -value 65 > logs\u2.log 2>&1"

timeout /t 2 /nobreak >nul

:: 3. START PROXY (DRZEWO)
echo.
echo [3/4] Budowa Drzewa Proxy...
echo    - Middle Proxy (8001) -> Serwery
start /B cmd /c "java -cp out Proxy -port 8001 -server localhost 9011 -server localhost 9012 -server localhost 9013 -server localhost 9014 -server localhost 9015 > logs\mid.log 2>&1"
timeout /t 2 /nobreak >nul

echo    - Root Proxy (8000)   -> Middle Proxy
start /B cmd /c "java -cp out Proxy -port 8000 -server localhost 8001 > logs\root.log 2>&1"
timeout /t 3 /nobreak >nul
echo.

:: 4. TESTY
echo [4/4] Wykonywanie testow...
echo.

:: === SEKCJA 200 PKT ===
echo [ETAP 1 - 200 PKT] Protokol jednorodny
call :run_test TCP "GET VALUE T_Lalka" "OK 25" "TCP -> TCP (T_Lalka)"
call :run_test UDP "GET VALUE U_Auto" "OK 15" "UDP -> UDP (U_Auto)"
echo.

:: === SEKCJA 400 PKT ===
echo [ETAP 2 - 400 PKT] Translacja
call :run_test TCP "GET VALUE U_Baton" "OK 65" "Translacja TCP -> UDP (U_Baton)"
call :run_test UDP "GET VALUE T_Mis" "OK 1013" "Translacja UDP -> TCP (T_Mis)"
echo.

:: === SEKCJA 500 PKT ===
echo [ETAP 3 - 500 PKT] Drzewo i Logika

:: 1. Discovery (GET NAMES)
echo|set /p="Test: Discovery (Root pyta o klucze)... "
java -cp out TCPClient -address localhost -port 8000 -command GET NAMES > temp_res.txt

:: Sprawdzamy obecność 5 kluczy
set FOUND_KEYS=0
findstr "T_Lalka" temp_res.txt >nul && set /a FOUND_KEYS+=1
findstr "T_Mis" temp_res.txt >nul && set /a FOUND_KEYS+=1
findstr "T_Klocki" temp_res.txt >nul && set /a FOUND_KEYS+=1
findstr "U_Auto" temp_res.txt >nul && set /a FOUND_KEYS+=1
findstr "U_Baton" temp_res.txt >nul && set /a FOUND_KEYS+=1

if %FOUND_KEYS% EQU 5 (
    echo [OK] (Widzi 5/5 kluczy)
) else (
    echo [FAIL] (Widzi %FOUND_KEYS%/5)
    type temp_res.txt
)
del temp_res.txt

:: 2. Deep Routing UDP
call :run_test UDP "GET VALUE U_Auto" "OK 15" "Deep Routing UDP (Przez cale drzewo)"

:: 3. Deep Routing TCP (Nowy serwer)
call :run_test TCP "GET VALUE T_Klocki" "OK 500" "Deep Routing TCP (Nowy serwer)"

:: 4. Zapis Krzyzowy (SET)
echo|set /p="Test: SET (Klient UDP modyfikuje Serwer TCP przez drzewo)... "
java -cp out UDPClient -address localhost -port 8000 -command SET T_Mis 777 >nul 2>&1
echo [Wyslano]

:: 5. Weryfikacja spojnosci (GET po SET)
call :run_test TCP "GET VALUE T_Mis" "OK 777" "Weryfikacja: TCP odczytuje zmiane"

:: 6. Obsluga bledow
call :run_test TCP "GET VALUE NieMa" "NA" "Obsluga bledow (nieistniejacy klucz)"

echo.
echo ==========================================================
echo        GOTOWE. Nocisnij ENTER aby zamknac.
echo ==========================================================
pause >nul

:: SPRZATANIE
echo Sprzatanie...
taskkill /F /IM java.exe >nul 2>&1
if exist temp_res.txt del temp_res.txt
exit /b

:: --- FUNKCJA TESTUJACA (STABILNA) ---
:run_test
set TYPE=%1
set CMD=%~2
set EXPECTED=%~3
set DESC=%~4

echo|set /p="Test: %DESC%... "

if "%TYPE%"=="TCP" (
    java -cp out TCPClient -address localhost -port 8000 -command %CMD% > temp_res.txt
) else (
    java -cp out UDPClient -address localhost -port 8000 -command %CMD% > temp_res.txt
)

:: PANCERNE SPRAWDZANIE WYNIKU
findstr /C:"%EXPECTED%" temp_res.txt >nul
if %ERRORLEVEL% EQU 0 goto :test_pass

:test_fail
echo [FAIL] (Oczekiwano: '%EXPECTED%')
echo      Otrzymano:
findstr /V "Creating Socket Sending Waiting" temp_res.txt
del temp_res.txt
exit /b 0

:test_pass
echo [OK] (%EXPECTED%)
del temp_res.txt
exit /b 0


