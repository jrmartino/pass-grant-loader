package org.dataconservancy.pass.grant.data;

import java.util.Collection;
import java.util.Map;

public interface PassUpdater {
    void updatePass(Collection<Map<String, String>> results, String mode);
}
