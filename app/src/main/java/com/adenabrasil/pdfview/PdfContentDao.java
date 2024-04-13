package com.adenabrasil.pdfview;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PdfContentDao {
    @Query("SELECT * FROM pdfcontent")
    List<PdfContent> getAll();

    @Insert
    void insert(PdfContent pdfContent);

    @Delete
    void delete(PdfContent pdfContent);
}

