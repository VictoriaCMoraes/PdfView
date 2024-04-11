package com.adenabrasil.pdfview;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.io.InputStream;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;

public class Pdf extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        TextView textView;

        // Referenciando a TextView
        textView = findViewById(R.id.textView);

        textView.setMovementMethod(new ScrollingMovementMethod());

        textView.setBackgroundColor(ContextCompat.getColor(this, R.color.beige));

        StringBuilder parsedText = new StringBuilder();

        try {
            // Carregando o documento PDF
            Uri uri = Uri.parse(getIntent().getStringExtra("pdfUri"));
            InputStream inputStream = getContentResolver().openInputStream(uri);

            if (inputStream != null) {
                PdfReader reader = new PdfReader(inputStream);

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
            } else {
                Log.e("PdfBox-Android-Sample", "InputStream is null");
            }
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while loading or reading PDF", e);
        }

        // Exibindo o texto extraído na TextView
        textView.setText(parsedText.toString());
    }
}