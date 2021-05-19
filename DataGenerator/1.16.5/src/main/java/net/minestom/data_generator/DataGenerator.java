package net.minestom.data_generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MaterialColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.minestom.data_generator.JsonGenerator.GSON;

public final class DataGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataGenerator.class);
    private static final Map<Block, String> blockNames = new HashMap<>();
    private static final Map<Property<?>, String> blockPropertyNames = new HashMap<>();
    private static final Map<EntityType<?>, String> entityNames = new HashMap<>();
    private static final Map<EntityType<?>, Class<?>> entityClasses = new HashMap<>();
    private static final Map<BlockEntityType<?>, String> blockEntityNames = new HashMap<>();
    private static final Map<Fluid, String> fluidNames = new HashMap<>();
    private static final Map<Item, String> itemNames = new HashMap<>();
    private static final Map<MobEffect, String> effectNames = new HashMap<>();
    private static final Map<Potion, String> potionNames = new HashMap<>();
    private static final Map<Attribute, String> attributeNames = new HashMap<>();
    private static final Map<Enchantment, String> enchantmentNames = new HashMap<>();
    private static final Map<ParticleType<?>, String> particleNames = new HashMap<>();
    private static final Map<SoundEvent, String> soundNames = new HashMap<>();
    private static final Map<VillagerProfession, String> villagerProfessionNames = new HashMap<>();
    private static final Map<VillagerType, String> villagerTypeNames = new HashMap<>();
    private static final Map<ResourceLocation, String> customStatisticNames = new HashMap<>();
    private static JsonGenerator jsonGenerator;

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.info("You must specify a version to generate data for.");
            return;
        }
        // version for the output.
        String version = args[0];

        // Folder for the output.
        File outputFolder = new File("../output/");
        if (args.length >= 2) {
            outputFolder = new File(args[1]);
        }
        jsonGenerator = new JsonGenerator(version, outputFolder);

        // Bootstrap minecraft
        Bootstrap.bootStrap();

        // Extra mapping for names
        {
            // Blocks
            for (Field declaredField : Blocks.class.getDeclaredFields()) {
                if (!Block.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    Block b = (Block) declaredField.get(null);
                    blockNames.put(b, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map block naming system.", e);
                    return;
                }
            }
            for (Field declaredField : BlockStateProperties.class.getDeclaredFields()) {
                if (!Property.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    Property<?> b = (Property<?>) declaredField.get(null);
                    blockPropertyNames.put(b, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map block state property naming system.", e);
                    return;
                }
            }
            // Fluids
            for (Field declaredField : Fluids.class.getDeclaredFields()) {
                if (!Fluid.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    Fluid f = (Fluid) declaredField.get(null);
                    fluidNames.put(f, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map fluid naming system.", e);
                    return;
                }
            }
            // Entities
            for (Field declaredField : EntityType.class.getDeclaredFields()) {
                if (!EntityType.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    EntityType<?> et = (EntityType<?>) declaredField.get(null);
                    // FIeld name
                    entityNames.put(et, declaredField.getName());
                    entityClasses.put(et, (Class<?>) ((ParameterizedType) declaredField.getGenericType()).getActualTypeArguments()[0]);
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map entity naming system.", e);
                    return;
                }
            }
            // BlockEntities
            for (Field declaredField : BlockEntityType.class.getDeclaredFields()) {
                if (!BlockEntityType.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    BlockEntityType<?> bet = (BlockEntityType<?>) declaredField.get(null);
                    blockEntityNames.put(bet, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map block entity naming system.", e);
                    return;
                }
            }
            // Items
            for (Field declaredField : Items.class.getDeclaredFields()) {
                if (!Item.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    Item i = (Item) declaredField.get(null);
                    itemNames.put(i, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map item naming system.", e);
                    return;
                }
            }
            // Effects
            for (Field declaredField : MobEffects.class.getDeclaredFields()) {
                if (!MobEffect.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    MobEffect me = (MobEffect) declaredField.get(null);
                    effectNames.put(me, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map effect naming system.", e);
                    return;
                }
            }
            // Potion Types
            for (Field declaredField : Potions.class.getDeclaredFields()) {
                if (!Potion.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    Potion p = (Potion) declaredField.get(null);
                    potionNames.put(p, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map potion naming system.", e);
                    return;
                }
            }
            // Attributes
            for (Field declaredField : Attributes.class.getDeclaredFields()) {
                if (!Attribute.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    Attribute a = (Attribute) declaredField.get(null);
                    attributeNames.put(a, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map attribute naming system.", e);
                    return;
                }
            }
            // Enchantments
            for (Field declaredField : Enchantments.class.getDeclaredFields()) {
                if (!Enchantment.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    Enchantment e = (Enchantment) declaredField.get(null);
                    enchantmentNames.put(e, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map enchantment naming system", e);
                    return;
                }
            }
            // Particles
            for (Field declaredField : ParticleTypes.class.getDeclaredFields()) {
                if (!ParticleType.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    ParticleType<?> pt = (ParticleType<?>) declaredField.get(null);
                    particleNames.put(pt, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map particle naming system", e);
                    return;
                }
            }
            // Sounds
            for (Field declaredField : SoundEvents.class.getDeclaredFields()) {
                if (!SoundEvent.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    SoundEvent se = (SoundEvent) declaredField.get(null);
                    soundNames.put(se, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map sound naming system", e);
                    return;
                }
            }
            // Villager Professions
            for (Field declaredField : VillagerProfession.class.getDeclaredFields()) {
                if (!VillagerProfession.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    VillagerProfession vp = (VillagerProfession) declaredField.get(null);
                    villagerProfessionNames.put(vp, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map villager profession naming system", e);
                    return;
                }
            }
            // Villager Types
            for (Field declaredField : VillagerType.class.getDeclaredFields()) {
                if (!VillagerType.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    VillagerType vt = (VillagerType) declaredField.get(null);
                    villagerTypeNames.put(vt, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map villager type naming system", e);
                    return;
                }
            }
            // Statistics
            for (Field declaredField : Stats.class.getDeclaredFields()) {
                if (!ResourceLocation.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                try {
                    ResourceLocation rl = (ResourceLocation) declaredField.get(null);
                    customStatisticNames.put(rl, declaredField.getName());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to map custom statistics naming system", e);
                    return;
                }
            }
        }

        generateBlocks();
        generateBlockProperties();
        generateFluids();
        generateEntities();
        generateBlockEntities();
        generateItems();
        generateEffects();
        generatePotions();
        generateAttributes();
        generateEnchantments();
        generateParticles();
        generateSounds();
        generateBiomes();
        generateVillagerProfessions();
        generateVillagerTypes();
        generateDimensionTypes();
        generateCustomStatistics();
        generateMapColors();
        // Create a temp file, run Mojang's data generator and "recompile" that data.
        File tempDirFile;
        try {
            tempDirFile = Files.createTempDirectory(version.replaceAll("\\.", "_") + "_gen_data").toFile();
            Main.main(new String[]{
                    "--all",
                    "--output=" + tempDirFile
            });
            // Delete tempFile when finished
            tempDirFile.deleteOnExit();
        } catch (IOException e) {
            LOGGER.error("Something went wrong while running Mojang's data generator.", e);
            return;
        }

        // Points to data/minecraft
        File dataFolder = new File(tempDirFile, "data" + File.separator + "minecraft");
        generateTags(new File(dataFolder, "tags"));
        generateLootTables(new File(dataFolder, "loot_tables"));


        LOGGER.info("Output data in: " + outputFolder.getAbsolutePath());
    }

    private static void generateBlocks() {
        Set<ResourceLocation> blockRLs = Registry.BLOCK.keySet();
        JsonArray blocks = new JsonArray();
        for (ResourceLocation blockRL : blockRLs) {
            Block b = Registry.BLOCK.get(blockRL);

            JsonObject block = new JsonObject();
            block.addProperty("id", blockRL.toString());
            block.addProperty("numericalID", Registry.BLOCK.getId(b));
            block.addProperty("name", blockNames.get(b));
            // block.addProperty("langId", b.getDescriptionId());
            block.addProperty("explosionResistance", b.getExplosionResistance());
            block.addProperty("friction", b.getFriction());
            block.addProperty("speedFactor", b.getSpeedFactor());
            block.addProperty("jumpFactor", b.getJumpFactor());
            block.addProperty("defaultBlockState", Block.BLOCK_STATE_REGISTRY.getId(b.defaultBlockState()));
            block.addProperty("itemId", Registry.ITEM.getKey(Item.BY_BLOCK.getOrDefault(b, Items.AIR)).toString());
            block.addProperty("blockEntity", b.isEntityBlock());
            // Block proprties
            {
                JsonArray properties = new JsonArray();
                for (Property<?> p : b.getStateDefinition().getProperties()) {
                    properties.add(blockPropertyNames.get(p));
                }
                block.add("properties", properties);
            }
            {
                // Block states
                JsonArray blockStates = new JsonArray();
                for (BlockState bs : b.getStateDefinition().getPossibleStates()) {
                    JsonObject state = new JsonObject();
                    state.addProperty("id", Block.BLOCK_STATE_REGISTRY.getId(bs));
                    state.addProperty("destroySpeed", bs.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
                    state.addProperty("lightEmission", bs.getLightEmission());
                    state.addProperty("doesOcclude", bs.canOcclude());

                    // Properties
                    {
                        JsonObject properties = new JsonObject();
                        for (Map.Entry<Property<?>, Comparable<?>> entry : bs.getValues().entrySet()) {
                            properties.addProperty(entry.getKey().getName(), String.valueOf(entry.getValue()));
                        }
                        state.add("properties", properties);
                    }

                    state.addProperty("pushReaction", bs.getMaterial().getPushReaction().toString());
                    state.addProperty("blocksMotion", bs.getMaterial().blocksMotion());
                    state.addProperty("isFlammable", bs.getMaterial().isFlammable());
                    state.addProperty("isAir", bs.isAir());
                    state.addProperty("isLiquid", bs.getMaterial().isLiquid());
                    state.addProperty("isReplaceable", bs.getMaterial().isReplaceable());
                    state.addProperty("isSolid", bs.getMaterial().isSolid());
                    state.addProperty("isSolidBlocking", bs.getMaterial().isSolidBlocking());
                    state.addProperty("mapColorId", bs.getMaterial().getColor().id);
                    state.addProperty("boundingBox", bs.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().toString());
                    blockStates.add(state);
                }
                block.add("states", blockStates);
            }

            blocks.add(block);
        }
        jsonGenerator.output(blocks, "blocks");
    }

    private static void generateBlockProperties() {
        JsonArray blockProperties = new JsonArray();
        // Block Properties
        for (Field declaredField : BlockStateProperties.class.getDeclaredFields()) {
            if (!Property.class.isAssignableFrom(declaredField.getType())) {
                continue;
            }
            JsonObject property = new JsonObject();
            try {
                Property<?> p = (Property<?>) declaredField.get(null);
                String fieldName = declaredField.getName();
                String propertyKey = p.getName();
                property.addProperty("name", fieldName);
                property.addProperty("key", propertyKey);
                // Properties
                JsonArray values = new JsonArray();
                if (p instanceof BooleanProperty) {
                    for (boolean possibleValue : ((BooleanProperty) p).getPossibleValues()) {
                        values.add(possibleValue);
                    }
                } else if (p instanceof IntegerProperty) {
                    for (int possibleValue : ((IntegerProperty) p).getPossibleValues()) {
                        values.add(possibleValue);
                    }
                } else if (p instanceof EnumProperty) {
                    for (Enum<? extends Enum<?>> possibleValue : ((EnumProperty<? extends Enum<?>>) p).getPossibleValues()) {
                        values.add(possibleValue.name());
                    }
                }
                property.add("values", values);
                blockProperties.add(property);
            } catch (IllegalAccessException e) {
                LOGGER.error("Failed to get property from the property mapping system.", e);
                return;
            }
        }
        jsonGenerator.output(blockProperties, "block_properties");
    }

    private static void generateFluids() {
        Set<ResourceLocation> fluidRLs = Registry.FLUID.keySet();
        JsonArray fluids = new JsonArray();

        for (ResourceLocation fluidRL : fluidRLs) {
            Fluid f = Registry.FLUID.get(fluidRL);

            JsonObject fluid = new JsonObject();
            fluid.addProperty("id", fluidRL.toString());
            fluid.addProperty("name", fluidNames.get(f));
            fluid.addProperty("bucketId", Registry.ITEM.getKey(f.getBucket()).toString());

            fluids.add(fluid);
        }
        jsonGenerator.output(fluids, "fluids");
    }

    private static void generateEntities() {
        Set<ResourceLocation> entityRLs = Registry.ENTITY_TYPE.keySet();
        JsonArray entities = new JsonArray();

        for (ResourceLocation entityRL : entityRLs) {
            EntityType<?> et = Registry.ENTITY_TYPE.get(entityRL);

            // Complicated but we need to get the Entity class of EntityType.
            // E.g. EntityType<T> we need to get T and check what classes T implements.

            Class<?> entityClass = entityClasses.get(et);
            String packetType;
            if (Player.class.isAssignableFrom(entityClass)) {
                packetType = "PLAYER";
            } else if (LivingEntity.class.isAssignableFrom(entityClass)) {
                packetType = "LIVING";
            } else if (Painting.class.isAssignableFrom(entityClass)) {
                packetType = "PAINTING";
            } else if (ExperienceOrb.class.isAssignableFrom(entityClass)) {
                packetType = "EXPERIENCE_ORB";
            } else {
                packetType = "BASE";
            }

            JsonObject entity = new JsonObject();
            entity.addProperty("id", entityRL.toString());
            entity.addProperty("name", entityNames.get(et));
            // entity.addProperty("langId", et.getDescriptionId());
            // entity.addProperty("category", et.getCategory().toString()); basically useless
            entity.addProperty("packetType", packetType);
            entity.addProperty("fireImmune", et.fireImmune());
            entity.addProperty("height", et.getHeight());
            entity.addProperty("width", et.getWidth());
            // entity.addProperty("fixed", et.getDimensions().fixed); also basically useless

            entities.add(entity);
        }
        jsonGenerator.output(entities, "entities");
    }

    @SuppressWarnings("unchecked")
    private static void generateBlockEntities() {
        Set<ResourceLocation> blockEntityRLs = Registry.BLOCK_ENTITY_TYPE.keySet();
        JsonArray blockEntities = new JsonArray();

        for (ResourceLocation blockEntityRL : blockEntityRLs) {
            BlockEntityType<?> bet = Registry.BLOCK_ENTITY_TYPE.get(blockEntityRL);

            JsonObject blockEntity = new JsonObject();
            blockEntity.addProperty("id", blockEntityRL.toString());
            blockEntity.addProperty("name", blockEntityNames.get(bet));

            // Use reflection to get valid blocks
            {
                JsonArray beBlocks = new JsonArray();
                try {
                    Field fcField = BlockEntityType.class.getDeclaredField("validBlocks");

                    fcField.setAccessible(true);

                    Set<Block> validBlocks = (Set<Block>) fcField.get(bet);
                    for (Block validBlock : validBlocks) {
                        JsonObject beBlock = new JsonObject();
                        beBlock.addProperty("id", Registry.BLOCK.getKey(validBlock).toString());

                        beBlocks.add(beBlock);
                    }
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    LOGGER.error("Failed to get block-entity blocks, skipping block-entity with ID '" + blockEntityRL + "'.", e);
                    continue;
                }
                blockEntity.add("blocks", beBlocks);
            }
            blockEntities.add(blockEntity);
        }
        jsonGenerator.output(blockEntities, "block_entities");
    }

    private static void generateItems() {
        Set<ResourceLocation> itemRLs = Registry.ITEM.keySet();
        JsonArray items = new JsonArray();

        for (ResourceLocation itemRL : itemRLs) {
            Item i = Registry.ITEM.get(itemRL);

            JsonObject item = new JsonObject();
            item.addProperty("id", itemRL.toString());
            item.addProperty("name", itemNames.get(i));
            // item.addProperty("langId", i.getDescriptionId());
            item.addProperty("depletes", i.canBeDepleted());
            item.addProperty("maxStackSize", i.getMaxStackSize());
            item.addProperty("maxDamage", i.getMaxDamage());
            // item.addProperty("complex", i.isComplex()); basically useless
            item.addProperty("edible", i.isEdible());
            item.addProperty("fireResistant", i.isFireResistant());
            item.addProperty("blockId", Registry.BLOCK.getKey(Block.byItem(i)).toString());
            ResourceLocation eatingSound = Registry.SOUND_EVENT.getKey(i.getEatingSound());
            if (eatingSound != null) {
                item.addProperty("eatingSound", eatingSound.toString());
            }
            ResourceLocation drinkingSound = Registry.SOUND_EVENT.getKey(i.getDrinkingSound());
            if (drinkingSound != null) {
                item.addProperty("drinkingSound", drinkingSound.toString());
            }
            // Food Properties
            if (i.isEdible() && i.getFoodProperties() != null) {
                FoodProperties fp = i.getFoodProperties();

                JsonObject foodProperties = new JsonObject();
                foodProperties.addProperty("alwaysEdible", fp.canAlwaysEat());
                foodProperties.addProperty("isFastFood", fp.isFastFood());
                foodProperties.addProperty("nutrition", fp.getNutrition());
                foodProperties.addProperty("saturationModifier", fp.getSaturationModifier());

                {
                    // Food effects
                    JsonArray effects = new JsonArray();
                    for (Pair<MobEffectInstance, Float> effect : fp.getEffects()) {
                        ResourceLocation rl = Registry.MOB_EFFECT.getKey(effect.getFirst().getEffect());

                        if (rl == null) {
                            continue;
                        }
                        JsonObject foodEffect = new JsonObject();
                        foodEffect.addProperty("id", rl.toString());
                        foodEffect.addProperty("amplifier", effect.getFirst().getAmplifier());
                        foodEffect.addProperty("duration", effect.getFirst().getDuration());
                        foodEffect.addProperty("chance", effect.getSecond());
                        effects.add(foodEffect);
                    }
                    foodProperties.add("effects", effects);
                }
                item.add("foodProperties", foodProperties);
            }
            // Armor properties
            if (i instanceof ArmorItem) {
                ArmorItem ai = (ArmorItem) i;

                JsonObject armorProperties = new JsonObject();
                armorProperties.addProperty("defense", ai.getDefense());
                armorProperties.addProperty("toughness", ai.getToughness());
                armorProperties.addProperty("slot", ai.getSlot().getName());

                item.add("armorProperties", armorProperties);
            }
            // SpawnEgg properties
            if (i instanceof SpawnEggItem) {
                SpawnEggItem sei = (SpawnEggItem) i;

                JsonObject spawnEggProperties = new JsonObject();
                spawnEggProperties.addProperty("entityType", Registry.ENTITY_TYPE.getKey(sei.getType(null)).toString());

                item.add("spawnEggProperties", spawnEggProperties);
            }

            items.add(item);
        }
        jsonGenerator.output(items, "items");
    }

    private static void generateEffects() {
        Set<ResourceLocation> effectRLs = Registry.MOB_EFFECT.keySet();
        JsonArray effects = new JsonArray();

        for (ResourceLocation effectRL : effectRLs) {
            MobEffect me = Registry.MOB_EFFECT.get(effectRL);

            JsonObject effect = new JsonObject();
            // Null safety check.
            if (me == null) {
                continue;
            }
            effect.addProperty("id", effectRL.toString());
            effect.addProperty("name", effectNames.get(me));
            // effect.addProperty("langId", me.getDescriptionId());
            effect.addProperty("color", me.getColor());
            effect.addProperty("instantaneous", me.isInstantenous());

            effects.add(effect);
        }
        jsonGenerator.output(effects, "potion_effects");
    }

    private static void generatePotions() {
        Set<ResourceLocation> effectRLs = Registry.POTION.keySet();
        JsonArray effects = new JsonArray();

        for (ResourceLocation effectRL : effectRLs) {
            Potion p = Registry.POTION.get(effectRL);

            JsonObject effect = new JsonObject();
            // Null safety check.
            effect.addProperty("id", effectRL.toString());
            effect.addProperty("name", potionNames.get(p));
            // effect.addProperty("langId", me.getDescriptionId());

            effects.add(effect);
        }
        jsonGenerator.output(effects, "potions");
    }

    private static void generateAttributes() {
        Set<ResourceLocation> attributeRLs = Registry.ATTRIBUTE.keySet();
        JsonArray attributes = new JsonArray();

        for (ResourceLocation attributeRL : attributeRLs) {
            Attribute a = Registry.ATTRIBUTE.get(attributeRL);

            JsonObject attribute = new JsonObject();
            if (a == null) {
                continue;
            }
            attribute.addProperty("id", attributeRL.toString());
            attribute.addProperty("name", attributeNames.get(a));
            attribute.addProperty("defaultValue", a.getDefaultValue());
            attribute.addProperty("clientSync", a.isClientSyncable());
            if (a instanceof RangedAttribute) {
                RangedAttribute ra = (RangedAttribute) a;
                // Unfortuantely get via reflection
                JsonObject range = new JsonObject();
                try {
                    Field maxV = RangedAttribute.class.getDeclaredField("maxValue");
                    maxV.setAccessible(true);
                    range.addProperty("maxValue", maxV.getDouble(ra));
                    Field minV = RangedAttribute.class.getDeclaredField("minValue");
                    minV.setAccessible(true);
                    range.addProperty("minValue", minV.getDouble(ra));
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    LOGGER.error("Failed to get attribute ranges, skipping attrobite with ID '" + attributeRL + "'.", e);
                }
                attribute.add("range", range);
            }

            attributes.add(attribute);
        }
        jsonGenerator.output(attributes, "attributes");
    }

    private static void generateEnchantments() {
        Set<ResourceLocation> enchantmentRLs = Registry.ENCHANTMENT.keySet();
        JsonArray enchantments = new JsonArray();

        for (ResourceLocation enchantmentRL : enchantmentRLs) {
            Enchantment e = Registry.ENCHANTMENT.get(enchantmentRL);
            if (e == null) {
                continue;
            }
            JsonObject enchantment = new JsonObject();

            enchantment.addProperty("id", enchantmentRL.toString());
            enchantment.addProperty("name", enchantmentNames.get(e));
            // enchantment.addProperty("langId", e.getDescriptionId());
            enchantment.addProperty("maxLevel", e.getMaxLevel());
            enchantment.addProperty("minLevel", e.getMinLevel());
            enchantment.addProperty("rarity", e.getRarity().toString());
            enchantment.addProperty("curse", e.isCurse());
            enchantment.addProperty("discoverable", e.isDiscoverable());
            enchantment.addProperty("tradeable", e.isTradeable());
            enchantment.addProperty("treasureOnly", e.isTreasureOnly());
            enchantment.addProperty("category", e.category.name());
            enchantments.add(enchantment);
        }
        jsonGenerator.output(enchantments, "enchantments");
    }

    private static void generateParticles() {
        Set<ResourceLocation> particleRLs = Registry.PARTICLE_TYPE.keySet();
        JsonArray particles = new JsonArray();

        for (ResourceLocation particleRL : particleRLs) {
            ParticleType<?> pt = Registry.PARTICLE_TYPE.get(particleRL);
            if (pt == null) {
                continue;
            }
            JsonObject particle = new JsonObject();

            particle.addProperty("id", particleRL.toString());
            particle.addProperty("name", particleNames.get(pt));
            particles.add(particle);
        }
        jsonGenerator.output(particles, "particles");
    }

    private static void generateSounds() {
        Set<ResourceLocation> soundRLs = Registry.SOUND_EVENT.keySet();
        JsonArray sounds = new JsonArray();

        for (ResourceLocation soundRL : soundRLs) {
            SoundEvent se = Registry.SOUND_EVENT.get(soundRL);
            if (se == null) {
                continue;
            }
            JsonObject sound = new JsonObject();

            sound.addProperty("id", soundRL.toString());
            sound.addProperty("name", soundNames.get(se));
            sounds.add(sound);
        }
        jsonGenerator.output(sounds, "sounds");
    }

    @SuppressWarnings("unchecked")
    private static void generateBiomes() {
        Set<ResourceLocation> biomeRLs = BuiltinRegistries.BIOME.keySet();
        JsonArray biomes = new JsonArray();

        for (ResourceLocation biomeRL : biomeRLs) {
            Biome b = BuiltinRegistries.BIOME.get(biomeRL);
            if (b == null) {
                continue;
            }
            JsonObject biome = new JsonObject();

            biome.addProperty("id", biomeRL.toString());
            biome.addProperty("humid", b.isHumid());
            biome.addProperty("scale", b.getScale());
            biome.addProperty("depth", b.getDepth());
            biome.addProperty("temperature", b.getBaseTemperature());
            biome.addProperty("downfall", b.getDownfall());
            biome.addProperty("precipitation", b.getPrecipitation().name());
            biome.addProperty("category", b.getBiomeCategory().name());

            {
                // Use reflection to access SpecialBiomeEffects (why the hell is this private anyway?)
                JsonObject biomeEffects = new JsonObject();
                try {
                    BiomeSpecialEffects bse = b.getSpecialEffects();
                    Field fcField = BiomeSpecialEffects.class.getDeclaredField("fogColor");
                    Field wcField = BiomeSpecialEffects.class.getDeclaredField("waterColor");
                    Field wfcField = BiomeSpecialEffects.class.getDeclaredField("waterFogColor");
                    Field scField = BiomeSpecialEffects.class.getDeclaredField("skyColor");
                    Field fcoField = BiomeSpecialEffects.class.getDeclaredField("foliageColorOverride");
                    Field gcoField = BiomeSpecialEffects.class.getDeclaredField("grassColorOverride");
                    Field gcmField = BiomeSpecialEffects.class.getDeclaredField("grassColorModifier");

                    fcField.setAccessible(true);
                    wcField.setAccessible(true);
                    wfcField.setAccessible(true);
                    scField.setAccessible(true);
                    fcoField.setAccessible(true);
                    gcoField.setAccessible(true);
                    gcmField.setAccessible(true);

                    int fogColor = fcField.getInt(bse);
                    int waterColor = wcField.getInt(bse);
                    int waterFogColor = wfcField.getInt(bse);
                    int skyColor = scField.getInt(bse);
                    Optional<Integer> foliageColorOverride = (Optional<Integer>) fcoField.get(bse);
                    Optional<Integer> grassColorOverride = (Optional<Integer>) gcoField.get(bse);
                    GrassColorModifier grassColorModifier = (GrassColorModifier) gcmField.get(bse);

                    biomeEffects.addProperty("fogColor", fogColor);
                    biomeEffects.addProperty("waterColor", waterColor);
                    biomeEffects.addProperty("waterFogColor", waterFogColor);
                    biomeEffects.addProperty("skyColor", skyColor);
                    biomeEffects.addProperty("foliageColorOverride", foliageColorOverride.orElse(null));
                    biomeEffects.addProperty("grassColorOverride", grassColorOverride.orElse(null));
                    biomeEffects.addProperty("grassColorModifier", grassColorModifier.name());
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    LOGGER.error("Failed to get biome effects, skipping biome with ID '" + biomeRL + "'.", e);
                    continue;
                }
                biome.add("effects", biomeEffects);
            }
            biomes.add(biome);
        }
        jsonGenerator.output(biomes, "biomes");
    }

    private static void generateVillagerProfessions() {
        Set<ResourceLocation> villagerProfessionRLs = Registry.VILLAGER_PROFESSION.keySet();
        JsonArray villagerProfessions = new JsonArray();

        for (ResourceLocation villagerProfessionRL : villagerProfessionRLs) {
            VillagerProfession vp = Registry.VILLAGER_PROFESSION.get(villagerProfessionRL);

            JsonObject villagerProfession = new JsonObject();
            villagerProfession.addProperty("id", villagerProfessionRL.toString());
            villagerProfession.addProperty("name", villagerProfessionNames.get(vp));
            SoundEvent workSound = vp.getWorkSound();
            if (workSound != null) {
                ResourceLocation workSoundRL = Registry.SOUND_EVENT.getKey(workSound);
                if (workSoundRL != null) {
                    villagerProfession.addProperty("workSound", workSoundRL.toString());
                }
            }

            villagerProfessions.add(villagerProfession);
        }
        jsonGenerator.output(villagerProfessions, "villager_professions");
    }

    private static void generateVillagerTypes() {
        Set<ResourceLocation> villagerTypeRLs = Registry.VILLAGER_TYPE.keySet();
        JsonArray villagerTypes = new JsonArray();

        for (ResourceLocation villagerTypeRL : villagerTypeRLs) {
            VillagerType vt = Registry.VILLAGER_TYPE.get(villagerTypeRL);

            JsonObject villagerType = new JsonObject();
            villagerType.addProperty("id", villagerTypeRL.toString());
            villagerType.addProperty("name", villagerTypeNames.get(vt));

            villagerTypes.add(villagerType);
        }
        jsonGenerator.output(villagerTypes, "villager_types");
    }

    private static void generateDimensionTypes() {
        Set<ResourceLocation> dimensionTypeRLs = RegistryAccess.RegistryHolder.builtin().dimensionTypes().keySet();
        JsonArray dimensionTypes = new JsonArray();

        for (ResourceLocation dimensionTypeRL : dimensionTypeRLs) {
            DimensionType dt = RegistryAccess.RegistryHolder.builtin().dimensionTypes().get(dimensionTypeRL);
            if (dt == null) {
                continue;
            }
            JsonObject dimensionType = new JsonObject();

            dimensionType.addProperty("id", dimensionTypeRL.toString());
            dimensionType.addProperty("bedWorks", dt.bedWorks());
            dimensionType.addProperty("coordinateScale", dt.coordinateScale());
            dimensionType.addProperty("ceiling", dt.hasCeiling());
            dimensionType.addProperty("fixedTime", dt.hasFixedTime());
            dimensionType.addProperty("raids", dt.hasRaids());
            dimensionType.addProperty("skyLight", dt.hasSkyLight());
            dimensionType.addProperty("piglinSafe", dt.piglinSafe());
            dimensionType.addProperty("logicalHeight", dt.logicalHeight());
            dimensionType.addProperty("natural", dt.natural());
            dimensionType.addProperty("ultraWarm", dt.ultraWarm());
            dimensionType.addProperty("respawnAnchorWorks", dt.respawnAnchorWorks());
            dimensionTypes.add(dimensionType);
        }
        jsonGenerator.output(dimensionTypes, "dimension_types");
    }

    private static void generateCustomStatistics() {
        Set<ResourceLocation> customStatisticsRLs = Registry.CUSTOM_STAT.keySet();
        JsonArray customStatistics = new JsonArray();

        for (ResourceLocation customStatisticRL : customStatisticsRLs) {
            ResourceLocation rl = Registry.CUSTOM_STAT.get(customStatisticRL);

            JsonObject customStatistic = new JsonObject();
            customStatistic.addProperty("id", customStatisticRL.toString());
            customStatistic.addProperty("name", customStatisticNames.get(rl));

            customStatistics.add(customStatistic);
        }
        jsonGenerator.output(customStatistics, "custom_statistics");
    }

    private static void generateMapColors() {
        HashMap<MaterialColor, String> names = new HashMap<>();
        // Map field name to material color.
        for (Field declaredField : MaterialColor.class.getDeclaredFields()) {
            if (declaredField.getName().equals("MATERIAL_COLORS") || declaredField.getType() != MaterialColor.class) {
                continue;
            }
            try {
                MaterialColor mc = (MaterialColor) declaredField.get(null);
                names.put(mc, declaredField.getName());
            } catch (IllegalAccessException e) {
                // Just stop I guess
                LOGGER.error("Failed to access map color naming system", e);
                return;
            }
        }

        JsonArray mapColors = new JsonArray();

        for (MaterialColor mc : MaterialColor.MATERIAL_COLORS) {
            if (mc == null) {
                continue;
            }
            JsonObject mapColor = new JsonObject();

            mapColor.addProperty("name", names.get(mc));
            mapColor.addProperty("id", mc.id);
            mapColor.addProperty("color", mc.col);
            mapColors.add(mapColor);
        }
        jsonGenerator.output(mapColors, "map_colors");
    }

    private static void generateTags(@NotNull File tagFolder) {
        // Block tags
        {
            File blockTagsFolder = new File(tagFolder, "blocks");
            File[] children = blockTagsFolder.listFiles();
            if (children != null) {
                JsonArray blockTags = new JsonArray();
                for (File file : children) {
                    JsonObject blockTag;
                    try {
                        blockTag = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read block tag located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json
                    blockTag.addProperty("tagName", fileName.substring(0, fileName.length() - 5));
                    blockTags.add(blockTag);
                }
                jsonGenerator.output(blockTags, "block_tags", "tags");
            } else {
                LOGGER.error("Failed to find block tags in data folder.");
            }
        }
        // Fluid tags
        {
            File fluidTagsFolder = new File(tagFolder, "fluids");
            File[] children = fluidTagsFolder.listFiles();
            if (children != null) {
                JsonArray fluidTags = new JsonArray();
                for (File file : children) {
                    JsonObject fluidTag;
                    try {
                        fluidTag = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read fluid tag located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json
                    fluidTag.addProperty("tagName", fileName.substring(0, fileName.length() - 5));
                    fluidTags.add(fluidTag);
                }
                jsonGenerator.output(fluidTags, "fluid_tags", "tags");
            } else {
                LOGGER.error("Failed to find fluid tags in data folder.");
            }
        }
        // EntityType tags
        {
            File entityTypeTagsFolder = new File(tagFolder, "entity_types");
            File[] children = entityTypeTagsFolder.listFiles();
            if (children != null) {
                JsonArray entityTypeTags = new JsonArray();
                for (File file : children) {
                    JsonObject entityTypeTag;
                    try {
                        entityTypeTag = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read entity type tag located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json
                    entityTypeTag.addProperty("tagName", fileName.substring(0, fileName.length() - 5));
                    entityTypeTags.add(entityTypeTag);
                }
                jsonGenerator.output(entityTypeTags, "entity_type_tags", "tags");
            } else {
                LOGGER.error("Failed to find entity type tags in data folder.");
            }
        }
        // Item tags
        {
            File itemTagsFolder = new File(tagFolder, "items");
            File[] children = itemTagsFolder.listFiles();
            if (children != null) {
                JsonArray fluidTags = new JsonArray();
                for (File file : children) {
                    JsonObject itemTag;
                    try {
                        itemTag = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read item tag located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json
                    itemTag.addProperty("tagName", fileName.substring(0, fileName.length() - 5));
                    fluidTags.add(itemTag);
                }
                jsonGenerator.output(fluidTags, "item_tags", "tags");
            } else {
                LOGGER.error("Failed to find item tags in data folder.");
            }
        }
    }

    private static void generateLootTables(@NotNull File lootTablesFolder) {
        // Block loot tables
        {
            File blockTables = new File(lootTablesFolder, "blocks");
            File[] listedFiles = blockTables.listFiles();
            if (listedFiles != null) {
                List<File> children = new ArrayList<>(Arrays.asList(listedFiles));
                JsonArray blockLootTables = new JsonArray();
                for (int i = 0; i < children.size(); i++) {
                    File file = children.get(i);
                    // Add subdirectories files to the for-loop.
                    if (file.isDirectory()) {
                        File[] subChildren = file.listFiles();
                        if (subChildren != null) {
                            children.addAll(Arrays.asList(subChildren));
                        }
                        continue;
                    }
                    JsonObject blockLootTable;
                    try {
                        blockLootTable = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read block loot table located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json by removing last 5 chars of the name.
                    blockLootTable.addProperty("blockId", "minecraft:" + fileName.substring(0, fileName.length() - 5));
                    blockLootTables.add(blockLootTable);
                }
                jsonGenerator.output(blockLootTables, "block_loot_tables", "loot_tables");
            } else {
                LOGGER.error("Failed to find block loot tables in data folder.");
            }
        }
        // Chest loot tables
        {
            File chestTables = new File(lootTablesFolder, "chests");
            File[] listedFiles = chestTables.listFiles();
            if (listedFiles != null) {
                List<File> children = new ArrayList<>(Arrays.asList(listedFiles));
                JsonArray chestLootTables = new JsonArray();
                for (int i = 0; i < children.size(); i++) {
                    File file = children.get(i);
                    // Add subdirectories files to the for-loop.
                    if (file.isDirectory()) {
                        File[] subChildren = file.listFiles();
                        if (subChildren != null) {
                            children.addAll(Arrays.asList(subChildren));
                        }
                        continue;
                    }
                    JsonObject chestLootTable;
                    try {
                        chestLootTable = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read chest loot table located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json by removing last 5 chars of the name.
                    chestLootTable.addProperty("chestType", fileName.substring(0, fileName.length() - 5));
                    chestLootTables.add(chestLootTable);
                }
                jsonGenerator.output(chestLootTables, "chest_loot_tables", "loot_tables");
            } else {
                LOGGER.error("Failed to find chest loot tables in data folder.");
            }
        }
        // Entity loot tables
        {
            File entityTables = new File(lootTablesFolder, "entities");
            File[] listedFiles = entityTables.listFiles();
            if (listedFiles != null) {
                List<File> children = new ArrayList<>(Arrays.asList(listedFiles));
                JsonArray entityLootTables = new JsonArray();
                for (int i = 0; i < children.size(); i++) {
                    File file = children.get(i);
                    // Add subdirectories files to the for-loop.
                    if (file.isDirectory()) {
                        File[] subChildren = file.listFiles();
                        if (subChildren != null) {
                            children.addAll(Arrays.asList(subChildren));
                        }
                        continue;
                    }
                    JsonObject entityLootTable;
                    try {
                        entityLootTable = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read entity loot table located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json by removing last 5 chars of the name.
                    entityLootTable.addProperty("entityId", "minecraft:" + fileName.substring(0, fileName.length() - 5));
                    entityLootTables.add(entityLootTable);
                }
                jsonGenerator.output(entityLootTables, "entity_loot_tables", "loot_tables");
            } else {
                LOGGER.error("Failed to find entity loot tables in data folder.");
            }
        }
        // Gameplay loot tables
        {
            File gameplayTables = new File(lootTablesFolder, "gameplay");
            File[] listedFiles = gameplayTables.listFiles();
            if (listedFiles != null) {
                List<File> children = new ArrayList<>(Arrays.asList(listedFiles));
                JsonArray gameplayLootTables = new JsonArray();
                for (int i = 0; i < children.size(); i++) {
                    File file = children.get(i);
                    // Add subdirectories files to the for-loop.
                    if (file.isDirectory()) {
                        File[] subChildren = file.listFiles();
                        if (subChildren != null) {
                            children.addAll(Arrays.asList(subChildren));
                        }
                        continue;
                    }
                    JsonObject gameplayLootTable;
                    try {
                        gameplayLootTable = GSON.fromJson(new JsonReader(new FileReader(file)), JsonObject.class);
                    } catch (FileNotFoundException e) {
                        LOGGER.error("Failed to read gameplay loot table located at '" + file + "'.", e);
                        continue;
                    }
                    String fileName = file.getName();
                    // Remove .json by removing last 5 chars of the name.
                    gameplayLootTable.addProperty("gameplayType", fileName.substring(0, fileName.length() - 5));
                    gameplayLootTables.add(gameplayLootTable);
                }
                jsonGenerator.output(gameplayLootTables, "gameplay_loot_tables", "loot_tables");
            } else {
                LOGGER.error("Failed to find gameplay loot tables in data folder.");
            }
        }
    }
}