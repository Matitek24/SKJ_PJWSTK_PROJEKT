#!/bin/bash


G='\033[1;32m'; R='\033[0;31m'; Y='\033[1;33m'; C='\033[1;36m'; NC='\033[0m'

cleanup() { kill $(jobs -p) 2>/dev/null; echo -e "\n${G}System zatrzymany.${NC}"; }
trap cleanup EXIT

clear
echo -e "${Y}>>> SKJ PROJEKT <<<${NC}\n"


echo -ne "Kompilacja... "
pkill -f "java.*(Proxy|Server)" 2>/dev/null
rm -rf out logs; mkdir -p out logs
find src -name "*.java" > s.txt; javac -d out @s.txt; rm s.txt
[ $? -eq 0 ] && echo -e "${G}OK${NC}" || { echo -e "${R}BŁĄD${NC}"; exit 1; }


echo -e "\n${C}--- KONFIGURACJA WĘZŁÓW KOŃCOWYCH ---${NC}"
echo "1. TCP : 9011 (T_Lalka = 25)"
echo "2. TCP : 9012 (T_Mis = 1013)"
echo "3. TCP : 9013 (T_Klocki = 500)"
echo "4. UDP : 9014 (U_Auto = 15)"
echo "5. UDP : 9015 (U_Baton = 65)"

java -cp out TCPServer -port 9011 -key T_Lalka  -value 25   > logs/t1.log 2>&1 &
java -cp out TCPServer -port 9012 -key T_Mis    -value 1013 > logs/t2.log 2>&1 &
java -cp out TCPServer -port 9013 -key T_Klocki -value 500  > logs/t3.log 2>&1 &
java -cp out UDPServer -port 9014 -key U_Auto   -value 15   > logs/u1.log 2>&1 &
java -cp out UDPServer -port 9015 -key U_Baton  -value 65   > logs/u2.log 2>&1 &
sleep 1


echo -e "\n${C}--- BUDOWA STRUKTURY DRZEWIASTEJ ---${NC}"
echo "(Middle :8001) -> Łączy się z 5 serwerami"
java -cp out Proxy -port 8001 \
    -server localhost 9011 -server localhost 9012 -server localhost 9013 \
    -server localhost 9014 -server localhost 9015 \
    > logs/mid.log 2>&1 &
sleep 2

echo "(Root :8000)   -> Łączy się TYLKO z Middle"
java -cp out Proxy -port 8000 -server localhost 8001 > logs/root.log 2>&1 &
sleep 3


run() {

    echo -ne "Test: $6 "

    if [ "$1" == "TCP" ]; then
        RES=$(java -cp out TCPClient -address localhost -port 8000 -command $2 $3 $4 | tr -d '\r')
    else
        RES=$(java -cp out UDPClient -address localhost -port 8000 -command $2 $3 $4 | tr -d '\r')
    fi

    if [[ "$RES" == *"$5"* ]]; then
        echo -e "${G}OK${NC} ($5)"
    else
        L=$(echo "$RES" | tail -n 1)
        echo -e "${R}FAIL${NC} (Oczekiwano: '$5', Jest: '$L')"
    fi
}

echo ""


echo -e "${C}[200 PKT] JEDEN PROTOKÓŁ ${NC}"
run TCP GET VALUE T_Lalka "OK 25" "Ścieżka: Klient TCP -> Root -> Middle -> Serwer TCP"
run UDP GET VALUE U_Auto  "OK 15" "Ścieżka: Klient UDP -> Root -> Middle -> Serwer UDP"

echo -e "\n${C}[400 PKT] TŁUMACZENIE PROTOKOŁÓW ${NC}"
run TCP GET VALUE U_Baton "OK 65"   "Translacja: Klient TCP -> [Proxy] -> Serwer UDP"
run UDP GET VALUE T_Mis   "OK 1013" "Translacja: Klient UDP -> [Proxy] -> Serwer TCP"

echo -e "\n${C}[500 PKT] 'INTELIGENCJA' SIECI PROXY-PROXY ${NC}"

# Discovery
echo -ne "Test: Agregacja wiedzy (Root pyta o klucze) "
N=$(java -cp out TCPClient -address localhost -port 8000 -command GET NAMES)
cnt=0; for k in T_Lalka T_Mis T_Klocki U_Auto U_Baton; do [[ "$N" == *"$k"* ]] && ((cnt++)); done
[ $cnt -eq 5 ] && echo -e "${G}OK${NC} (Root widzi 5/5 kluczy z głębi sieci)" || echo -e "${R}FAIL${NC} (Widzi $cnt/5)"

# Deep Routing (Nowy serwer)
run TCP GET VALUE T_Klocki "OK 500" "Routing: Klient TCP -> Root -> Middle -> Nowy Serwer TCP"

# Zapis Krzyżowy
echo -ne "Test: SET (Klient UDP modyfikuje Serwer TCP przez drzewo) "
java -cp out UDPClient -address localhost -port 8000 -command SET T_Mis 777 >/dev/null
echo -e "${G}Wysłano${NC}"

run TCP GET VALUE T_Mis "OK 777" "Weryfikacja: Klient TCP odczytuje zmianę (Spójność Danych)"

# Error Handling
run TCP GET VALUE NieMa "NA" "Obsługa Błędów: Zapytanie o nieistniejący klucz"

echo -e "\n${Y}Gotowe naciśnij Enter.${NC}"
read