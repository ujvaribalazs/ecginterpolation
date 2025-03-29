DOKUMENTÁCIÓ

1. Fő komponensek

ECGMenuApp

JavaFX főmenü, amelyből elindítható:

Az EKG megjelenítő alkalmazás

Az XML elemző segédeszköz

EcgPlotterApplication

Fő megjelenítő komponens

Jel betöltése XML-ből (pl. ecg1.xml)

Szűrők regisztrálása és alkalmazása

GUI elemek:

SignalCanvas (jel kirajzolása)

FilterControlPanel

NavigationPanel

FilterVisibilityPanel

StatusPanel

2. Adatmodell

SignalData.java

Az eredeti és szűrt jelek tárolása

Viewport kezelés (zoom, mozgatás)

Min/max értékek kalkulálása

3. Szűrők

FilterInterface

Alapműködés meghatározása: filter(), getName(), getParameters()

Adapterek

SgFilter, LoessFilter, SplineFilter

Ezek az ecgprocessor csomagban lévő implementációkat használják

Szegmentált szűrők

SegmentedFilterAdapter: bármilyen szűrőt szegmentáltan alkalmaz

4. Szegmentálás

ECGSegmenter

R-csúcspont detekció (ablakos módszerrel)

Szakaszokra bontás

Szűrő alkalmazás minden szakaszra

Lineáris átmenetek biztosítása

5. Megjelenítés

SignalCanvas

Eredeti és szűrt jelek rajzolása

R csúcspontok jelölése

Zoom, navigáció

ViewController

Interaktív elemek és nézetek kezelése