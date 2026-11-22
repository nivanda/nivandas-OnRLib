package net.nivanda.onrlib.item;

import java.util.Arrays;
import java.util.List;

/** Stable IDs used by existing item saves and model predicates. Never reuse an ID. */
public enum Metal {
    IRON(0, "iron"),
    MITHRIL(1, "mithril"),
    ADAMANTITE(2, "adamantite"),
    ORICHALCUM(3, "orichalcum"),
    STARSTEEL(4, "starsteel"),
    MOONSILVER(5, "moonsilver"),
    SUNBRONZE(6, "sunbronze"),
    VOIDIRON(7, "voidiron"),
    DRAGONSTEEL(8, "dragonsteel"),
    AETHERIUM(9, "aetherium"),
    SHADOWSTEEL(10, "shadowsteel"),
    BLOODIRON(11, "bloodiron"),
    FROSTSILVER(12, "frostsilver"),
    EMBERITE(13, "emberite"),
    STORMSTEEL(14, "stormsteel"),
    DUSKGOLD(15, "duskgold"),
    DAWNBRONZE(16, "dawnbronze"),
    VERDANITE(17, "verdanite"),
    AZURITE(18, "azurite"),
    CRIMSONITE(19, "crimsonite"),
    NIGHTSILVER(20, "nightsilver"),
    RUNESTEEL(21, "runesteel"),
    SOULSILVER(22, "soulsilver"),
    NETHERBRASS(23, "netherbrass"),
    SKYIRON(24, "skyiron"),
    DEEPIRON(25, "deepiron"),
    THORNSTEEL(26, "thornsteel"),
    VENOMITE(27, "venomite"),
    GLIMMERITE(28, "glimmerite"),
    ARCANIUM(29, "arcanium"),
    CELESTIUM(30, "celestium"),
    INFERNIUM(31, "infernium"),
    GLACIUM(32, "glacium"),
    TERRANITE(33, "terranite"),
    TIDESTEEL(34, "tidesteel"),
    LUMINITE(35, "luminite"),
    OBSCURIUM(36, "obscurium"),
    PYROSTEEL(37, "pyrosteel"),
    CRYOSTEEL(38, "cryosteel"),
    GALVANITE(39, "galvanite"),
    PHANTOMSILVER(40, "phantomsilver"),
    WRAITHIRON(41, "wraithiron"),
    SPIRITBRONZE(42, "spiritbronze"),
    ELDERBRASS(43, "elderbrass"),
    ANCIENTITE(44, "ancientite"),
    PRIMORDIUM(45, "primordium"),
    ZEPHYRITE(46, "zephyrite"),
    QUICKSTEEL(47, "quicksteel"),
    HEARTGOLD(48, "heartgold"),
    ROSESTEEL(49, "rosesteel"),
    VIOLIUM(50, "violium"),
    INDIGITE(51, "indigite"),
    CERULEUM(52, "ceruleum"),
    VIRIDIUM(53, "viridium"),
    AMBERITE(54, "amberite"),
    SCARLETITE(55, "scarletite"),
    IVORITE(56, "ivorite"),
    ONYXIUM(57, "onyxium"),
    PEARLSTEEL(58, "pearlsteel"),
    CORALITE(59, "coralite"),
    JADESTEEL(60, "jadesteel"),
    OPALINE(61, "opaline"),
    GARNETITE(62, "garnetite"),
    TOPAZIUM(63, "topazium"),
    SAPPHIRIUM(64, "sapphirium"),
    EMERALDINE(65, "emeraldine"),
    RUBIUM(66, "rubium"),
    AMETHINE(67, "amethine"),
    CITRINIUM(68, "citrinium"),
    TURQUOISITE(69, "turquoisite"),
    MAGMASTEEL(70, "magmasteel"),
    LAVABRONZE(71, "lavabronze"),
    ASHIRON(72, "ashiron"),
    CINDERITE(73, "cinderite"),
    SMOKESTEEL(74, "smokesteel"),
    MISTSILVER(75, "mistsilver"),
    CLOUDSTEEL(76, "cloudsteel"),
    THUNDERITE(77, "thunderite"),
    LIGHTNINGSTEEL(78, "lightningsteel"),
    GALESILVER(79, "galesilver"),
    TEMPESTITE(80, "tempestite"),
    OCEANITE(81, "oceanite"),
    ABYSSIUM(82, "abyssium"),
    REEFBRONZE(83, "reefbronze"),
    SWAMPIRON(84, "swampiron"),
    MOSSTEEL(85, "mossteel"),
    BLOOMSILVER(86, "bloomsilver"),
    PETALITE(87, "petalite"),
    HONEYGOLD(88, "honeygold"),
    WYRMBRASS(89, "wyrmbrass"),
    GRIFFONSTEEL(90, "griffonsteel"),
    PHOENIXITE(91, "phoenixite"),
    HYDRASTEEL(92, "hydrasteel"),
    KRAKENITE(93, "krakenite"),
    TITANBRONZE(94, "titanbronze"),
    GIANTSTEEL(95, "giantsteel"),
    FAIRYSILVER(96, "fairysilver"),
    PIXIEGOLD(97, "pixiegold"),
    DEMONITE(98, "demonite"),
    ANGELITE(99, "angelite"),
    CHAOSTEEL(100, "chaosteel");

    private final int id;
    private final String serializedName;
    private static final Metal[] BY_ID;
    public static final int MAX_ID;
    public static final List<String> NAMES;

    static {
        MAX_ID = Arrays.stream(values()).mapToInt(Metal::id).max().orElseThrow();
        BY_ID = new Metal[MAX_ID + 1];
        for (Metal metal : values()) {
            if (metal.id < 0 || BY_ID[metal.id] != null) {
                throw new IllegalStateException("Duplicate or negative metal ID: " + metal.id);
            }
            BY_ID[metal.id] = metal;
        }
        if (Arrays.stream(BY_ID).anyMatch(java.util.Objects::isNull)) {
            throw new IllegalStateException("Metal IDs must be contiguous; do not remove historical IDs");
        }
        NAMES = Arrays.stream(BY_ID).map(Metal::serializedName).toList();
    }

    Metal(int id, String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    public int id() { return id; }
    public String serializedName() { return serializedName; }

    public static Metal byId(int id) {
        if (id < 0 || id > MAX_ID) throw new IllegalArgumentException("Unknown metal ID: " + id);
        return BY_ID[id];
    }

    /** Keep historical floor/clamp semantics, canonicalize -0, and recover NaN as iron. */
    public static float normalizeId(float value) {
        if (Float.isNaN(value)) return 0f;
        return (float) (int) Math.max(0f, Math.min((float) MAX_ID, value));
    }
}
