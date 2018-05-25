package com.mopub.mobileads.util;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XmlUtils {
    private XmlUtils() {}

    /**
     * Gets the first direct child of the given node with a node named {@code nodeName}.
     *
     * Only direct children are checked.
     */
    public static Node getFirstMatchingChildNode(final Node node, final String nodeName) {
        return getFirstMatchingChildNode(node, nodeName, null, null);
    }

    /**
     * Gets the first direct child of the given node with a node named {@code nodeName} that has an
     * attribute named {@code attributeName} with a value that matches one of {@code attributeValues}.
     *
     * Only direct children are checked.
     *
     * @param nodeName matching nodes must have this name.
     * @param attributeName matching nodes must have an attribute with this name.
     *                      Use null to match nodes with any attributes.
     * @param attributeValues all matching child nodes' matching attribute will have a value that
     *                        matches one of these values. Use null to match nodes with any attribute
     *                        value.
     */
    public static Node getFirstMatchingChildNode(final Node node, final String nodeName,
            final String attributeName, final List<String> attributeValues) {
        if (node == null || nodeName == null) {
            return null;
        }

        final List<Node> nodes = getMatchingChildNodes(node, nodeName, attributeName, attributeValues);
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }

    /**
     * Return children of the {@code node} parameter with a matching {@code nodeName}.
     *
     * @param node the root node to look beneath.
     * @param nodeName all child nodes will match this element.
     * @return child nodes that match the nodeName
     */
    public static List<Node> getMatchingChildNodes(final Node node, final String nodeName) {
        return getMatchingChildNodes(node, nodeName, null, null);
    }

    /**
     * Return children of the {@code node} parameter with a matching {@code nodeName} &
     * {@code attributeName} that matches at least one of the passed-in {@code attributeValues}.
     * If {@code attributeValues} is empty, no nodes will match. To match names only,
     * pass null for both {@code attributeName} and {@code attributeValues}.
     *
     * @param node the root node to look beneath.
     * @param nodeName all child nodes will match this element.
     * @param attributeName all matching child nodes will have an attribute of this name.
     * @param attributeValues all matching child nodes' matching attribute will have a value that
     *                        matches one of these values.
     * @return child nodes that match all parameters
     */
    public static List<Node> getMatchingChildNodes(final Node node, final String nodeName,
            final String attributeName, final List<String> attributeValues) {
        if (node == null || nodeName == null) {
            return null;
        }

        final List<Node> nodes = new ArrayList<Node>();
        final NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node childNode = nodeList.item(i);
            if (childNode.getNodeName().equals(nodeName)
                    && nodeMatchesAttributeFilter(childNode, attributeName, attributeValues)) {
                nodes.add(childNode);
            }
        }
        return nodes;
    }

    /**
     * Returns {@code true} iff the node has the attribute {@code attributeName} with a value that
     * matches one of {@code attributeValues}.
     */
    public static boolean nodeMatchesAttributeFilter(final Node node, final String attributeName, final List<String> attributeValues) {
        if (attributeName == null || attributeValues == null) {
            return true;
        }

        final NamedNodeMap attrMap = node.getAttributes();
        if (attrMap != null) {
            Node attrNode = attrMap.getNamedItem(attributeName);
            if (attrNode != null && attributeValues.contains(attrNode.getNodeValue())) {
                return true;
            }
        }

        return false;
    }

    public static String getNodeValue(final Node node) {
        if (node != null
                && node.getFirstChild() != null
                && node.getFirstChild().getNodeValue() != null) {
            return node.getFirstChild().getNodeValue().trim();
        }
        return null;
    }

    public static Integer getAttributeValueAsInt(final Node node, final String attributeName) {
        if (node == null || attributeName == null) {
            return null;
        }

        try {
            return Integer.parseInt(getAttributeValue(node, attributeName));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getAttributeValue(final Node node, final String attributeName) {
        if (node == null || attributeName == null) {
            return null;
        }

        final NamedNodeMap attrMap = node.getAttributes();
        final Node attrNode = attrMap.getNamedItem(attributeName);

        // XXX: the attribute value must be retrieved using attrNode.getNodeValue(). Even though
        // getNodeValue(attrNode) works in unit testing, it does not work in production. We were
        // unable to figure out exactly why.
        if (attrNode != null) {
            return attrNode.getNodeValue();
        }
        return null;
    }

    /**
     * Get a list of data from a {@code Document]'s elements that match the {@code elementName},
     * {@code attributeName}, and {@code attributeValue} filters. Each node that matches these is
     * processed by the {@code nodeProcessor} and all non-null results returned by the processor are
     * returned.
     *
     * @param vastDoc The {@link org.w3c.dom.Document} we wish to extract data from.
     * @param elementName Only elements with this name are processed.
     * @param attributeName Only elements with this attribute are processed.
     * @param attributeValue Only elements whose attribute with attributeName matches this value are processed.
     * @param nodeProcessor Takes matching nodes and produces output data for that node.
     * @return a {@code List<T>} with processed node data.
     */
    public static <T> List<T> getListFromDocument(final Document vastDoc, final String elementName,
            final String attributeName, final String attributeValue, NodeProcessor<T> nodeProcessor) {
        final ArrayList<T> results = new ArrayList<T>();

        if (vastDoc == null) {
            return results;
        }

        final NodeList nodes = vastDoc.getElementsByTagName(elementName);
        if (nodes == null) {
            return results;
        }

        List<String> attributeValues = attributeValue == null ? null : Arrays.asList(attributeValue);

        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);

            if (node != null && nodeMatchesAttributeFilter(node, attributeName, attributeValues)) {
                T processed = nodeProcessor.process(node);
                if (processed != null) {
                    results.add(processed);
                }
            }
        }

        return results;
    }

    /**
     * Get first matching data from a {@code Document]'s elements that match the {@code elementName},
     * {@code attributeName}, and {@code attributeValue} filters. Nodes that match are processed by
     * the {@code nodeProcessor} until the first non-null result returned by the processor is
     * returned.
     *
     * @param vastDoc The {@link org.w3c.dom.Document} we wish to extract data from.
     * @param elementName Only elements with this name are processed.
     * @param attributeName Only elements with this attribute are processed.
     * @param attributeValue Only elements whose attribute with attributeName matches this value are processed.
     * @param nodeProcessor Takes matching nodes and produces output data for that node.
     * @return node data of type {@code <T>} from first node that matches.
     */
    public static <T> T getFirstMatchFromDocument(final Document vastDoc, final String elementName,
            final String attributeName, final String attributeValue, NodeProcessor<T> nodeProcessor) {
        if (vastDoc == null) {
            return null;
        }

        final NodeList nodes = vastDoc.getElementsByTagName(elementName);
        if (nodes == null) {
            return null;
        }

        List<String> attributeValues = attributeValue == null ? null : Arrays.asList(attributeValue);

        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);

            if (node != null && nodeMatchesAttributeFilter(node, attributeName, attributeValues)) {
                T processed = nodeProcessor.process(node);
                if (processed != null) {
                    return processed;
                }
            }
        }

        return null;
    }

    public static String getFirstMatchingStringData(final Document vastDoc, final String elementName) {
        return getFirstMatchingStringData(vastDoc, elementName, null, null);
    }

    public static String getFirstMatchingStringData(final Document vastDoc, final String elementName, final String attributeName, final String attributeValue) {
        return getFirstMatchFromDocument(vastDoc, elementName, attributeName, attributeValue, new NodeProcessor<String>() {
            @Override
            public String process(final Node node) {
                return getNodeValue(node);
            }
        });
    }

    public static List<String> getStringDataAsList(final Document vastDoc, final String elementName) {
        return getStringDataAsList(vastDoc, elementName, null, null);
    }

    public static List<String> getStringDataAsList(final Document vastDoc, final String elementName, final String attributeName, final String attributeValue) {
        return getListFromDocument(vastDoc, elementName, attributeName, attributeValue, new NodeProcessor<String>() {
            @Override
            public String process(final Node node) {
                return getNodeValue(node);
            }
        });
    }

    public static List<Node> getNodesWithElementAndAttribute(final Document vastDoc, final String elementName, final String attributeName, final String attributeValue) {
       return getListFromDocument(vastDoc, elementName, attributeName, attributeValue, new NodeProcessor<Node>() {
           @Override
           public Node process(final Node node) {
               return node;
           }
       });
    }

    public interface NodeProcessor<T> {
        T process(Node node);
    }
}
