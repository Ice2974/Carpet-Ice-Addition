package com.ice2974.carpeticeaddition.rules;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class RecipeConflictOrchestratorTest {
    @Test void mixedRulesLockUnlockAndStayLockedInSameReloadPass() {
        var a = new Rule("craft_a");
        var b = new Rule("craft_b");
        var c = new Rule("stone_a");
        var d = new Rule("stone_b");
        a.conflict = true;
        c.conflict = true;
        var adapter = new ServerAdapter() {
            public boolean isServerThread() { return true; }
            public Collection<String> selectedPackIds() { return List.of(); }
            public CompletableFuture<Void> reloadResources(Collection<String> ids) { throw new AssertionError("unexpected reload"); }
        };
        Object server = new Object();
        var orchestrator = new RecipePackOrchestrator(List.of(a.pack(), b.pack(), c.pack(), d.pack()), adapter);
        orchestrator.onServerLoadedWorlds(server, null);
        assertTrue(a.lock.isLocked()); assertFalse(b.lock.isLocked());
        assertTrue(c.lock.isLocked()); assertFalse(d.lock.isLocked());
        a.conflict = false;
        b.conflict = true;
        d.conflict = true;
        orchestrator.onReloadStart(server, null);
        orchestrator.onReloadEnd(server, true);
        assertFalse(a.lock.isLocked()); assertTrue(a.field);
        assertTrue(b.lock.isLocked()); assertFalse(b.field);
        assertTrue(c.lock.isLocked()); assertFalse(c.field);
        assertTrue(d.lock.isLocked()); assertFalse(d.field);
        a.conflict = true; b.conflict = false; c.conflict = false; d.conflict = false;
        orchestrator.onReloadStart(server, null);
        orchestrator.onReloadEnd(server, true);
        assertTrue(a.lock.isLocked()); assertFalse(b.lock.isLocked());
        assertFalse(c.lock.isLocked()); assertFalse(d.lock.isLocked());
        orchestrator.onServerClosed(server);
        assertFalse(a.lock.isLocked());
    }
    private static final class Rule {
        final String name;
        final RecipeConflictLock lock = new RecipeConflictLock();
        boolean conflict;
        boolean field = true;
        Rule(String name) { this.name = name; }
        RecipePackCoordinator.ManagedPack pack() {
            return new RecipePackCoordinator.ManagedPack(Identifier.tryParse("test:" + name), name, "test.message",
                    lock::isLocked, () -> false,
                    s -> lock.apply(conflict ? RecipeConflictLock.Detection.CONFLICT : RecipeConflictLock.Detection.NO_CONFLICT,
                            () -> field, v -> field = v),
                    s -> {}, s -> {}, (s, p) -> {}, lock::reset);
        }
    }
}
