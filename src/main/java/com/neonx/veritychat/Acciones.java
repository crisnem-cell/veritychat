package com.neonx.veritychat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lo UNICO que la IA puede activar. No son comandos libres: la IA elige un nombre de esta lista
 * y el mod ejecuta la funcion del datapack correspondiente.
 */
public final class Acciones {
    private Acciones() {
    }

    public static final Map<String, String> LISTA = new LinkedHashMap<>();

    static {
        // Minijuegos
        LISTA.put("minijuego_caceria", "mj:caceria");
        LISTA.put("minijuego_asesino", "mj:asesino");
        LISTA.put("minijuego_escape", "mj:escape");
        LISTA.put("minijuego_tesoro", "mj:tesoro");
        LISTA.put("minijuego_aleatorio", "mj:aleatorio");
        // Ruleta
        LISTA.put("ruleta", "ruleta:girar");
        // Desastres
        LISTA.put("desastre_tormenta", "cuenta:ev_tormenta");
        LISTA.put("desastre_meteoros", "cuenta:ev_meteoros");
        LISTA.put("desastre_plaga", "cuenta:ev_plaga");
        LISTA.put("desastre_oscuridad", "cuenta:ev_oscuridad");
        LISTA.put("desastre_terremoto", "cuenta:ev_terremoto");
        LISTA.put("desastre_hambruna", "cuenta:ev_hambruna");
        LISTA.put("desastre_esqueletos", "cuenta:ev_esqueletos");
        LISTA.put("desastre_creepers", "cuenta:ev_creepers");
        LISTA.put("desastre_tnt", "cuenta:ev_tnt");
        LISTA.put("desastre_aranas", "cuenta:ev_aranas");
        LISTA.put("desastre_phantoms", "cuenta:ev_phantoms");
        LISTA.put("desastre_brujas", "cuenta:ev_brujas");
        LISTA.put("desastre_niebla", "cuenta:ev_niebla");
        LISTA.put("desastre_gravedad", "cuenta:ev_gravedad");
        LISTA.put("desastre_acida", "cuenta:ev_acida");
        LISTA.put("desastre_nether", "cuenta:ev_nether");
        LISTA.put("desastre_jefe", "cuenta:ev_jefe");
        LISTA.put("desastre_maldicion", "cuenta:ev_maldicion");
        LISTA.put("desastre_calor", "cuenta:ev_calor");
        LISTA.put("desastre_apocalipsis", "cuenta:ev_apocalipsis");
        LISTA.put("evento_chiste", "cuenta:menor");
    }

    /** Devuelve la funcion del datapack, o null si el nombre no esta en la lista. */
    public static String resolver(String accion) {
        return accion == null ? null : LISTA.get(accion.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
