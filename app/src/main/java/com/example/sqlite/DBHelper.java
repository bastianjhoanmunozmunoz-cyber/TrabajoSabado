package com.example.sqlite;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public class DBHelper extends SQLiteOpenHelper {
 private static final String DB_NAME="BD_BODEGA.db"; private static final int DB_VERSION=2;
 public DBHelper(Context c){super(c,DB_NAME,null,DB_VERSION);}
 @Override public void onConfigure(SQLiteDatabase db){super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);}
 @Override public void onCreate(SQLiteDatabase db){schema(db);seed(db);}
 private void schema(SQLiteDatabase db){
  db.execSQL("CREATE TABLE IF NOT EXISTS persona (id INTEGER PRIMARY KEY AUTOINCREMENT,nombre TEXT NOT NULL,apellido TEXT NOT NULL,edad INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE IF NOT EXISTS ROL (id_rol INTEGER PRIMARY KEY AUTOINCREMENT,nombre_rol TEXT NOT NULL UNIQUE,descripcion TEXT NOT NULL DEFAULT '',activo INTEGER NOT NULL DEFAULT 1)");
  db.execSQL("CREATE TABLE IF NOT EXISTS USUARIO (id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,username TEXT NOT NULL UNIQUE,password_hash TEXT NOT NULL,nombre_completo TEXT NOT NULL,id_rol INTEGER NOT NULL,activo INTEGER NOT NULL DEFAULT 1,FOREIGN KEY(id_rol) REFERENCES ROL(id_rol))");
  db.execSQL("CREATE TABLE IF NOT EXISTS CATEGORIA (id_categoria INTEGER PRIMARY KEY AUTOINCREMENT,nombre_categoria TEXT NOT NULL UNIQUE,descripcion TEXT NOT NULL DEFAULT '',activo INTEGER NOT NULL DEFAULT 1)");
  db.execSQL("CREATE TABLE IF NOT EXISTS PROVEEDOR (id_proveedor INTEGER PRIMARY KEY AUTOINCREMENT,rut_empresa TEXT NOT NULL UNIQUE,razon_social TEXT NOT NULL,contacto TEXT NOT NULL,activo INTEGER NOT NULL DEFAULT 1)");
  db.execSQL("CREATE TABLE IF NOT EXISTS PRODUCTO (id_producto INTEGER PRIMARY KEY AUTOINCREMENT,sku_codigo TEXT NOT NULL UNIQUE,nombre TEXT NOT NULL,descripcion TEXT NOT NULL DEFAULT '',stock_actual INTEGER NOT NULL DEFAULT 0 CHECK(stock_actual>=0),stock_minimo INTEGER NOT NULL DEFAULT 0 CHECK(stock_minimo>=0),id_categoria INTEGER NOT NULL,activo INTEGER NOT NULL DEFAULT 1,FOREIGN KEY(id_categoria) REFERENCES CATEGORIA(id_categoria))");
  db.execSQL("CREATE TABLE IF NOT EXISTS MOVIMIENTO (id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,tipo TEXT NOT NULL CHECK(tipo IN ('ENTRADA','SALIDA')),cantidad INTEGER NOT NULL CHECK(cantidad>0),fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,id_producto INTEGER NOT NULL,id_usuario INTEGER,id_proveedor INTEGER,nota TEXT NOT NULL DEFAULT '',FOREIGN KEY(id_producto) REFERENCES PRODUCTO(id_producto),FOREIGN KEY(id_usuario) REFERENCES USUARIO(id_usuario),FOREIGN KEY(id_proveedor) REFERENCES PROVEEDOR(id_proveedor))");
 }
 private void seed(SQLiteDatabase db){db.execSQL("INSERT OR IGNORE INTO ROL(nombre_rol,descripcion) VALUES('Administrador','Acceso completo'),('Operador','Operación de bodega')");long role=scalar(db,"SELECT id_rol FROM ROL WHERE nombre_rol='Administrador'");ContentValues u=new ContentValues();u.put("username","admin");u.put("password_hash",hash("admin123"));u.put("nombre_completo","Administrador");u.put("id_rol",role);db.insertWithOnConflict("USUARIO",null,u,SQLiteDatabase.CONFLICT_IGNORE);db.execSQL("INSERT OR IGNORE INTO CATEGORIA(nombre_categoria,descripcion) VALUES('General','Categoría inicial')");}
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){schema(db);seed(db);}
 public static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}}
 public static String hash(String value){try{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);byte[] key=derive(value,salt);return Base64.encodeToString(salt,Base64.NO_WRAP)+":"+Base64.encodeToString(key,Base64.NO_WRAP);}catch(Exception e){throw new IllegalStateException(e);}}
 public static boolean verify(String value,String stored){try{String[] p=stored.split(":");return p.length==2&&MessageDigest.isEqual(Base64.decode(p[1],Base64.NO_WRAP),derive(value,Base64.decode(p[0],Base64.NO_WRAP)));}catch(Exception e){return false;}}
 private static byte[] derive(String value,byte[] salt)throws Exception{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(value.toCharArray(),salt,120000,256)).getEncoded();}
}
