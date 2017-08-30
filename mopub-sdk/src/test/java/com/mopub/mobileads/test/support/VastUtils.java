package com.mopub.mobileads.test.support;

import com.mopub.mobileads.VastTracker;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class VastUtils {
    public static Node createNode(String xml) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setCoalescing(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new StringReader(xml)));
        return document.getFirstChild();
    }

    public static List<VastTracker> stringsToVastTrackers(String... strings) {
        List<VastTracker> vastTrackers = new ArrayList<VastTracker>(strings.length);
        for (int i=0; i<strings.length; i++) {
            vastTrackers.add(new VastTracker(strings[i]));
        }
        return vastTrackers;
    }

    public static List<String> vastTrackersToStrings(List<VastTracker> vastTrackers) {
        List<String> strings = new ArrayList<String>(vastTrackers.size());
        for (VastTracker vastTracker : vastTrackers) {
            strings.add(vastTracker.getContent());
        }
        return strings;
    }
}
