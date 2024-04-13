package com.adenabrasil.pdfview;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class MainActivity extends AppCompatActivity {
    private List<String> pdfNames;
    private PdfAdapter pdfAdapter;
    private List<Uri> pdfUris;
    private List<String> pdfContents = new ArrayList<>();
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String pdfName = getFileNameFromUri(uri);
                    if (pdfName != null) {
                        if (!pdfUris.contains(uri)) {
                            pdfUris.add(uri);
                            pdfNames.add(pdfName);
                            StringBuilder parsedText = new StringBuilder();
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(uri);
                                if (inputStream != null) {
                                    PdfReader reader = new PdfReader(inputStream);
                                    int numberOfPages = reader.getNumberOfPages();
                                    for (int i = 1; i <= numberOfPages; i++) {
                                        String pageText = PdfTextExtractor.getTextFromPage(reader, i);
                                        pageText = pageText.replaceAll("(?m)(^\\s*$\\n)", "<p>");
                                        parsedText.append(pageText);
                                    }
                                    reader.close();
                                } else {
                                    Log.e("PdfBox-Android-Sample", "InputStream is null");
                                }
                            } catch (IOException e) {
                                Log.e("PdfBox-Android-Sample", "Exception thrown while loading or reading PDF", e);
                            }
                            pdfContents.add(parsedText.toString());
                            pdfAdapter.notifyDataSetChanged();
                            Intent intent = new Intent(MainActivity.this, Pdf.class);
                            intent.putExtra("pdfContent", parsedText.toString());
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Este PDF já foi adicionado", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Não foi possível obter o nome do PDF", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pdfNames = new ArrayList<>();
        pdfUris = new ArrayList<>();
        pdfAdapter = new PdfAdapter(this, pdfNames, pdfUris);
        RecyclerView recyclerViewPdf = findViewById(R.id.recyclerViewPdf);
        recyclerViewPdf.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPdf.setAdapter(pdfAdapter);
        Button buttonOpenPdf = findViewById(R.id.buttonOpenPdf);
        buttonOpenPdf.setOnClickListener(v -> mGetContent.launch("application/pdf"));
        pdfAdapter.setOnPdfClickListener(position -> {
            if (position >= 0 && position < pdfContents.size()) {
                String pdfContent = pdfContents.get(position);
                Intent intent = new Intent(MainActivity.this, Pdf.class);
                intent.putExtra("pdfContent", pdfContent);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Conteúdo do PDF inválido", Toast.LENGTH_SHORT).show();
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("pdf_list", Context.MODE_PRIVATE);
        String savedNames = sharedPreferences.getString("pdf_names", "");
        String savedContents = sharedPreferences.getString("pdf_contents", "");
        if (!savedNames.isEmpty()) {
            pdfNames.addAll(Arrays.asList(savedNames.split(",")));
            pdfContents.addAll(Arrays.asList(savedContents.split(";")));
            pdfAdapter.notifyDataSetChanged();
        }
    }
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
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
        return result;
    }
    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences = getSharedPreferences("pdf_list", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pdf_names", TextUtils.join(",", pdfNames));
        editor.putString("pdf_contents", TextUtils.join(";", pdfContents));
        editor.apply();
    }
}
