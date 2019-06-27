package org.dataconservancy.pass.grant.data;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.Grant;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

public interface PassUpdater {
    void updatePass(Collection<Map<String, String>> results, String mode);

    String getLatestUpdate();

    String getReport();

    PassUpdateStatistics getStatistics();

    Map<URI, Grant> getGrantUriMap();

    PassClient getPassClient();
}
