package com.adenabrasil.pdfview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.ViewHolder> {

    private final Context context;
    private final List<String> pdfNames;
    private final List<String> pdfImagePath;
    private final List<Integer> pdfScrollPosition;
    private final List<Integer> pdfWebViewHeight;
    private OnPdfClickListener onPdfClickListener;

    public PdfAdapter(Context context, List<String> pdfNames, List<String> pdfImagePath,
                      List<Integer> pdfScrollPosition, List<Integer> pdfWebViewHeight) {
        this.context = context;
        this.pdfNames = pdfNames;
        this.pdfImagePath = pdfImagePath;
        this.pdfScrollPosition = pdfScrollPosition;
        this.pdfWebViewHeight = pdfWebViewHeight;
    }

    public interface OnPdfClickListener {
        void onPdfClick(int position);
    }

    public void setOnPdfClickListener(OnPdfClickListener listener) {
        this.onPdfClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!pdfNames.isEmpty() && !pdfImagePath.isEmpty() ) {
            // Verifica se a posição está dentro dos limites da lista pdfNames
            if (position >= 0 && position < pdfNames.size() && position < pdfImagePath.size() && position < pdfScrollPosition.size()) {
                // Obtém o nome do PDF
                String pdfName = pdfNames.get(position);
                holder.textViewPdfName.setText(pdfName);

                String imageFilePath = pdfImagePath.get(position);

                // Define o conteúdo do PDF (texto) e a imagem associada
                // Se imagePath for não nulo, carrega a imagem no ImageView
                if (imageFilePath != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
                    holder.imageViewPdf.setImageBitmap(bitmap);
                }

                // Defina a posição da SeekBar com base na posição de rolagem
                int scale = 5; // Arrumar scale (salvar)
                int progress = (int) (((float) pdfScrollPosition.get(position) / (pdfWebViewHeight.get(position)) * scale) * 100);

                holder.seekBar.setProgress(progress);
                // Desabilita o clique na SeekBar
                holder.seekBar.setClickable(false);
                holder.seekBar.setEnabled(false);

                // Define um ouvinte de clique para o item da lista
                holder.itemView.setOnClickListener(view -> {
                    if (onPdfClickListener != null) {
                        onPdfClickListener.onPdfClick(holder.getAdapterPosition());
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return pdfNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPdfName;
        ImageView imageViewPdf;
        SeekBar seekBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPdfName = itemView.findViewById(R.id.textViewPdfName);
            imageViewPdf = itemView.findViewById(R.id.imageViewPdf);
            seekBar = itemView.findViewById(R.id.seekBarMain);
        }
    }
}
