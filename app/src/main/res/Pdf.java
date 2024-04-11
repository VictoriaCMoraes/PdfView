import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import java.io.IOException;

public class Pdf extends AppCompatActivity {
    private AssetManager assetManager;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        // Inicializando o AssetManager
        assetManager = getAssets();

        // Referenciando a TextView
        textView = findViewById(R.id.textView);
    }

    public void stripText(View v) {
        String parsedText = null;
        PDDocument document = null;
        try {
            // Carregando o documento PDF da pasta assets
            document = PDDocument.load(assetManager.open("the-world-to-come.pdf"));
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while loading document to strip", e);
        }

        try {
            // Instanciando o PDFTextStripper
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(0);
            pdfStripper.setEndPage(1);
            // Extraindo texto do documento
            parsedText = "Parsed text: " + pdfStripper.getText(document);
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while stripping text", e);
        } finally {
            try {
                // Fechando o documento
                if (document != null) document.close();
            } catch (IOException e) {
                Log.e("PdfBox-Android-Sample", "Exception thrown while closing document", e);
            }
        }
        // Exibindo o texto extraído na TextView
        textView.setText(parsedText);
    }
}
