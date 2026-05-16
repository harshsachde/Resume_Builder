package com.resumebuilder.tailor

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object DocxTailor {

    const val W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val XML_NS = "http://www.w3.org/XML/1998/namespace"
    private const val DEFAULT_CHARS_PER_PAGE = 2_700

    fun tailorDocx(
        docxBytes: ByteArray,
        context: JobContext,
        strictAts: Boolean,
    ): ByteArray {
        val keywords = ParagraphScorer.keywordSet(context)
        val doc = parseDocumentXml(extractEntry(docxBytes, "word/document.xml"))
        val body = findBody(doc) ?: return docxBytes
        val targetChars = context.targetPages.coerceIn(1, 5) * DEFAULT_CHARS_PER_PAGE

        val removable = collectRemovableBlocks(body)
        val scored = removable
            .map { node -> Triple(node, ParagraphScorer.score(textContent(node), keywords), textContent(node).length) }
            .sortedBy { it.second }

        var totalChars = removable.sumOf { textContent(it).length }
        val toRemove = LinkedHashSet<Node>()
        for ((node, _, len) in scored) {
            if (totalChars <= targetChars) break
            if (mustKeep(node)) continue
            toRemove.add(node)
            totalChars -= len
        }
        toRemove.forEach { body.removeChild(it) }

        if (strictAts) {
            stripComplexGraphics(doc.documentElement)
        }

        val newDocXml = serializeXml(doc)
        return replaceZipEntry(docxBytes, "word/document.xml", newDocXml.toByteArray(Charsets.UTF_8))
    }

    fun tailorPdfWithTemplate(
        paragraphs: List<Pair<Int, String>>,
        templateDocxBytes: ByteArray,
        context: JobContext,
        strictAts: Boolean,
    ): ByteArray {
        val keywords = ParagraphScorer.keywordSet(context)
        val targetChars = context.targetPages.coerceIn(1, 5) * DEFAULT_CHARS_PER_PAGE

        val scored = paragraphs
            .map { (idx, text) -> Triple(idx, text, ParagraphScorer.score(text, keywords)) }
            .sortedByDescending { it.third }

        val picked = linkedSetOf<Int>()
        var chars = 0
        for ((idx, text, sc) in scored) {
            if (chars >= targetChars) break
            if (picked.isNotEmpty() && chars + text.length > targetChars + 200 && sc < 25.0) {
                continue
            }
            picked.add(idx)
            chars += text.length
        }
        if (picked.isEmpty() && paragraphs.isNotEmpty()) {
            picked.add(paragraphs.first().first)
        }

        val ordered = paragraphs.filter { it.first in picked }.sortedBy { it.first }.map { it.second }

        val doc = parseDocumentXml(extractEntry(templateDocxBytes, "word/document.xml"))
        val body = findBody(doc) ?: return templateDocxBytes
        val sect = detachSectPr(body)
        clearBody(body)

        val normalStyle = detectDefaultParagraphStyle(templateDocxBytes)
        for (line in ordered) {
            body.appendChild(buildSimpleParagraph(doc, line, normalStyle))
        }
        if (sect != null) {
            body.appendChild(sect)
        } else {
            appendMinimalSectPr(body, doc)
        }

        if (strictAts) {
            stripComplexGraphics(doc.documentElement)
        }

        val newDocXml = serializeXml(doc)
        return replaceZipEntry(templateDocxBytes, "word/document.xml", newDocXml.toByteArray(Charsets.UTF_8))
    }

    private fun extractEntry(docxBytes: ByteArray, path: String): ByteArray {
        ZipInputStream(ByteArrayInputStream(docxBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == path) {
                    val data = zis.readBytes()
                    zis.closeEntry()
                    return data
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        error("Missing $path in docx package")
    }

    private fun replaceZipEntry(docxBytes: ByteArray, path: String, newBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            var replaced = false
            ZipInputStream(ByteArrayInputStream(docxBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val data = if (name == path) {
                        replaced = true
                        newBytes
                    } else {
                        zis.readBytes()
                    }
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(data)
                    zos.closeEntry()
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            if (!replaced) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(newBytes)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun parseDocumentXml(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }

    private fun findBody(doc: Document): Element? {
        val list = doc.getElementsByTagNameNS(W_NS, "body")
        return if (list.length > 0) list.item(0) as Element else null
    }

    private fun collectRemovableBlocks(body: Element): List<Element> {
        val out = ArrayList<Element>()
        val children = body.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n is Element) {
                val name = n.localName ?: n.tagName
                if (name == "p" || name == "tbl") {
                    out.add(n)
                }
            }
        }
        return out
    }

    private fun textContent(el: Element): String {
        val sb = StringBuilder()
        appendText(el, sb)
        return sb.toString().trim()
    }

    private fun appendText(node: Node, sb: StringBuilder) {
        when (node.nodeType) {
            Node.TEXT_NODE -> sb.append(node.nodeValue)
            Node.ELEMENT_NODE -> {
                val e = node as Element
                val isTab = e.namespaceURI == W_NS && e.localName == "tab"
                if (isTab) sb.append('\t')
                val kids = e.childNodes
                for (i in 0 until kids.length) {
                    appendText(kids.item(i), sb)
                }
            }
        }
    }

    private fun mustKeep(node: Node): Boolean {
        val t = textContent(node as Element)
        return ParagraphScorer.isProtected(t)
    }

    private fun detachSectPr(body: Element): Element? {
        val kids = body.childNodes
        for (i in 0 until kids.length) {
            val n = kids.item(i)
            if (n is Element && n.localName == "sectPr") {
                body.removeChild(n)
                return n
            }
        }
        return null
    }

    private fun clearBody(body: Element) {
        val toRemove = mutableListOf<Node>()
        val kids = body.childNodes
        for (i in 0 until kids.length) {
            toRemove.add(kids.item(i))
        }
        toRemove.forEach { body.removeChild(it) }
    }

    private fun detectDefaultParagraphStyle(templateDocxBytes: ByteArray): String {
        return try {
            val stylesBytes = extractEntry(templateDocxBytes, "word/styles.xml")
            val s = stylesBytes.decodeToString()
            val m1 = Regex("""w:styleId="Normal"""").find(s)
            if (m1 != null) return "Normal"
            val m2 = Regex("""w:styleId="([^"]+)"""").find(s)
            m2?.groupValues?.get(1) ?: "Normal"
        } catch (_: Exception) {
            "Normal"
        }
    }

    private fun buildSimpleParagraph(doc: Document, text: String, styleId: String?): Element {
        val p = doc.createElementNS(W_NS, "w:p")
        if (!styleId.isNullOrBlank()) {
            val pPr = doc.createElementNS(W_NS, "w:pPr")
            val pStyle = doc.createElementNS(W_NS, "w:pStyle")
            pStyle.setAttributeNS(W_NS, "val", styleId)
            pPr.appendChild(pStyle)
            p.appendChild(pPr)
        }
        val r = doc.createElementNS(W_NS, "w:r")
        val t = doc.createElementNS(W_NS, "w:t")
        t.setAttributeNS(XML_NS, "xml:space", "preserve")
        t.appendChild(doc.createTextNode(text))
        r.appendChild(t)
        p.appendChild(r)
        return p
    }

    private fun appendMinimalSectPr(body: Element, doc: Document) {
        val sect = doc.createElementNS(W_NS, "w:sectPr")
        body.appendChild(sect)
    }

    private fun stripComplexGraphics(root: Element) {
        val drawingNs = W_NS
        removeByLocalName(root, drawingNs, "drawing")
        removeByLocalName(root, drawingNs, "pict")
        val pictNs = "urn:schemas-microsoft-com:vml"
        removeByLocalName(root, pictNs, "imagedata")
    }

    private fun removeByLocalName(root: Element, ns: String, local: String) {
        val matches = mutableListOf<Element>()
        collectElements(root, ns, local, matches)
        matches.forEach { n ->
            val p = n.parentNode
            if (p != null) p.removeChild(n)
        }
    }

    private fun collectElements(node: Node, ns: String, local: String, acc: MutableList<Element>) {
        if (node is Element) {
            if (local == node.localName && ns == node.namespaceURI) {
                acc.add(node)
            }
            val kids = node.childNodes
            for (i in 0 until kids.length) {
                collectElements(kids.item(i), ns, local, acc)
            }
        }
    }

    private fun serializeXml(doc: Document): String {
        val tf = TransformerFactory.newInstance()
        val transformer = tf.newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        transformer.setOutputProperty(OutputKeys.INDENT, "no")
        val bos = ByteArrayOutputStream()
        transformer.transform(DOMSource(doc), StreamResult(bos))
        return bos.toString(Charsets.UTF_8.name())
    }
}
