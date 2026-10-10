package dev.horsefrostwalker;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * Horse Frost Walker (Forge 1.20.1)
 *
 * Si el jinete de un caballo (o burro, mula, llama, camello...) lleva botas con
 * Paso helado, el caballo congela el agua igual que lo haria el jugador a pie.
 *
 * Usa directamente FrostWalkerEnchantment.onEntityMoved de vanilla, pasando al
 * caballo como entidad: mismo radio por nivel (2 + nivel), mismo hielo
 * escarchado y se respetan los eventos de colocacion de bloques de Forge.
 */
@Mod(HorseFrostWalker.MOD_ID)
@Mod.EventBusSubscriber(modid = HorseFrostWalker.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HorseFrostWalker {
    public static final String MOD_ID = "horsefrostwalker";

    /**
     * Ultima posicion de bloque procesada por cada jugador montado (vanilla solo
     * aplica Paso helado al cambiar de bloque). Solo se toca desde el hilo del servidor.
     */
    private static final Map<Player, BlockPos> LAST_POS = new WeakHashMap<>();

    public HorseFrostWalker() {
    }

    /**
     * Se escucha el tick de los jugadores (pocos) en vez del de todas las entidades
     * vivas del mundo, y se parte desde el jinete hacia su montura.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Primero el lado: el mapa no debe tocarse desde el hilo del cliente.
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        Player player = event.player;

        if (!(player.getVehicle() instanceof AbstractHorse horse)) {
            if (!LAST_POS.isEmpty()) LAST_POS.remove(player);
            return;
        }

        BlockPos pos = horse.blockPosition();
        if (pos.equals(LAST_POS.put(player, pos))) return; // sigue en el mismo bloque

        int enchLevel = frostWalkerLevel(player);
        if (enchLevel <= 0) return;

        // Camellos con dos jinetes: solo actua el primero que lleve Paso helado,
        // para no aplicar el efecto dos veces por bloque.
        for (Entity passenger : horse.getPassengers()) {
            if (passenger == player) break;
            if (passenger instanceof Player other && frostWalkerLevel(other) > 0) return;
        }

        // onEntityMoved ya comprueba que el caballo este en el suelo
        FrostWalkerEnchantment.onEntityMoved(horse, horse.level(), pos, enchLevel);
    }

    /** Extra: igual que el jugador con Paso helado, el caballo no se quema con bloques de magma. */
    @SubscribeEvent
    public static void onHorseAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (!event.getSource().is(DamageTypes.HOT_FLOOR)) return;
        if (!horse.isVehicle() || horse.level().isClientSide()) return;

        for (Entity passenger : horse.getPassengers()) {
            if (passenger instanceof Player rider && frostWalkerLevel(rider) > 0) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static int frostWalkerLevel(Player player) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.FROST_WALKER, player.getItemBySlot(EquipmentSlot.FEET));
    }
}
