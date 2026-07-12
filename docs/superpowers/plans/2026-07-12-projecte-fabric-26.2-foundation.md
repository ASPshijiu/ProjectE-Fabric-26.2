# ProjectE Fabric 26.2 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Create a reproducible, buildable Fabric 26.2 ProjectE repository with licensing, entrypoints, checked EMC numeric primitives, a deterministic ProjectE 1.21.1 baseline exporter, CI, and the initial completeness matrix.

**Architecture:** Start a new ProjectE-Fabric-26.2 Git repository from Fabric's official 26.2 example configuration. Keep public API and loader-neutral EMC primitives in main sources, client-only code in the split client source set, and porting-analysis code in an isolated porting source set that is never packaged into the mod jar.

**Tech Stack:** Minecraft 26.2, Java 25, Gradle 9.5.1, Fabric Loom 1.17-SNAPSHOT, Fabric Loader 0.19.3, Fabric API 0.154.2+26.2, JUnit 5.11.4, Mojang mappings.

## Global Constraints

- ProjectE commit 15d4ce65bd06eb4222709b984255fbf5080e78bc is the minimum parity baseline.
- Minecraft version is exactly 26.2 and Java release is exactly 25.
- Use Loom 1.17-SNAPSHOT, Gradle 9.5.1, Loader 0.19.3, and Fabric API 0.154.2+26.2.
- Mod ID remains projecte and Java packages remain under moze_intel.projecte.
- Preserve the ProjectE MIT license and copyright notice.
- Main sources must not reference NeoForge classes or client-only classes.
- Porting tools and upstream snapshots must not be packaged in the runtime jar.
- Every task ends in a build/test command and a focused Git commit.
- Do not add empty behavior methods, permanent default returns, or untracked parity exclusions.

---

## File Structure

The foundation phase creates this structure:

- ProjectE-Fabric-26.2/settings.gradle — plugin repositories and root project name.
- ProjectE-Fabric-26.2/build.gradle — Loom, split source sets, JUnit, porting source set, baseline tasks, jar and publication configuration.
- ProjectE-Fabric-26.2/gradle.properties — exact toolchain and dependency versions.
- ProjectE-Fabric-26.2/gradle/wrapper/* — Gradle 9.5.1 wrapper copied from the official Fabric 26.2 example.
- ProjectE-Fabric-26.2/LICENSE — upstream ProjectE MIT license.
- ProjectE-Fabric-26.2/NOTICE.md — source provenance and frozen upstream commits.
- ProjectE-Fabric-26.2/src/main/resources/fabric.mod.json — runtime metadata and main entrypoint.
- ProjectE-Fabric-26.2/src/main/java/moze_intel/projecte/ProjectE.java — common initializer.
- ProjectE-Fabric-26.2/src/main/java/moze_intel/projecte/api/ProjectEAPI.java — stable mod identity API.
- ProjectE-Fabric-26.2/src/main/java/moze_intel/projecte/emc/EmcValue.java — non-negative checked long EMC value.
- ProjectE-Fabric-26.2/src/client/java/moze_intel/projecte/client/ProjectEClient.java — client initializer.
- ProjectE-Fabric-26.2/src/porting/java/moze_intel/projecte/porting/BaselineExporter.java — deterministic upstream inventory exporter.
- ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/emc/EmcValueTest.java — numeric primitive tests.
- ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/porting/BaselineExporterTest.java — exporter fixture test.
- ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/packaging/PackagedJarTest.java — runtime jar audit.
- ProjectE-Fabric-26.2/docs/porting/baseline/projecte-1.21.1.json — frozen source/resource manifest.
- ProjectE-Fabric-26.2/docs/porting/feature-matrix.md — authoritative parity tracker.
- ProjectE-Fabric-26.2/.github/workflows/build.yml — build, test and deterministic baseline verification.

---

### Task 1: Bootstrap the Fabric 26.2 Repository

**Files:**
- Create: ProjectE-Fabric-26.2/.gitignore
- Create: ProjectE-Fabric-26.2/settings.gradle
- Create: ProjectE-Fabric-26.2/gradle.properties
- Create: ProjectE-Fabric-26.2/build.gradle
- Copy: .upstream/fabric-example-mod-26.2/gradlew
- Copy: .upstream/fabric-example-mod-26.2/gradlew.bat
- Copy: .upstream/fabric-example-mod-26.2/gradle/wrapper/gradle-wrapper.jar
- Copy: .upstream/fabric-example-mod-26.2/gradle/wrapper/gradle-wrapper.properties
- Copy: docs/superpowers/specs/2026-07-12-projecte-fabric-26.2-design.md
- Copy: docs/superpowers/plans/2026-07-12-projecte-fabric-26.2-roadmap.md

**Interfaces:**
- Consumes: FabricMC/fabric-example-mod branch 26.2 frozen in .upstream/fabric-example-mod-26.2.
- Produces: A Gradle project named ProjectE-Fabric-26.2 with main, client and porting source sets.

- [ ] **Step 1: Create and initialize the repository**

Run from C:/Users/xw130/Desktop/mcmod:

~~~powershell
New-Item -ItemType Directory -Path ProjectE-Fabric-26.2
git -C ProjectE-Fabric-26.2 init
git -C ProjectE-Fabric-26.2 branch -M main
~~~

Expected: an empty Git repository on branch main.

- [ ] **Step 2: Copy the official Gradle wrapper and approved documents**

~~~powershell
Copy-Item .upstream/fabric-example-mod-26.2/gradlew ProjectE-Fabric-26.2/gradlew
Copy-Item .upstream/fabric-example-mod-26.2/gradlew.bat ProjectE-Fabric-26.2/gradlew.bat
New-Item -ItemType Directory -Path ProjectE-Fabric-26.2/gradle/wrapper
Copy-Item .upstream/fabric-example-mod-26.2/gradle/wrapper/* ProjectE-Fabric-26.2/gradle/wrapper/
New-Item -ItemType Directory -Path ProjectE-Fabric-26.2/docs/superpowers/specs
New-Item -ItemType Directory -Path ProjectE-Fabric-26.2/docs/superpowers/plans
Copy-Item docs/superpowers/specs/2026-07-12-projecte-fabric-26.2-design.md ProjectE-Fabric-26.2/docs/superpowers/specs/
Copy-Item docs/superpowers/plans/2026-07-12-projecte-fabric-26.2-roadmap.md ProjectE-Fabric-26.2/docs/superpowers/plans/
~~~

Expected: wrapper and approved documents exist under the new repository.

- [ ] **Step 3: Add exact project settings**

Create settings.gradle:

~~~groovy
pluginManagement {
    repositories {
        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = 'ProjectE-Fabric-26.2'
~~~

Create gradle.properties:

~~~properties
org.gradle.jvmargs=-Xmx4G -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.configuration-cache=false
minecraft_version=26.2
loader_version=0.19.3
loom_version=1.17-SNAPSHOT
fabric_api_version=0.154.2+26.2
junit_version=5.11.4
junit_platform_version=1.11.4
mod_version=0.1.0-alpha.1
maven_group=moze_intel.projecte
archives_base_name=projecte-fabric
~~~

Create .gitignore:

~~~gitignore
.gradle/
build/
run/
out/
.idea/
*.iml
.DS_Store
~~~

- [ ] **Step 4: Add the minimal build script**

Create build.gradle:

~~~groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group
base { archivesName = project.archives_base_name }

loom {
    splitEnvironmentSourceSets()
    mods {
        projecte {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}

sourceSets {
    porting {
        java.srcDir 'src/porting/java'
        resources.srcDir 'src/porting/resources'
        compileClasspath += sourceSets.main.compileClasspath
        runtimeClasspath += output + compileClasspath
    }
    test {
        compileClasspath += sourceSets.porting.output
        runtimeClasspath += sourceSets.porting.output
    }
}

configurations {
    portingImplementation.extendsFrom implementation
    testImplementation.extendsFrom portingImplementation
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
    testImplementation "org.junit.jupiter:junit-jupiter:${junit_version}"
    testRuntimeOnly "org.junit.platform:junit-platform-launcher:${junit_platform_version}"
}

processResources {
    inputs.property 'version', project.version
    filesMatching('fabric.mod.json') { expand version: project.version }
}

tasks.withType(JavaCompile).configureEach {
    options.release = 25
    options.encoding = 'UTF-8'
}

tasks.withType(Test).configureEach {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

jar {
    from('LICENSE') { rename { "${it}_${base.archivesName.get()}" } }
}
~~~

- [ ] **Step 5: Verify the wrapper and dependency graph**

Run:

~~~powershell
cd ProjectE-Fabric-26.2
./gradlew.bat --version
./gradlew.bat dependencies --configuration runtimeClasspath
~~~

Expected: Gradle 9.5.1, JVM 25, Minecraft 26.2 dependencies, Loader 0.19.3 and Fabric API 0.154.2+26.2 resolve successfully.

- [ ] **Step 6: Commit the bootstrap**

~~~powershell
git add .
git commit -m "build: bootstrap Fabric 26.2 project"
~~~

Expected: first repository commit contains only build infrastructure and approved documents.

---

### Task 2: Add Metadata, Licensing, and Safe Entrypoints

**Files:**
- Create: ProjectE-Fabric-26.2/LICENSE
- Create: ProjectE-Fabric-26.2/NOTICE.md
- Create: ProjectE-Fabric-26.2/src/main/resources/fabric.mod.json
- Create: ProjectE-Fabric-26.2/src/main/java/moze_intel/projecte/ProjectE.java
- Create: ProjectE-Fabric-26.2/src/main/java/moze_intel/projecte/api/ProjectEAPI.java
- Create: ProjectE-Fabric-26.2/src/client/java/moze_intel/projecte/client/ProjectEClient.java
- Test: ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/ProjectEIdentityTest.java

**Interfaces:**
- Consumes: Fabric ModInitializer and ClientModInitializer.
- Produces: ProjectEAPI.MOD_ID:String, ProjectEAPI.id(String):Identifier, safe main/client entrypoints.

- [ ] **Step 1: Write the failing identity test**

~~~java
package moze_intel.projecte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import moze_intel.projecte.api.ProjectEAPI;
import org.junit.jupiter.api.Test;

class ProjectEIdentityTest {
    @Test
    void keepsStableProjectEIdentity() {
        assertEquals("projecte", ProjectEAPI.MOD_ID);
        assertEquals("projecte:test", ProjectEAPI.id("test").toString());
    }
}
~~~

- [ ] **Step 2: Run the test and verify the missing API failure**

Run: ./gradlew.bat test --tests moze_intel.projecte.ProjectEIdentityTest

Expected: FAIL because ProjectEAPI does not exist.

- [ ] **Step 3: Implement the identity API and entrypoints**

~~~java
package moze_intel.projecte.api;

import net.minecraft.resources.Identifier;

public final class ProjectEAPI {
    public static final String MOD_ID = "projecte";

    private ProjectEAPI() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
~~~

~~~java
package moze_intel.projecte;

import moze_intel.projecte.api.ProjectEAPI;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectE implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ProjectE for Fabric 26.2");
    }
}
~~~

~~~java
package moze_intel.projecte.client;

import moze_intel.projecte.api.ProjectEAPI;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectEClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID + "/client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing ProjectE client for Fabric 26.2");
    }
}
~~~

Create fabric.mod.json:

~~~json
{
  "schemaVersion": 1,
  "id": "projecte",
  "version": "${version}",
  "name": "ProjectE",
  "description": "A complete Fabric 26.2 port of ProjectE.",
  "authors": ["ProjectE contributors", "Fabric 26.2 port contributors"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["moze_intel.projecte.ProjectE"],
    "client": ["moze_intel.projecte.client.ProjectEClient"]
  },
  "depends": {
    "fabricloader": ">=0.19.3",
    "minecraft": "~26.2",
    "java": ">=25",
    "fabric-api": ">=0.154.2"
  }
}
~~~

Copy .upstream/projecte/LICENSE without modification. Create NOTICE.md with this exact initial content:

~~~markdown
# ProjectE Fabric 26.2 Source Notice

This port is derived from ProjectE, licensed under the MIT License.

- ProjectE repository: https://github.com/sinkillerj/ProjectE
- Frozen ProjectE commit: 15d4ce65bd06eb4222709b984255fbf5080e78bc
- Fabricated Exchange reference commit: 3a35db450988b4633587cf7287470d0ff08355d4
- FTB Equivalent Exchange research reference: c663ab17b0c1f16c53f76ba846ae04046bc3dcd2

Fabricated Exchange code or assets may be reused only under its applicable license and with attribution recorded here. FTB Equivalent Exchange code, assets, and text are behavior-research references only unless a separate compatible license or written permission is documented.
~~~

- [ ] **Step 4: Run identity tests and build**

Run:

~~~powershell
./gradlew.bat test --tests moze_intel.projecte.ProjectEIdentityTest
./gradlew.bat build
~~~

Expected: both commands PASS and produce build/libs/projecte-fabric-0.1.0-alpha.1.jar.

- [ ] **Step 5: Commit metadata and entrypoints**

~~~powershell
git add LICENSE NOTICE.md src
git commit -m "feat: add ProjectE identity and Fabric entrypoints"
~~~

Expected: commit contains no content registration and no NeoForge imports.

---

### Task 3: Implement Checked EMC Values Test-First

**Files:**
- Create: ProjectE-Fabric-26.2/src/main/java/moze_intel/projecte/emc/EmcValue.java
- Test: ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/emc/EmcValueTest.java

**Interfaces:**
- Consumes: primitive long values from the ProjectE 1.21.1 compatibility boundary.
- Produces: EmcValue.ZERO, EmcValue.of(long), add(EmcValue), subtract(EmcValue), multiply(long), compareTo(EmcValue), longValue().

- [ ] **Step 1: Write the failing tests**

~~~java
package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EmcValueTest {
    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> EmcValue.of(-1));
    }

    @Test
    void performsCheckedArithmetic() {
        assertEquals(EmcValue.of(12), EmcValue.of(5).add(EmcValue.of(7)));
        assertEquals(EmcValue.of(15), EmcValue.of(5).multiply(3));
        assertEquals(EmcValue.of(3), EmcValue.of(5).subtract(EmcValue.of(2)));
    }

    @Test
    void rejectsUnderflowAndOverflow() {
        assertThrows(ArithmeticException.class, () -> EmcValue.of(2).subtract(EmcValue.of(3)));
        assertThrows(ArithmeticException.class, () -> EmcValue.of(Long.MAX_VALUE).add(EmcValue.of(1)));
        assertThrows(ArithmeticException.class, () -> EmcValue.of(Long.MAX_VALUE).multiply(2));
    }
}
~~~

- [ ] **Step 2: Run tests and verify the missing type failure**

Run: ./gradlew.bat test --tests moze_intel.projecte.emc.EmcValueTest

Expected: FAIL because EmcValue does not exist.

- [ ] **Step 3: Implement the minimal checked value type**

~~~java
package moze_intel.projecte.emc;

public record EmcValue(long longValue) implements Comparable<EmcValue> {
    public static final EmcValue ZERO = new EmcValue(0);

    public EmcValue {
        if (longValue < 0) {
            throw new IllegalArgumentException("EMC cannot be negative: " + longValue);
        }
    }

    public static EmcValue of(long value) {
        return value == 0 ? ZERO : new EmcValue(value);
    }

    public EmcValue add(EmcValue other) {
        return of(Math.addExact(longValue, other.longValue));
    }

    public EmcValue subtract(EmcValue other) {
        long result = Math.subtractExact(longValue, other.longValue);
        if (result < 0) {
            throw new ArithmeticException("EMC underflow");
        }
        return of(result);
    }

    public EmcValue multiply(long factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("EMC factor cannot be negative: " + factor);
        }
        return of(Math.multiplyExact(longValue, factor));
    }

    @Override
    public int compareTo(EmcValue other) {
        return Long.compare(longValue, other.longValue);
    }
}
~~~

- [ ] **Step 4: Run the focused test and full test suite**

Run:

~~~powershell
./gradlew.bat test --tests moze_intel.projecte.emc.EmcValueTest
./gradlew.bat test
~~~

Expected: PASS with three EmcValue tests and the identity test.

- [ ] **Step 5: Commit the EMC primitive**

~~~powershell
git add src/main/java/moze_intel/projecte/emc src/test/java/moze_intel/projecte/emc
git commit -m "feat: add checked EMC value type"
~~~

---

### Task 4: Build a Deterministic Upstream Baseline Exporter

**Files:**
- Create: ProjectE-Fabric-26.2/src/porting/java/moze_intel/projecte/porting/BaselineExporter.java
- Test: ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/porting/BaselineExporterTest.java
- Modify: ProjectE-Fabric-26.2/build.gradle

**Interfaces:**
- Consumes: Path to a clean ProjectE upstream checkout and its Git commit.
- Produces: BaselineExporter.scan(Path):SortedMap<String,List<String>> and write(Path,Path,String):void; deterministic UTF-8 JSON ending with one newline.

- [ ] **Step 1: Write the failing fixture test**

~~~java
package moze_intel.projecte.porting;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaselineExporterTest {
    @TempDir Path temp;

    @Test
    void exportsSortedDeterministicCategories() throws Exception {
        Path upstream = temp.resolve("upstream");
        Files.createDirectories(upstream.resolve("src/datagen/generated/data/projecte/recipe"));
        Files.createDirectories(upstream.resolve("src/main/resources/assets/projecte/textures/item"));
        Files.writeString(upstream.resolve("src/datagen/generated/data/projecte/recipe/z.json"), "{}");
        Files.writeString(upstream.resolve("src/datagen/generated/data/projecte/recipe/a.json"), "{}");
        Files.writeString(upstream.resolve("src/main/resources/assets/projecte/textures/item/test.png"), "png");

        SortedMap<String, List<String>> result = BaselineExporter.scan(upstream);
        assertEquals(List.of("a", "z"), result.get("recipes"));
        assertEquals(List.of("item/test.png"), result.get("textures"));

        Path first = temp.resolve("first.json");
        Path second = temp.resolve("second.json");
        BaselineExporter.write(upstream, first, "abc123");
        BaselineExporter.write(upstream, second, "abc123");
        assertEquals(Files.readString(first), Files.readString(second));
        assertTrue(Files.readString(first).endsWith("
"));
    }
}
~~~

- [ ] **Step 2: Run the test and verify the missing exporter failure**

Run: ./gradlew.bat test --tests moze_intel.projecte.porting.BaselineExporterTest

Expected: FAIL because BaselineExporter does not exist.

- [ ] **Step 3: Implement the exporter**

Create BaselineExporter.java:

~~~java
package moze_intel.projecte.porting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class BaselineExporter {
    private static final SortedMap<String, String> CATEGORY_ROOTS;

    static {
        SortedMap<String, String> roots = new TreeMap<>();
        roots.put("advancements", "src/datagen/generated/data/projecte/advancement");
        roots.put("blockstates", "src/datagen/generated/assets/projecte/blockstates");
        roots.put("custom_conversions", "src/datagen/generated/data/projecte/pe_custom_conversions");
        roots.put("item_models", "src/datagen/generated/assets/projecte/models/item");
        roots.put("loot_tables", "src/datagen/generated/data/projecte/loot_table");
        roots.put("recipes", "src/datagen/generated/data/projecte/recipe");
        roots.put("sounds", "src/main/resources/assets/projecte/sounds");
        roots.put("tags", "src/datagen/generated/data/projecte/tags");
        roots.put("textures", "src/main/resources/assets/projecte/textures");
        roots.put("world_transmutations", "src/datagen/generated/data/projecte/pe_world_transmutations");
        CATEGORY_ROOTS = Collections.unmodifiableSortedMap(roots);
    }

    private BaselineExporter() {
    }

    public static SortedMap<String, List<String>> scan(Path upstream) throws IOException {
        SortedMap<String, List<String>> categories = new TreeMap<>();
        for (Map.Entry<String, String> entry : CATEGORY_ROOTS.entrySet()) {
            Path categoryRoot = upstream.resolve(entry.getValue());
            List<String> ids = new ArrayList<>();
            if (Files.isDirectory(categoryRoot)) {
                try (var paths = Files.walk(categoryRoot)) {
                    paths.filter(Files::isRegularFile)
                        .map(categoryRoot::relativize)
                        .map(Path::toString)
                        .map(value -> value.replace('\\', '/'))
                        .map(BaselineExporter::removeJsonExtension)
                        .sorted()
                        .forEach(ids::add);
                }
            }
            categories.put(entry.getKey(), List.copyOf(ids));
        }
        return Collections.unmodifiableSortedMap(categories);
    }

    public static void write(Path upstream, Path output, String commit) throws IOException {
        if (commit.isBlank()) {
            throw new IllegalArgumentException("Source commit must not be blank");
        }
        Path absoluteOutput = output.toAbsolutePath();
        Path parent = absoluteOutput.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absoluteOutput.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, toJson(scan(upstream), commit), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absoluteOutput, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absoluteOutput, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String removeJsonExtension(String value) {
        return value.endsWith(".json") ? value.substring(0, value.length() - 5) : value;
    }

    private static String toJson(SortedMap<String, List<String>> categories, String commit) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"schema\": 1,\n  \"source_commit\": \"")
            .append(escape(commit))
            .append("\",\n  \"categories\": {\n");
        int categoryIndex = 0;
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            if (categoryIndex++ > 0) {
                json.append(",\n");
            }
            json.append("    \"").append(escape(entry.getKey())).append("\": [");
            for (int itemIndex = 0; itemIndex < entry.getValue().size(); itemIndex++) {
                if (itemIndex > 0) {
                    json.append(", ");
                }
                json.append("\"").append(escape(entry.getValue().get(itemIndex))).append("\"");
            }
            json.append(']');
        }
        return json.append("\n  }\n}\n").toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: BaselineExporter <upstream> <output> <commit>");
        }
        write(Path.of(args[0]), Path.of(args[1]), args[2]);
    }
}
~~~

Add this lazy Gradle task so normal builds do not require upstream path properties:

~~~groovy
tasks.register('exportProjectEBaseline', JavaExec) {
    dependsOn portingClasses
    classpath = sourceSets.porting.runtimeClasspath
    mainClass = 'moze_intel.projecte.porting.BaselineExporter'
    doFirst {
        setArgs([
            providers.gradleProperty('projecte_upstream').get(),
            layout.projectDirectory.file('docs/porting/baseline/projecte-1.21.1.json').asFile.absolutePath,
            providers.gradleProperty('projecte_upstream_commit').get()
        ])
    }
}
~~~

- [ ] **Step 4: Run exporter tests**

Run: ./gradlew.bat test --tests moze_intel.projecte.porting.BaselineExporterTest

Expected: PASS; repeated outputs are byte-identical.

- [ ] **Step 5: Commit the porting tool**

~~~powershell
git add build.gradle src/porting src/test/java/moze_intel/projecte/porting
git commit -m "build: add deterministic ProjectE baseline exporter"
~~~

---

### Task 5: Freeze the ProjectE Baseline and Create the Feature Matrix

**Files:**
- Create: ProjectE-Fabric-26.2/docs/porting/baseline/projecte-1.21.1.json
- Create: ProjectE-Fabric-26.2/docs/porting/feature-matrix.md
- Create: ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/porting/FrozenBaselineTest.java
- Modify: ProjectE-Fabric-26.2/build.gradle

**Interfaces:**
- Consumes: BaselineExporter.write and frozen upstream commit.
- Produces: verifyBaseline Gradle task and committed parity inventory.

- [ ] **Step 1: Generate the frozen baseline**

Run from the target repository:

~~~powershell
./gradlew.bat exportProjectEBaseline "-Pprojecte_upstream=../.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
~~~

Expected: docs/porting/baseline/projecte-1.21.1.json is created and includes the frozen commit.

- [ ] **Step 2: Write the failing baseline assertions**

~~~java
package moze_intel.projecte.porting;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class FrozenBaselineTest {
    private static final Path BASELINE = Path.of("docs/porting/baseline/projecte-1.21.1.json");

    @Test
    void baselineMatchesFrozenProjectECommitAndMinimumCounts() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(BASELINE)).getAsJsonObject();
        assertEquals("15d4ce65bd06eb4222709b984255fbf5080e78bc", root.get("source_commit").getAsString());
        JsonObject categories = root.getAsJsonObject("categories");
        assertEquals(156, categories.getAsJsonArray("recipes").size());
        assertEquals(173, categories.getAsJsonArray("advancements").size());
        assertEquals(21, categories.getAsJsonArray("loot_tables").size());
        assertEquals(4, categories.getAsJsonArray("world_transmutations").size());
        assertEquals(2, categories.getAsJsonArray("custom_conversions").size());
        assertEquals(170, categories.getAsJsonArray("textures").size());
        assertEquals(15, categories.getAsJsonArray("sounds").size());
    }
}
~~~

- [ ] **Step 3: Run the focused test**

Run: ./gradlew.bat test --tests moze_intel.projecte.porting.FrozenBaselineTest

Expected: PASS. If a count differs, inspect exporter path semantics and the frozen upstream checkout; do not weaken assertions without documenting the verified reason in the design spec.

- [ ] **Step 4: Create the initial feature matrix**

Create docs/porting/feature-matrix.md with these columns:

~~~markdown
# ProjectE Fabric 26.2 Feature Matrix

Baseline: ProjectE 1.21.1 commit 15d4ce65bd06eb4222709b984255fbf5080e78bc

| Category | Upstream evidence | Fabric implementation | Status | Verification | Difference |
| --- | --- | --- | --- | --- | --- |
| Build and metadata | fabric.mod.json, Gradle build | Foundation tasks 1-2 | Verified | gradlew build; PackagedJarTest | None |
| EMC numeric bounds | ProjectE long EMC semantics | EmcValue | Verified | EmcValueTest | Checked overflow is stricter |
| Registration IDs | PEItems, PEBlocks, registries | Not started | Not started | Frozen baseline | None recorded |
| EMC mapping | emc package and data files | Not started | Not started | EMC core plan | None recorded |
| Player EMC and knowledge | components and network packages | Not started | Not started | Player data plan | None recorded |
| Transmutation | table, tablet, stone and world data | Not started | Not started | Transmutation plan | None recorded |
| Machines and storage | block entities and containers | Not started | Not started | Machines plan | None recorded |
| Tools and equipment | item classes, armor and pedestal | Not started | Not started | Equipment plan | None recorded |
| Assets and datagen | frozen baseline categories | Not started | Not started | Content plan | None recorded |
| Optional integrations | integration package | Not started | Not started | Compatibility plan | None recorded |
~~~

- [ ] **Step 5: Add deterministic verification**

Add these tasks to build.gradle:

~~~groovy
def verificationBaseline = layout.buildDirectory.file('porting/projecte-1.21.1.json')

tasks.register('generateVerificationBaseline', JavaExec) {
    dependsOn portingClasses
    classpath = sourceSets.porting.runtimeClasspath
    mainClass = 'moze_intel.projecte.porting.BaselineExporter'
    outputs.file verificationBaseline
    doFirst {
        setArgs([
            providers.gradleProperty('projecte_upstream').get(),
            verificationBaseline.get().asFile.absolutePath,
            providers.gradleProperty('projecte_upstream_commit').get()
        ])
    }
}

tasks.register('verifyBaseline') {
    dependsOn generateVerificationBaseline
    inputs.file layout.projectDirectory.file('docs/porting/baseline/projecte-1.21.1.json')
    inputs.file verificationBaseline
    doLast {
        def expected = layout.projectDirectory.file('docs/porting/baseline/projecte-1.21.1.json').asFile.toPath()
        def actual = verificationBaseline.get().asFile.toPath()
        long mismatch = java.nio.file.Files.mismatch(expected, actual)
        if (mismatch != -1) {
            throw new GradleException("ProjectE baseline differs at byte ${mismatch}: expected ${expected}, actual ${actual}")
        }
    }
}
~~~

- [ ] **Step 6: Verify and commit**

Run:

~~~powershell
./gradlew.bat test --tests moze_intel.projecte.porting.FrozenBaselineTest
./gradlew.bat verifyBaseline "-Pprojecte_upstream=../.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
~~~

Expected: PASS and no change to the committed baseline.

Commit:

~~~powershell
git add docs/porting build.gradle src/test/java/moze_intel/projecte/porting/FrozenBaselineTest.java
git commit -m "docs: freeze ProjectE 1.21.1 parity baseline"
~~~

---

### Task 6: Audit the Packaged Runtime Jar

**Files:**
- Create: ProjectE-Fabric-26.2/src/test/java/moze_intel/projecte/packaging/PackagedJarTest.java
- Modify: ProjectE-Fabric-26.2/build.gradle

**Interfaces:**
- Consumes: remapJar output.
- Produces: packageAudit test task proving metadata, license and source-set isolation.

- [ ] **Step 1: Write the jar audit test**

~~~java
package moze_intel.projecte.packaging;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "projecte.runtimeJar", matches = ".+")
class PackagedJarTest {
    @Test
    void runtimeJarContainsRequiredFilesAndNoPortingTools() throws Exception {
        Path jar = Path.of(System.getProperty("projecte.runtimeJar"));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            assertNotNull(zip.getEntry("fabric.mod.json"));
            assertNotNull(zip.getEntry("moze_intel/projecte/ProjectE.class"));
            assertNotNull(zip.getEntry("moze_intel/projecte/client/ProjectEClient.class"));
            assertTrue(zip.stream().anyMatch(e -> e.getName().startsWith("LICENSE_")));
            assertNull(zip.getEntry("moze_intel/projecte/porting/BaselineExporter.class"));
        }
    }
}
~~~

- [ ] **Step 2: Add the packageAudit task**

~~~groovy
tasks.register('packageAudit', Test) {
    dependsOn remapJar
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform()
    systemProperty 'projecte.runtimeJar', remapJar.archiveFile.get().asFile.absolutePath
    filter { includeTestsMatching 'moze_intel.projecte.packaging.PackagedJarTest' }
}
~~~

- [ ] **Step 3: Run the audit and correct packaging only if it fails**

Run: ./gradlew.bat packageAudit

Expected: PASS. The porting exporter is absent while metadata, both entrypoint classes and renamed MIT license are present.

- [ ] **Step 4: Commit the packaging gate**

~~~powershell
git add build.gradle src/test/java/moze_intel/projecte/packaging
git commit -m "test: audit packaged ProjectE runtime jar"
~~~

---

### Task 7: Add Continuous Integration

**Files:**
- Create: ProjectE-Fabric-26.2/.github/workflows/build.yml

**Interfaces:**
- Consumes: build, test, packageAudit and verifyBaseline Gradle tasks.
- Produces: reproducible Windows and Linux validation plus uploaded remapped jar.

- [ ] **Step 1: Add the workflow**

~~~yaml
name: build
on:
  push:
  pull_request:
permissions:
  contents: read
jobs:
  test:
    strategy:
      matrix:
        os: [windows-latest, ubuntu-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
      - uses: gradle/actions/setup-gradle@v4
      - name: Checkout frozen ProjectE baseline
        uses: actions/checkout@v4
        with:
          repository: sinkillerj/ProjectE
          ref: 15d4ce65bd06eb4222709b984255fbf5080e78bc
          path: .upstream/projecte
      - name: Build and test on Windows
        if: runner.os == 'Windows'
        run: .\gradlew.bat build packageAudit verifyBaseline "-Pprojecte_upstream=.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
      - name: Build and test on Linux
        if: runner.os == 'Linux'
        run: ./gradlew build packageAudit verifyBaseline "-Pprojecte_upstream=.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
      - name: Upload remapped jar
        if: matrix.os == 'ubuntu-latest'
        uses: actions/upload-artifact@v4
        with:
          name: projecte-fabric-26.2
          path: build/libs/*-0.1.0-alpha.1.jar
~~~

- [ ] **Step 2: Reproduce the CI command locally**

Run:

~~~powershell
./gradlew.bat clean build packageAudit verifyBaseline "-Pprojecte_upstream=../.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
~~~

Expected: BUILD SUCCESSFUL with unit tests, jar audit and baseline verification passing.

- [ ] **Step 3: Commit CI**

~~~powershell
git add .github/workflows/build.yml
git commit -m "ci: verify Fabric build and parity baseline"
~~~

---

### Task 8: Foundation Completion Audit

**Files:**
- Modify: ProjectE-Fabric-26.2/docs/porting/feature-matrix.md
- Modify: ProjectE-Fabric-26.2/docs/superpowers/specs/2026-07-12-projecte-fabric-26.2-design.md
- Create: ProjectE-Fabric-26.2/docs/porting/evidence/foundation.md

**Interfaces:**
- Consumes: all foundation commits and test outputs.
- Produces: reviewable evidence and a clean handoff to the EMC core plan.

- [ ] **Step 1: Run the complete foundation gate**

Run:

~~~powershell
./gradlew.bat clean build packageAudit verifyBaseline "-Pprojecte_upstream=../.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
git status --short
git log --oneline --decorate -8
~~~

Expected: BUILD SUCCESSFUL; status is clean before evidence is added; log contains the seven focused commits from this plan.

- [ ] **Step 2: Record exact evidence**

Create docs/porting/evidence/foundation.md containing:

~~~markdown
# Foundation Verification

- Minecraft: 26.2
- Java: 25
- Loom: 1.17-SNAPSHOT
- Gradle: 9.5.1
- Loader: 0.19.3
- Fabric API: 0.154.2+26.2
- ProjectE baseline: 15d4ce65bd06eb4222709b984255fbf5080e78bc
- Commands: gradlew clean build packageAudit verifyBaseline
- Result: PASS
- Runtime jar audit: PASS
- Deterministic baseline: PASS
- Remaining feature-matrix categories: EMC mapping, player data, transmutation, machines, tools/equipment, content/datagen, compatibility, final runtime verification
~~~

Replace PASS only with actual observed results. If any command fails, do not create successful evidence; fix the owning task and rerun the complete gate.

- [ ] **Step 3: Update document status**

Change the copied design status to “规格已批准；基础阶段已验证” and mark only Build and metadata plus EMC numeric bounds as Verified in the feature matrix. All later categories remain Not started.

- [ ] **Step 4: Commit the audit**

~~~powershell
git add docs
git commit -m "docs: record foundation verification"
git status --short
~~~

Expected: final status is clean.

- [ ] **Step 5: Review gate before EMC implementation**

Confirm that the next plan, 2026-07-12-projecte-fabric-26.2-emc-core.md, will consume EmcValue and the committed baseline without changing their public contracts. Do not begin EMC recipe inference until this foundation review is accepted.
