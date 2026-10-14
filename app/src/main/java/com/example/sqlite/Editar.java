package com.example.sqlite;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Editar extends AppCompatActivity {

    // Referencias a los campos y botones de la UI
    private EditText ed_nombre, ed_apellido, ed_edad, ed_id;
    private Button b_editar, b_eliminar, b_volver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit); // Carga el layout de edición

        // Enlaza vistas del layout
        ed_nombre = findViewById(R.id.et_nombre);
        ed_apellido = findViewById(R.id.et_apellido);
        ed_edad = findViewById(R.id.et_edad);
        ed_id = findViewById(R.id.id);

        b_editar = findViewById(R.id.btn_editar);
        b_eliminar = findViewById(R.id.btn_eliminar);
        b_volver = findViewById(R.id.btn_volver);

        // Recupera el Intent con los datos enviados desde la lista (Leer)
        Intent i = getIntent();

        // Extrae los datos de la persona seleccionada para precargar la UI
        String et_id = (i != null && i.getStringExtra("id") != null) ? i.getStringExtra("id") : "";
        String et_nombre = (i != null && i.getStringExtra("nombre") != null) ? i.getStringExtra("nombre") : "";
        String et_apellido = (i != null && i.getStringExtra("apellido") != null) ? i.getStringExtra("apellido") : "";
        String et_edad = (i != null && i.getStringExtra("edad") != null) ? i.getStringExtra("edad") : "";

        // Muestra los datos en los EditText para editar/eliminar
        ed_id.setText(et_id);
        ed_nombre.setText(et_nombre);
        ed_apellido.setText(et_apellido);
        ed_edad.setText(et_edad);

        // Acciones de botones: actualizar, eliminar y volver a la lista
        b_editar.setOnClickListener(view -> editar());
        b_eliminar.setOnClickListener(view -> eliminar());
        b_volver.setOnClickListener(view -> {
            // Vuelve a la Activity que lista los registros
            Intent go = new Intent(getApplicationContext(), Leer.class);
            startActivity(go);
        });
    }

    public void eliminar() {
        String id = ed_id.getText().toString().trim();
        if (id.isEmpty()) {
            Toast.makeText(this, "ID vacío. No se puede eliminar.", Toast.LENGTH_SHORT).show();
            return; // No seguimos si no hay ID
        }

        // Usar siempre DBHelper garantiza abrir la MISMA BD (nombre, versión y esquema)
        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = null;
        SQLiteStatement stmt = null;

        try {
            db = helper.getWritableDatabase(); // Abre BD en modo escritura
            String sql = "DELETE FROM persona WHERE id = ?"; // Sentencia parametrizada evita inyección SQL
            stmt = db.compileStatement(sql);

            // id es INTEGER en la tabla → bindLong (más correcto que bindString)
            stmt.bindLong(1, Long.parseLong(id));

            int filas = stmt.executeUpdateDelete(); // devuelve filas afectadas (0 si no encontró el ID)
            if (filas > 0) {
                Toast.makeText(this, "Registro eliminado (id " + id + ").", Toast.LENGTH_LONG).show();

                // Limpia la UI para evitar mostrar datos que ya no existen
                ed_nombre.setText("");
                ed_apellido.setText("");
                ed_edad.setText("");

                // Opcional: volver a la lista y cerrar esta pantalla
                startActivity(new Intent(getApplicationContext(), Leer.class));
                finish();
            } else {
                Toast.makeText(this, "No se encontró el ID " + id + ".", Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException nfe) {
            // Captura conversión de String a Long inválida para el ID
            Toast.makeText(this, "ID inválido.", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            // Cubre cualquier otro error (bloqueo, esquema, etc.)
            Toast.makeText(this, "Error al eliminar: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            // Cierra recursos para evitar fugas
            if (stmt != null) stmt.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    public void editar() {
        // Lee valores de la UI y valida campos
        String nombre = ed_nombre.getText().toString().trim();
        String apellido = ed_apellido.getText().toString().trim();
        String edadStr = ed_edad.getText().toString().trim();
        String idStr = ed_id.getText().toString().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || edadStr.isEmpty() || idStr.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos.", Toast.LENGTH_SHORT).show();
            return; // No continuar si faltan datos
        }

        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = null;
        SQLiteStatement stmt = null;

        try {
            // Conversión y validaciones mínimas
            int edad = Integer.parseInt(edadStr);
            long id = Long.parseLong(idStr);

            db = helper.getWritableDatabase(); // BD en modo escritura
            String sql = "UPDATE persona SET nombre = ?, apellido = ?, edad = ? WHERE id = ?";
            stmt = db.compileStatement(sql);

            // Orden de parámetros debe coincidir con la sentencia SQL
            stmt.bindString(1, nombre);
            stmt.bindString(2, apellido);
            stmt.bindLong(3, edad);
            stmt.bindLong(4, id);

            int filas = stmt.executeUpdateDelete(); // cuántas filas se actualizaron
            if (filas > 0) {
                Toast.makeText(this, "Datos actualizados.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No se encontró el ID " + id + ".", Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException nfe) {
            // Edad o ID no numéricos
            Toast.makeText(this, "Edad/ID inválidos.", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Error al actualizar: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            // Cierra statement y base de datos
            if (stmt != null) stmt.close();
            if (db != null && db.isOpen()) db.close();
        }
    }
}