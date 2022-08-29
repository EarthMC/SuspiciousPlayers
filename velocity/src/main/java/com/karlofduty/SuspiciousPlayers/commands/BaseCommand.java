package com.karlofduty.SuspiciousPlayers.commands;

import java.util.Collection;
import java.util.List;

public class BaseCommand {
    public List<String> filterByStart(Collection<String> strings, String startingWith) {
        return strings.stream().filter(s -> s.regionMatches(true, 0, startingWith, 0, startingWith.length())).toList();
    }
}
