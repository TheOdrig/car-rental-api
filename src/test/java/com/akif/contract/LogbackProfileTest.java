package com.akif.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Logback profiles")
class LogbackProfileTest {

    private static final Path CONFIG = Path.of("src", "main", "resources", "logback-spring.xml");
    private static final Set<String> VERBOSE = Set.of("DEBUG", "TRACE", "ALL");

    @Test
    @DisplayName("DEBUG and TRACE levels only apply to the default, dev and test profiles")
    void verboseLevelsStayOutOfProd() throws Exception {
        Document document = parse();
        List<String> findings = new ArrayList<>();
        for (String tag : List.of("logger", "root")) {
            NodeList nodes = document.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String level = element.getAttribute("level").toUpperCase();
                if (VERBOSE.contains(level) && !insideNonProdProfile(element)) {
                    findings.add(tag + " " + element.getAttribute("name") + " " + level);
                }
            }
        }

        assertThat(findings).as("verbose loggers must sit inside a non-prod springProfile").isEmpty();
    }

    @Test
    @DisplayName("the prod profile sets the root logger and the application packages to INFO")
    void prodProfileIsInfo() throws Exception {
        Document document = parse();
        NodeList profiles = document.getElementsByTagName("springProfile");
        boolean foundProd = false;
        for (int i = 0; i < profiles.getLength(); i++) {
            Element profile = (Element) profiles.item(i);
            if (profile.getAttribute("name").equals("prod")) {
                foundProd = true;
                NodeList roots = profile.getElementsByTagName("root");
                assertThat(roots.getLength()).isEqualTo(1);
                assertThat(((Element) roots.item(0)).getAttribute("level")).isEqualTo("INFO");
                NodeList loggers = profile.getElementsByTagName("logger");
                for (int j = 0; j < loggers.getLength(); j++) {
                    Element logger = (Element) loggers.item(j);
                    if (logger.getAttribute("name").equals("com.akif")) {
                        assertThat(logger.getAttribute("level")).isEqualTo("INFO");
                    }
                }
            }
        }
        assertThat(foundProd).isTrue();
    }

    private static boolean insideNonProdProfile(Element element) {
        Node parent = element.getParentNode();
        while (parent instanceof Element parentElement) {
            if (parentElement.getTagName().equals("springProfile")) {
                List<String> names = List.of(parentElement.getAttribute("name").split("\\s*,\\s*"));
                return !names.contains("prod") && !names.contains("!dev") && !names.contains("!test");
            }
            parent = parentElement.getParentNode();
        }
        return false;
    }

    private static Document parse() throws IOException, javax.xml.parsers.ParserConfigurationException,
            org.xml.sax.SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(Files.newInputStream(CONFIG));
    }
}
