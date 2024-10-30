package com.adenabrasil.pdfview;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PdfContent {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "image_path")
    public String imagePath;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "scroll_position")
    public int scrollPosition;

    @ColumnInfo(name = "progress")
    public int progress;

    @ColumnInfo(name = "last_time_opened")
    public Long lastTimeOpened;
}