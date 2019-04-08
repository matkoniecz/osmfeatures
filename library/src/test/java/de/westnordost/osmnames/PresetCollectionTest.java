package de.westnordost.osmnames;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static de.westnordost.osmnames.GeometryType.*;
import static de.westnordost.osmnames.MapEntry.*;
import static org.junit.Assert.*;

public class PresetCollectionTest
{
	@Test public void presets_not_found_produces_runtime_exception()
	{
		try
		{
			new PresetCollection(new NamesDictionary.FileAccessAdapter()
			{
				@Override public boolean exists(String name) { return false; }
				@Override public InputStream open(String name) throws IOException { throw new FileNotFoundException(); }
			});
			fail();
		} catch (RuntimeException ignored) { }
	}

	@Test public void load_presets_only()
	{
		PresetCollection presets = create("one_preset_full.json", null);

		assertEquals(1, presets.getAll().size());
		Preset preset = presets.get("some/id");
		assertEquals("some/id", preset.id);
		assertEquals(mapOf(tag("a","b"),tag("c","d")), preset.tags);
		assertEquals(listOf(POINT, VERTEX, LINE, AREA, RELATION), preset.geometry);

		assertEquals(listOf("DE", "GB"), preset.countryCodes);
		assertEquals("foo", preset.name);
		assertTrue(preset.suggestion);
		assertEquals(listOf("1","2"), preset.terms);
		assertEquals(0.5f, preset.matchScore);
		assertFalse(preset.searchable);
		assertEquals(mapOf(tag("e","f")), preset.addTags);
	}

	@Test public void load_presets_only_defaults()
	{
		PresetCollection presets = create("one_preset_min.json", null);

		assertEquals(1, presets.getAll().size());
		Preset preset = presets.get("some/id");

		assertEquals("some/id", preset.id);
		assertEquals(mapOf(tag("a","b"),tag("c","d")), preset.tags);
		assertEquals(listOf(POINT), preset.geometry);

		assertTrue(preset.countryCodes.isEmpty());
		assertNull(preset.name);
		assertFalse(preset.suggestion);
		assertTrue(preset.terms.isEmpty());
		assertEquals(1.0f, preset.matchScore);
		assertTrue(preset.searchable);
		assertTrue(preset.addTags.isEmpty());
	}

	@Test public void load_presets_no_wildcards()
	{
		PresetCollection presets = create("one_preset_wildcard.json", null);
		assertTrue(presets.getAll().isEmpty());
	}

	@Test public void load_presets_and_localization()
	{
		PresetCollection presets = create("one_preset_min.json", "localizations.json");

		assertEquals(1, presets.getAll().size());
		Preset preset = presets.get("some/id");

		assertEquals("some/id", preset.id);
		assertEquals(mapOf(tag("a","b"),tag("c","d")), preset.tags);
		assertEquals(listOf(POINT), preset.geometry);
		assertEquals("bar", preset.name);
		assertEquals(listOf("a", "b"), preset.terms);
	}

	@Test public void load_presets_and_localization_defaults()
	{
		PresetCollection presets = create("one_preset_min.json", "localizations_min.json");

		assertEquals(1, presets.getAll().size());
		Preset preset = presets.get("some/id");

		assertEquals("some/id", preset.id);
		assertEquals(mapOf(tag("a","b"),tag("c","d")), preset.tags);
		assertEquals(listOf(POINT), preset.geometry);
		assertEquals("bar", preset.name);
		assertTrue(preset.terms.isEmpty());
	}

	@Test public void load_presets_and_two_localizations()
	{
		PresetCollection presets = new PresetCollection(new NamesDictionary.FileAccessAdapter()
		{
			@Override public boolean exists(String name)
			{
				return Arrays.asList("presets.json", "en.json", "de.json").contains(name);
			}

			@Override public InputStream open(String name) throws IOException
			{
				if (name.equals("presets.json")) return getStream("some_presets_min.json");
				if (name.equals("en.json")) return getStream("localizations_en.json");
				if (name.equals("de.json")) return getStream("localizations_de.json");
				throw new IOException("File not found");
			}
		});

		// this also tests if the fallback from de-DE to de works if de-DE.json does not exist
		assertEquals(1, presets.getAll(Locale.ENGLISH).size());
		assertEquals(2, presets.getAll(Locale.GERMANY).size());
		assertEquals("Bäckerei", presets.get("some/id", Locale.GERMANY).name);
		assertEquals("Gullideckel", presets.get("another/id", Locale.GERMANY).name);
		assertEquals("Bakery", presets.get("some/id", Locale.ENGLISH).name);
	}

	@Test public void load_presets_and_merge_localizations()
	{
		PresetCollection presets = new PresetCollection(new NamesDictionary.FileAccessAdapter()
		{
			@Override public boolean exists(String name)
			{
				return Arrays.asList("presets.json", "de-AT.json", "de.json").contains(name);
			}

			@Override public InputStream open(String name) throws IOException
			{
				if (name.equals("presets.json")) return getStream("some_presets_min.json");
				if (name.equals("de-AT.json")) return getStream("localizations_de-AT.json");
				if (name.equals("de.json")) return getStream("localizations_de.json");
				throw new IOException("File not found");
			}
		});

		// this also tests if the fallback from de-DE to de works if de-DE.json does not exist
		Locale AUSTRIA = new Locale("de", "AT");
		assertEquals(2, presets.getAll(Locale.GERMAN).size());
		assertEquals(3, presets.getAll(new Locale("de", "AT")).size());
		assertEquals("Bäckerei", presets.get("some/id", Locale.GERMAN).name);
		assertEquals("Gullideckel", presets.get("another/id", Locale.GERMANY).name);
		assertEquals("Backhusl", presets.get("some/id", AUSTRIA).name);
		assertEquals("Gullideckel", presets.get("another/id", AUSTRIA).name);
		assertEquals("Brückle", presets.get("yet/another/id", AUSTRIA).name);
	}

	private PresetCollection create(String presetFile, String localizationsFile)
	{
		return new PresetCollection(new NamesDictionary.FileAccessAdapter()
		{
			@Override public boolean exists(String name)
			{
				return name.equals("presets.json") || localizationsFile != null;
			}
			@Override public InputStream open(String name) throws IOException
			{
				if(name.equals("presets.json")) return getStream(presetFile);
				if(localizationsFile != null) return getStream(localizationsFile);
				throw new FileNotFoundException();
			}
		});
	}

	@SafeVarargs private static <T> List<T> listOf(T... items) { return Arrays.asList(items); }

	private InputStream getStream(String file)
	{
		return getClass().getClassLoader().getResourceAsStream(file);
	}
}
