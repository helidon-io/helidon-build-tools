package io.helidon.build.common;


import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link Patterns}.
 */
class PatternsTest {
   @Test
   void groupNamesSingleGroup() {
      Set<String> groupNames = Patterns.groupNames("(?<group1>\\d+)");
      assertThat(groupNames, is(Set.of("group1")));
   }

   @Test
   void groupNamesMultipleGroups() {
      Set<String> groupNames = Patterns.groupNames("(?<group1>\\d+)(?<group2>\\w+)");
      assertThat(groupNames, is(Set.of("group1", "group2")));
   }

   @Test
   void groupNamesNoGroups() {
      Set<String> groupNames = Patterns.groupNames("\\d+\\w+");
      assertThat(groupNames, is(Set.of()));
   }

   @Test
   void groupNamesInvalidGroup() {
      Set<String> groupNames = Patterns.groupNames("(?<group!>\\d+)");
      assertThat(groupNames, is(Set.of("group!")));
   }

   @Test
   void groupNamesNestedGroups() {
      Set<String> groupNames = Patterns.groupNames("(?<outer>(?<inner>\\d+))");
      assertThat(groupNames, is(Set.of("outer", "inner")));
   }
}