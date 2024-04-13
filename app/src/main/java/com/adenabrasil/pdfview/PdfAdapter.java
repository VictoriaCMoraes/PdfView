package com.adenabrasil.pdfview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import android.net.Uri;
import android.widget.Toast;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.ViewHolder> {

    private Context context;
    private List<String> pdfNames;
    private List<Uri> pdfUris;
    private OnPdfClickListener onPdfClickListener;

    public PdfAdapter(Context context, List<String> pdfNames, List<Uri> pdfUris) {
        this.context = context;
        this.pdfNames = pdfNames;
        this.pdfUris = pdfUris;
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
        // Verifica se a posição está dentro dos limites da lista pdfNames
        if (position >= 0 && position < pdfNames.size()) {
            // Obtém o nome do PDF
            String pdfName = pdfNames.get(position);
            holder.textViewPdfName.setText(pdfName);

            // Define um ouvinte de clique para o item da lista
            holder.itemView.setOnClickListener(view -> {
                if (onPdfClickListener != null) {
                    onPdfClickListener.onPdfClick(holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return pdfNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPdfName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPdfName = itemView.findViewById(R.id.textViewPdfName);
        }
    }
}
