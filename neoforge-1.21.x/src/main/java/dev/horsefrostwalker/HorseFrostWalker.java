package dev.horsefrostwalker;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Horse Frost Walker (NeoForge 1.21 / 1.21.1)
 *
 * Si el jinete de un caballo (o burro, mula, llama, camello...) lleva botas con
 * Paso helado, el caballo congela el agua igual que lo haria el jugador a pie.
 *
 * En 1.21 los encantamientos son data-driven: en lugar de copiar la logica del
 * hielo, ejecutamos los efectos "location_changed" del propio encantamiento
 * Paso helado, pero usando al caballo como entidad. Asi el radio por nivel,
 * el bloque generado y los cambios de datapacks se respetan automaticamente.
 */
@Mod(HorseFrostWalker.MOD_ID)
@EventBusSubscriber(modid = HorseFrostWalker.MOD_ID)
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
     * del mundo (pueden ser miles), y se parte desde el jinete hacia su montura.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Primero el lado: el mapa no debe tocarse desde el hilo del cliente.
        if (!(player.level() instanceof ServerLevel level)) return;

        if (!(player.getVehicle() instanceof AbstractHorse horse)) {
            if (!LAST_POS.isEmpty()) LAST_POS.remove(player);
            return;
        }

        BlockPos pos = horse.blockPosition();
        if (pos.equals(LAST_POS.put(player, pos))) return; // sigue en el mismo bloque

        Holder<Enchantment> frostWalker = frostWalker(level);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(frostWalker, boots);
        if (enchLevel <= 0) return;

        // Camellos con dos jinetes: solo actua el primero que lleve Paso helado,
        // para no aplicar el efecto dos veces por bloque.
        for (Entity passenger : horse.getPassengers()) {
            if (passenger == player) break;
            if (passenger instanceof Player other && hasFrostWalker(other, frostWalker)) return;
        }

        // Ejecuta los efectos de Paso helado con el caballo como entidad.
        // Las condiciones vanilla (estar en el suelo y no ir montado en nada)
        // se evaluan sobre el caballo, que si las cumple.
        EnchantedItemInUse item = new EnchantedItemInUse(boots, EquipmentSlot.FEET, player);
        frostWalker.value().runLocationChangedEffects(level, enchLevel, item, horse);
    }

    /**
     * Extra: igual que el jugador con Paso helado, el caballo no se quema al pisar
     * bloques de magma ni fogatas (mismo tag de dano que usa vanilla).
     */
    @SubscribeEvent
    public static void onHorseDamaged(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (!event.getSource().is(DamageTypeTags.BURN_FROM_STEPPING)) return;
        if (!horse.isVehicle() || !(horse.level() instanceof ServerLevel level)) return;

        Holder<Enchantment> frostWalker = frostWalker(level);
        for (Entity passenger : horse.getPassengers()) {
            if (passenger instanceof Player rider && hasFrostWalker(rider, frostWalker)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static boolean hasFrostWalker(Player player, Holder<Enchantment> frostWalker) {
        return EnchantmentHelper.getItemEnchantmentLevel(frostWalker, player.getItemBySlot(EquipmentSlot.FEET)) > 0;
    }

    private static Holder<Enchantment> frostWalker(ServerLevel level) {
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FROST_WALKER);
    }
}
