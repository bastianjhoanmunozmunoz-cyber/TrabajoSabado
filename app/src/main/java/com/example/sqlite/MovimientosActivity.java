package com.example.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MovimientosActivity extends AppCompatActivity {
 private DBHelper helper;private EditText sku,qty;private Spinner provider;private ArrayList<Long> providerIds;
 @Override protected void onCreate(Bundle b){super.onCreate(b);if(!new SessionStore(this).active()){finish();return;}helper=new DBHelper(this);setContentView(R.layout.activity_movements);sku=findViewById(R.id.etSku);qty=findViewById(R.id.etQuantity);provider=findViewById(R.id.spinnerProvider);loadProviders();findViewById(R.id.btnEntry).setOnClickListener(v->record("ENTRADA"));findViewById(R.id.btnExit).setOnClickListener(v->record("SALIDA"));findViewById(R.id.btnHistory).setOnClickListener(v->history());}
 private void loadProviders(){providerIds=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();providerIds.add(-1L);labels.add("Seleccionar proveedor");SQLiteDatabase db=helper.getReadableDatabase();try(Cursor c=db.rawQuery("SELECT id_proveedor,razon_social FROM PROVEEDOR WHERE activo=1 ORDER BY razon_social",null)){while(c.moveToNext()){providerIds.add(c.getLong(0));labels.add(c.getString(1));}}db.close();provider.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));}
 private void record(String type){String code=sku.getText().toString().trim();int count;try{count=Integer.parseInt(qty.getText().toString().trim());if(count<=0)throw new Exception();}catch(Exception e){toast("La cantidad debe ser un entero mayor que cero.");return;}long supplier=providerIds.get(provider.getSelectedItemPosition());if(type.equals("ENTRADA")&&supplier<0){toast("Selecciona un proveedor para la entrada.");return;}SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();try(Cursor c=db.rawQuery("SELECT id_producto,stock_actual FROM PRODUCTO WHERE sku_codigo=? AND activo=1",new String[]{code})){if(!c.moveToFirst()){toast("No hay producto activo con ese SKU.");return;}long id=c.getLong(0);int current=c.getInt(1);if(type.equals("SALIDA")&&count>current){toast("Stock insuficiente. Disponible: "+current);return;}int after=type.equals("ENTRADA")?current+count:current-count;android.content.ContentValues p=new android.content.ContentValues();p.put("stock_actual",after);db.update("PRODUCTO",p,"id_producto=?",new String[]{String.valueOf(id)});android.content.ContentValues m=new android.content.ContentValues();m.put("tipo",type);m.put("cantidad",count);m.put("id_producto",id);m.put("id_usuario",new SessionStore(this).id());if(type.equals("ENTRADA"))m.put("id_proveedor",supplier);db.insertOrThrow("MOVIMIENTO",null,m);db.setTransactionSuccessful();sku.setText("");qty.setText("");toast(type+" registrada. Stock nuevo: "+after);}catch(Exception e){toast("No se pudo guardar el movimiento.");}finally{db.endTransaction();db.close();}}
 private void history(){ArrayList<String> lines=new ArrayList<>();SQLiteDatabase db=helper.getReadableDatabase();try(Cursor c=db.rawQuery("SELECT M.tipo,M.cantidad,M.fecha,P.nombre,P.sku_codigo,COALESCE(V.razon_social,'—'),COALESCE(U.username,'—') FROM MOVIMIENTO M JOIN PRODUCTO P ON P.id_producto=M.id_producto LEFT JOIN PROVEEDOR V ON V.id_proveedor=M.id_proveedor LEFT JOIN USUARIO U ON U.id_usuario=M.id_usuario ORDER BY M.id_movimiento DESC LIMIT 50",null)){while(c.moveToNext())lines.add(c.getString(0)+" "+c.getInt(1)+" · "+c.getString(3)+" ("+c.getString(4)+") · "+c.getString(2)+" · "+c.getString(5)+" · "+c.getString(6));}db.close();if(lines.isEmpty()){toast("Aún no hay movimientos.");return;}new AlertDialog.Builder(this).setTitle("Historial reciente").setItems(lines.toArray(new String[0]),null).setPositiveButton("Cerrar",null).show();}
 private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}@Override protected void onDestroy(){helper.close();super.onDestroy();}
}

