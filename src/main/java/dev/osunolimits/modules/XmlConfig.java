package dev.osunolimits.modules;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlConfig {
    private static final String FILE_PATH = "data/local-storage.xml";
    private static XmlConfig instance;
    private final File configFile;
    private Document document;

    private XmlConfig() {
        this.configFile = new File(FILE_PATH);
        load();
    }

    public static XmlConfig getInstance() {
        if (instance == null) {
            instance = new XmlConfig();
        }
        return instance;
    }

    private void load() {
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs(); // Ensure .data directory exists
                configFile.createNewFile();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.newDocument();
                Element root = document.createElement("config");
                document.appendChild(root);
                save();
            } else {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(configFile);
                document.getDocumentElement().normalize();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load XML configuration", e);
        }
    }

    public String getOrDefault(String key, String defaultValue) {
        Element root = document.getDocumentElement();
        NodeList nodes = root.getElementsByTagName(key);

        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        } else {
            set(key, defaultValue);
            return defaultValue;
        }
    }

    public HashMap<String, Object> getAll() {
        HashMap<String, Object> keyValues = new HashMap<>();
        Element root = document.getDocumentElement();
        NodeList nodes = root.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                keyValues.put(node.getNodeName(), node.getTextContent());
            }
        }
        return keyValues;
    }

    public void set(String key, String value) {
        Element root = document.getDocumentElement();
        NodeList nodes = root.getElementsByTagName(key);

        if (nodes.getLength() > 0) {
            nodes.item(0).setTextContent(value);
        } else {
            Element newElement = document.createElement(key);
            newElement.setTextContent(value);
            root.appendChild(newElement);
        }

        save();
    }

    private void save() {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(configFile);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new RuntimeException("Failed to save XML configuration", e);
        }
    }
}
