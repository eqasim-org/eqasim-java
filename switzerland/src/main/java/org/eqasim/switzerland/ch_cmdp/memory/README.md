# Scenario cleanup and final attribute restoration

`RunSimulation` enables cleanup by default (`--lighten-scenario true`). It now
archives the removed attributes of persons, links and households and restores
them immediately before MATSim's final scenario writer runs. No new argument is
required. `--lighten-scenario false` disables both cleanup and archiving.

During cleanup, each removal is added to a compact binary buffer. It groups
attributes by owner type and stable object ID, uses shared dictionaries for
attribute names and converted type names, writes numbers in their native binary
types and compresses 1 MiB blocks with LZ4. The compressed buffer uses segmented
arrays instead of one large growing array or maps of all removed values. After
cleanup it is saved in one sequential write and the buffers are released before
the controller starts. Restoration reads the file sequentially and attaches the
values to the existing scenario objects.

The archive is created under Java's temporary directory in a unique
`eqasim-scenario-attributes-*` folder; its path and size are logged. Use
`-Djava.io.tmpdir=<local directory>` if a different temporary disk is desired.
Normal completion removes the temporary file and its empty directory after final
writing. An unexpected shutdown retains the archive and logs its path.

Only original person, link and household IDs are matched. Missing/deleted objects
are skipped; newly cloned persons have no donor mapping. Attributes already
present at restoration keep their current values, including calibration results.
Consolidated `home_coordinate_x/y` aliases are archived too, while normal
`home_x/y` remain available during simulation. Other cleaned object types
(activities, legs, plans, nodes, facilities, transit and vehicles) are not restored.

Common scalar types, primitive numeric arrays, byte arrays and coordinates use
binary encoding. Additional supported values use MATSim's default attribute
converters, including enums and supported collections/maps. Values without a
converter remain in memory instead of being removed, so they cannot be lost.
The archive includes format/version markers, checksummed compression blocks and
an entry-count footer. An unreadable archive stops final scenario export and is
retained instead of silently producing incomplete metadata.

`AttributeRestoringDumpDataAtEnd` lives in `ch_cmdp.memory`. It resolves the
package-private `DumpDataAtEndImpl` type once during setup and lets Guice construct
and inject the original writer. It uses public listener interfaces to delegate;
no upstream class is replaced and no reflective member-access override is needed.
Restoration occurs before `output_plans`, `output_network` and
`output_households` are written, including normal early termination. If final
scenario dumping is disabled, restoration still occurs on normal shutdown.

This restores the final scenario objects. Earlier iteration files and previously
generated experienced-plan snapshots are not enriched retrospectively.

Tests: `TestScenarioAttributeCleaner`, `TestScenarioAttributeArchive` and
`TestScenarioAttributeArchiveOutput`. The last test runs the real MATSim output
writer and reloads all three final XML files, including an early-termination run.
