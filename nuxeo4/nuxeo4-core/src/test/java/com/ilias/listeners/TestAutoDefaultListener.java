package com.ilias.listeners;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import jakarta.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy({"com.ilias.nuxeo4-core"})
public class TestAutoDefaultListener {

    protected final List<String> events = Arrays.asList("emptyDocumentModelCreated", "aboutToCreate", "documentCreated", "beforeDocumentModification", "documentModified", "aboutToRemove", "documentRemoved", "aboutToCopy", "documentCreatedByCopy", "aboutToMove", "documentMoved", "documentLocked", "documentUnlocked", "aboutToRemoveVersion", "versionRemoved", "aboutToRemoveVersion", "aboutToRemoveVersion", "versionremoved");

    @Inject
    protected EventService s;

    @Test
    public void listenerRegistration() {
        EventListenerDescriptor listener = s.getEventListener("autodefaultlistener");
        assertNotNull(listener);
        assertTrue(events.stream().allMatch(listener::acceptEvent));
    }
}
