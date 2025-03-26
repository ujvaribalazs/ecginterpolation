package hu.ujvari.ecgmodel;

import java.util.List;

public class Signal {
    private String leadName;

    private List<Double> values;

    private double originValue;
    private String originUnit;

    private double scaleValue;
    private String scaleUnit;

    // Konstruktor
    public Signal(String leadName, List<Double> values,
                  double originValue, String originUnit,
                  double scaleValue, String scaleUnit) {
        this.leadName = leadName;
        this.values = values;
        this.originValue = originValue;
        this.originUnit = originUnit;
        this.scaleValue = scaleValue;
        this.scaleUnit = scaleUnit;
    }

    // Getters (egyelőre csak olvasáshoz)
    public String getLeadName() {
        return leadName;
    }

    public List<Double> getValues() {
        return values;
    }

    public double getOriginValue() {
        return originValue;
    }

    public String getOriginUnit() {
        return originUnit;
    }

    public double getScaleValue() {
        return scaleValue;
    }

    public String getScaleUnit() {
        return scaleUnit;
    }

    // (Opcionálisan toString, ha szeretnéd később kiírni)
    @Override
    public String toString() {
        return "Signal{" +
                "leadName='" + leadName + '\'' +
                ", values(size)=" + (values != null ? values.size() : 0) +
                ", origin=" + originValue + " " + originUnit +
                ", scale=" + scaleValue + " " + scaleUnit +
                '}';
    }
}
