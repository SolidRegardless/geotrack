package com.geotrack.simulator.route;

import jakarta.enterprise.context.ApplicationScoped;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses GPX 1.1 files into an ordered list of {@link RoutePoint}s.
 * Extracts track points ({@code <trkpt>}) with lat/lon/ele/time.
 */
@ApplicationScoped
public class GpxParser {

    public List<RoutePoint> parse(InputStream input) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            // Disable XXE (XML External Entity) processing — OWASP recommendation
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
            factory.setNamespaceAware(false);
            Document doc = factory.newDocumentBuilder().parse(input);

            NodeList trkpts = doc.getElementsByTagName("trkpt");
            List<RoutePoint> points = new ArrayList<>(trkpts.getLength());

            for (int i = 0; i < trkpts.getLength(); i++) {
                Element pt = (Element) trkpts.item(i);
                double lat = Double.parseDouble(pt.getAttribute("lat"));
                double lon = Double.parseDouble(pt.getAttribute("lon"));

                double elevation = extractDouble(pt, "ele", 0.0);

                Instant time = Instant.now();
                NodeList timeNodes = pt.getElementsByTagName("time");
                if (timeNodes.getLength() > 0) {
                    time = Instant.parse(timeNodes.item(0).getTextContent().trim());
                }

                points.add(new RoutePoint(lat, lon, elevation, time));
            }

            return points;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse GPX: " + e.getMessage(), e);
        }
    }

    private double extractDouble(Element parent, String tagName, double defaultValue) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return Double.parseDouble(nodes.item(0).getTextContent().trim());
        }
        return defaultValue;
    }
}
