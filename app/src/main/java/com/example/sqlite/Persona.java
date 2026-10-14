package com.example.sqlite;


// Clase modelo: representa una persona dentro de la aplicación
//    Sirve como "molde" para los registros de la base de datos (tabla persona).
public class Persona {

    //  Atributos que corresponden exactamente con las columnas de la tabla "persona"
    protected String id;       // Identificador único (clave primaria autoincremental)
    protected String nombre;   // Nombre de la persona
    protected String apellido; // Apellido de la persona
    protected String edad;     // Edad (guardada como texto, pero puede ser numérica en la BD)

}