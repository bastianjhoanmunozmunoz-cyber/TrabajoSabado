package com.example.sqlite;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
 private DBHelper helper; private SessionStore session;
 @Override protected void onCreate(Bundle b){super.onCreate(b);helper=new DBHelper(this);helper.getWritableDatabase();session=new SessionStore(this);if(session.active()&&refreshSession())dashboard();else{session.clear();login();}}
 private boolean refreshSession(){long id=session.id();if(id<1)return false;SQLiteDatabase db=helper.getReadableDatabase();try(Cursor c=db.rawQuery("SELECT U.username,U.nombre_completo,U.activo,R.nombre_rol,R.activo FROM USUARIO U JOIN ROL R ON R.id_rol=U.id_rol WHERE U.id_usuario=?",new String[]{String.valueOf(id)})){if(!c.moveToFirst()||c.getInt(2)!=1||c.getInt(4)!=1)return false;session.save(id,c.getString(0),c.getString(1),c.getString(3));return true;}finally{db.close();}} private void login(){setContentView(R.layout.activity_main);EditText user=findViewById(R.id.etUsername),pass=findViewById(R.id.etPassword);Button enter=findViewById(R.id.btnLogin);enter.setOnClickListener(v->{String u=user.getText().toString().trim(),p=pass.getText().toString();if(u.isEmpty()||p.isEmpty()){toast("Ingresa tu usuario y contraseña.");return;}SQLiteDatabase db=helper.getReadableDatabase();try(Cursor c=db.rawQuery("SELECT U.id_usuario,U.username,U.nombre_completo,U.activo,U.password_hash,R.nombre_rol,R.activo FROM USUARIO U JOIN ROL R ON R.id_rol=U.id_rol WHERE U.username=?",new String[]{u})){if(!c.moveToFirst()||!DBHelper.verify(p,c.getString(4))){pass.setText("");toast("Credenciales inválidas.");return;}if(c.getInt(3)!=1){toast("Usuario deshabilitado. Contacta al administrador.");return;}if(c.getInt(6)!=1){toast("El rol está deshabilitado.");return;}session.save(c.getLong(0),c.getString(1),c.getString(2),c.getString(5));dashboard();}finally{db.close();}});}
 private void dashboard(){setContentView(R.layout.activity_dashboard);TextView welcome=findViewById(R.id.tvWelcome);String displayName=session.name();String displayRole=session.role();welcome.setText(displayName.equalsIgnoreCase(displayRole)?displayRole:displayName+" · "+displayRole);Button inventory=findViewById(R.id.btnInventory),movements=findViewById(R.id.btnMovements),admin=findViewById(R.id.btnAdmin),change=findViewById(R.id.btnChangePassword),logout=findViewById(R.id.btnLogout),categories=findViewById(R.id.btnCategories);inventory.setOnClickListener(v->startActivity(new Intent(this,Leer.class)));categories.setOnClickListener(v->startActivity(new Intent(this,CategoriasActivity.class)));movements.setOnClickListener(v->startActivity(new Intent(this,MovimientosActivity.class)));admin.setVisibility(session.role().equalsIgnoreCase("Administrador")?View.VISIBLE:View.GONE);admin.setOnClickListener(v->startActivity(new Intent(this,AdminActivity.class)));change.setOnClickListener(v->changePassword());logout.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Cerrar sesión").setMessage("¿Cerrar la sesión actual?").setNegativeButton("Cancelar",null).setPositiveButton("Cerrar",(d,w)->{session.clear();Intent intent=new Intent(this,MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(intent);}).show());}
 private EditText input(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);return e;}
 private void changePassword(){LinearLayout box=new LinearLayout(this);box.setOrientation(1);EditText old=input("Contraseña actual",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD),next=input("Nueva contraseña (mínimo 4 caracteres)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD),again=input("Repetir nueva contraseña",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);box.addView(old);box.addView(next);box.addView(again);new AlertDialog.Builder(this).setTitle("Cambiar contraseña").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{if(next.length()<4||!next.getText().toString().equals(again.getText().toString())){toast("La nueva contraseña debe tener 4 caracteres y coincidir.");return;}SQLiteDatabase db=helper.getWritableDatabase();try(Cursor c=db.rawQuery("SELECT password_hash FROM USUARIO WHERE id_usuario=?",new String[]{String.valueOf(session.id())})){if(!c.moveToFirst()||!DBHelper.verify(old.getText().toString(),c.getString(0))){toast("La contraseña actual no coincide.");return;}android.content.ContentValues values=new android.content.ContentValues();values.put("password_hash",DBHelper.hash(next.getText().toString()));db.update("USUARIO",values,"id_usuario=?",new String[]{String.valueOf(session.id())});toast("Contraseña actualizada.");}finally{db.close();}}).show();}
 private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){if(helper!=null)helper.close();super.onDestroy();}
}





