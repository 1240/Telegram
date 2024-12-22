package org.telegram.ui.Components.Paint;

import org.telegram.messenger.AndroidUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UndoStore {

    public interface UndoStoreDelegate {
        void historyChanged();
    }

    private UndoStoreDelegate delegate;
    private Map<UUID, Runnable> uuidToOperationMap = new HashMap<>();
    private List<UUID> operations = new ArrayList<>();

    public int localUndo = 0;

    public boolean canUndo() {
        return !operations.isEmpty();
    }

    public UndoStoreDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(UndoStoreDelegate undoStoreDelegate) {
        delegate = undoStoreDelegate;
    }

    public void registerUndo(UUID uuid, Runnable undoRunnable) {
        uuidToOperationMap.put(uuid, undoRunnable);
        operations.add(uuid);
        localUndo +=1;

        notifyOfHistoryChanges();
    }

    public void unregisterUndo(UUID uuid) {
        uuidToOperationMap.remove(uuid);
        operations.remove(uuid);
        localUndo -=1;

        notifyOfHistoryChanges();
    }

    public void undo() {
        if (operations.size() == 0) {
            return;
        }

        int lastIndex = operations.size() - 1;
        UUID uuid = operations.get(lastIndex);
        Runnable undoRunnable = uuidToOperationMap.get(uuid);
        uuidToOperationMap.remove(uuid);
        operations.remove(lastIndex);
        localUndo -=1;

        undoRunnable.run();
        notifyOfHistoryChanges();
    }

    public void clear() {
        while (!operations.isEmpty()) {
            undo();
        }
    }

    public void clearLocal() {
        final int c = localUndo;
        for (int i = 0; i < c; i++) {
            undo();
        }
    }

    public void reset() {
        operations.clear();
        uuidToOperationMap.clear();
        localUndo = 0;

        notifyOfHistoryChanges();
    }

    public void resetLocal() {
        localUndo = 0;
    }

    private void notifyOfHistoryChanges() {
        AndroidUtilities.runOnUIThread(() -> {
            if (delegate != null) {
                delegate.historyChanged();
            }
        });
    }
}
