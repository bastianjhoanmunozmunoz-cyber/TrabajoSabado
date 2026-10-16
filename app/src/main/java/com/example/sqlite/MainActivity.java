package com.example.sqlite;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private DBHelper helper;
    private SessionStore session;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        helper = new DBHelper(this);
        helper.getWritableDatabase();
        session = new SessionStore(this);
        if (session.active() && refreshSession()) dashboard();
        else {
            session.clear();
            showLogin();
        }
    }

    private boolean refreshSession() {
        long id = session.id();
        if (id < 1) return false;
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT U.username,U.nombre_completo,U.activo,R.nombre_rol,R.activo " +
                        "FROM USUARIO U JOIN ROL R ON R.id_rol=U.id_rol WHERE U.id_usuario=?",
                new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst() || c.getInt(2) != 1 || c.getInt(4) != 1) return false;
            session.save(id, c.getString(0), c.getString(1), c.getString(3));
            return true;
        } finally {
            db.close();
        }
    }

    private void showLogin() {
        setContentView(R.layout.activity_main);
        EditText username = findViewById(R.id.etUsername);
        EditText password = findViewById(R.id.etPassword);
        Button login = findViewById(R.id.btnLogin);
        login.setOnClickListener(v -> authenticate(username, password));
    }

    private void authenticate(EditText username, EditText password) {
        String user = username.getText().toString().trim();
        String pass = password.getText().toString();
        if (user.isEmpty() || pass.isEmpty()) {
            toast("Ingresa tu usuario y contraseña.");
            return;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT U.id_usuario,U.username,U.nombre_completo,U.activo,U.password_hash," +
                        "R.nombre_rol,R.activo FROM USUARIO U JOIN ROL R ON R.id_rol=U.id_rol " +
                        "WHERE U.username=?",
                new String[]{user})) {
            if (!c.moveToFirst() || !DBHelper.verify(pass, c.getString(4))) {
                password.setText("");
                toast("Credenciales inválidas.");
                return;
            }
            if (c.getInt(3) != 1) {
                toast("Usuario deshabilitado. Contacta al administrador.");
                return;
            }
            if (c.getInt(6) != 1) {
                toast("El rol está deshabilitado.");
                return;
            }
            session.save(c.getLong(0), c.getString(1), c.getString(2), c.getString(5));
            dashboard();
        } finally {
            db.close();
        }
    }

    private void dashboard() {
        setContentView(R.layout.activity_dashboard);
        TextView welcome = findViewById(R.id.tvWelcome);
        String name = session.name();
        String role = session.role();
        welcome.setText(name.equalsIgnoreCase(role) ? role : name + " · " + role);

        Button inventory = findViewById(R.id.btnInventory);
        Button movements = findViewById(R.id.btnMovements);
        Button categories = findViewById(R.id.btnCategories);
        Button admin = findViewById(R.id.btnAdmin);
        Button logout = findViewById(R.id.btnLogout);

        inventory.setOnClickListener(v -> startActivity(new Intent(this, Leer.class)));
        movements.setOnClickListener(v -> startActivity(new Intent(this, MovimientosActivity.class)));
        categories.setOnClickListener(v -> startActivity(new Intent(this, CategoriasActivity.class)));
        admin.setVisibility(role.equalsIgnoreCase("Administrador") ? View.VISIBLE : View.GONE);
        admin.setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));
        logout.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Cerrar la sesión actual?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar", (dialog, which) -> {
                    session.clear();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }).show());
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
