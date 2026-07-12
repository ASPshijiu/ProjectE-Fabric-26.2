package moze_intel.projecte.emc.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IngredientChoiceExpander {
    public <K extends Comparable<? super K>> List<List<K>> expand(List<? extends List<K>> slots, int maximum, String description) {
        if (maximum <= 0) {
            throw new IllegalArgumentException("maximum must be positive");
        }
        long combinations = 1;
        List<List<K>> normalized = new ArrayList<>();
        for (List<K> slot : slots) {
            if (slot.isEmpty()) {
                return List.of();
            }
            List<K> choices = new ArrayList<>(slot);
            Collections.sort(choices);
            normalized.add(List.copyOf(choices));
            combinations = Math.multiplyExact(combinations, choices.size());
            if (combinations > maximum) {
                throw new IllegalArgumentException(description + " expands to more than " + maximum + " ingredient combinations");
            }
        }
        List<List<K>> result = new ArrayList<>((int) combinations);
        expandAt(normalized, 0, new ArrayList<>(), result);
        return List.copyOf(result);
    }

    private <K> void expandAt(List<List<K>> slots, int index, List<K> current, List<List<K>> result) {
        if (index == slots.size()) {
            result.add(List.copyOf(current));
            return;
        }
        for (K choice : slots.get(index)) {
            current.add(choice);
            expandAt(slots, index + 1, current, result);
            current.removeLast();
        }
    }
}
