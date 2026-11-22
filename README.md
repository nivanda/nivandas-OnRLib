# nivanda's OnRLib

A core Minecraft **1.21.1 / NeoForge 21.1.248+** mod providing component-driven
metal ingots and their tooltip properties. Build and run with **Java 21**.

The library currently supplies 101 metal appearances and descriptive stats. Ore
generation, recipes and gameplay effects for those stats are not implemented.

## Build

```sh
sh gradlew clean build
```

The distributable mod is written to `build/libs/`.

## Create and update ingots

Call these methods after mod registration has finished:

```java
ItemStack mithril = Ingot.create(Metal.MITHRIL);
Ingot.setResistance(mithril, 150);    // stores 100, the maximum valid resistance
Ingot.setMeltingPoint(mithril, 3);
Ingot.setMagnetic(mithril, true);

ItemStack numeric = Ingot.create(1.5f); // historical numeric API: canonical Mithril ID 1
```

Numeric bounds are: melting point 1–3, resistance 1–100, heat transfer rate 0–100,
and weight 1–100. Boolean setters are `setMagnetic`, `setConductive`,
`setArcanePowers`, `setRadioactive` and `setStable`.

Use these helpers to keep components canonical before putting stacks into
inventories. Integrations that set components directly can call
`Ingot.normalizeComponents(stack)` after their changes. A raw `ItemStack.set`
can still temporarily construct noncanonical in-memory data; the library does
not intercept Minecraft's generic component mutation API. Getters are read-only.

## Data compatibility and normalization

- The existing numeric metal IDs **0–100 retain their meanings**, and the saved
  float/network-float representation is unchanged. `Metal` declares explicit IDs;
  its declaration order does not determine saved identity. Never reuse a
  historical ID or rename its serialized name.
- Fractional IDs use the historical floor semantics: `1.5` becomes `1` (Mithril).
  Negative IDs and negative infinity become iron (`0`); IDs above the maximum
  and positive infinity become the highest defined ID. NaN becomes iron.
  Negative zero is canonicalized to positive zero.
- Numeric stats are clamped on **both encoding and decoding**, for persistence
  and network codecs. A malformed raw stat that displays as 100 also saves and
  transmits as 100, instead of failing save encoding.
- Loaded, copied and bulk-updated ingot stacks are normalized through
  `verifyComponentsAfterLoad`. Explicit default components are removed so the
  safe API does not produce duplicate forms of otherwise identical ingots.
  Unrelated components such as custom names are preserved.

This is a normalization policy: malformed numeric input is repaired, rather
than rejected. Other mods should inspect the canonical values returned by the
library, rather than relying on out-of-range raw component values.

## Regression tests

The independent GameTests cover old saves, all 101 historical identities, all
32 boolean combinations, stacking/default normalization, raw network inputs,
10,000 malformed-stack serialization round trips and 100,000 factory inputs.
All tests are required; a failed assertion fails the GameTest server run.

```sh
sh gradlew -I gradle/regression-tests.gradle runGameTestServer
```

The optional init script includes `src/gametest` only for development. Plain
builds exclude the test classes and resources. Results are written to
`build/reports/gametest.xml`. The GameTest server uses a development test world;
it does not modify personal Minecraft saves.

After running the tests, use `sh gradlew clean build` to produce the distributable
without development-only test classes.

## Mapping and template information

The project uses Mojang mapping names under their applicable license:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

`TEMPLATE_LICENSE.txt` applies to the original NeoForged MDK template files.
The mod metadata declares All Rights Reserved for the mod itself.

NeoForge documentation: https://docs.neoforged.net/
