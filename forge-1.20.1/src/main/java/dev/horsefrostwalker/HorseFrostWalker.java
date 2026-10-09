package dev.horsefrostwalker;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

    /** Ultima posicion de bloque procesada por caballo (igual que vanilla: solo al cambiar de bloque). */
    private static final Map<AbstractHorse, BlockPos> LAST_POS = new WeakHashMap<>();

    public HorseFrostWalker() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (horse.level().isClientSide()) return;

        Player rider = getRider(horse);
        if (rider == null || rider.isSpectator()) {
            LAST_POS.remove(horse);
            return;
        }

        BlockPos pos = horse.blockPosition();
        if (pos.equals(LAST_POS.get(horse))) return; // vanilla solo actua al cambiar de bloque
        LAST_POS.put(horse, pos);

        int enchLevel = frostWalkerLevel(rider);
        if (enchLevel > 0) {
            // onEntityMoved ya comprueba que el caballo este en el suelo
            FrostWalkerEnchantment.onEntityMoved(horse, horse.level(), pos, enchLevel);
        }
    }

    /** Extra: igual que el jugador con Paso helado, el caballo no se quema con bloques de magma. */
    @SubscribeEvent
    public static void onHorseAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (horse.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypes.HOT_FLOOR)) return;

        Player rider = getRider(horse);
        if (rider != null && frostWalkerLevel(rider) > 0) {
            event.setCanceled(true);
        }
    }

    private static int frostWalkerLevel(Player rider) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.FROST_WALKER, rider.getItemBySlot(EquipmentSlot.FEET));
    }

    private static Player getRider(AbstractHorse horse) {
        LivingEntity controller = horse.getControllingPassenger();
        if (controller instanceof Player p) return p;
        // Burros/mulas/llamas sin silla: cualquier jugador montado cuenta igualmente
        for (Entity passenger : horse.getPassengers()) {
            if (passenger instanceof Player p) return p;
        }
        return null;
    }
}
