@echo off
setlocal EnableDelayedExpansion

:: === KONFIGURACJA ===
color 07
cls

echo ==========================================================
echo       SKJ PROJEKT - FINALNY TEST (500 PKT) [WINDOWS]
echo ==========================================================
echo.

:: 1. PRZYGOTOWANIE
echo [0] Kompilacja i start srodowiska...

:: Zabijamy stare procesy
taskkill /F /IM java.exe >nul 2>&1

:: Czyszczenie
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
echo [OK] Kompilacja zakonczona.
echo.

:: 2. START SERWEROW
echo [INFO] --- KONFIGURACJA WEZLOW KONCOWYCH ---
echo 1. TCP : 9011 (T_Lalka = 25)
echo 2. TCP : 9012 (T_Mis = 1013)
echo 3. TCP : 9013 (T_Klocki = 500)
echo 4. UDP : 9014 (U_Auto = 15)
echo 5. UDP : 9015 (U_Baton = 65)

start /B cmd /c "java -cp out TCPServer -port 9011 -key T_Lalka -value 25 > logs\t1.log 2>&1"
start /B cmd /c "java -cp out TCPServer -port 9012 -key T_Mis -value 1013 > logs\t2.log 2>&1"
start /B cmd /c "java -cp out TCPServer -port 9013 -key T_Klocki -value 500 > logs\t3.log 2>&1"
start /B cmd /c "java -cp out UDPServer -port 9014 -key U_Auto -value 15 > logs\u1.log 2>&1"
start /B cmd /c "java -cp out UDPServer -port 9015 -key U_Baton -value 65 > logs\u2.log 2>&1"

timeout /t 2 /nobreak >nul

:: 3. START PROXY (DRZEWO)
echo.
echo [INFO] --- BUDOWA STRUKTURY DRZEWIASTEJ ---
echo (Middle :8001) - Laczy sie z 5 serwerami...
start /B cmd /c "java -cp out Proxy -port 8001 -server localhost 9011 -server localhost 9012 -server localhost 9013 -server localhost 9014 -server localhost 9015 > logs\mid.log 2>&1"
timeout /t 2 /nobreak >nul

echo (Root :8000)   - Laczy sie TYLKO z Middle...
start /B cmd /c "java -cp out Proxy -port 8000 -server localhost 8001 > logs\root.log 2>&1"
timeout /t 3 /nobreak >nul
echo.

:: === TESTY ===

echo [200 PKT] JEDEN PROTOKOL
call :run_test TCP GET VALUE T_Lalka "OK 25" "Sciezka: Klient TCP -> Root -> Middle -> Serwer TCP"
call :run_test UDP GET VALUE U_Auto "OK 15" "Sciezka: Klient UDP -> Root -> Middle -> Serwer UDP"
echo.

echo [400 PKT] TLUMACZENIE PROTOKOLOW
call :run_test TCP GET VALUE U_Baton "OK 65" "Translacja: Klient TCP -> [Proxy] -> Serwer UDP"
call :run_test UDP GET VALUE T_Mis "OK 1013" "Translacja: Klient UDP -> [Proxy] -> Serwer TCP"
echo.

echo [500 PKT] INTELIGENCJA SIECI PROXY-PROXY

:: Test Discovery
echo|set /p="Test: Agregacja wiedzy (Root pyta o klucze) "
java -cp out TCPClient -address localhost -port 8000 -command GET NAMES > temp_res.txt

:: Liczymy klucze przeszukujac caly plik
set COUNT=0
findstr "T_Lalka" temp_res.txt >nul && set /a COUNT+=1
findstr "T_Mis" temp_res.txt >nul && set /a COUNT+=1
findstr "T_Klocki" temp_res.txt >nul && set /a COUNT+=1
findstr "U_Auto" temp_res.txt >nul && set /a COUNT+=1
findstr "U_Baton" temp_res.txt >nul && set /a COUNT+=1

if %COUNT% EQU 5 (
    echo [OK] (Root widzi 5/5 kluczy z glebi sieci)
) else (
    echo [FAIL] (Widzi %COUNT%/5)
    type temp_res.txt
)
del temp_res.txt

:: Deep Routing
call :run_test TCP GET VALUE T_Klocki "OK 500" "Routing: Klient TCP -> Root -> Middle -> Nowy Serwer TCP"

:: Zapis Krzyzowy
echo|set /p="Test: SET (Klient UDP modyfikuje Serwer TCP przez drzewo) "
java -cp out UDPClient -address localhost -port 8000 -command SET T_Mis 777 >nul 2>&1
echo [Wyslano]

:: Weryfikacja
call :run_test TCP GET VALUE T_Mis "OK 777" "Weryfikacja: Klient TCP odczytuje zmiane (Spojnosc Danych)"

:: Error Handling
call :run_test TCP GET VALUE NieMa "NA" "Obsluga Bledow: Zapytanie o nieistniejacy klucz"

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

:: --- FUNKCJA TESTUJACA (POPRAWIONA DLA WINDOWS) ---
:run_test
set TYPE=%1
set CMD1=%2
set CMD2=%3
set KEY=%4
set EXPECT=%~5
set DESC=%~6

echo|set /p="Test: %DESC% "

if "%TYPE%"=="TCP" (
    java -cp out TCPClient -address localhost -port 8000 -command %CMD1% %CMD2% %KEY% > temp_res.txt
) else (
    java -cp out UDPClient -address localhost -port 8000 -command %CMD1% %CMD2% %KEY% > temp_res.txt
)

:: Zamiast czytac pierwsza linie, przeszukujemy plik w poszukiwaniu oczekiwanego stringa
findstr /C:"%EXPECT%" temp_res.txt >nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] (%EXPECT%)
) else (
    echo [FAIL] (Oczekiwano: '%EXPECT%')
    :: Wyswietlamy cala zawartosc pliku zeby zobaczyc co poszlo nie tak (bez 'Creating socket...')
    type temp_res.txt | findstr /V "Creating Socket Sending Waiting"
)
del temp_res.txt
exit /b 0