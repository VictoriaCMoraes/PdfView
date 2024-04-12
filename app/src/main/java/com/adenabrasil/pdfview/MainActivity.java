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
                        // Verifica se o URI já está na lista pdfUris
                        if (!pdfUris.contains(uri)) {
                            // Adiciona o URI do PDF à lista pdfUris apenas se ainda não estiver lá
                            pdfUris.add(uri);
                            pdfNames.add(pdfName); // Adiciona o nome do PDF à lista pdfNames

                            // Extrair o conteúdo do PDF
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
                            pdfContents.add(parsedText.toString()); // Adiciona o conteúdo do PDF à lista pdfContents

                            // Notificar o adapter sobre a atualização dos dados
                            pdfAdapter.notifyDataSetChanged();

                            // Abrir o PDF na atividade PDF
                            Intent intent = new Intent(MainActivity.this, Pdf.class);
                            intent.putExtra("pdfUri", uri.toString());
                            intent.putExtra("pdfContent", parsedText.toString());
                            startActivity(intent);
                        } else {
                            // Caso o PDF já esteja na lista, exibir uma mensagem ou tomar outra ação
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
        pdfUris = new ArrayList<>(); // Inicializa a lista de URIs dos PDFs
        pdfAdapter = new PdfAdapter(this, pdfNames, pdfUris); // Passe a lista de URIs para o adapter

        // O restante do código permanece o mesmo
        RecyclerView recyclerViewPdf = findViewById(R.id.recyclerViewPdf);
        recyclerViewPdf.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPdf.setAdapter(pdfAdapter);

        Button buttonOpenPdf = findViewById(R.id.buttonOpenPdf);
        buttonOpenPdf.setOnClickListener(v -> mGetContent.launch("application/pdf"));

        // Configurar o listener de clique para abrir a WebView com o PDF correspondente
        pdfAdapter.setOnPdfClickListener(pdfUri -> {
            if (pdfUri != null) {
                Intent intent = new Intent(MainActivity.this, Pdf.class);
                intent.putExtra("pdfUri", pdfUri.toString());
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "URI do PDF inválida", Toast.LENGTH_SHORT).show();
            }
        });

        // Recuperar os nomes salvos de SharedPreferences e adicionar de volta à lista pdfNames
        SharedPreferences sharedPreferences = getSharedPreferences("pdf_list", Context.MODE_PRIVATE);
        String savedNames = sharedPreferences.getString("pdf_names", "");
        if (!savedNames.isEmpty()) {
            List<String> savedPdfNames = Arrays.asList(savedNames.split(","));
            for (String name : savedPdfNames) {
                if (!pdfNames.contains(name)) {
                    pdfNames.add(name);
                }
            }
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
        editor.putString("pdf_names", TextUtils.join(",", pdfNames)); // Converta a lista de nomes em uma string separada por vírgulas
        editor.putString("pdf_contents", TextUtils.join(";", pdfContents)); // Converta a lista de conteúdos em uma string separada por ponto e vírgula
        editor.apply();
    }
}