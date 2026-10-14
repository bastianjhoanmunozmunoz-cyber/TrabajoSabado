package com.example.sqlite;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Editar extends AppCompatActivity {

    private EditText ed_id, ed_nombre_completo, ed_username;
    private Spinner spinner_rol;
    private Button btn_editar, btn_eliminar, btn_volver;
    private DBHelper helper;

    // Listas para manejar los IDs de los roles en el Spinner
    private ArrayList<Long> rolIds = new ArrayList<>();
    private ArrayList<String> rolNombres = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit); // Usa el layout actualizado de usuario

        helper = new DBHelper(this);

        // Enlazar vistas con los IDs del nuevo XML de edición de usuarios
        ed_id = findViewById(R.id.id);
        ed_nombre_completo = findViewById(R.id.et_nombre_completo);
        ed_username = findViewById(R.id.et_username);
        spinner_rol = findViewById(R.id.spinner_rol);

        btn_editar = findViewById(R.id.btn_editar);
        btn_eliminar = findViewById(R.id.btn_eliminar); // Funciona como botón de deshabilitar/baja lógica
        btn_volver = findViewById(R.id.btn_volver);

        // Cargar los roles activos en el Spinner
        cargarRolesEnSpinner();

        // Recuperar los datos enviados por Intent desde la lista de usuarios
        Intent i = getIntent();
        if (i != null) {
            String idStr = i.getStringExtra("id_usuario");
            String nombreStr = i.getStringExtra("nombre_completo");
            String userStr = i.getStringExtra("username");
            long idRolActual = i.getLongExtra("id_rol", -1);

            ed_id.setText(idStr != null ? idStr : "");
            ed_nombre_completo.setText(nombreStr != null ? nombreStr : "");
            ed_username.setText(userStr != null ? userStr : "");

            // Posicionar el Spinner en el rol actual del usuario
            if (idRolActual != -1) {
                for (int idx = 0; idx < rolIds.size(); idx++) {
                    if (rolIds.get(idx) == idRolActual) {
                        spinner_rol.setSelection(idx);
                        break;
                    }
                }
            }
        }

        // Configurar acciones de los botones
        btn_editar.setOnClickListener(v -> actualizarUsuario());
        btn_eliminar.setOnClickListener(v -> cambiarEstadoUsuario()); // Baja lógica
        btn_volver.setOnClickListener(v -> finish()); // Cierra la activity y vuelve atrás
    }

    private void cargarRolesEnSpinner() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id_rol, nombre_rol FROM ROL WHERE activo=1 ORDER BY nombre_rol", null)) {
            while (c.moveToNext()) {
                rolIds.add(c.getLong(0));
                rolNombres.add(c.getString(1));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar roles: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rolNombres);
        spinner_rol.setAdapter(adapter);
    }

    private void actualizarUsuario() {
        String idStr = ed_id.getText().toString().trim();
        String nombre = ed_nombre_completo.getText().toString().trim();
        String username = ed_username.getText().toString().trim();

        if (idStr.isEmpty() || nombre.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rolIds.isEmpty()) {
            Toast.makeText(this, "No hay roles disponibles.", Toast.LENGTH_SHORT).show();
            return;
        }

        long idRolSeleccionado = rolIds.get(spinner_rol.getSelectedItemPosition());

        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("nombre_completo", nombre);
            values.put("username", username);
            values.put("id_rol", idRolSeleccionado);

            int filas = db.update("USUARIO", values, "id_usuario = ?", new String[]{idStr});
            if (filas > 0) {
                Toast.makeText(this, "Usuario actualizado exitosamente.", Toast.LENGTH_SHORT).show();
                finish(); // Cierra y regresa
            } else {
                Toast.makeText(this, "No se pudo actualizar el registro.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error (posible usuario duplicado): " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.close();
        }
    }

    private void cambiarEstadoUsuario() {
        String idStr = ed_id.getText().toString().trim();
        if (idStr.isEmpty()) {
            Toast.makeText(this, "ID de usuario inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            // Realiza la baja lógica alternando o poniendo activo = 0
            db.execSQL("UPDATE USUARIO SET activo = CASE activo WHEN 1 THEN 0 ELSE 1 END WHERE id_usuario = ?", new Object[]{idStr});
            Toast.makeText(this, "Estado del usuario modificado (Baja lógica).", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cambiar estado: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.close();
        }
    }

    @Override
    protected void onDestroy() {
        if (helper != null) {
            helper.close();
        }
        super.onDestroy();
    }
}