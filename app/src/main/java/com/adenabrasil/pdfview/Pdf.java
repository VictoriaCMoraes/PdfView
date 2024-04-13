package com.adenabrasil.pdfview;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.WebView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.io.InputStream;
import android.net.Uri;
public class Pdf extends AppCompatActivity {
    // Adicione essas variáveis para salvar a posição de rolagem
    private int scrollX = 0;
    private int scrollY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        WebView webView = findViewById(R.id.webview);
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        @ColorInt int backgroundColor = typedValue.data;
        webView.setBackgroundColor(backgroundColor);
        String pdfContent = getIntent().getStringExtra("pdfContent");
        String hexBackgroundColor = String.format("#%06X", (0xFFFFFF & backgroundColor));
        boolean isBackgroundBeige = (hexBackgroundColor.equals("#FFFFDF"));
        String textColor = isBackgroundBeige ? "black" : "white";
        String htmlText = "<html><head><style>body {text-align: justify; word-wrap: break-word; font-size: 20px; color: %s;}</style></head><body>%s</body></html>";
        String data = String.format(htmlText, textColor, pdfContent);
        webView.loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Salvando a posição de rolagem da WebView
        WebView webView = findViewById(R.id.webview);
        if (webView != null) {
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
            outState.putInt("scrollX", scrollX);
            outState.putInt("scrollY", scrollY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restaurando a posição de rolagem da WebView
        WebView webView = findViewById(R.id.webview);
        if (webView != null) {
            webView.post(() -> webView.scrollTo(scrollX, scrollY));
        }
    }
}
