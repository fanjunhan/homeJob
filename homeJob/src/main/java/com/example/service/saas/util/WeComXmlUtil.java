package com.example.service.saas.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/** 企业微信回调 XML 解析工具（回调报文为简单的单层结构） */
public final class WeComXmlUtil {

    private WeComXmlUtil() {
    }

    /** 解析为扁平 Map（子节点名 -> 文本内容） */
    public static Map<String, String> parse(String xml) {
        Map<String, String> result = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 关闭外部实体，避免 XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.put(node.getNodeName(), node.getTextContent());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("企微回调 XML 解析失败", e);
        }
        return result;
    }

    /** 取出加密串 Encrypt */
    public static String extractEncrypt(String xml) {
        String encrypt = parse(xml).get("Encrypt");
        if (encrypt == null || encrypt.isBlank()) {
            throw new IllegalArgumentException("回调报文缺少 Encrypt 字段");
        }
        return encrypt.trim();
    }
}
