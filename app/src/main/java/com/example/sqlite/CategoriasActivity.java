package com.example.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class CategoriasActivity extends AppCompatActivity {
    private DBHelper helper;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!new SessionStore(this).active()) { finish(); return; }
        helper = new DBHelper(this);
        setContentView(R.layout.activity_categories);
        ListView list = findViewById(R.id.listActiveCategories);
        list.setEmptyView(findViewById(R.id.tvNoCategories));
        ArrayList<String> rows = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT nombre_categoria,descripcion FROM CATEGORIA WHERE activo=1 ORDER BY nombre_categoria COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                String description = c.getString(1);
                rows.add(c.getString(0) + (description == null || description.trim().isEmpty() ? "" : "\n" + description));
            }
        } finally { db.close(); }
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
    }
    @Override protected void onDestroy() { if (helper != null) helper.close(); super.onDestroy(); }
}
