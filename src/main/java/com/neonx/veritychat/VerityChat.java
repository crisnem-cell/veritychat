package com.neonx.veritychat;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Mod SOLO DE SERVIDOR. Lee el chat y, si alguien nombra a Verity,
 * ejecuta una funcion del datapack (verity:chat/<tema>) como ese jugador.
 */
@Mod(VerityChat.MODID)
public class VerityChat {
    public static final String MODID = "veritychat";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Segundos minimos entre respuestas al mismo jugador. */
    private static final long COOLDOWN_MS = 4000L;

    /** Palabra para despertar a Verity: "verity" o "veri". */
    private static final Pattern DESPERTAR = Pattern.compile(" veri(ty)? ");

    /** tema (nombre de funcion) -> palabras clave. El orden = prioridad. */
    private static final Map<String, String[]> TEMAS = new LinkedHashMap<>();

    static {
        TEMAS.put("adios", new String[]{"adios", "chao", "chau", "bye", "me voy", "hasta luego", "nos vemos", "buenas noches", "hasta manana"});
        TEMAS.put("insulto", new String[]{"feo", "fea", "tonta", "tonto", "idiota", "estupida", "estupido", "odio", "mala", "malo", "callate", "inutil", "basura", "tarada", "tarado", "boba", "bobo"});
        TEMAS.put("amor", new String[]{"te quiero", "te amo", "linda", "lindo", "hermosa", "bonita", "guapa", "preciosa", "cariño", "carino", "mejor perrita", "buena perrita", "buena chica", "buena nina", "buen lobo", "eres la mejor", "te adoro", "eres genial"});
        TEMAS.put("gracias", new String[]{"gracias", "grax", "thanks", "thx", "te agradezco"});
        TEMAS.put("chiste", new String[]{"chiste", "broma", "chistoso", "cuentame", "hazme reir", "reir", "ríete", "riete", "chistes"});
        TEMAS.put("quien", new String[]{"quien eres", "que eres", "quien es verity", "que haces", "como te llamas", "que animal eres", "eres una ia", "eres real"});
        TEMAS.put("ayuda", new String[]{"ayuda", "ayudame", "socorro", "help", "auxilio"});
        TEMAS.put("hambre", new String[]{"hambre", "comer", "comida", "hambriento", "hambrienta", "hueso", "carne"});
        TEMAS.put("aburrido", new String[]{"aburrido", "aburrida", "aburro", "aburrimos", "aburre", "no hay nada que hacer"});
        TEMAS.put("desastre", new String[]{"desastre", "desastres", "evento", "eventos", "apocalipsis", "terremoto", "meteoros", "tormenta", "fin del mundo", "dias"});
        TEMAS.put("ven", new String[]{"ven", "vente", "ven aqui", "ven aca", "sigueme", "acercate", "ven conmigo", "ven para aca"});
        TEMAS.put("saludo", new String[]{"hola", "holi", "holis", "buenas", "hey", "ola", "saludos", "buenos dias", "que tal", "wenas", "alo"});
    }

    private final Map<UUID, Long> ultimaRespuesta = new HashMap<>();

    public VerityChat() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("VerityChat cargado: Verity ahora lee el chat.");
    }

    @SubscribeEvent
    public void alChatear(ServerChatEvent event) {
        ServerPlayer jugador = event.getPlayer();
        if (jugador == null) {
            return;
        }
        MinecraftServer server = jugador.getServer();
        if (server == null) {
            return;
        }

        String texto = normalizar(event.getRawText());
        if (!DESPERTAR.matcher(texto).find()) {
            return; // no la nombraron
        }

        long ahora = System.currentTimeMillis();
        Long antes = ultimaRespuesta.get(jugador.getUUID());
        if (antes != null && ahora - antes < COOLDOWN_MS) {
            return; // anti-spam
        }
        ultimaRespuesta.put(jugador.getUUID(), ahora);

        String tema = buscarTema(texto);
        final String uuid = jugador.getStringUUID();
        final String comando = "execute as " + uuid + " at @s"
                + " unless score #chatoff vy matches 1"
                + " if entity @e[type=wolf,tag=verity_mob]"
                + " run function verity:chat/" + tema;

        server.execute(() -> {
            CommandSourceStack fuente = server.createCommandSourceStack().withSuppressedOutput();
            server.getCommands().performPrefixedCommand(fuente, comando);
        });
    }

    private static String buscarTema(String textoNormalizado) {
        for (Map.Entry<String, String[]> e : TEMAS.entrySet()) {
            for (String clave : e.getValue()) {
                String limpia = normalizar(clave);
                if (textoNormalizado.contains(limpia)) {
                    return e.getKey();
                }
            }
        }
        return "otro";
    }

    /** minusculas, sin tildes ni signos, espacios a los lados: " hola verity ". */
    static String normalizar(String s) {
        String n = Normalizer.normalize(s.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+", "");
        n = n.replaceAll("(.)\\1{2,}", "$1");
        n = n.replaceAll("[^a-z0-9 ]", " ");
        n = n.replaceAll("\\s+", " ").trim();
        return " " + n + " ";
    }
}
