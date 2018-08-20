package ru.inovus.ms.rdm.file.export;


import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.*;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.*;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.ElementHandlerPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import ru.inovus.ms.rdm.exception.RdmException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class PdfCreatorUtil {

    private static final Font baseFont;
    private static final Font baseFontSmall;
    private static final Font archFont;
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_THREAD_LOCAL = new ThreadLocal<>();

    static {
        BaseFont font;
        try {
            font = BaseFont.createFont("fonts/calibri.ttf", "cp1251", BaseFont.EMBEDDED);
        } catch (IOException | DocumentException e) {
            throw new RdmException(e);
        }
        baseFont = new Font(font, 14);
        baseFontSmall = new Font(font, 7);
        archFont = new Font(font, 16, Font.BOLD);
    }

    public void writeDocument(OutputStream out, Map<String, String> keyValue, String paragraphName) throws DocumentException, IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, byteArrayOutputStream);
            document.open();

            Paragraph elements = new Paragraph(paragraphName, archFont);
            elements.setAlignment(Element.ALIGN_CENTER);
            elements.add(new Paragraph(" "));
            for (Map.Entry<String, String> entry : keyValue.entrySet()) {
                createParagraph(elements, entry.getKey(), entry.getValue());
            }
            document.add(elements);
            document.close();
            out.write(byteArrayOutputStream.toByteArray());
        }
    }


    private void createParagraph(Paragraph elements, String message, String field) throws IOException {
        if (field != null && !field.isEmpty()) {
            elements.add(new Paragraph(message, archFont));
            int size = elements.size();
            XMLWorkerFontProvider fontImp = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
            fontImp.register("fonts/calibri.ttf");
            FontFactory.setFontImp(fontImp);

            for (Element e : parseToElementList(field, "table, p, ol, ul, span {font-family:Helvetica, Calibri, Arial, sans-serif; font-size: 14px}")) {
                for (Chunk chunk : e.getChunks()) {
                    if (chunk.getAttributes() == null || !chunk.getAttributes().containsKey("SUBSUPSCRIPT"))
                        chunk.getFont().setSize(14);
                }
                elements.add(e);
            }
            if (elements.size() == size)
                elements.add(new Paragraph(field, baseFont));
        }
    }

    public static ElementList parseToElementList(String html, String css) throws IOException {
        // CSS
        CSSResolver cssResolver = new StyleAttrCSSResolver();
        if (css != null) {
            CssFile cssFile = XMLWorkerHelper.getCSS(new ByteArrayInputStream(css.getBytes()));
            cssResolver.addCss(cssFile);
        }

        // HTML
        CssAppliers cssAppliers = new CssAppliersImpl(FontFactory.getFontImp());
        HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
        TagProcessorFactory factory = Tags.getHtmlTagProcessorFactory();
        factory.addProcessor(
                new Span() {
                    @Override
                    public List<Element> end(WorkerContext ctx, Tag tag, List<Element> l) {
                        List<Element> list = new ArrayList<>(1);
                        String value = ((Chunk) l.get(0)).getContent();
                        Chunk chunk = new Chunk(value, baseFontSmall);
                        chunk.setTextRise(6);
                        list.add(chunk);
                        return list;
                    }
                },
                "sup");
        factory.addProcessor(
                new Span() {
                    @Override
                    public List<Element> end(WorkerContext ctx, Tag tag, List<Element> l) {
                        List<Element> list = new ArrayList<>(1);
                        String value = ((Chunk) l.get(0)).getContent();
                        Chunk chunk = new Chunk(value, baseFontSmall);
                        chunk.setTextRise(-3);
                        list.add(chunk);
                        return list;
                    }
                },
                "sub");
        htmlContext.setTagFactory(factory);
        htmlContext.autoBookmark(false);

        // Pipelines
        ElementList elements = new ElementList();
        ElementHandlerPipeline end = new ElementHandlerPipeline(elements, null);
        HtmlPipeline htmlPipeline = new HtmlPipeline(htmlContext, end);
        CssResolverPipeline cssPipeline = new CssResolverPipeline(cssResolver, htmlPipeline);

        // XML Worker
        XMLWorker worker = new XMLWorker(cssPipeline, true);
        XMLParser p = new XMLParser(worker);
        p.parse(new ByteArrayInputStream(html.getBytes()));

        return elements;
    }

    private static SimpleDateFormat getFormat() {
        SimpleDateFormat format = DATE_FORMAT_THREAD_LOCAL.get();
        if (format == null) {
            format = new SimpleDateFormat("dd-MM-y");
            DATE_FORMAT_THREAD_LOCAL.set(format);
        }
        return format;
    }
}
