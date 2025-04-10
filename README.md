ECG Interpolation

Leírás

Ez a JavaFX-alapú alkalmazás EKG jelek interaktív megjelenítését és szűrését teszi lehetővé különböző algoritmusok segítségével. Támogatja az R csúcspontok alapján történő szegmentált szűrést is.

Fő funkciók

EKG jelek beolvasása XML fájlokból

JavaFX alapú grafikus megjelenítés

Szűrési algoritmusok:

Gaussian Moving Average

Savitzky-Golay

LOESS

Cubic Spline

Wavelet (kísérleti)

Szegmentált szűrés R-csúcspontok körül

Interaktív felhasználói felület

Telepítés

Java 17+ és Maven 3.6+ telepítése

Projekt klónozása: git clone https://github.com/ujvaribalazs/ecginterpolation.git

Projekt buildelése: mvn clean install

Futtatás: mvn javafx:run

Projekt felépítése

```text
ecginterpolation/
├── pom.xml
├── src/
│   ├── main/java/hu/ujvari/...
│   ├── main/resources/xml/ (teszt EKG fájlok)
│   └── test/java/...
├── .vscode/ (VSCode konfiguráció)
└── .gitignore
```


Főosztály

Az alkalmazás belépési pontja: hu.ujvari.ECGMenuApp

