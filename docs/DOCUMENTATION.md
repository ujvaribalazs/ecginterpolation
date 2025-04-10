Project Documentation

Configuration Files

.vscode/launch.json: VS Code run configurations

.vscode/settings.json: Project-specific settings

pom.xml: Maven configuration file

Source Structure

## Projekt struktúra

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

Resources

resources/xml/: Contains ECG data files

1. Main Components

ECGMenuApp

JavaFX main menu from which the following can be launched:

ECG plotting application

XML analysis tool

EcgPlotterApplication

Main visualization component:

Loads signals from XML (e.g., ecg1.xml)

Registers and applies filters

GUI components:

SignalCanvas (signal rendering)

FilterControlPanel

NavigationPanel

FilterVisibilityPanel

StatusPanel

2. Data Model

SignalData.java

Stores original and filtered signals

Manages viewport (zoom, pan)

Calculates min/max values

3. Filters

FilterInterface

Defines core functionality: filter(), getName(), getParameters()

Adapter Classes

SgFilter, LoessFilter, SplineFilter

These use implementations from the ecgprocessor package

Segmented Filters

SegmentedFilterAdapter: applies any base filter in a segmented manner

4. Segmentation

ECGSegmenter

Detects R-peaks (using windowed method)

Splits signal into segments

Applies filter to each segment

Ensures smooth linear transitions between segments

5. Visualization

SignalCanvas

Draws original and filtered signals

Marks R-peaks

Provides zoom and navigation

ViewController

Manages interactive elements and views