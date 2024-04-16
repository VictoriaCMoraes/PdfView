package com.adenabrasil.pdfview;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class MainActivity extends AppCompatActivity {
    private List<String> pdfNames;
    private PdfAdapter pdfAdapter;
    private List<String> pdfImagePaths = new ArrayList<>();
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeDatabase();
        initializeViews();
        loadPdfContentsFromDatabase();
        setButtonClickListeners();
    }

    private void initializeDatabase() {
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "pdf-content-database").build();
    }

    private void initializeViews() {
        pdfNames = new ArrayList<>();
        pdfImagePaths = new ArrayList<>();
        pdfAdapter = new PdfAdapter(this, pdfNames, pdfImagePaths);
        RecyclerView recyclerViewPdf = findViewById(R.id.recyclerViewPdf);
        recyclerViewPdf.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPdf.setAdapter(pdfAdapter);
        Button buttonOpenPdf = findViewById(R.id.buttonOpenPdf);
        buttonOpenPdf.setOnClickListener(v -> mGetContent.launch("application/pdf"));
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadPdfContentsFromDatabase() {
        executor.execute(() -> {
            List<PdfContent> pdfContentsList = db.pdfContentDao().getAll();
            for (PdfContent pdfContent : pdfContentsList) {
                pdfNames.add(pdfContent.title);
                pdfImagePaths.add(pdfContent.imagePath);
            }
            // Invertendo a ordem da lista pdfNames
            Collections.reverse(pdfNames);
            Collections.reverse(pdfImagePaths);

            handler.post(() -> pdfAdapter.notifyDataSetChanged());
        });
    }

    private void setButtonClickListeners() {
        pdfAdapter.setOnPdfClickListener(position -> {
            if (position >= 0 && position < pdfNames.size()) {
                String pdfName = pdfNames.get(position);
                Intent intent = new Intent(MainActivity.this, Pdf.class);
                intent.putExtra("pdfName", pdfName);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Conteúdo do PDF inválido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String pdfName = getFileNameFromUri(uri);
                    showLoading();
                    if (!pdfNames.contains(pdfName)) {
                        pdfNames.add(0,pdfName);
                        executor.execute(() -> processPdfContent(uri, pdfName));
                    } else {
                        Toast.makeText(MainActivity.this, "Este PDF já foi adicionado", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri != null && uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        if (result != null && result.contains(".")) {
            result = result.substring(0, result.lastIndexOf("."));
        }
        return result;
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void processPdfContent(Uri uri, String pdfName) {
        executor.execute(() -> {
            StringBuilder parsedText = new StringBuilder();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    PdfReader reader = new PdfReader(inputStream);
                    int numberOfPages = reader.getNumberOfPages();
                    for (int i = 1; i <= numberOfPages; i++) {
                        String pageText = PdfTextExtractor.getTextFromPage(reader, i);
                        String[] lines = pageText.split("\\n");
                        StringBuilder paragraph = new StringBuilder();
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                paragraph.append(line).append(" ");
                                if (line.endsWith(".")) {
                                    parsedText.append("<p>").append(paragraph).append("</p>");
                                    paragraph = new StringBuilder();
                                } else if (line.length() < 40) {
                                    parsedText.append("<p>").append(paragraph).append("</p>");
                                    paragraph = new StringBuilder();
                                }
                            }
                        }
                        if (paragraph.length() > 0) {
                            parsedText.append("<p>").append(paragraph).append("</p>");
                        }
                    }
                    reader.close();
                } else {
                    Log.e("PdfBox-Android-Sample", "InputStream is null");
                }
            } catch (IOException e) {
                Log.e("PdfBox-Android-Sample", "Exception thrown while loading or reading PDF", e);
            }
            // Renderize a primeira página em um Bitmap
            try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
                if (pfd != null) {
                    PdfRenderer renderer = new PdfRenderer(pfd);
                    PdfRenderer.Page page = renderer.openPage(0);
                    Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    renderer.close();

                    // Salve o Bitmap como uma imagem
                    File imageFile = new File(getFilesDir(), pdfName + ".png");  // Defina imageFile aqui
                    try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        Log.d("PdfBox-Android-Sample", "Image saved at: " + imageFile.getAbsolutePath());
                    }
                    // Crie uma nova instância de PdfContent
                    PdfContent pdfContent = new PdfContent();

                    // Salve o caminho da imagem no banco de dados
                    pdfContent.imagePath = imageFile.getAbsolutePath();

                    // Salve o título e o conteúdo no banco de dados
                    pdfContent.title = pdfName;
                    pdfContent.content = parsedText.toString();
                    db.pdfContentDao().insert(pdfContent);

                    // Atualize a UI na thread principal
                    handler.post(() -> {
                        pdfImagePaths.add(0, imageFile.getAbsolutePath());
                        pdfAdapter.notifyItemInserted(0);
                        hideLoading();
                    });
                }
            } catch (IOException e) {
                Log.e("PdfBox-Android-Sample", "Exception thrown while rendering PDF", e);
            }
        });
    }
}
