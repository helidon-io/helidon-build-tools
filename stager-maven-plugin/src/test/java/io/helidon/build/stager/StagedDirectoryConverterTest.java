package io.helidon.build.stager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link StagedDirectoryConverter}.
 */
class StagedDirectoryConverterTest {

    @Test
    public void testConverter() throws IOException, XmlPullParserException {
        Reader reader = new InputStreamReader(StagedDirectoryConverterTest.class.getResourceAsStream("/testconfig.xml"));
        PlexusConfiguration plexusConfiguration = new XmlPlexusConfiguration(Xpp3DomBuilder.build(reader));
        StagedDirectory stagedDirectory = new StagedDirectoryConverter().fromConfiguration(plexusConfiguration);
        assertThat(stagedDirectory, is(not(nullValue())));
    }
}