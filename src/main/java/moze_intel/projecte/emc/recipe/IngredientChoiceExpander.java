package moze_intel.projecte.emc.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class IngredientChoiceExpander {
    public <K extends Comparable<? super K>> List<List<K>> expand(List<? extends List<K>> slots, int maximum, String description) {
        if (maximum <= 0) {
            throw new IllegalArgumentException("maximum must be positive");
        }
        Map<List<K>, Integer> groupedSlots = new LinkedHashMap<>();
        for (List<K> slot : slots) {
            if (slot.isEmpty()) {
                return List.of();
            }
            List<K> choices = List.copyOf(new TreeSet<>(slot));
            groupedSlots.merge(choices, 1, Math::addExact);
        }
        List<List<K>> result = new ArrayList<>();
        expandGroups(new ArrayList<>(groupedSlots.entrySet()), 0, new ArrayList<>(),
              result, maximum, description);
        return List.copyOf(result);
    }

    private <K> void expandGroups(
          List<Map.Entry<List<K>, Integer>> groups,
          int groupIndex,
          List<K> current,
          List<List<K>> result,
          int maximum,
          String description
    ) {
        if (groupIndex == groups.size()) {
            if (result.size() >= maximum) {
                throw new IllegalArgumentException(
                      description + " expands to more than " + maximum + " ingredient combinations");
            }
            result.add(List.copyOf(current));
            return;
        }
        Map.Entry<List<K>, Integer> group = groups.get(groupIndex);
        expandGroup(groups, groupIndex, group.getKey(), group.getValue(), 0,
              current, result, maximum, description);
    }

    private <K> void expandGroup(
          List<Map.Entry<List<K>, Integer>> groups,
          int groupIndex,
          List<K> choices,
          int remaining,
          int firstChoice,
          List<K> current,
          List<List<K>> result,
          int maximum,
          String description
    ) {
        if (remaining == 0) {
            expandGroups(groups, groupIndex + 1, current, result, maximum, description);
            return;
        }
        for (int choiceIndex = firstChoice; choiceIndex < choices.size(); choiceIndex++) {
            current.add(choices.get(choiceIndex));
            expandGroup(groups, groupIndex, choices, remaining - 1, choiceIndex,
                  current, result, maximum, description);
            current.removeLast();
        }
    }
}
