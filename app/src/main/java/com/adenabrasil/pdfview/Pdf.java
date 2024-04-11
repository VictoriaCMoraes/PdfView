package com.adenabrasil.pdfview;

import static java.sql.DriverManager.println;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;

public class Pdf extends AppCompatActivity {
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        // Referenciando a TextView
        textView = findViewById(R.id.textView);

        textView.setMovementMethod(new ScrollingMovementMethod());

        StringBuilder parsedText = new StringBuilder();
        try {
            // Carregando o documento PDF
            Uri uri = Uri.parse(getIntent().getStringExtra("pdfUri"));
            PdfReader reader = new PdfReader(getContentResolver().openInputStream(uri));

            // Extraindo o texto do PDF
            int numberOfPages = reader.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; i++) {
                String pageText = PdfTextExtractor.getTextFromPage(reader, i); // Extrai o texto da página i
                // Remove quebras de linha desnecessárias
                pageText = pageText.replace("\n", " ");
                pageText = pageText.replace("   ", "\n");
                parsedText.append(pageText);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while loading or reading PDF", e);
        }

        // Exibindo o texto extraído na TextView
        textView.setText(parsedText.toString());
    }
}