package dev.horsefrostwalker;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Horse Frost Walker (NeoForge 1.21.1)
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

    /** Ultima posicion de bloque procesada por caballo (igual que vanilla: solo al cambiar de bloque). */
    private static final Map<AbstractHorse, BlockPos> LAST_POS = new WeakHashMap<>();

    public HorseFrostWalker() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (!(horse.level() instanceof ServerLevel level)) return;

        Player rider = getRider(horse);
        if (rider == null || rider.isSpectator()) {
            LAST_POS.remove(horse);
            return;
        }

        BlockPos pos = horse.blockPosition();
        if (pos.equals(LAST_POS.get(horse))) return; // vanilla solo actua al cambiar de bloque
        LAST_POS.put(horse, pos);

        Holder<Enchantment> frostWalker = frostWalker(level);
        ItemStack boots = rider.getItemBySlot(EquipmentSlot.FEET);
        int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(frostWalker, boots);
        if (enchLevel <= 0) return;

        // Ejecuta los efectos de Paso helado con el caballo como entidad.
        // Las condiciones vanilla (estar en el suelo y no ir montado en nada)
        // se evaluan sobre el caballo, que si las cumple.
        EnchantedItemInUse item = new EnchantedItemInUse(boots, EquipmentSlot.FEET, rider);
        frostWalker.value().runLocationChangedEffects(level, enchLevel, item, horse);
    }

    /** Extra: igual que el jugador con Paso helado, el caballo no se quema con bloques de magma. */
    @SubscribeEvent
    public static void onHorseDamaged(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (!(horse.level() instanceof ServerLevel level)) return;
        if (!event.getSource().is(DamageTypes.HOT_FLOOR)) return;

        Player rider = getRider(horse);
        if (rider == null) return;

        int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(
                frostWalker(level), rider.getItemBySlot(EquipmentSlot.FEET));
        if (enchLevel > 0) {
            event.setCanceled(true);
        }
    }

    private static Player getRider(AbstractHorse horse) {
        LivingEntity controller = horse.getControllingPassenger();
        if (controller instanceof Player p) return p;
        // Burros/mulas/llamas sin silla: cualquier jugador montado cuenta igualmente
        for (var passenger : horse.getPassengers()) {
            if (passenger instanceof Player p) return p;
        }
        return null;
    }

    private static Holder<Enchantment> frostWalker(ServerLevel level) {
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FROST_WALKER);
    }
}
