package com.lb.apkmanifestfetcher

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap

/**a tiny bit more efficient way to parse manifest attributes alone*/
class ManifestParser2 {
    var isSplitApk: Boolean? = null
    var manifestAttributes: HashMap<String, String>? = null

    companion object {
        fun parse(file: File) = parse(java.io.FileInputStream(file))
        fun parse(filePath: String) = parse(File(filePath))
        fun parse(inputStream: InputStream): ManifestParser2? {
            val result = ManifestParser2()
            val manifestXmlString = ApkManifestFetcher.getManifestXmlFromInputStream(inputStream)
                    ?: return null
//            val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
//            val builder: DocumentBuilder = factory.newDocumentBuilder()
//            val document: Document? = builder.parse(manifestXmlString.byteInputStream())
//            if (document != null) {
//                document.documentElement.normalize()
//                val manifestNode: Node? = document.getElementsByTagName("manifest")?.item(0)
//                if (manifestNode != null) {
//                    val manifestAttributes = HashMap<String, String>()
//                    for (i in 0 until manifestNode.attributes.length) {
//                        val node = manifestNode.attributes.item(i)
//                        manifestAttributes[node.nodeName] = node.nodeValue
//                    }
//                    result.manifestAttributes = manifestAttributes
//                }
//            }
            result.manifestAttributes = getApkManifestTagAttributes(manifestXmlString)
            result.manifestAttributes?.let {
                result.isSplitApk = (it["android:isFeatureSplit"]?.toBoolean()
                        ?: false) || (it.containsKey("split"))
            }
            return result
        }

        private fun getApkManifestTagAttributes(manifestXmlString: String): HashMap<String, String>? {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xmlParser = factory.newPullParser()
            xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            xmlParser.setInput(manifestXmlString.byteInputStream(), null)
            var currentTag: XmlTag? = null
            val tagsStack = Stack<XmlTag>()
            xmlParser.next()
            var eventType = xmlParser.eventType
            var doneParsing = false

            while (eventType != XmlPullParser.END_DOCUMENT && !doneParsing) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {
                    }
                    XmlPullParser.START_TAG -> {
                        val xmlTagName = xmlParser.name
                        currentTag = XmlTag(xmlTagName)
                        tagsStack.push(currentTag)
                        val numberOfAttributes = xmlParser.attributeCount
                        if (numberOfAttributes > 0) {
                            val attributes = HashMap<String, String>(numberOfAttributes)
                            for (i in 0 until numberOfAttributes) {
                                val attrName = xmlParser.getAttributeName(i)
                                val attrValue = xmlParser.getAttributeValue(i)
                                attributes[attrName] = attrValue
                            }
                            if (xmlTagName == "manifest") {
                                return attributes
                            }
                            currentTag.tagAttributes = attributes
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        currentTag = tagsStack.pop()
                        if (!tagsStack.isEmpty()) {
                            val parentTag = tagsStack.peek()
                            parentTag.addInnerXmlTag(currentTag)
                            currentTag = parentTag
                        } else
                            doneParsing = true
                    }
                    XmlPullParser.TEXT -> {
                        val innerText = xmlParser.text
                        currentTag?.addInnerText(innerText)
                    }
                }
                eventType = xmlParser.next()
            }
            return null
        }
    }
}
