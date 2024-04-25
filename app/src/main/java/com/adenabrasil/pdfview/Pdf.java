package com.adenabrasil.pdfview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pdf extends AppCompatActivity {
    private int scrollY = 0;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String pdfName;
    private SeekBar seekBar;
    private GestureDetector gestureDetector;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_pdf);

        // Inicialize o banco de dados
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "pdf-content-database").build();

        // Obtenha o nome do PDF do Intent
        pdfName = getIntent().getStringExtra("pdfName");

        WebView webView = findViewById(R.id.webview);
        seekBar = findViewById(R.id.seekBar);
        toolbar = findViewById(R.id.toolbar);

        TextView pdfNameTextView = findViewById(R.id.pdfNameTextView);
        pdfNameTextView.setText(pdfName);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Inicialize o GestureDetector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (seekBar.getVisibility() == View.VISIBLE) {
                    seekBar.setVisibility(View.GONE);
                    toolbar.setVisibility(View.GONE);
                } else {
                    seekBar.setVisibility(View.VISIBLE);
                    toolbar.setVisibility(View.VISIBLE);
                }
                return super.onSingleTapConfirmed(e);
            }
        });

        webView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });

        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int webViewHeight = (int) ((float) webView.getContentHeight() * webView.getScale()); // Ajuste para escala
            int progress = (int) (((float) scrollY / webViewHeight) * 100);
            seekBar.setProgress(progress);
            Log.d("progress pdf", "O progresso pdf é: " + progress);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int webViewHeight = (int) ((float) webView.getContentHeight() * webView.getScale()); // Ajuste para escala
                    int scrollY = (int) ((progress / 100.0) * webViewHeight);
                    webView.scrollTo(0, scrollY);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.postDelayed(() -> view.scrollTo(0, scrollY), 100); // Aplica a rolagem após um pequeno atraso
                // Recupere o PdfContent do banco de dados usando o nome
                executor.execute(() -> {
                    PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                    if (pdfContent != null) {
                        scrollY = pdfContent.scrollPosition;

                        // Defina a posição de rolagem depois que o conteúdo da WebView for totalmente carregado
                        handler.postDelayed(() -> {
                            webView.scrollTo(0, scrollY);
                            // Atualize a posição da SeekBar com base na posição de rolagem
                            int webViewHeight = (int) ((float) webView.getContentHeight() * webView.getScale()); // Ajuste para escala
                            int progress = (int) (((float) scrollY / webViewHeight) * 100);
                            seekBar.setProgress(progress);
                        }, 100);
                    }
                });
            }
        });

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        @ColorInt int backgroundColor = typedValue.data;
        webView.setBackgroundColor(backgroundColor);

        executor.execute(() -> {
            // Recupere o PdfContent do banco de dados usando o nome
            PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
            if (pdfContent != null) {
                handler.post(() -> {
                    // Use o conteúdo do PdfContent
                    String pdfContentString = pdfContent.content;

                    String hexBackgroundColor = String.format("#%06X", (0xFFFFFF & backgroundColor));
                    boolean isBackgroundBeige = (hexBackgroundColor.equals("#FFFFDF"));
                    String textColor = isBackgroundBeige ? "black" : "white";
                    String htmlText = "<html><head><style>body {text-align: justify; word-wrap: break-word; font-size: 20px; color: %s;}</style></head><body>%s</body></html>";
                    String data = String.format(htmlText, textColor, pdfContentString);
                    webView.loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
                });
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        WebView webView = findViewById(R.id.webview);
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (webView != null && seekBar != null) {
            int scrollY = webView.getScrollY();
            int progressBarPosition = seekBar.getProgress();
            outState.putInt("scrollY", scrollY);
            outState.putInt("progressBarPosition", progressBarPosition);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView webView = findViewById(R.id.webview);
        if (webView != null) {
            executor.execute(() -> {
                PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                if (pdfContent != null) {
                    scrollY = pdfContent.scrollPosition;
                    handler.post(() -> webView.scrollTo(0, scrollY));
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
            int position = seekBar.getProgress();
            // Salve a posição de rolagem no banco de dados
            executor.execute(() -> {
                PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                if (pdfContent != null) {
                    pdfContent.scrollPosition = scrollY;
                    pdfContent.progress = position;
                    db.pdfContentDao().update(pdfContent);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webview);
        if (webView != null) {
            scrollY = webView.getScrollY();
            int position = seekBar.getProgress();
            // Salve a posição de rolagem no banco de dados
            executor.execute(() -> {
                PdfContent pdfContent = db.pdfContentDao().getByTitle(pdfName);
                if (pdfContent != null) {
                    pdfContent.scrollPosition = scrollY;
                    pdfContent.progress = position;
                    db.pdfContentDao().update(pdfContent);
                }
            });
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra("pdfName", pdfName);
        resultIntent.putExtra("pdfProgress", seekBar.getProgress());
        setResult(Activity.RESULT_OK, resultIntent);
        super.onBackPressed();
    }

}