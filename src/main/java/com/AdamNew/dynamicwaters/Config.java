package com.AdamNew.dynamicwaters;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class Config {
        public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public static final ModConfigSpec SPEC;

        public static final ModConfigSpec.DoubleValue PLAYER_SPEED;
        public static final ModConfigSpec.DoubleValue ANIMAL_SPEED;
        public static final ModConfigSpec.DoubleValue WATER_MOB_SPEED;
        public static final ModConfigSpec.DoubleValue ITEM_SPEED;
        public static final ModConfigSpec.DoubleValue DEFAULT_SPEED;
        public static final ModConfigSpec.DoubleValue SABLE_SHIP_SPEED;
        public static final ModConfigSpec.DoubleValue RAIN_CURRENT_BOOST;
        public static final ModConfigSpec.DoubleValue STORM_CURRENT_BOOST;
        public static final ModConfigSpec.BooleanValue VANILLA_RIVER_FLOW;
        public static final ModConfigSpec.BooleanValue LIVING_CURRENT;
        public static final ModConfigSpec.BooleanValue REALISTIC_CURRENT;
        public static final ModConfigSpec.BooleanValue RIVER_PLUMES;
        public static final ModConfigSpec.BooleanValue WATERFALL_CURRENTS;
        public static final ModConfigSpec.BooleanValue CURRENT_WOBBLE;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> CUSTOM_SPEEDS;

        public static final ModConfigSpec.BooleanValue ENABLE_RIVER_FOAM;
        public static final ModConfigSpec.BooleanValue ENABLE_RIVER_MIST;
        public static final ModConfigSpec.BooleanValue RIVER_WATER_COLOR;
        public static final ModConfigSpec.ConfigValue<String> MOUNTAIN_RIVER_WATER_COLOR;
        public static final ModConfigSpec.ConfigValue<String> LOWLAND_RIVER_WATER_COLOR;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIOME_WATER_COLORS;
        public static final ModConfigSpec.BooleanValue BUILT_IN_WATER_COLORS;
        public static final ModConfigSpec.BooleanValue MUDDY_RAIN_RIVERS;
        public static final ModConfigSpec.BooleanValue CLEAR_RIVER_WATER;
        public static final ModConfigSpec.IntValue RIVER_PARTICLE_DENSITY;
        public static final ModConfigSpec.BooleanValue OLD_RIVER_PARTICLES;
        public static final ModConfigSpec.BooleanValue ENABLE_RIVER_VISUAL_FLOW;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIOME_BLACKLIST;
        public static final ModConfigSpec.BooleanValue ENABLE_RIVER_SOUND;
        public static final ModConfigSpec.BooleanValue CUSTOM_MAIN_MENU_PANORAMA;
        public static final ModConfigSpec.BooleanValue SHOW_WELCOME_MESSAGE;
        public static final ModConfigSpec.BooleanValue DISABLE_TECTONIC_UNDERGROUND_RIVERS;
        public static final ModConfigSpec.BooleanValue SHOW_MAIN_MENU_BUTTON;

        public enum DensityLevel {
                VERY_LOW, LOW, MEDIUM, HIGH, ULTRA, EXTREME, INSANE
        }

        public enum LowlandValley {
                GORGE,

                VALLEY,

                VALLEY_GORGE,

                GORGE_TUNNEL,

                VALLEY_GORGE_TUNNEL
        }

        public enum LowlandCutBy {
                HEIGHT,

                BIOME
        }

        public enum RiverSplits {
                OFF(0.0),
                RARE(0.1),
                ENOUGH(0.2),
                NORMAL(0.3),
                OFTEN(0.6);

                public final double chance;

                RiverSplits(double chance) {
                        this.chance = chance;
                }
        }

        public enum RaggedShores {
                OFF(2.0),
                RARE(0.55),
                NORMAL(0.42),
                OFTEN(0.25);

                public final double from;

                RaggedShores(double from) {
                        this.from = from;
                }
        }

        public enum FallenLogs {
                OFF(0.0),
                RARE(0.15),
                NORMAL(0.4),
                OFTEN(0.85);

                public final double chance;

                FallenLogs(double chance) {
                        this.chance = chance;
                }
        }

        public enum TunnelFrequency {
                RARE(0.05),
                NORMAL(0.15),
                OFTEN(1.0);

                public final double chance;

                TunnelFrequency(double chance) {
                        this.chance = chance;
                }
        }

        public enum IceFloes {
                OFF,
                RARE,
                NORMAL,
                OFTEN,
                MANY
        }

        public enum MountainRiverIce {
                OPEN,

                CALM,

                FULL
        }

        public enum WaterfallLevel {
                CLIFFS,

                FEW,

                NORMAL,

                MANY
        }

        public enum NormalRiverMode {
                DYNAMIC,

                VANILLA,

                NONE
        }

        public static final ModConfigSpec.BooleanValue ENABLE_watersheds;
        public static final ModConfigSpec.EnumValue<NormalRiverMode> NORMAL_RIVER_MODE;
        public static final ModConfigSpec.EnumValue<DensityLevel> MOUNTAIN_RIVER_DENSITY;
        public static final ModConfigSpec.BooleanValue ENABLE_MOUNTAIN_LAKES;
        public static final ModConfigSpec.BooleanValue LAKE_BIOME;
        public static final ModConfigSpec.DoubleValue NORMAL_RIVER_CANYON_HEIGHT;
        public static final ModConfigSpec.BooleanValue NORMAL_RIVER_TUNNELS;
        public static final ModConfigSpec.IntValue NORMAL_TUNNEL_HEIGHT;
        public static final ModConfigSpec.IntValue NORMAL_TUNNEL_LEDGE;
        public static final ModConfigSpec.BooleanValue NORMAL_TUNNEL_PILLARS;
        public static final ModConfigSpec.BooleanValue NORMAL_TUNNEL_FORMATIONS;
        public static final ModConfigSpec.IntValue DELTA_INLAND_REACH;
        public static final ModConfigSpec.DoubleValue LOWLAND_RIVER_WIDTH;
        public static final ModConfigSpec.DoubleValue LOWLAND_RIVER_DEPTH;
        public static final ModConfigSpec.EnumValue<LowlandValley> LOWLAND_RIVER_VALLEY;
        public static final ModConfigSpec.EnumValue<LowlandCutBy> LOWLAND_CUT_BY;
        public static final ModConfigSpec.DoubleValue LOWLAND_VALLEY_BELOW;
        public static final ModConfigSpec.DoubleValue LOWLAND_TUNNEL_ABOVE;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> LOWLAND_VALLEY_BIOMES;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> LOWLAND_TUNNEL_BIOMES;
        public static final ModConfigSpec.DoubleValue MOUNTAIN_RIVER_WIDTH;
        public static final ModConfigSpec.DoubleValue MOUNTAIN_RIVER_DEPTH;
        public static final ModConfigSpec.EnumValue<RiverSplits> RIVER_SPLITS;
        public static final ModConfigSpec.BooleanValue OXBOW_LAKES;
        public static final ModConfigSpec.EnumValue<FallenLogs> RIVER_FALLEN_LOGS;
        public static final ModConfigSpec.EnumValue<DensityLevel> NORMAL_RIVER_DENSITY;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RIVER_BIOME_CHANCE;
        public static final ModConfigSpec.BooleanValue MOUNTAIN_RIVER_TUNNELS;
        public static final ModConfigSpec.EnumValue<TunnelFrequency> MOUNTAIN_TUNNEL_FREQUENCY;
        public static final ModConfigSpec.IntValue MOUNTAIN_TUNNEL_LENGTH;
        public static final ModConfigSpec.IntValue MOUNTAIN_TUNNEL_DEPTH;
        public static final ModConfigSpec.IntValue MOUNTAIN_TUNNEL_HEIGHT;
        public static final ModConfigSpec.IntValue MOUNTAIN_TUNNEL_LEDGE;
        public static final ModConfigSpec.EnumValue<WaterfallLevel> MOUNTAIN_RIVER_WATERFALLS;
        public static final ModConfigSpec.BooleanValue WATERFALL_CAVES;
        public static final ModConfigSpec.EnumValue<MountainRiverIce> MOUNTAIN_RIVER_ICE;
        public static final ModConfigSpec.EnumValue<IceFloes> RIVER_ICE_FLOES;
        public static final ModConfigSpec.BooleanValue SEASONAL_ICE_FLOES;
        public static final ModConfigSpec.BooleanValue MOUNTAIN_RIVER_BIOME;
        public static final ModConfigSpec.BooleanValue RIVERBANK_PALETTES;
        public static final ModConfigSpec.BooleanValue WATER_PLANTS;
        public static final ModConfigSpec.BooleanValue RIVER_BOULDERS;
        public static final ModConfigSpec.BooleanValue RIVER_ARCHES;
        public static final ModConfigSpec.BooleanValue RIVER_FORDS;
        public static final ModConfigSpec.BooleanValue RIVER_BEACHES;
        public static final ModConfigSpec.BooleanValue RIVERBANK_PLANTS;
        public static final ModConfigSpec.DoubleValue RIVERBANK_PLANT_DENSITY;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RIVERBANK_PLANT_MIXES;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RIVERBANK_BUSH_LEAVES;
        public static final ModConfigSpec.BooleanValue RIVERBANK_OWN_PLANTS;
        public static final ModConfigSpec.EnumValue<RaggedShores> RAGGED_SHORES;
        public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> GRAVEL_BAR_BIOMES;

        static {
                BUILDER.comment("How strongly river water pushes players, animals, items and boats.").push("River Flow Speeds");
                PLAYER_SPEED = BUILDER.comment("RiverFlow speed multiplier for players in the river.").defineInRange(
                                "playerSpeed",
                                0.2, 0.0, 10.0);
                ANIMAL_SPEED = BUILDER.comment(
                                "RiverFlow speed multiplier for animals. Note: Animals have strong swimming AI, you may want to decrease this.")
                                .defineInRange("animalSpeed", 0.1, 0.0, 10.0);
                WATER_MOB_SPEED = BUILDER.comment(
                                "RiverFlow speed multiplier for water animals (squids, fishes, dolphins). Set to 0 to prevent them from being swept away when their AI pauses.")
                                .defineInRange("waterMobSpeed", 0.0, 0.0, 10.0);
                ITEM_SPEED = BUILDER.comment("RiverFlow speed multiplier for dropped items.").defineInRange("itemSpeed",
                                0.2,
                                0.0,
                                10.0);
                DEFAULT_SPEED = BUILDER.comment("RiverFlow speed multiplier for generic unidentified entities.")
                                .defineInRange("defaultSpeed", 0.1, 0.0, 10.0);
                SABLE_SHIP_SPEED = BUILDER.comment("RiverFlow speed multiplier for physics ships: Sable / Aeronautics, Valkyrien Skies.")
                                .defineInRange("sableShipSpeed", 2.0, 0.0, 10.0);
                RAIN_CURRENT_BOOST = BUILDER.comment(
                                "How much faster the rivers run in the rain, as a share of their speed (0.25 = a quarter faster),\n"
                                                + "growing as the rain sets in - only where it rains, not where it snows or never rains.")
                                .defineInRange("rainCurrentBoost", 0.25, 0.0, 2.0);
                STORM_CURRENT_BOOST = BUILDER
                                .comment("How much faster the rivers run in a thunderstorm, as a share of their speed (in place of\n"
                                                + "rainCurrentBoost).")
                                .defineInRange("stormCurrentBoost", 0.5, 0.0, 2.0);
                VANILLA_RIVER_FLOW = BUILDER
                                .comment("Give Minecraft's own rivers a current too, when they are the world's lowland rivers\n"
                                                + "(normalRiverMode = VANILLA): along the river, toward the sea. The server works the direction\n"
                                                + "out and sends it to players with the chunks, so it works in multiplayer.")
                                .define("vanillaRiverFlow", true);
                LIVING_CURRENT = BUILDER
                                .comment("The rivers' current follows what players build in the water and dig out of the banks: it\n"
                                                + "stands still before a dam and for a stretch below it, runs round a pier and faster through\n"
                                                + "a narrowing, and down a canal that leads back into the river. The water's level never\n"
                                                + "changes. Off = the current runs as the world laid the river, whatever is built in it.")
                                .define("livingCurrent", true);
                REALISTIC_CURRENT = BUILDER
                                .comment("Rivers run at their own pace: fastest down the middle and at the surface, slow along the\n"
                                                + "banks and by the bed, faster round the outside of a bend than inside it; mountain rivers\n"
                                                + "faster the steeper they fall, lowland rivers lazier the broader they are, and faster where\n"
                                                + "they narrow. The water pushes players, boats and items as the square of its speed.\n"
                                                + "Off = the same current all across and all along every river.")
                                .define("realisticCurrent", true);
                RIVER_PLUMES = BUILDER
                                .comment("A river carries its current on into the lake or sea it runs into, spreading and fading out\n"
                                                + "over a few dozen blocks, and a lake's water is drawn into the river that leaves it.\n"
                                                + "Off = the current stops dead where the river meets still water.")
                                .define("riverPlumes", true);
                WATERFALL_CURRENTS = BUILDER
                                .comment("The water speeds up toward the edge of a waterfall, the falling water pulls things down it,\n"
                                                + "and in the pool at its foot the water boils out away from the fall while eddies along the\n"
                                                + "banks run back toward it, so floating things go round there.")
                                .define("waterfallCurrents", true);
                CURRENT_WOBBLE = BUILDER
                                .comment("Now and then the current in the fastest water - rapids, steep mountain reaches - swings a\n"
                                                + "little from side to side and surges, so a boat there sways.")
                                .define("currentWobble", true);
                CUSTOM_SPEEDS = BUILDER.comment(
                                "Custom flow speeds for specific entities, or for every entity in a tag.\n"
                                                + "Format: modid:name=speed or #modid:tag=speed (Example: minecraft:pig=0.8, alexmobs:bird=0.0, #minecraft:aquatic=0.0)")
                                .defineListAllowEmpty(java.util.Collections.singletonList("customEntitySpeeds"),
                                                () -> java.util.List.of("minecraft:warden=1.0"),
                                                o -> o instanceof String);
                BUILDER.pop();

                BUILDER.comment("How rivers look, and the mod's touches on the main menu.").push("Visuals");
                ENABLE_RIVER_FOAM = BUILDER.comment(
                                "Foam on the rivers: drifting down them out to 48 blocks, more in fast water, churned up in\n"
                                                + "rapids and thrown out where a waterfall lands.")
                                .define("enableRiverFoam", true);
                ENABLE_RIVER_MIST = BUILDER.comment(
                                "Spray on the rivers: a mist at the foot of every waterfall, faint spray off a bank the\n"
                                                + "current runs into, and where foam is dashed against a bank or goes over the lip of a fall.")
                                .define("enableRiverMist", true);
                RIVER_WATER_COLOR = BUILDER.comment(
                                "The rivers' own water colour: mountain rivers clear and green-blue, lowland rivers and deltas a\n"
                                                + "little murky (mountainRiverWaterColor, lowlandRiverWaterColor), blended into the biomes'\n"
                                                + "water like any biome's. Lakes and the sea keep theirs.")
                                .define("riverWaterColor", true);
                MOUNTAIN_RIVER_WATER_COLOR = BUILDER.comment(
                                "The colour mountain rivers' water is taken toward, as #RRGGBB (riverWaterColor).")
                                .define("mountainRiverWaterColor", "#2E9FC4", o -> o instanceof String s && hexColor(s) >= 0);
                LOWLAND_RIVER_WATER_COLOR = BUILDER.comment(
                                "The colour lowland rivers' and deltas' water is taken toward, as #RRGGBB (riverWaterColor).")
                                .define("lowlandRiverWaterColor", "#4279D2", o -> o instanceof String s && hexColor(s) >= 0);
                BIOME_WATER_COLORS = BUILDER.comment(
                                "Your own water colour for a biome, or every biome in a tag, as #RRGGBB - in place of the biome's\n"
                                                + "own and of the rivers' (riverWaterColor).\n"
                                                + "Format: modid:biome=#RRGGBB or #modid:tag=#RRGGBB (Example: minecraft:swamp=#4C6559,\n"
                                                + "#minecraft:is_river=#3A8FC0)")
                                .defineListAllowEmpty(java.util.Collections.singletonList("biomeWaterColors"),
                                                () -> java.util.List.of(), o -> o instanceof String s && s.contains("="));
                BUILT_IN_WATER_COLORS = BUILDER.comment(
                                "Water colours the mod brings for the biomes of other mods it knows (Regions Unexplored,\n"
                                                + "Oh The Biomes We've Gone, Terralith), used while that mod is installed. A biome or tag in\n"
                                                + "biomeWaterColors goes before them.")
                                .define("builtInWaterColors", true);
                MUDDY_RAIN_RIVERS = BUILDER.comment(
                                "Rivers run muddy in the rain, browner in a thunderstorm, and clear again a while after it -\n"
                                                + "only where it rains, not where it snows or never rains. Client side.")
                                .define("muddyRainRivers", true);
                RIVER_ICE_FLOES = BUILDER
                                .comment("How often a group of thin ice floes comes floating down a mountain river's open water in\n"
                                                + "cold land, carried by the current - over a waterfall they drop - until they crack apart a few\n"
                                                + "minutes later, the small ones first. A blow breaks one; a player goes through it into the water.\n"
                                                + "OFF = never. RARE = now and then. NORMAL = every few seconds by such a river (default).\n"
                                                + "OFTEN = more of them. MANY = a steady drift.")
                                .defineEnum("riverIceFloes", IceFloes.NORMAL);
                SEASONAL_ICE_FLOES = BUILDER
                                .comment("With Serene Seasons: from early to late winter, ice floes come down every river, lowland or\n"
                                                + "mountain, in any land with a winter - not in deserts nor in the biomes it keeps tropical\n"
                                                + "seasons in. None in spring. As often as riverIceFloes has them.")
                                .define("seasonalIceFloes", true);
                CLEAR_RIVER_WATER = BUILDER.comment(
                                "Under a river's water you see far at once: the game's underwater fog, which closes in to a few\n"
                                                + "blocks as you go under and clears only slowly, is pushed out to the render distance. Lakes\n"
                                                + "and the sea keep it. Off unless turned on.")
                                .define("clearRiverWater", false);
                RIVER_PARTICLE_DENSITY = BUILDER
                                .comment("How often patches of foam (and the old bubbles, if on) appear on rivers. Higher = fewer.")
                                .defineInRange("riverParticleDensity", 50, 1, 100);
                OLD_RIVER_PARTICLES = BUILDER.comment(
                                "The old river particles: bubbles on the rivers, and puffs of cloud in rapids while the foam\n"
                                                + "is off.")
                                .define("oldRiverParticles", false);
                ENABLE_RIVER_VISUAL_FLOW = BUILDER.comment(
                                "Show the flowing-water texture on rivers. Off = the water looks still but still pushes entities\n"
                                                + "(for compatibility with Physics Mod). Press F3+T after changing it.")
                                .define("enableRiverVisualFlow", true);
                CUSTOM_MAIN_MENU_PANORAMA = BUILDER.comment("Enable the custom main menu panorama for Dynamic Waters.")
                                .define("customMainMenuPanorama", true);
                SHOW_MAIN_MENU_BUTTON = BUILDER.comment("Show the Dynamic Waters 'D' button on the main menu.")
                                .define("showMainMenuButton", true);
                BUILDER.pop();

                BUILDER.comment("How rivers and lakes are generated. Changes apply to newly generated chunks only.")
                                .push("Generation");
                NORMAL_RIVER_MODE = BUILDER
                                .comment("Which long lowland rivers a world gets. Only one system can own them.\n"
                                                + "DYNAMIC = Dynamic Waters' own rivers (default): wide channels with canyons and tunnels,\n"
                                                + "          carrying the minecraft:river biome along their course.\n"
                                                + "VANILLA = Minecraft's own rivers, exactly as in an unmodded world, with their normal\n"
                                                + "          minecraft:river biome. Dynamic Waters stops touching lowland river terrain.\n"
                                                + "NONE    = no lowland rivers at all. Mountain rivers are unaffected.\n"
                                                + "Takes effect when a world is loaded, not while one is running.")
                                .defineEnum("normalRiverMode", NormalRiverMode.DYNAMIC);
                NORMAL_RIVER_DENSITY = BUILDER
                                .comment("How many lowland rivers a world gets, against the default (HIGH): VERY_LOW about 1/9,\n"
                                                + "LOW about 4/9, MEDIUM about 2/3, ULTRA about 1.8x, EXTREME about 2.8x, INSANE about 7x.")
                                .defineEnum("normalRiverDensity", DensityLevel.HIGH);
                RIVER_BIOME_CHANCE = BUILDER
                                .comment("How likely rivers are in a biome, lowland and mountain rivers alike: each entry\n"
                                                + "\"<biomes> <chance>\", the first whose biomes match used. Biomes: an id (minecraft:desert),\n"
                                                + "a tag after # (#minecraft:is_badlands) or an id ending in * (terralith:*), several apart\n"
                                                + "by spaces; chance 0 to 1, 1 = as anywhere, 0 = none. A lowland river runs only through\n"
                                                + "that share of such a biome's land, a mountain stream needs that much more water to begin.\n"
                                                + "E.g. [\"minecraft:desert 0.2\", \"#minecraft:is_badlands 0.5\"]. New chunks only.")
                                .defineListAllowEmpty(java.util.Collections.singletonList("riverBiomeChance"),
                                                () -> java.util.List.of(), o -> o instanceof String);
                NORMAL_RIVER_CANYON_HEIGHT = BUILDER
                                .comment("Terrain height (Y level) above which a lowland river runs as a closed tunnel under the ground\n"
                                                + "instead of an open canyon. Higher = more open canyons, fewer tunnels. Only with normalRiverTunnels on.")
                                .defineInRange("normalRiverCanyonHeight", 105.0, 60.0, 2000.0);
                NORMAL_RIVER_TUNNELS = BUILDER
                                .comment("Let lowland rivers run under high ground as closed tunnels (above normalRiverCanyonHeight).\n"
                                                + "Off = no lowland tunnels at all: where the ground rises high over a lowland river it cuts an\n"
                                                + "open gorge instead - steep rock walls stepping back in terraces as they rise, wider at the top\n"
                                                + "than at the water, the river filling its floor. Mountain rivers end in the gorge.\n"
                                                + "Off by default. New chunks only.")
                                .define("normalRiverTunnels", false);
                NORMAL_TUNNEL_HEIGHT = BUILDER
                                .comment("Height of a lowland river tunnel's roof over the water at its middle, in blocks, give or take\n"
                                                + "a few. Only with normalRiverTunnels on. New chunks only.")
                                .defineInRange("normalTunnelHeight", 16, 8, 40);
                NORMAL_TUNNEL_LEDGE = BUILDER
                                .comment("Width of a dry ledge along each side of the water in a lowland river tunnel, in blocks.\n"
                                                + "0 = the water reaches the walls. Only with normalRiverTunnels on. New chunks only.")
                                .defineInRange("normalTunnelLedge", 0, 0, 12);
                NORMAL_TUNNEL_PILLARS = BUILDER
                                .comment("Rare single pillars of rock standing in a lowland river tunnel.\n"
                                                + "Only with normalRiverTunnels on. New chunks only.")
                                .define("normalTunnelPillars", true);
                NORMAL_TUNNEL_FORMATIONS = BUILDER
                                .comment("Rock rising from the bed and hanging from the roof of a lowland river tunnel, as in a cave.\n"
                                                + "Only with normalRiverTunnels on. New chunks only.")
                                .define("normalTunnelFormations", true);
                DELTA_INLAND_REACH = BUILDER
                                .comment("How far inland a big lowland river's delta may begin, in blocks up the river from the coast.\n"
                                                + "Its arms and creeks spread from there to the sea, so lower = deltas nearer the coast.\n"
                                                + "0 = no deltas at all; under 90 counts as 90; 512 = as far as the river's size allows.\n"
                                                + "New chunks only - best set before creating a world, or a delta may not line up with\n"
                                                + "the terrain already generated.")
                                .defineInRange("deltaInlandReach", 256, 0, 512);
                LOWLAND_RIVER_WIDTH = BUILDER
                                .comment("How wide the lowland rivers are: their water's width times this. 1.0 = default.\n"
                                                + "WARNING: NEW WORLDS ONLY. A world keeps the width it was created with: worlds created\n"
                                                + "before this option keep 1.0, and changing it later does nothing to an existing world.")
                                .defineInRange("lowlandRiverWidth", 1.0, 0.5, 2.0);
                LOWLAND_RIVER_DEPTH = BUILDER
                                .comment("How deep the lowland rivers are: their bed's depth under the water times this. 1.0 = default.\n"
                                                + "WARNING: NEW WORLDS ONLY, as lowlandRiverWidth.")
                                .defineInRange("lowlandRiverDepth", 1.0, 0.5, 2.0);
                LOWLAND_RIVER_VALLEY = BUILDER
                                .comment("How a lowland river is cut into higher ground.\n"
                                                + "GORGE = steep banks, and a gorge's walls where the ground is high.\n"
                                                + "VALLEY = a wide, smooth valley: a strip of flat ground at the water, slopes rising gently,\n"
                                                + "the hills around cut back much further; where they are higher still, the slope goes on up\n"
                                                + "steeper, as a gorge's wall.\n"
                                                + "VALLEY_GORGE, GORGE_TUNNEL, VALLEY_GORGE_TUNNEL = each of these where lowlandRiverCutBy puts\n"
                                                + "it, changing gradually along the river: a tunnel runs closed under the ground, as with\n"
                                                + "normalRiverTunnels. VALLEY_GORGE is the default. GORGE_TUNNEL and VALLEY_GORGE_TUNNEL are\n"
                                                + "experimental. Only with normalRiverTunnels off.\n"
                                                + "WARNING: NEW WORLDS ONLY, as lowlandRiverWidth - and so are the five settings below.")
                                .defineEnum("lowlandRiverValley", LowlandValley.VALLEY_GORGE);
                LOWLAND_CUT_BY = BUILDER
                                .comment("For VALLEY_GORGE, GORGE_TUNNEL and VALLEY_GORGE_TUNNEL: what chooses the cut along a river.\n"
                                                + "HEIGHT = how high the ground stands over the river: a valley where it is under\n"
                                                + "lowlandValleyBelow, a tunnel where it is over lowlandTunnelAbove, a gorge between (default).\n"
                                                + "BIOME = the land's biome along the river: a valley in lowlandValleyBiomes, a tunnel in\n"
                                                + "lowlandTunnelBiomes, a gorge in every other.")
                                .defineEnum("lowlandRiverCutBy", LowlandCutBy.HEIGHT);
                LOWLAND_VALLEY_BELOW = BUILDER
                                .comment("With lowlandRiverCutBy HEIGHT: a valley where the ground over the river stands under this Y,\n"
                                                + "a gorge (or a tunnel) where it is higher.")
                                .defineInRange("lowlandValleyBelow", 100.0, 64.0, 2000.0);
                LOWLAND_TUNNEL_ABOVE = BUILDER
                                .comment("With lowlandRiverCutBy HEIGHT: a tunnel where the ground over the river stands over this Y.")
                                .defineInRange("lowlandTunnelAbove", 150.0, 64.0, 2000.0);
                LOWLAND_VALLEY_BIOMES = BUILDER
                                .comment("With lowlandRiverCutBy BIOME: the biomes a lowland river runs through in a valley. A biome's\n"
                                                + "id (minecraft:plains), a tag after # (#minecraft:is_forest), or an id ending in * for\n"
                                                + "every biome it begins (terralith:*). A biome in both lists is a valley.")
                                .defineListAllowEmpty(java.util.Collections.singletonList("lowlandValleyBiomes"),
                                                () -> java.util.List.of("#minecraft:is_forest", "#minecraft:is_taiga",
                                                                "#minecraft:is_savanna", "#minecraft:is_jungle", "minecraft:plains",
                                                                "minecraft:sunflower_plains", "minecraft:snowy_plains",
                                                                "minecraft:meadow", "minecraft:cherry_grove", "minecraft:swamp",
                                                                "minecraft:mangrove_swamp"),
                                                o -> o instanceof String);
                LOWLAND_TUNNEL_BIOMES = BUILDER
                                .comment("With lowlandRiverCutBy BIOME: the biomes a lowland river runs through in a tunnel, as\n"
                                                + "lowlandValleyBiomes.")
                                .defineListAllowEmpty(java.util.Collections.singletonList("lowlandTunnelBiomes"),
                                                () -> java.util.List.of("minecraft:jagged_peaks", "minecraft:frozen_peaks",
                                                                "minecraft:stony_peaks", "minecraft:snowy_slopes"),
                                                o -> o instanceof String);
                RIVER_SPLITS = BUILDER
                                .comment("How often a lowland river divides for a stretch: its water runs in two or three arms round\n"
                                                + "low islands, then comes together again. Mountain rivers never do. On open grassland it does\n"
                                                + "more often, round gravel bars (with riverBeaches on).\n"
                                                + "OFF = never. RARE = here and there. ENOUGH = on one stretch in five (default).\n"
                                                + "NORMAL = on many a stretch. OFTEN = on more stretches than not. New chunks only.")
                                .defineEnum("riverSplits", RiverSplits.ENOUGH);
                RIVER_FALLEN_LOGS = BUILDER
                                .comment("How often a fallen trunk lies over the water between a lowland river's islands, of the wood\n"
                                                + "of the trees around it. Only with riverSplits on and normalRiverTunnels off. New chunks only.")
                                .defineEnum("riverFallenLogs", FallenLogs.NORMAL);
                OXBOW_LAKES = BUILDER
                                .comment("Oxbow lakes beside the lowland rivers: an old bend the river has cut off, a still crescent of\n"
                                                + "water on the outside of a bend, a strip of land from the river, its reeds and lily pads.\n"
                                                + "Only on low, flat land. New chunks only.")
                                .define("oxbowLakes", true);
                ENABLE_watersheds = BUILDER
                                .comment("Generate mountain rivers: rivers that follow the terrain's own drainage down from the mountains.\n"
                                                + "Off = faster world generation.")
                                .define("enableWatersheds", true);
                MOUNTAIN_RIVER_DENSITY = BUILDER
                                .comment("Density of mountain rivers. Mountain rivers follow the terrain's real drainage: a stream starts wherever enough terrain drains into one spot.\n"
                                                + "Each step down roughly halves the streams: VERY_LOW = only the largest valleys carry a river, MEDIUM = half\n"
                                                + "of the default, HIGH = default, INSANE = a dense network of small streams.")
                                .defineEnum("mountainRiverDensity", DensityLevel.HIGH);
                MOUNTAIN_RIVER_WIDTH = BUILDER
                                .comment("How wide the mountain rivers are: their channel's width times this. 1.0 = default.\n"
                                                + "WARNING: NEW WORLDS ONLY. A world keeps the width it was created with: worlds created\n"
                                                + "before this option keep 1.0, and changing it later does nothing to an existing world.")
                                .defineInRange("mountainRiverWidth", 1.0, 0.5, 2.0);
                MOUNTAIN_RIVER_DEPTH = BUILDER
                                .comment("How deep the mountain rivers are: their bed's depth under the water times this. 1.0 = default.\n"
                                                + "WARNING: NEW WORLDS ONLY, as mountainRiverWidth.")
                                .defineInRange("mountainRiverDepth", 1.0, 0.5, 2.0);
                MOUNTAIN_RIVER_TUNNELS = BUILDER
                                .comment("Let mountain rivers go underground. A river coming down a slope may go into the hillside,\n"
                                                + "run through the rock and come out further down; see the mountainTunnel options. Where one\n"
                                                + "meets a lowland river (or the sea) far below the surface it descends gradually and joins it\n"
                                                + "through a tunnel, instead of dropping in down a vertical shaft from the surface; and any\n"
                                                + "stretch running deep under the ground gets a roof rather than an open canyon. A lowland river\n"
                                                + "running as a tunnel under a mountain drains the mountain streams into itself; they reach it\n"
                                                + "underground. Off = the old sheer drop, and mountain rivers ignore lowland tunnels.\n"
                                                + "New chunks only.")
                                .define("mountainRiverTunnels", false);
                MOUNTAIN_TUNNEL_FREQUENCY = BUILDER
                                .comment("How often a mountain river goes into a hillside, on slopes with room for a tunnel.\n"
                                                + "OFTEN = wherever the ground has room for one. Only with mountainRiverTunnels on. New chunks only.")
                                .defineEnum("mountainTunnelFrequency", TunnelFrequency.NORMAL);
                MOUNTAIN_TUNNEL_LENGTH = BUILDER
                                .comment("Longest a mountain river runs level through the rock, in blocks, before its way in.\n"
                                                + "The tunnel ends sooner where the ground lets it. Only with mountainRiverTunnels on. New chunks only.")
                                .defineInRange("mountainTunnelLength", 256, 64, 640);
                MOUNTAIN_TUNNEL_DEPTH = BUILDER
                                .comment("How far under its old level a mountain river goes in a tunnel, in blocks. Deeper = more rock\n"
                                                + "over it and longer tunnels, on steeper ground. Only with mountainRiverTunnels on. New chunks only.")
                                .defineInRange("mountainTunnelDepth", 32, 16, 64);
                MOUNTAIN_TUNNEL_HEIGHT = BUILDER
                                .comment("Height of a mountain river tunnel's roof over the water, in blocks. A wide river's tunnel\n"
                                                + "is taller than this. Only with mountainRiverTunnels on. New chunks only.")
                                .defineInRange("mountainTunnelHeight", 8, 5, 24);
                MOUNTAIN_TUNNEL_LEDGE = BUILDER
                                .comment("Width of the dry ledge along each side of the water in a mountain river tunnel, in blocks.\n"
                                                + "Only with mountainRiverTunnels on. New chunks only.")
                                .defineInRange("mountainTunnelLedge", 3, 1, 8);
                MOUNTAIN_RIVER_WATERFALLS = BUILDER
                                .comment("How often a steep mountain stream breaks into a flat pool and a vertical waterfall.\n"
                                                + "Wherever the ground itself breaks off there is a waterfall at every setting; the rest are\n"
                                                + "cut into the stream's slope, never built up on it.\n"
                                                + "CLIFFS = only where the ground breaks off; elsewhere a steep stream runs down as rapids.\n"
                                                + "FEW    = a waterfall every 50-90 blocks of steep stream.\n"
                                                + "NORMAL = every 20-40 blocks (default).\n"
                                                + "MANY   = wherever the slope has room for one, every few blocks on a steep stream.\n"
                                                + "New chunks only.")
                                .defineEnum("mountainRiverWaterfalls", WaterfallLevel.NORMAL);
                WATERFALL_CAVES = BUILDER
                                .comment("Now and then a hollow in the rock behind a tall waterfall: a ledge over the pool below it,\n"
                                                + "reached through the falling water. New chunks only.")
                                .define("waterfallCaves", true);
                MOUNTAIN_RIVER_ICE = BUILDER
                                .comment("How mountain rivers freeze in cold land. Lakes, the sea and the lowland rivers in their\n"
                                                + "frozen river biome freeze either way.\n"
                                                + "OPEN = none of their running water freezes.\n"
                                                + "CALM = their calm stretches freeze like still water; waterfalls and rapids stay open, so\n"
                                                + "       the falling water does not run out over the ice (default).\n"
                                                + "FULL = all of it, waterfalls and rapids too: their running water freezes and stops running.")
                                .defineEnum("mountainRiverIce", MountainRiverIce.CALM);
                MOUNTAIN_RIVER_BIOME = BUILDER
                                .comment("Give the middle of a mountain river the river biome (the frozen river biome in cold land).\n"
                                                + "Banks, beds and plants stay those of the land round the river; it shows in the weather, the\n"
                                                + "mobs, the colours and the F3 screen. Only with dynamic lowland rivers. New chunks only.")
                                .define("mountainRiverBiome", true);
                ENABLE_MOUNTAIN_LAKES = BUILDER
                                .comment("Fill natural terrain depressions in the mountains with lakes, at whatever height the basin sits.")
                                .define("enableMountainLakes", true);
                LAKE_BIOME = BUILDER
                                .comment("Give the lakes' water the river biome (the frozen river biome in cold land), as mountainRiverBiome\n"
                                                + "gives it to the rivers: shores and plants stay those of the land round the lake; it shows in\n"
                                                + "the weather, the mobs, the colours and the F3 screen. Only with dynamic lowland rivers.\n"
                                                + "New chunks only.")
                                .define("lakeBiome", true);
                BIOME_BLACKLIST = BUILDER.comment(
                                "List of biome IDs that will be treated like oceans by Dynamic Waters: no river flow and no river carving.\n"
                                                + "Use the full biome resource location, e.g. minecraft:warm_ocean or mymod:dead_sea.\n"
                                                + "Supports wildcard suffixes when the entry ends with '*', e.g. mymod:*")
                                .defineListAllowEmpty(
                                                java.util.Collections.singletonList("biomeBlacklist"),
                                                () -> java.util.List.of(),
                                                o -> o instanceof String);
                RIVER_BOULDERS = BUILDER
                                .comment("Rounded boulders in the lowland rivers, along their shallows, and very rarely a rock in a\n"
                                                + "mountain stream's rapids. New chunks only.")
                                .define("riverBoulders", true);
                RIVER_ARCHES = BUILDER
                                .comment("Now and then a natural rock arch over a river running between high rock walls, of the\n"
                                                + "walls' own rock, the river running free under it. New chunks only.")
                                .define("riverArches", false);
                RIVER_FORDS = BUILDER
                                .comment("Fords in the lowland rivers: now and then the river runs shallow across, a block deep,\n"
                                                + "with round boulders across it and room for a boat between them, and often a log sunk on\n"
                                                + "the bed beside them. Off unless turned on. New chunks only.")
                                .define("riverFords", false);
                RIVER_BEACHES = BUILDER
                                .comment("A strip of shore along the lowland rivers and round their islands: a block or two of ground at\n"
                                                + "the water's level, and the bed just under it, by the biome: podzol and coarse dirt with cobbles\n"
                                                + "at the water in a wood, gravel and clay on grassland (the whole bed gravel), sand by the sea,\n"
                                                + "red sand and terracotta in the badlands. The gently running stretches of the mountain rivers\n"
                                                + "get the same on their banks. New chunks only.")
                                .define("riverBeaches", true);
                RIVERBANK_PLANTS = BUILDER
                                .comment("On the bank's top behind a river's shore: grass, tall grass, ferns and flowers by the biome,\n"
                                                + "in wooded land bushes of its trees' leaves; sugar cane at the water, small dripleaf in the\n"
                                                + "shallows. Only with riverBeaches on. New chunks only.")
                                .define("riverbankPlants", true);
                RIVERBANK_PLANT_DENSITY = BUILDER
                                .comment("How thickly the riverbank plants grow: 1.0 = default, 0.5 = half as many, 2.0 = twice as\n"
                                                + "many (as far as there is room). New chunks only.")
                                .defineInRange("riverbankPlantDensity", 1.0, 0.0, 2.0);
                RIVERBANK_PLANT_MIXES = BUILDER
                                .comment("Which plants grow on the bank's top behind a river's shore, by the land: each entry\n"
                                                + "\"<lands> -> <plant> <weight>, <plant> <weight>, ...\", the first whose lands match the\n"
                                                + "biome used. Lands: a biome's id (minecraft:plains), a tag after # (#minecraft:is_forest),\n"
                                                + "an id ending in * (terralith:*), * for any, or a kind of land as the shores have it:\n"
                                                + "@forest @grassland @lush @cold @dry @desert @badlands @beach. Plants: any plant block,\n"
                                                + "another mod's too (a tall one is set in its two halves); the weights are shares of the\n"
                                                + "plants. How many bank columns get one at all: riverbankPlantDensity. New chunks only.")
                                .defineListAllowEmpty(java.util.Collections.singletonList("riverbankPlantMixes"),
                                                () -> java.util.List.of(
                                                                "@desert @badlands -> minecraft:dead_bush 1",
                                                                "@grassland -> minecraft:tall_grass 70, minecraft:oxeye_daisy 1.5, minecraft:cornflower 1.5, "
                                                                                + "minecraft:azure_bluet 1, minecraft:dandelion 1, minecraft:poppy 0.7, minecraft:allium 0.7, "
                                                                                + "minecraft:peony 0.4, minecraft:lilac 0.4, minecraft:short_grass 22.8",
                                                                "@cold -> minecraft:tall_grass 55, minecraft:fern 10, minecraft:large_fern 4, minecraft:short_grass 31",
                                                                "@lush -> minecraft:tall_grass 55, minecraft:fern 10, minecraft:large_fern 5, "
                                                                                + "minecraft:blue_orchid 2, minecraft:short_grass 28",
                                                                "@dry -> minecraft:tall_grass 60, minecraft:short_grass 40",
                                                                "* -> minecraft:tall_grass 65, minecraft:fern 7, minecraft:large_fern 3, minecraft:dandelion 1, "
                                                                                + "minecraft:poppy 1, minecraft:short_grass 23"),
                                                o -> o instanceof String);
                RIVERBANK_BUSH_LEAVES = BUILDER
                                .comment("The leaves of the bushes on the bank's top in wooded land, by the land: each entry\n"
                                                + "\"<lands> -> <leaves>\", lands as riverbankPlantMixes, the first that matches used.\n"
                                                + "<leaves>: a leaves block (any mod's), \"auto\" = the leaves of the biome's own trees,\n"
                                                + "found in what it grows - another mod's biomes and trees alike - or \"none\" = no bushes.\n"
                                                + "New chunks only.")
                                .defineListAllowEmpty(java.util.Collections.singletonList("riverbankBushLeaves"),
                                                () -> java.util.List.of("minecraft:jungle minecraft:sparse_jungle -> minecraft:jungle_leaves",
                                                                "* -> auto"),
                                                o -> o instanceof String);
                RIVERBANK_OWN_PLANTS = BUILDER
                                .comment("The bank's top also grows other mods' plants a biome grows - its grasses, ferns and\n"
                                                + "flowers, found in what it grows - in the share the biome has them, besides the plants\n"
                                                + "riverbankPlantMixes names: another mod's biomes, and vanilla ones that mod adds plants to.\n"
                                                + "Without other mods, nothing changes. New chunks only.")
                                .define("riverbankOwnPlants", true);
                RAGGED_SHORES = BUILDER
                                .comment("How often a lowland river's shore breaks up for a stretch into tufts of grass and earth with\n"
                                                + "puddles and runnels of the river's water between them, in temperate land. Only with\n"
                                                + "riverBeaches on. OFF, RARE, NORMAL (default), OFTEN. New chunks only.")
                                .defineEnum("raggedShores", RaggedShores.NORMAL);
                GRAVEL_BAR_BIOMES = BUILDER
                                .comment("The biomes where a lowland river's islands are gravel bars - low, flat and all gravel, no\n"
                                                + "trees - and where it divides round them more often. Elsewhere its islands are of the land,\n"
                                                + "trees and all. A biome's id (minecraft:plains), a tag after # (#minecraft:is_forest), or an id\n"
                                                + "ending in * (terralith:*). Empty = no gravel bars. Only with riverBeaches on. New chunks only.")
                                .defineListAllowEmpty(java.util.Collections.singletonList("gravelBarBiomes"),
                                                () -> java.util.List.of("minecraft:plains"), o -> o instanceof String);
                RIVERBANK_PALETTES = BUILDER
                                .comment("Lay riverbank palettes on the ground rivers (lowland and mountain) and lakes cut: the bed\n"
                                                + "sorted by the current - gravel and cobbles where the water runs hard, sand, clay and silt\n"
                                                + "where it slackens - spots of shingle, sand, roots or mud along the shore, sand and gravel bars\n"
                                                + "inside the bends; each laid in spots on one ground, by the biome. A lowland river keeps its\n"
                                                + "river biome but takes the palette of the land it runs through. The palettes are JSON files in\n"
                                                + "config/dynamicwaters/riverbanks/; edit them in game (Riverbank palettes) or see the README\n"
                                                + "there. New chunks only.")
                                .define("riverbankPalettes", true);
                WATER_PLANTS = BUILDER
                                .comment("Plants in and along the rivers (lowland and mountain) and lakes: kelp in the lakes, seagrass\n"
                                                + "in the mountain rivers and lakes, lily pads and reeds along calm water, a few sea pickles in\n"
                                                + "warm waters - by the biome, a list for the lowland rivers, one for the mountain rivers and one\n"
                                                + "for the lakes. The sets are JSON files in config/dynamicwaters/water_plants/; edit them in game\n"
                                                + "(Water plants) or see the README there. Off = none of them. New chunks only.")
                                .define("waterPlants", true);
                BUILDER.pop();

                BUILDER.comment("River ambience.").push("Sounds");
                ENABLE_RIVER_SOUND = BUILDER.comment("Enable the ambient flowing sound for rivers.").define(
                                "enableRiverSound",
                                true);
                BUILDER.pop();

                BUILDER.comment("The welcome message, and compatibility with other mods.").push("General");
                SHOW_WELCOME_MESSAGE = BUILDER.comment("Show the welcome message when joining the world.")
                                .define("showWelcomeMessage", true);
                DISABLE_TECTONIC_UNDERGROUND_RIVERS = BUILDER.comment(
                                "Automatically disable underground rivers from the Tectonic mod to improve performance and compatibility with Dynamic Waters.")
                                .define("disableTectonicUndergroundRivers", true);
                BUILDER.pop();

                SPEC = BUILDER.build();
        }

        public static NormalRiverMode normalRiverMode() {
                try {
                        return NORMAL_RIVER_MODE.get();
                } catch (IllegalStateException notLoadedYet) {
                        return NormalRiverMode.DYNAMIC;
                }
        }

        public static WaterfallLevel waterfalls() {
                try {
                        return MOUNTAIN_RIVER_WATERFALLS.get();
                } catch (IllegalStateException notLoadedYet) {
                        return WaterfallLevel.NORMAL;
                }
        }

        public static boolean dynamicNormalRivers() {
                return normalRiverMode() == NormalRiverMode.DYNAMIC;
        }

        public static boolean vanillaNormalRivers() {
                return normalRiverMode() == NormalRiverMode.VANILLA;
        }

        private static java.util.List<? extends String> lastCustomSpeedsStr = null;
        private static final java.util.Map<String, Double> parsedCustomSpeeds = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>>, Double> parsedTagSpeeds = new java.util.concurrent.ConcurrentHashMap<>();

        private static volatile java.util.List<? extends String> lastBiomeBlacklistRaw = null;
        private static volatile java.util.Set<String> parsedBiomeExact = java.util.Collections.emptySet();
        private static volatile java.util.List<String> parsedBiomeWildcardPrefixes = java.util.Collections.emptyList();

        private static void updateBiomeBlacklist() {
                java.util.List<? extends String> current = BIOME_BLACKLIST.get();
                if (current == lastBiomeBlacklistRaw)
                        return;
                lastBiomeBlacklistRaw = current;
                java.util.Set<String> exact = new java.util.HashSet<>();
                java.util.List<String> prefixes = new java.util.ArrayList<>();
                if (current != null) {
                        for (String entry : current) {
                                String trimmed = entry.trim();
                                if (trimmed.endsWith("*")) {
                                        prefixes.add(trimmed.substring(0, trimmed.length() - 1));
                                } else if (!trimmed.isEmpty()) {
                                        exact.add(trimmed);
                                }
                        }
                }
                parsedBiomeExact = java.util.Collections.unmodifiableSet(exact);
                parsedBiomeWildcardPrefixes = java.util.Collections.unmodifiableList(prefixes);
        }

        public static int hexColor(String text) {
                if (text == null) return -1;
                String s = text.trim();
                if (s.startsWith("#")) s = s.substring(1);
                if (s.length() != 6) return -1;
                try {
                        return Integer.parseInt(s, 16);
                } catch (NumberFormatException notHex) {
                        return -1;
                }
        }

        public static boolean isBiomeBlacklisted(net.minecraft.resources.ResourceLocation biomeId) {
                if (biomeId == null)
                        return false;
                updateBiomeBlacklist();
                String idStr = biomeId.toString();
                if (parsedBiomeExact.contains(idStr))
                        return true;
                for (String prefix : parsedBiomeWildcardPrefixes) {
                        if (idStr.startsWith(prefix))
                                return true;
                }
                return false;
        }

        public static boolean isBiomeBlacklisted(
                        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder) {
                if (biomeHolder == null)
                        return false;
                net.minecraft.resources.ResourceLocation id = biomeHolder.unwrapKey()
                                .map(net.minecraft.resources.ResourceKey::location).orElse(null);
                return isBiomeBlacklisted(id);
        }

        private static void updateCustomSpeeds() {
                java.util.List<? extends String> currentList = CUSTOM_SPEEDS.get();
                if (currentList != lastCustomSpeedsStr) {
                        parsedCustomSpeeds.clear();
                        parsedTagSpeeds.clear();
                        if (currentList != null) {
                                for (String entry : currentList) {
                                        String[] split = entry.split("=");
                                        if (split.length == 2) {
                                                try {
                                                        String key = split[0].trim();
                                                        double val = Double.parseDouble(split[1].trim());
                                                        if (key.startsWith("#")) {
                                                                String tagName = key.substring(1);
                                                                net.minecraft.resources.ResourceLocation resLoc = net.minecraft.resources.ResourceLocation
                                                                                .parse(tagName);
                                                                net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tag = net.minecraft.tags.TagKey
                                                                                .create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                                                                                resLoc);
                                                                parsedTagSpeeds.put(tag, val);
                                                        } else {
                                                                parsedCustomSpeeds.put(key, val);
                                                        }
                                                } catch (Exception ignored) {
                                                }
                                        }
                                }
                        }
                        lastCustomSpeedsStr = currentList;
                }
        }

        public static double getFlowMultiplierForEntity(net.minecraft.world.entity.Entity entity) {
                if (entity == null)
                        return DEFAULT_SPEED.get();

                updateCustomSpeeds();

                double baseSpeed = DEFAULT_SPEED.get();

                net.minecraft.resources.ResourceLocation rl = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                                .getKey(entity.getType());
                if (rl != null) {
                        String entityId = rl.toString();
                        if (parsedCustomSpeeds.containsKey(entityId)) {
                                baseSpeed = parsedCustomSpeeds.get(entityId);
                                return applyWeatherBoost(baseSpeed, entity);
                        }
                }

                for (java.util.Map.Entry<net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>>, Double> entry : parsedTagSpeeds
                                .entrySet()) {
                        if (entity.getType().is(entry.getKey())) {
                                baseSpeed = entry.getValue();
                                return applyWeatherBoost(baseSpeed, entity);
                        }
                }

                if (entity instanceof net.minecraft.world.entity.player.Player) {
                        baseSpeed = PLAYER_SPEED.get();
                } else if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                        baseSpeed = ITEM_SPEED.get();
                } else if (entity instanceof net.minecraft.world.entity.animal.WaterAnimal
                                || entity instanceof net.minecraft.world.entity.animal.axolotl.Axolotl) {
                        baseSpeed = WATER_MOB_SPEED.get();
                } else if (entity instanceof net.minecraft.world.entity.animal.Animal) {
                        baseSpeed = ANIMAL_SPEED.get();
                }

                return applyWeatherBoost(baseSpeed, entity);
        }

        private static double applyWeatherBoost(double baseSpeed, net.minecraft.world.entity.Entity entity) {
                if (entity == null || entity.level() == null) return baseSpeed;
                final net.minecraft.world.level.Level level = entity.level();
                final float rain = level.getRainLevel(1.0F);
                if (rain <= 0.0F) return baseSpeed;
                final net.minecraft.core.BlockPos pos = entity.blockPosition();
                if (level.getBiome(pos).value().getPrecipitationAt(pos)
                                != net.minecraft.world.level.biome.Biome.Precipitation.RAIN) {
                        return baseSpeed;
                }
                final double storm = level.getThunderLevel(1.0F);
                final double boost = RAIN_CURRENT_BOOST.get() + (STORM_CURRENT_BOOST.get() - RAIN_CURRENT_BOOST.get()) * storm;
                return baseSpeed * (1.0 + boost * rain);
        }
}
