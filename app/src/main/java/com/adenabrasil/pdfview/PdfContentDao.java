package com.adenabrasil.pdfview;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PdfContentDao {
    @Query("SELECT * FROM pdfcontent ORDER BY last_time_opened DESC")
    List<PdfContent> getAll();

    @Query("SELECT * FROM pdfcontent WHERE id = :id")
    PdfContent getById(long id);

    @Query("SELECT * FROM pdfcontent WHERE title = :title")
    PdfContent getByTitle(String title);

    @Insert
    void insert(PdfContent pdfContent);

    @Update
    void update(PdfContent pdfContent);

    @Delete
    void delete(PdfContent pdfContent);

    @Query("DELETE FROM pdfcontent WHERE title = :title")
    void deleteByName(String title);
}



