PISSIR – Distributed Breakout Platform (Erasmus Practice)
Good! This is the practice for the Distributed Systems subject during Erasmus. It is basically a clone of Breakout (the old Arkanoid) but built with a distributed microservices architecture. It allows you to play in local mode (Edge) and, if there is a connection, it connects to a cloud (MySQL) to synchronize scores automatically.

📝 What is the project about
The main idea is to have a Breakout that implements Edge capability. That is, the game does not hang if the central database goes down. The system allows:
✅ Offline Mode: Play independently locally without the need to be connected to the server.
✅ Automatic synchronization: When MySQL comes back to life, it uploads the scores you had saved locally.
✅ Multi-process / Multi-instance: You can open 3 or 4 games at the same time on the same computer and they do not overlap.
✅ REST API: In case you want to control the racket by sending cURL or from Postman instead of using the keyboard.
✅ Swing GUI: Pretty classic.
✅ Web Ranking: A simple HTML/JS page to see who has the best score.
✅ User Login: Basic system to register and log in before playing.

🌟 Features requested by the guide
1. Edge Mode (Offline-First) If the Cloud MySQL server goes down or is unavailable, the app continues to work. The games are saved in a local JSON file called edge_data_<uuid>.json. So that two instances on the same PC do not break the JSON, each instance generates its own file with a random UUID.
2. The Synchronizer (SyncService) There is a secondary thread with a @Scheduled that does a "ping" every 2 seconds to see if there are things pending in the local JSON to upload to MySQL. Transparent management: if it goes from offline to online, it dumps everything to the DB at once.
3. Game Physics in Dedicated Thread (BallThread) So that the REST API does not freeze while the ball moves, the simulation runs in an independent Java thread (Runnable). The game loop does a Thread.sleep(50) (about 20 ticks per second). It handles basic collisions (blocks, racket and walls) and drops some random power-ups that we have programmed.
4. Complete REST API The entire game can be controlled by making HTTP requests.
5. Difficulties Three levels hardcoded in the properties file: Easy: slow ball, huge racket, blocks of the same life. Normal: the standard game. Hard: the ball goes faster, small racket and the blocks have more lives.

⚙️ How the logic works inside
1️⃣ Avoiding port collision (Multi-instance)
If you want to open more than one game at the same time, port 8080 would crash. To solve this, we check if the port is busy. If it is, we tell Spring to take port 0 (which causes the operating system to give it a random free port).

2️⃣ The Game Loop (Multi-threading)
When you create a game by sending a POST /api/games/start:
We instantiate the coordinates of everything (Ball, Paddle, Blocks).
We start the BallThread thread.
We return a hash of type A1B2C3D4 (the gameId) so that the client knows who to control.

3️⃣ Authentication

⚠️ IMPORTANT (SECURITY NOTICE): Passwords are stored in plain text in the database. I know it's a bad practice (don't use your real passwords!!), but since it's an academic project and we were short on time, we saved ourselves the trouble of using BCrypt and Spring Security to avoid making life complicated with session tokens.
The user model includes a list of parameters such as username, a profile image formatted in Base64 and a boolean loggedIn to control that there are no two people with the same account at the same time.

📦 How to make it work (Installation and Execution)
System requirements, You should have installed:JDK 17 (make sure you have JAVA_HOME in the environment variables). Maven (mvn in the terminal). A MySQL server running on port 3306 (optional, if it's not there, the app will automatically enter local mode).
Step 1: Enter the project folder Open the Git or Windows terminal and go to the path where you downloaded the practice: Bashcd c:\Users\Usuari\Desktop\...\PractErasmus
Step 2: From VisualStudio or any code runner, run Breakout Application.java as many times as you want for each separate terminal/start.



CATALÀ:

PISSIR – Plataforma Breakout Distribuïda (Pràctica de Erasmus)
Bones! Això és la pràctica per l'assignatura de Sistemes Distribuïts durant l'Erasmus. És bàsicament un clon del Breakout (l'Arcanoid de tota la vida) però muntat amb una arquitectura de microserveis distribuïts. Permet jugar en mode local (Edge) i, si hi ha connexió, es connecta a un cloud (MySQL) per sincronitzar les puntuacions de forma automàtica.

📝 De què va el projecte
La idea principal és tenir un Breakout que implementi capacitat Edge. És a dir, que el joc no es quedi penjat si cau la base de dades central. El sistema permet:
✅ Mode Offline: Jugar de manera independent en local sense necessitat d'estar connectat al servidor.
✅ Sincronització automàtica: Quan el MySQL torna a viure, puja els scores que tenies guardats en local.
✅ Multiprocés / Multi-instància: Pots obrir 3 o 4 jocs alhora al mateix ordinador i no es trepitgen.
✅ API REST: Per si vols controlar la raqueta enviant cURL o des de Postman en comptes d'usar el teclat.
✅ Interfície Gràfica en Swing: Bastant clàssica.
✅ Rànquing Web: Una pàgina senzilla en HTML/JS per veure qui te millor puntuació.
✅ Login d'usuaris: Sistema bàsic per registrar-se i fer el login abans de jugar.

🌟 Funcionalitats que demanava la guia
1. Mode Edge (Offline-First) Si el servidor de MySQL de Cloud cau o no està disponible, l'app segueix funcionant. Les partides se salven en un fitxer JSON local anomenat edge_data_<uuid>.json. Perquè dues instàncies al mateix PC no es reventin el JSON, cada instància genera el seu propi fitxer amb un UUID aleatori.
2. El Sincronitzador (SyncService) Hi ha un fil secundari amb un @Scheduled que va fent un "ping" cada 2 segons per veure si hi ha coses pendents al JSON local per pujar al MySQL. Gestió transparent: si passa de offline a online, ho buida tot cap a la BD de cop.
3. Física del Joc en Fil Dedicat (BallThread) Perquè l'API REST no es quedi congelada mentre es mou la pilota, la simulació va en un fil de Java independent (Runnable). El game loop fa un Thread.sleep(50) (uns 20 ticks per segon). Gestiona col·lisions bàsiques (blocs, raqueta i parets) i fa caure uns power-ups aleatoris que hem programat.
4. API REST CompletaEs pot controlar tota la partida fent peticions HTTP.
5. Dificultats Tres nivells hardcoded al fitxer de propietats: Easy: pilota lenta, raqueta enorme, blocs de una mateixa vida. Normal: el joc estàndard. Hard: la pilota va més ràpida, raqueta petita i els blocs tenen mes vides.



⚙️ Com funciona la lògica per dins
1️⃣ Evitar el xoc de ports (Multi-instància)
Si vols obrir més d'un joc alhora, el port 8080 es col·lapsaria. Per resoldre-ho, comprovem si el port està ocupat. Si ho està, li diem a Spring que agafi el port 0 (que fa que el sistema operatiu li doni un port lliure a l'atzar).


2️⃣ El Game Loop (Multi-threading)
Quan crees una partida enviant un POST /api/games/start:
Instanciem les coordenades de tot (Ball, Paddle, Blocks).
Fiquem a córrer el fil del BallThread.
Retornem un hash tipus A1B2C3D4 (el gameId) perquè el client sàpiga a qui ha de controlar.


3️⃣ Autenticació

⚠️ IMPORTANT (AVÍS DE SEGURETAT): Les contrasenyes es guarden en text pla a la base de dades. Se que és una pràctica nefasta (no useu contrasenyes reals vostre!!), però com que és un projecte acadèmic i anàvem justos de temps, ens hem estalviat posar el BCrypt i el Spring Security per no complicar-nos la vida amb els tokens de sessió.
El model d'usuari inclou una llista de paràmetres com el username, una imatge de perfil formatejada en Base64 i un booleà loggedIn per controlar que no hi hagi dues persones amb el mateix compte alhora.



📦 Com fer-ho funcionar (Instal·lació i Execució)
Requisits del sistema, Hauries de tenir instal·lat:JDK 17 (assegura't de tenir el JAVA_HOME a les variables d'entorn). Maven (mvn a la terminal).Un servidor MySQL rulant al port 3306 (opcional, si no hi és, l'app entrarà en mode local automàticament).
Pas 1: Entrar a la carpeta del projecteObre la terminal de Git o de Windows i vés a la ruta on hagis descarregat la pràctica:Bashcd c:\Users\Usuari\Desktop\...\PractErasmus
Pas 2: Desde el VisualStudio o qualsevol executor de codi, executar Breakout Application.java les vegades que es vulgui per cada terminal/Partida separada.