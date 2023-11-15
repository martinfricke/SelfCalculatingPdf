package com.martinfricke;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SelfCalculatingPdf {

    public static void main(String[] args) {
        final SelfCalculatingPdf selfCalcPdf = new SelfCalculatingPdf();
        final String tempDir = System.getProperty("java.io.tmpdir");
        final File texFile = Paths.get(tempDir, "example.tex").toFile(); // tex file
        final Path procPath = Paths.get(tempDir); // process path
        final List<Article> articles = selfCalcPdf.createArticleList(10); // Create random articles
        selfCalcPdf.createTexFile(texFile, articles); // Create tex-file
        selfCalcPdf.createPdf(procPath, texFile); // Create pdf
        selfCalcPdf.openPdf(texFile); // Open pdf
    }

    // Create articles
    private List<Article> createArticleList(int size) {
        final List<Article> articles = new ArrayList<>();
        final Random rand = new Random();
        for (int i = 0; i < size; i++) {
            final double price = (double) rand.nextInt(100000) / 100;
            articles.add(new Article("\\lipsum[2][1-5]", price));
        }
        return articles;
    }

    // Create tex-file
    private void createTexFile(File texFile, List<Article> articles) {
        try (PrintWriter pW = new PrintWriter(texFile, StandardCharsets.UTF_8.name())) {
            pW.println("\\documentclass{article}");
            pW.println("\\usepackage{babel}");
            pW.println("\\usepackage{xcolor}"); // bordercolor white
            pW.println("\\usepackage{booktabs}"); // midrule
            pW.println("\\usepackage{xltabular}"); // table with page breaks
            pW.println("\\usepackage{lipsum}");
            pW.println("\\usepackage{hyperref}"); // hyperref for pdf controls
            pW.println("\\begin{document}");
            pW.println("\\begin{Form}"); // pdf controls
            pW.println("\\renewcommand{\\arraystretch}{1.5}"); // table line spacing
            pW.println("\\begin{xltabular}{\\linewidth}{rXrc}"); // 4 tables columns
            pW.println("Nr. & Description & Price & Selected\\\\"); // table header, 4 tables columns
            pW.println("\\toprule");
            pW.println("\\endhead");
            pW.println("\\multicolumn{4}{r}{\\footnotesize\\textit{Continued\\ldots}}\\\\"); // 4 tables columns
            pW.println("\\endfoot");
            pW.println("\\endlastfoot");
            int i = 1;
            final DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.ENGLISH));
            for (Article article : articles)
                pW.println(i + " & " + article.getDescription() + " & " + df.format(article.getPrice()) + " & " +
                        "\\raisebox{-2.5pt}{\\CheckBox[name=cb" + i++ + ", height=1.2em, bordercolor=black]{}}\\\\"); // checkbox
            pW.println("\\midrule");
            pW.println("\\multicolumn{4}{r}{"); // result field, 4 tables columns
            pW.println("\\mbox{\\TextField[name=result,charsize=10pt,width=\\linewidth,readonly,bordercolor=white,backgroundcolor=gray!10,height=1.5em,align=2,");
            pW.println("calculate = {");
            pW.println("var erg = 0;");
            i = 1;
            for (Article article : articles)
                pW.println("if(this.getField('cb" + i++ + "').value == 'Yes') erg += " + article.getPrice() + ";");
            pW.println("event.value = erg.toFixed(2);");
            pW.println("}]{}}}");
            pW.println("\\end{xltabular}\\par");
            pW.println("\\end{Form}");
            pW.println("\\end{document}");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // Create pdf
    private void createPdf(Path procPath, File file) {
        for (int i = 0; i < 2; i++) { // 2 runs are necessary for cross-page tables
            final ProcessBuilder pBuilder = new ProcessBuilder("lualatex", file.getPath());
            pBuilder.directory(procPath.toFile());
            try {
                final Process proc = pBuilder.start();
                final BufferedReader bReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                final Thread td = new Thread(() -> {
                    try {
                        String line;
                        while ((line = bReader.readLine()) != null) System.out.println(line);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                td.start();
                td.join(); // Wait until the previous run has ended
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Open pdf
    private void openPdf(File file) {
        final Path pdfPath = Paths.get(file.getPath().replace(".tex", ".pdf")); // Change file extension to pdf
        // https://mkyong.com/java/how-to-open-a-pdf-file-in-java/
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                if (pdfPath.toFile().exists())
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + pdfPath).waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

