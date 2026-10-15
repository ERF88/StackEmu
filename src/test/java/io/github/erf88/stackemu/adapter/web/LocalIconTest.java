package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class LocalIconTest {
  @Test
  void selectedIconsAreDecorativeLocalSvgWithoutIdsOrExecutableContent() throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    for (String name :
        new String[] {
          "dashboard",
          "connection",
          "buckets",
          "components",
          "sun",
          "moon",
          "system",
          "language",
          "refresh",
          "upload",
          "download",
          "folder",
          "file",
          "success",
          "error",
          "warning",
          "info",
          "chevron",
          "add",
          "shield"
        }) {
      try (var source =
          LocalIconTest.class.getResourceAsStream("/templates/icons/" + name + ".html")) {
        assertNotNull(source, name);
        var document = factory.newDocumentBuilder().parse(source);
        var root = document.getDocumentElement();
        assertEquals("svg", root.getLocalName(), name);
        assertEquals("http://www.w3.org/2000/svg", root.getNamespaceURI(), name);
        assertEquals("icon", root.getAttribute("class"), name);
        assertEquals("true", root.getAttribute("aria-hidden"), name);
        assertEquals("false", root.getAttribute("focusable"), name);
        var nodes = document.getElementsByTagName("*");
        for (int index = 0; index < nodes.getLength(); index++) {
          var node = nodes.item(index);
          assertFalse(
              Set.of("script", "style", "foreignObject", "image", "use")
                  .contains(node.getLocalName()),
              name);
          var attributes = node.getAttributes();
          for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
            String key = attributes.item(attribute).getNodeName();
            assertFalse(
                key.equals("id")
                    || key.equals("style")
                    || key.endsWith("href")
                    || key.startsWith("on"),
                name + ": " + key);
          }
        }
      }
    }
  }
}
