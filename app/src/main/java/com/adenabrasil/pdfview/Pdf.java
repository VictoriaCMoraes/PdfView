package com.adenabrasil.pdfview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pdf extends AppCompatActivity {
    // Adicione essas variáveis para salvar a posição de rolagem
    private int scrollX = 0;
    private int scrollY = 0;
    private AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private String pdfName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        // Inicialize o banco de dados
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "pdf-content-database").build();
        // Obtenha o nome do PDF do Intent
        pdfName = getIntent().getStringExtra("pdfName");
        WebView webView = findViewById(R.id.webview);

        // Adicione um WebViewClient à sua WebView
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Restaure a posição de rolagem aqui
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                        if (pdfContent != null) {
                            scrollY = pdfContent.scrollPosition;
                            handler.postDelayed(() -> webView.scrollTo(0, scrollY), 100);
                        }
                    }
                });
            }
        });
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
        WebView webView = findViewById(R.id.webview);
        if (webView != null) {
            scrollY = webView.getScrollY();
            outState.putInt("scrollY", scrollY);

            // Salve a posição de rolagem no banco de dados
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                    if (pdfContent != null) {
                        pdfContent.scrollPosition = scrollY;
                        db.pdfContentDao().update(pdfContent);
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView webView = findViewById(R.id.webview);
        if (webView != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                    if (pdfContent != null) {
                        scrollY = pdfContent.scrollPosition;
                        handler.post(() -> webView.scrollTo(0, scrollY));
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebView webView = findViewById(R.id.webview);
        if (webView != null) {
            scrollY = webView.getScrollY();

            // Salve a posição de rolagem no banco de dados
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                    if (pdfContent != null) {
                        pdfContent.scrollPosition = scrollY;
                        db.pdfContentDao().update(pdfContent);
                    }
                }
            });
        }
    }

}
