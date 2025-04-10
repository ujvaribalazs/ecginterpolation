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
│   ├── .gitignore
│   ├── README.md
│   ├── docs
│   │   ├── DOCUMENTATION.md
│   │   ├── DOCUMENTATION_HU.md
│   │   ├── Detailed documentation.pdf
│   │   └── Detailed_doc_hu.pdf
│   ├── pom.xml
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── hu
│   │   │   │       └── ujvari
│   │   │   │           ├── ECGMenuApp.java
│   │   │   │           ├── ecgmodel
│   │   │   │           │   └── Signal.java
│   │   │   │           ├── ecgplotter
│   │   │   │           │   ├── app
│   │   │   │           │   │   └── EcgPlotterApplication.java
│   │   │   │           │   ├── controller
│   │   │   │           │   │   ├── FilterController.java
│   │   │   │           │   │   └── ViewController.java
│   │   │   │           │   ├── filter
│   │   │   │           │   │   ├── FilterInterface.java
│   │   │   │           │   │   ├── GaussianFilter.java
│   │   │   │           │   │   ├── LoessFilter.java
│   │   │   │           │   │   ├── SegmentedFilterAdapter.java
│   │   │   │           │   │   ├── SgFilter.java
│   │   │   │           │   │   ├── SplineFilter.java
│   │   │   │           │   │   └── WaveletFilter.java
│   │   │   │           │   ├── model
│   │   │   │           │   │   ├── FilterParameters.java
│   │   │   │           │   │   └── SignalData.java
│   │   │   │           │   ├── unused
│   │   │   │           │   │   ├── CanvasEcgPlotter.java
│   │   │   │           │   │   ├── EcgPlotter.java
│   │   │   │           │   │   └── FastEcgPlotter.java
│   │   │   │           │   └── view
│   │   │   │           │       ├── FilterControlPanel.java
│   │   │   │           │       ├── FilterVisibilityPanel.java
│   │   │   │           │       ├── NavigationPanel.java
│   │   │   │           │       ├── SignalCanvas.java
│   │   │   │           │       └── StatusPanel.java
│   │   │   │           ├── ecgprocessor
│   │   │   │           │   ├── CubicSplineFilter.java
│   │   │   │           │   ├── ECGSegmenter.java
│   │   │   │           │   ├── GaussianMovingAverage.java
│   │   │   │           │   ├── LoessFilter.java
│   │   │   │           │   ├── SavitzkyGolayFilter.java
│   │   │   │           │   └── WaveletFilter.java
│   │   │   │           └── ecgreader
│   │   │   │               └── XmlEcgReader.java
│   │   │   └── resources
│   │   │       └── xml
│   │   │           ├── ecg1.xml
│   │   │           ├── ecg2.xml
│   │   │           └── ecg3.xml
```


Főosztály

Az alkalmazás belépési pontja: hu.ujvari.ECGMenuApp

