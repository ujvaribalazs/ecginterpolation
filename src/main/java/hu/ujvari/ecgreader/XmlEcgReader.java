package hu.ujvari.ecgreader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import hu.ujvari.ecgmodel.Signal;


public class XmlEcgReader {

    private Document xmlDocument;
    private double[] Signal;
    private List<Signal> signals = new ArrayList<>();


    public void loadXmlFile(String filePath) {
    try (InputStream is = new FileInputStream(new File(filePath))) {
        loadFromInputStream(is);
    } catch (IOException e) {
        System.err.println("Fájl nem olvasható: " + e.getMessage());
        this.xmlDocument = null;
    }
}
    public void loadFromInputStream(InputStream is) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            this.xmlDocument = dBuilder.parse(is);
            this.xmlDocument.getDocumentElement().normalize();
            System.out.println("XML beolvasva InputStream-ből");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("Hiba az XML beolvasásakor: " + e.getMessage());
            this.xmlDocument = null;
        }
    }

    public void loadFromResource(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Nem található az erőforrás: " + resourcePath);
                return;
            }
            loadFromInputStream(is);
        } catch (IOException e) {
            System.err.println("Hiba a resource beolvasásakor: " + e.getMessage());
        }
    }
    
    
    public void printXmlStructure() {
        if (this.xmlDocument == null) {
            System.out.println("Nincs betöltve XML dokumentum.");
            return;
        }
    
        Element root = this.xmlDocument.getDocumentElement();
        System.out.println("<" + root.getNodeName() + ">");
        printElementStructure(root, 1, false);  // majd ezt írjuk meg később
    }
    
    private void printElementStructure(Element element, int depth, boolean printAttributes) {
        String indent = "  ".repeat(depth);
        System.out.print(indent + "<" + element.getNodeName());
    
        if (printAttributes && element.hasAttributes()) {
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                String attrName = element.getAttributes().item(i).getNodeName();
                String attrValue = element.getAttributes().item(i).getNodeValue();
                System.out.print(" " + attrName + "=\"" + attrValue + "\"");
            }
        }
    
        System.out.println(">");
    
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                printElementStructure((Element) child, depth + 1, printAttributes);
            }
        }
    }

    private List<Element> findLeadComponents() {
        List<Element> leadComponents = new ArrayList<>();
        NodeList components = xmlDocument.getElementsByTagName("component");
    
        for (int i = 0; i < components.getLength(); i++) {
            Node node = components.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
    
            Element component = (Element) node;
            NodeList codeList = component.getElementsByTagName("code");
    
            if (codeList.getLength() > 0) {
                Element codeElement = (Element) codeList.item(0);
                String code = codeElement.getAttribute("code");
    
                if (code != null && code.startsWith("MDC_ECG_LEAD_")) {
                    leadComponents.add(component);
                }
            }
        }
    
        return leadComponents;
    }
    
    public void printAvailableLeads() {
        if (this.xmlDocument == null) {
            System.out.println("Nincs betöltve XML dokumentum.");
            return;
        }
    
        Set<String> leads = new TreeSet<>();  // duplikáció nélkül, rendezve
    
        for (Element component : findLeadComponents()) {
            NodeList codeList = component.getElementsByTagName("code");
            if (codeList.getLength() > 0) {
                Element codeElement = (Element) codeList.item(0);
                String codeValue = codeElement.getAttribute("code");
                if (codeValue != null) {
                    leads.add(codeValue);
                }
            }
        }
    
        System.out.println("Azonosított elvezetések az XML-ben:");
        for (String lead : leads) {
            System.out.println("  - " + lead);
        }
    }
    

    public void extractSignals() {
        if (this.xmlDocument == null) {
            System.out.println("Nincs betöltve XML dokumentum.");
            return;
        }
    
        this.signals.clear(); // előző jelek törlése
    
        List<Element> leadComponents = findLeadComponents();
    
        for (Element component : leadComponents) {
            // 1. <code> elem a lead azonosításához
            NodeList codeList = component.getElementsByTagName("code");
            if (codeList.getLength() == 0) continue;
            String leadName = ((Element) codeList.item(0)).getAttribute("code");
    
            // 2. <value> elem megkeresése
            Element valueElement = (Element) component.getElementsByTagName("value").item(0);
            if (valueElement == null) continue;
    
            // 3. <origin>, <scale>, <digits> elemek
            Element originElement = (Element) valueElement.getElementsByTagName("origin").item(0);
            Element scaleElement = (Element) valueElement.getElementsByTagName("scale").item(0);
            Element digitsElement = (Element) valueElement.getElementsByTagName("digits").item(0);
    
            if (originElement == null || scaleElement == null || digitsElement == null) continue;
    
            // 4. Értékek kinyerése
            double originVal = Double.parseDouble(originElement.getAttribute("value"));
            String originUnit = originElement.getAttribute("unit");
    
            double scaleVal = Double.parseDouble(scaleElement.getAttribute("value"));
            String scaleUnit = scaleElement.getAttribute("unit");
    
            // 5. digits szövegből számok listává alakítása
            String digitsText = digitsElement.getTextContent().trim();
            List<Double> values = new ArrayList<>();
            for (String token : digitsText.split("\\s+")) {
                try {
                    values.add(Double.parseDouble(token));
                } catch (NumberFormatException ignored) {}
            }
    
            // 6. Signal példány létrehozása és eltárolása
            Signal signal = new Signal(leadName, values, originVal, originUnit, scaleVal, scaleUnit);
            this.signals.add(signal);
        }
    
        System.out.println("Beolvasott elvezetések száma: " + signals.size());
    }

    public List<Signal> getSignals() {
        return signals;
    }
    
    

    
}
