package com.example.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class Leer extends AppCompatActivity {
 private DBHelper helper; private EditText search; private Spinner category; private ListView list; private ArrayList<Long> productIds,categoryIds; private ArrayList<String> rows; private ArrayAdapter<String> adapter;
 @Override protected void onCreate(Bundle b){super.onCreate(b);if(!new SessionStore(this).active()){finish();return;}helper=new DBHelper(this);setContentView(R.layout.activity_inventory);search=findViewById(R.id.etSearch);category=findViewById(R.id.spinnerCategory);list=findViewById(R.id.listProducts);Button add=findViewById(R.id.btnAddProduct);rows=new ArrayList<>();productIds=new ArrayList<>();adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,rows);list.setAdapter(adapter);list.setEmptyView(findViewById(R.id.tvEmpty));loadCategories();category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){loadProducts();}});search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int before,int count){loadProducts();}public void afterTextChanged(Editable e){}});add.setOnClickListener(v->productForm(null));list.setOnItemClickListener((p,v,pos,id)->actions(productIds.get(pos)));}
 private void loadCategories(){categoryIds=new ArrayList<>();ArrayList<String> names=new ArrayList<>();categoryIds.add(-1L);names.add("Todas las categorías");SQLiteDatabase db=helper.getReadableDatabase();try(Cursor c=db.rawQuery("SELECT id_categoria,nombre_categoria FROM CATEGORIA WHERE activo=1 ORDER BY nombre_categoria",null)){while(c.moveToNext()){categoryIds.add(c.getLong(0));names.add(c.getString(1));}}db.close();category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));}
 private void loadProducts(){if(adapter==null||categoryIds==null)return;rows.clear();productIds.clear();long cat=categoryIds.get(Math.max(0,category.getSelectedItemPosition()));String q="%"+search.getText().toString().trim()+"%";String sql="SELECT P.id_producto,P.sku_codigo,P.nombre,P.stock_actual,P.stock_minimo,C.nombre_categoria FROM PRODUCTO P JOIN CATEGORIA C ON C.id_categoria=P.id_categoria WHERE P.activo=1 AND (P.nombre LIKE ? OR P.sku_codigo LIKE ?)"+(cat>0?" AND P.id_categoria=?":"")+" ORDER BY P.nombre COLLATE NOCASE";SQLiteDatabase db=helper.getReadableDatabase();try(Cursor c=cat>0?db.rawQuery(sql,new String[]{q,q,String.valueOf(cat)}):db.rawQuery(sql,new String[]{q,q})){while(c.moveToNext()){productIds.add(c.getLong(0));String low=c.getInt(3)<=c.getInt(4)?"  ⚠ STOCK BAJO":"";rows.add(c.getString(2)+" · SKU "+c.getString(1)+"\nStock "+c.getInt(3)+" / mínimo "+c.getInt(4)+" · "+c.getString(5)+low);}}db.close();adapter.notifyDataSetChanged();}
 private EditText field(String s,boolean number){EditText e=new EditText(this);e.setHint(s);if(number)e.setInputType(2);return e;}
 private void productForm(Long id){LinearLayout box=new LinearLayout(this);box.setOrientation(1);EditText sku=field("SKU único",false),name=field("Nombre",false),desc=field("Descripción",false),stock=field("Stock inicial",true),min=field("Stock mínimo",true);Spinner cat=new Spinner(this);box.addView(sku);box.addView(name);box.addView(desc);box.addView(stock);box.addView(min);box.addView(cat);ArrayList<Long> cats=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();SQLiteDatabase db=helper.getReadableDatabase();try(Cursor c=db.rawQuery("SELECT id_categoria,nombre_categoria FROM CATEGORIA WHERE activo=1 ORDER BY nombre_categoria",null)){while(c.moveToNext()){cats.add(c.getLong(0));labels.add(c.getString(1));}}db.close();cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));if(id!=null){sku.setVisibility(View.GONE);try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT sku_codigo,nombre,descripcion,stock_actual,stock_minimo,id_categoria FROM PRODUCTO WHERE id_producto=?",new String[]{String.valueOf(id)})){if(c.moveToFirst()){name.setText(c.getString(1));desc.setText(c.getString(2));stock.setText(c.getString(3));min.setText(c.getString(4));for(int i=0;i<cats.size();i++)if(cats.get(i)==c.getLong(5))cat.setSelection(i);}}}
  new AlertDialog.Builder(this).setTitle(id==null?"Registrar producto":"Editar producto").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{try{String n=name.getText().toString().trim();int st=stock.getText().toString().trim().isEmpty()?0:Integer.parseInt(stock.getText().toString()),mn=min.getText().toString().trim().isEmpty()?0:Integer.parseInt(min.getText().toString());if(n.isEmpty()||cats.isEmpty()||st<0||mn<0)throw new Exception();android.content.ContentValues v=new android.content.ContentValues();v.put("nombre",n);v.put("descripcion",desc.getText().toString().trim());v.put("stock_minimo",mn);v.put("id_categoria",cats.get(cat.getSelectedItemPosition()));SQLiteDatabase x=helper.getWritableDatabase();if(id==null){String code=sku.getText().toString().trim();if(code.isEmpty())throw new Exception();v.put("sku_codigo",code);v.put("stock_actual",st);x.insertOrThrow("PRODUCTO",null,v);}else{x.update("PRODUCTO",v,"id_producto=?",new String[]{String.valueOf(id)});}x.close();loadProducts();toast("Producto guardado.");}catch(Exception e){toast("Revisa los datos: SKU único, campos obligatorios y stock no negativo.");}}).show();}
 private void actions(long id){new AlertDialog.Builder(this).setTitle("Producto").setItems(new String[]{"Editar","Deshabilitar"},(d,w)->{if(w==0)productForm(id);else new AlertDialog.Builder(this).setMessage("Se ocultará del catálogo y se conservará el historial. ¿Continuar?").setNegativeButton("Cancelar",null).setPositiveButton("Deshabilitar",(a,b)->{SQLiteDatabase db=helper.getWritableDatabase();android.content.ContentValues v=new android.content.ContentValues();v.put("activo",0);db.update("PRODUCTO",v,"id_producto=?",new String[]{String.valueOf(id)});db.close();loadProducts();}).show();}).show();}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override protected void onResume(){super.onResume();if(adapter!=null){loadCategories();loadProducts();}}
 @Override protected void onDestroy(){helper.close();super.onDestroy();}
}


