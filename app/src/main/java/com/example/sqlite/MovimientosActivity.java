package com.example.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MovimientosActivity extends AppCompatActivity {
    private DBHelper helper;
    private EditText sku;
    private EditText quantity;
    private Spinner provider;
    private ArrayList<Long> providerIds;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!new SessionStore(this).active()) {
            finish();
            return;
        }
        helper = new DBHelper(this);
        setContentView(R.layout.activity_movements);
        sku = findViewById(R.id.etSku);
        quantity = findViewById(R.id.etQuantity);
        provider = findViewById(R.id.spinnerProvider);
        loadProviders();
        findViewById(R.id.btnEntry).setOnClickListener(v -> record("ENTRADA"));
        findViewById(R.id.btnExit).setOnClickListener(v -> record("SALIDA"));
    }

    private void loadProviders() {
        providerIds = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        providerIds.add(-1L);
        labels.add("Seleccionar proveedor");
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id_proveedor,razon_social FROM PROVEEDOR " +
                        "WHERE activo=1 ORDER BY razon_social COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                providerIds.add(c.getLong(0));
                labels.add(c.getString(1));
            }
        } finally {
            db.close();
        }
        provider.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void record(String type) {
        String code = sku.getText().toString().trim();
        int count;
        try {
            count = Integer.parseInt(quantity.getText().toString().trim());
            if (count <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            toast("La cantidad debe ser un entero mayor que cero.");
            return;
        }
        long supplierId = providerIds.get(provider.getSelectedItemPosition());
        if (type.equals("ENTRADA") && supplierId < 0) {
            toast("Selecciona un proveedor para la entrada.");
            return;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try (Cursor c = db.rawQuery(
                "SELECT id_producto,stock_actual FROM PRODUCTO " +
                        "WHERE sku_codigo=? AND activo=1", new String[]{code})) {
            if (!c.moveToFirst()) {
                toast("No hay producto activo con ese SKU.");
                return;
            }
            long productId = c.getLong(0);
            int currentStock = c.getInt(1);
            if (type.equals("SALIDA") && count > currentStock) {
                toast("Stock insuficiente. Disponible: " + currentStock);
                return;
            }
            int newStock = type.equals("ENTRADA")
                    ? currentStock + count : currentStock - count;

            android.content.ContentValues product = new android.content.ContentValues();
            product.put("stock_actual", newStock);
            db.update("PRODUCTO", product, "id_producto=?",
                    new String[]{String.valueOf(productId)});

            android.content.ContentValues movement = new android.content.ContentValues();
            movement.put("tipo", type);
            movement.put("cantidad", count);
            movement.put("id_producto", productId);
            movement.put("id_usuario", new SessionStore(this).id());
            if (type.equals("ENTRADA")) movement.put("id_proveedor", supplierId);
            db.insertOrThrow("MOVIMIENTO", null, movement);
            db.setTransactionSuccessful();

            sku.setText("");
            quantity.setText("");
            toast(type + " registrada. Stock nuevo: " + newStock);
        } catch (Exception e) {
            toast("No se pudo guardar el movimiento.");
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (helper != null) helper.close();
        super.onDestroy();
    }
}
