package dev.strengthsmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrengthSMP implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("strengthsmp");
    public static final Identifier STRENGTH_MODIFIER_ID = Identifier.of("strengthsmp", "kill_strength");

    private static final Map<UUID, Integer> strengthMap = new HashMap<>();

    public static final int MAX_STRENGTH = 10;
    public static final int MIN_STRENGTH = -5;
    public static final double DAMAGE_PER_LEVEL = 1.0;

    @Override
    public void onInitialize() {
        LOGGER.info("Strength SMP loaded");

        StrengthPersistence.register();
        StrengthCommand.register();

        ServerLivingEntityEvents.AFTER_DEATH.register(this::onDeath);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            applyStrength(handler.player));
    }

    private void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayerEntity victim)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;
        if (killer == victim) return;

        int victimStrength = Math.max(getStrength(victim.getUuid()) - 1, MIN_STRENGTH);
        int killerStrength = Math.min(getStrength(killer.getUuid()) + 1, MAX_STRENGTH);

        setStrength(victim, victimStrength);
        setStrength(killer, killerStrength);

        killer.sendMessage(Text.literal("§a+1 Strength! §7[" + killerStrength + "]"), false);
        victim.sendMessage(Text.literal("§c-1 Strength! §7[" + victimStrength + "]"), false);

        killer.getServer().getPlayerManager().broadcast(
            Text.literal("§6☆ §e" + killer.getName().getString() +
                " §7killed §e" + victim.getName().getString() +
                " §8| §aStrength " + killerStrength +
                " §8→ §cStrength " + victimStrength),
            false
        );

        StrengthPersistence.save();
    }

    public static int getStrength(UUID uuid) {
        return strengthMap.getOrDefault(uuid, 0);
    }

    public static void setStrength(ServerPlayerEntity player, int level) {
        strengthMap.put(player.getUuid(), level);
        applyStrength(player);
    }

    public static void loadStrength(UUID uuid, int level) {
        strengthMap.put(uuid, level);
    }

    public static Map<UUID, Integer> getAllStrengths() {
        return Collections.unmodifiableMap(strengthMap);
    }

    public static void applyStrength(ServerPlayerEntity player) {
        var attr = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (attr == null) return;

        attr.removeModifier(STRENGTH_MODIFIER_ID);

        int level = getStrength(player.getUuid());
        if (level == 0) return;

        double bonus = level * DAMAGE_PER_LEVEL;
        attr.addPersistentModifier(new EntityAttributeModifier(
            STRENGTH_MODIFIER_ID,
            bonus,
            EntityAttributeModifier.Operation.ADD_VALUE
        ));
    }
}
